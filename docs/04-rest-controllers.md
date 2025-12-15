# REST Controllers and Exception Handling

This document covers building RESTful APIs with Spring Boot, including controller implementation, HTTP semantics, global exception handling, and testing strategies.

## Table of Contents
1. [What is a REST Controller?](#what-is-a-rest-controller)
2. [HTTP Method Annotations](#http-method-annotations)
3. [Request Handling](#request-handling)
4. [Response Handling](#response-handling)
5. [HTTP Status Codes](#http-status-codes)
6. [Global Exception Handling](#global-exception-handling)
7. [Controller Testing](#controller-testing)
8. [Best Practices](#best-practices)
9. [Key Learning Points](#key-learning-points)

---

## What is a REST Controller?

A REST controller handles HTTP requests and returns data (usually JSON) instead of views.

### @RestController vs @Controller

```java
// @Controller - Returns view names (HTML templates)
@Controller
public class WebController {
    @GetMapping("/home")
    public String home() {
        return "home";  // Returns view name "home.html"
    }
}

// @RestController - Returns data directly (JSON)
@RestController
public class ApiController {
    @GetMapping("/api/data")
    public User getData() {
        return new User("john");  // Serialized to JSON automatically
    }
}
```

**@RestController** = `@Controller` + `@ResponseBody`

### Basic Controller Structure

```java
@RestController
@RequestMapping("/api/users")  // Base path for all endpoints
public class AppUserController {

    private final AppUserService appUserService;

    // Constructor injection (recommended)
    public AppUserController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    // Endpoints go here...
}
```

**Key Points:**
- `@RestController` marks this as a REST API controller
- `@RequestMapping("/api/users")` sets the base URL path
- Constructor injection for dependencies (no `@Autowired` needed)

---

## HTTP Method Annotations

Spring provides annotations for each HTTP method:

| Annotation | HTTP Method | Use Case |
|------------|-------------|----------|
| `@GetMapping` | GET | Retrieve resources |
| `@PostMapping` | POST | Create new resources |
| `@PutMapping` | PUT | Update existing resources |
| `@DeleteMapping` | DELETE | Delete resources |
| `@PatchMapping` | PATCH | Partial updates |

### Examples from AppUserController

```java
@PostMapping                    // POST /api/users
@GetMapping("/{id}")           // GET /api/users/1
@GetMapping                    // GET /api/users
@PutMapping("/{id}")           // PUT /api/users/1
@DeleteMapping("/{id}")        // DELETE /api/users/1
```

---

## Request Handling

### Path Variables with @PathVariable

Extract values from the URL path:

```java
@GetMapping("/{id}")
public ResponseEntity<AppUserResponseDto> getUserById(@PathVariable Long id) {
    // /api/users/42 → id = 42
    return ResponseEntity.ok(appUserService.getById(id));
}

@GetMapping("/username/{username}")
public ResponseEntity<AppUserResponseDto> getUserByUsername(@PathVariable String username) {
    // /api/users/username/john → username = "john"
    return ResponseEntity.ok(appUserService.getByUsername(username));
}
```

### Request Body with @RequestBody

Deserialize JSON request body to an object:

```java
@PostMapping
public ResponseEntity<AppUserResponseDto> createUser(@RequestBody AppUserCreateDto createDto) {
    // JSON body automatically converted to AppUserCreateDto
    // {
    //   "username": "john",
    //   "email": "john@example.com",
    //   "password": "secret123"
    // }
    AppUserResponseDto created = appUserService.createAppUser(createDto);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
}
```

### Query Parameters with @RequestParam (Future Topic)

```java
@GetMapping
public ResponseEntity<List<TaskResponseDto>> getTasks(
    @RequestParam(required = false) Long userId,
    @RequestParam(required = false) TaskStatus status
) {
    // /api/tasks?userId=1&status=TODO
}
```

---

## Response Handling

### ResponseEntity

`ResponseEntity` gives full control over the HTTP response:

```java
// Return with status code and body
return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);

// Shorthand for 200 OK with body
return ResponseEntity.ok(user);

// 204 No Content (for DELETE)
return ResponseEntity.noContent().build();

// 404 Not Found
return ResponseEntity.notFound().build();
```

### Response Examples

```java
// CREATE - 201 Created
@PostMapping
public ResponseEntity<AppUserResponseDto> createUser(@RequestBody AppUserCreateDto createDto) {
    AppUserResponseDto createdUser = appUserService.createAppUser(createDto);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
}

// READ - 200 OK
@GetMapping("/{id}")
public ResponseEntity<AppUserResponseDto> getUserById(@PathVariable Long id) {
    AppUserResponseDto user = appUserService.getById(id);
    return ResponseEntity.ok(user);
}

// UPDATE - 200 OK
@PutMapping("/{id}")
public ResponseEntity<AppUserResponseDto> updateUser(
    @PathVariable Long id,
    @RequestBody AppUserUpdateDto updateDto
) {
    AppUserResponseDto updatedUser = appUserService.updateAppUser(id, updateDto);
    return ResponseEntity.ok(updatedUser);
}

// DELETE - 204 No Content
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    appUserService.deleteById(id);
    return ResponseEntity.noContent().build();
}
```

---

## HTTP Status Codes

### Common Status Codes

| Code | Name | When to Use |
|------|------|-------------|
| **2xx Success** | | |
| 200 | OK | Successful GET, PUT |
| 201 | Created | Successful POST (resource created) |
| 204 | No Content | Successful DELETE (no body to return) |
| **4xx Client Error** | | |
| 400 | Bad Request | Validation failed, invalid input |
| 401 | Unauthorized | Authentication required |
| 403 | Forbidden | Authenticated but not authorized |
| 404 | Not Found | Resource doesn't exist |
| 409 | Conflict | Resource conflict (e.g., duplicate) |
| **5xx Server Error** | | |
| 500 | Internal Server Error | Unexpected server error |

### Why 204 for DELETE?

```java
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    appUserService.deleteById(id);
    return ResponseEntity.noContent().build();  // 204
}
```

**Why not 200?**
- The resource no longer exists - nothing meaningful to return
- 204 is the RESTful convention for "success, no content"
- Saves bandwidth (empty body)

---

## Global Exception Handling

### The Problem

Without centralized handling, exceptions bubble up and result in ugly 500 errors:

```json
{
  "timestamp": "2024-01-15T10:30:00.000+00:00",
  "status": 500,
  "error": "Internal Server Error",
  "path": "/api/users/999"
}
```

### The Solution: @RestControllerAdvice

`@RestControllerAdvice` provides centralized exception handling for all controllers:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String UNEXPECTED_ERROR_MESSAGE = "An unexpected error occurred";

    // 404 Not Found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // 400 Bad Request - Validation errors
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // 400 Bad Request - Invalid arguments
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // 500 Internal Server Error - Fallback
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        LOGGER.error(UNEXPECTED_ERROR_MESSAGE, ex);  // Log full details
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_ERROR_MESSAGE);
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String message) {
        ErrorResponse errorResponse = ErrorResponse.of(status, message);
        return ResponseEntity.status(status).body(errorResponse);
    }
}
```

### Error Response Format

Use a consistent error response structure:

```java
public record ErrorResponse(
    LocalDateTime timestamp,
    int status,
    String error,
    String message
) {
    public static ErrorResponse of(HttpStatus status, String message) {
        return new ErrorResponse(
            LocalDateTime.now(),
            status.value(),
            status.getReasonPhrase(),
            message
        );
    }
}
```

**Example response:**
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "appUser with id '999' not found"
}
```

### Security Consideration

**Never expose internal error details to clients:**

```java
// BAD - Exposes internal details
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
    return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    // Could expose: "NullPointerException at UserService.java:42"
}

// GOOD - Generic message, log details
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
    LOGGER.error("Unexpected error", ex);  // Full details in logs
    return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
}
```

---

## Controller Testing

### @WebMvcTest Slice Tests

`@WebMvcTest` loads only the web layer (controllers, filters) - not the full application:

```java
@WebMvcTest(AppUserController.class)  // Only load this controller
@DisplayName("AppUserController Tests")
class AppUserControllerTest {

    @Autowired
    private MockMvc mockMvc;  // Simulates HTTP requests

    @Autowired
    private ObjectMapper objectMapper;  // JSON serialization

    @MockitoBean  // Spring Boot 3.4+ (replaces @MockBean)
    private AppUserService appUserService;  // Mock service in Spring context

    // Tests...
}
```

### @MockitoBean vs @Mock

| Annotation | Context | Use Case |
|------------|---------|----------|
| `@Mock` | Mockito only | Unit tests (no Spring) |
| `@MockitoBean` | Spring context | Slice tests (@WebMvcTest) |

**Why @MockitoBean for controllers?**
- `@WebMvcTest` creates a real controller in Spring's context
- Spring needs to inject dependencies
- `@MockitoBean` puts a mock into Spring's context for injection

**Note:** `@MockBean` is deprecated in Spring Boot 3.4+. Use `@MockitoBean` from `org.springframework.test.context.bean.override.mockito.MockitoBean`.

### Testing HTTP Requests with MockMvc

```java
@Test
@DisplayName("Should create user and return 201 Created")
void createUser_Success_Returns201() throws Exception {
    // Arrange
    AppUserCreateDto createDto = AppUserCreateDto.builder()
        .username("testuser")
        .email("test@example.com")
        .password("password123")
        .build();

    AppUserResponseDto responseDto = AppUserResponseDto.builder()
        .id(1L)
        .username("testuser")
        .email("test@example.com")
        .build();

    when(appUserService.createAppUser(any(AppUserCreateDto.class))).thenReturn(responseDto);

    // Act & Assert
    mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
            .andExpect(status().isCreated())                          // 201
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.email").value("test@example.com"));

    verify(appUserService).createAppUser(any(AppUserCreateDto.class));
}
```

### Testing Error Responses

```java
@Test
@DisplayName("Should return 404 when user ID not found")
void getUserById_NotFound_Returns404() throws Exception {
    // Arrange
    when(appUserService.getById(999L))
        .thenThrow(new ResourceNotFoundException("appUser", 999L));

    // Act & Assert
    mockMvc.perform(get("/api/users/{id}", 999L))
            .andExpect(status().isNotFound());

    verify(appUserService).getById(999L);
}
```

### Testing GlobalExceptionHandler

Test exception handlers as simple unit tests:

```java
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Should return 404 Not Found for ResourceNotFoundException")
    void handleResourceNotFoundException_Returns404() {
        // Arrange
        ResourceNotFoundException ex = new ResourceNotFoundException("appUser", 999L);

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleResourceNotFoundException(ex);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().error()).isEqualTo("Not Found");
        assertThat(response.getBody().message()).isEqualTo("appUser with id '999' not found");
    }
}
```

---

## Best Practices

### 1. Keep Controllers Thin

Controllers should only handle HTTP concerns - delegate business logic to services:

```java
// GOOD - Thin controller
@PostMapping
public ResponseEntity<AppUserResponseDto> createUser(@RequestBody AppUserCreateDto createDto) {
    AppUserResponseDto created = appUserService.createAppUser(createDto);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
}

// BAD - Business logic in controller
@PostMapping
public ResponseEntity<AppUserResponseDto> createUser(@RequestBody AppUserCreateDto createDto) {
    if (userRepository.existsByUsername(createDto.getUsername())) {
        throw new IllegalArgumentException("Username exists");
    }
    // More business logic...
}
```

### 2. Use Consistent URL Patterns

```
/api/users              # Collection
/api/users/{id}         # Single resource
/api/users/{id}/tasks   # Nested resource
```

### 3. Return Appropriate Status Codes

| Operation | Success Status |
|-----------|---------------|
| POST (create) | 201 Created |
| GET (read) | 200 OK |
| PUT (update) | 200 OK |
| DELETE | 204 No Content |

### 4. Use DTOs, Not Entities

Never expose JPA entities directly in controllers:

```java
// BAD - Exposes entity
@GetMapping("/{id}")
public AppUser getUser(@PathVariable Long id) { ... }

// GOOD - Returns DTO
@GetMapping("/{id}")
public ResponseEntity<AppUserResponseDto> getUser(@PathVariable Long id) { ... }
```

### 5. Centralize Exception Handling

Use `@RestControllerAdvice` instead of try-catch in every controller.

---

## Key Learning Points

1. **@RestController** = `@Controller` + `@ResponseBody` - returns JSON automatically

2. **HTTP Method Annotations** - Use `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping` for RESTful endpoints

3. **@PathVariable** extracts values from URL paths (`/users/{id}` → `@PathVariable Long id`)

4. **@RequestBody** deserializes JSON request body to a DTO

5. **ResponseEntity** provides full control over HTTP responses (status code, headers, body)

6. **HTTP Status Codes**:
   - 201 Created for POST
   - 200 OK for GET/PUT
   - 204 No Content for DELETE
   - 400 Bad Request for validation errors
   - 404 Not Found for missing resources

7. **@RestControllerAdvice** provides centralized exception handling across all controllers

8. **@ExceptionHandler** maps specific exceptions to HTTP responses

9. **Security**: Never expose internal error details - log them, return generic messages

10. **@WebMvcTest** loads only the web layer for fast, focused controller tests

11. **@MockitoBean** (Spring Boot 3.4+) replaces `@MockBean` - puts mocks into Spring context

12. **MockMvc** simulates HTTP requests without starting a real server

---

## Files Reference

| File | Description |
|------|-------------|
| `controller/AppUserController.java` | User REST endpoints (CRUD) |
| `controller/TaskController.java` | Task REST endpoints with query param filtering |
| `controller/ProjectController.java` | Project REST endpoints with query param filtering |
| `exception/GlobalExceptionHandler.java` | Centralized exception handling |
| `exception/ResourceNotFoundException.java` | Custom 404 exception |
| `exception/ValidationException.java` | Custom validation exception |
| `test/controller/AppUserControllerTest.java` | User controller slice tests |
| `test/controller/TaskControllerTest.java` | Task controller slice tests |
| `test/controller/ProjectControllerTest.java` | Project controller slice tests |
| `test/exception/GlobalExceptionHandlerTest.java` | Exception handler tests |
