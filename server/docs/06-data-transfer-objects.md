# Data Transfer Objects (DTOs)

## What are DTOs?

**Data Transfer Objects (DTOs)** are simple objects used to transfer data between different layers of an application, particularly between the service layer and the presentation layer (controllers). They act as a contract for what data is sent to and received from your API.

### Why Use DTOs?

❌ **Without DTOs (Anti-pattern):**
```java
@PostMapping("/users")
public AppUser createUser(@RequestBody AppUser user) {
    return userService.save(user);  // Exposing entity directly!
}
```

**Problems:**
- Exposes internal domain model to clients
- Security risk: client can set any field (including ID, timestamps)
- Tight coupling: API changes when entity changes
- Password exposure: entity fields directly serialized to JSON
- Circular references: JPA relationships cause infinite loops

✅ **With DTOs (Best Practice):**
```java
@PostMapping("/users")
public AppUserResponseDto createUser(@RequestBody AppUserCreateDto createDto) {
    AppUser user = userService.createUser(createDto);
    return new AppUserResponseDto(user);  // Clean, controlled response
}
```

**Benefits:**
- Clear API contract (separate from database schema)
- Security: only expose/accept specific fields
- Flexibility: different DTOs for create/update/response
- No circular references (DTOs don't have entity relationships)
- Prevents over-fetching/over-posting

---

## DTO Design Patterns

### Pattern 1: Single DTO per Purpose (Recommended)

Create **separate DTOs** for different operations:

```java
// For creating a new user
public class AppUserCreateDto {
    private String username;
    private String email;
    private String password;
}

// For updating an existing user
public class AppUserUpdateDto {
    private String email;      // Allowed to update
    private String password;   // Allowed to update
    // No username - it's immutable!
    // No id - comes from path parameter
}

// For returning user data
public class AppUserResponseDto {
    private Long id;
    private String username;
    private String email;
    // No password - never expose it!
}
```

**When to use:**
- ✅ Enterprise applications
- ✅ Public APIs
- ✅ Security-critical applications
- ✅ When fields differ significantly between operations

**Pros:**
- Clear intent: each DTO has ONE purpose
- Type safety: can't use wrong DTO for wrong operation
- Security: response DTO never includes password
- Validation: different rules per operation
- Immutability: can enforce (e.g., username not in update DTO)

**Cons:**
- More files to maintain
- More code (perceived as "over-engineering" for small projects)

---

### Pattern 2: Single DTO per Entity (Simpler, but less secure)

Use **one DTO** for all operations:

```java
public class AppUserDto {
    private Long id;
    private String username;
    private String email;
    private String password;  // Risk: might accidentally expose
}
```

**When to use:**
- Small projects / prototypes
- Internal APIs (not public-facing)
- CRUD-heavy applications with simple logic

**Pros:**
- Less code to maintain
- Simpler structure
- Faster to implement

**Cons:**
- Security risk (easy to expose password)
- Confusing contract (which fields required for create vs update?)
- Can't enforce immutability
- All-or-nothing updates

---

## DTO Naming Conventions

Industry-standard suffixes:

| Suffix | Purpose | Example |
|--------|---------|---------|
| `*CreateDto` | Creating resources | `AppUserCreateDto` |
| `*UpdateDto` | Updating resources | `AppUserUpdateDto` |
| `*ResponseDto` | Returning data | `AppUserResponseDto` |
| `*SummaryDto` | List views (minimal fields) | `AppUserSummaryDto` |
| `*DetailDto` | Single resource (full details) | `AppUserDetailDto` |

**Alternative naming:**
- `*Request` / `*Response` (common in REST APIs)
- `Create*Request` / `Update*Request`
- Just `*Dto` for responses

---

## Handling Entity Relationships in DTOs

### ❌ Anti-pattern: Nested DTOs (Circular References)

```java
// BAD - Creates circular dependency!
public class AppUserDto {
    private Long id;
    private String username;
    private List<TaskDto> tasks;  // TaskDto contains AppUserDto!
}

public class TaskDto {
    private Long id;
    private String title;
    private AppUserDto appUser;   // Circular reference!
}
```

**Result:** `StackOverflowError`, infinite JSON loops

---

### ✅ Best Practice: Use IDs Instead of Nested Objects

```java
// GOOD - Use IDs to represent relationships
public class TaskResponseDto {
    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private LocalDateTime dueDate;

    // Relationships as IDs
    private Long appUserId;
    private String appUserUsername;  // Optional: for display

    private Long projectId;
    private String projectName;      // Optional: for display
}
```

**Benefits:**
- No circular references
- Clients can fetch related resources if needed (HATEOAS principle)
- Prevents over-fetching
- Clean JSON output

---

### ✅ Alternative: Summary DTOs for Collections

```java
public class AppUserDetailDto {
    private Long id;
    private String username;
    private String email;

    // Instead of full tasks, include summaries
    private List<TaskSummaryDto> recentTasks;  // Only 5 recent

    // Or just counts
    private int totalTasks;
    private int completedTasks;
}

public class TaskSummaryDto {
    private Long id;
    private String title;
    private TaskStatus status;
    // Minimal fields for list views
}
```

---

## DTO-Entity Conversion

### Pattern 1: Constructor in DTO (Current Implementation)

```java
public class AppUserResponseDto {
    private Long id;
    private String username;
    private String email;

    // Entity-to-DTO conversion
    public AppUserResponseDto(AppUser appUser) {
        this.id = appUser.getId();
        this.username = appUser.getUsername();
        this.email = appUser.getEmail();
    }
}

public class AppUserCreateDto {
    private String username;
    private String email;
    private String password;

    // DTO-to-Entity conversion
    public AppUser convert() {
        return AppUser.builder()
            .username(username)
            .email(email)
            .password(password)
            .build();
    }
}
```

**Pros:**
- Simple and straightforward
- Co-located with DTO definition
- No extra dependencies

**Cons:**
- DTO depends on entity (coupling)
- Harder to test in isolation
- Conversion logic mixed with data structure

---

### Pattern 2: Static Factory Methods

```java
public class AppUserResponseDto {
    private Long id;
    private String username;
    private String email;

    public static AppUserResponseDto from(AppUser user) {
        AppUserResponseDto dto = new AppUserResponseDto();
        dto.id = user.getId();
        dto.username = user.getUsername();
        dto.email = user.getEmail();
        return dto;
    }
}

// Usage
AppUserResponseDto dto = AppUserResponseDto.from(user);
```

---

### Pattern 3: Mapper Classes (Enterprise Pattern)

```java
@Component
public class AppUserMapper {

    public AppUser toEntity(AppUserCreateDto dto) {
        return AppUser.builder()
            .username(dto.getUsername())
            .email(dto.getEmail())
            .password(dto.getPassword())
            .build();
    }

    public AppUserResponseDto toDto(AppUser user) {
        AppUserResponseDto dto = new AppUserResponseDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        return dto;
    }

    public void updateEntityFromDto(AppUserUpdateDto dto, AppUser user) {
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getPassword() != null) {
            user.setPassword(dto.getPassword());
        }
    }
}
```

**Usage in service:**
```java
@Service
public class AppUserService {
    private final AppUserRepository repository;
    private final AppUserMapper mapper;  // Injected

    public AppUser createUser(AppUserCreateDto dto) {
        AppUser user = mapper.toEntity(dto);
        return repository.save(user);
    }
}
```

**Pros:**
- Clean separation of concerns
- Testable in isolation
- Can use MapStruct or ModelMapper libraries
- Centralized conversion logic

**Cons:**
- More classes to maintain
- Extra layer of abstraction

---

### Pattern 4: MapStruct (Auto-generation)

```java
@Mapper(componentModel = "spring")
public interface AppUserMapper {

    AppUser toEntity(AppUserCreateDto dto);

    AppUserResponseDto toDto(AppUser user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", ignore = true)  // Immutable
    void updateEntityFromDto(AppUserUpdateDto dto, @MappingTarget AppUser user);
}
```

MapStruct generates the implementation at compile-time.

---

## Using DTOs in the Service Layer

### Example: AppUserService with DTOs

```java
@Service
public class AppUserService {
    private final AppUserRepository repository;

    // Create: Takes CreateDto, returns Entity
    public AppUser createAppUser(AppUserCreateDto createDto) {
        // Validation
        if (repository.existsByUsername(createDto.getUsername())) {
            throw new IllegalArgumentException("username already exists");
        }

        // Convert DTO to entity
        AppUser user = createDto.convert();

        // Save and return
        return repository.save(user);
    }

    // Update: Takes UpdateDto, returns Entity
    public AppUser updateAppUser(Long id, AppUserUpdateDto updateDto) {
        // Find existing
        AppUser existingUser = getById(id);

        // Update only allowed fields
        if (updateDto.getEmail() != null) {
            if (!existingUser.getEmail().equalsIgnoreCase(updateDto.getEmail())) {
                if (repository.existsByEmail(updateDto.getEmail())) {
                    throw new IllegalArgumentException("email already exists");
                }
            }
            existingUser.setEmail(updateDto.getEmail());
        }

        if (updateDto.getPassword() != null) {
            existingUser.setPassword(updateDto.getPassword());
        }

        return repository.save(existingUser);
    }
}
```

**Key Points:**
- Service works with entities internally
- DTOs used as input/output boundaries
- Validation happens before conversion
- Update methods modify existing entities (not replace)

---

## Using DTOs in Controllers

```java
@RestController
@RequestMapping("/api/users")
public class AppUserController {
    private final AppUserService userService;

    @PostMapping
    public ResponseEntity<AppUserResponseDto> createUser(
            @Valid @RequestBody AppUserCreateDto createDto) {

        AppUser user = userService.createAppUser(createDto);
        AppUserResponseDto response = new AppUserResponseDto(user);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AppUserResponseDto> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody AppUserUpdateDto updateDto) {

        AppUser user = userService.updateAppUser(id, updateDto);
        AppUserResponseDto response = new AppUserResponseDto(user);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppUserResponseDto> getUser(@PathVariable Long id) {
        AppUser user = userService.getById(id);
        AppUserResponseDto response = new AppUserResponseDto(user);

        return ResponseEntity.ok(response);
    }
}
```

**Flow:**
1. Client sends JSON → Deserialized to CreateDto/UpdateDto
2. Controller validates DTO (`@Valid`)
3. Controller calls service with DTO
4. Service converts DTO to entity, performs business logic
5. Service returns entity
6. Controller converts entity to ResponseDto
7. ResponseDto serialized to JSON → Client

---

## Avoiding Circular References with @JsonIgnore

### ❌ Not Recommended: Using @JsonIgnore on Entities

```java
@Entity
public class AppUser {
    @OneToMany(mappedBy = "appUser")
    @JsonIgnore  // Quick fix, but mixing concerns
    private List<Task> tasks;
}
```

**Problems:**
- Mixes JPA (persistence) with JSON (presentation)
- Not flexible: what if you sometimes WANT to include tasks?
- Still exposing entities directly (security risk)

---

### ✅ Recommended: Use DTOs (No @JsonIgnore Needed)

```java
// Entity - no Jackson annotations
@Entity
public class AppUser {
    @OneToMany(mappedBy = "appUser")
    private List<Task> tasks;  // No @JsonIgnore
}

// Response DTO - naturally avoids circular refs
public class AppUserResponseDto {
    private Long id;
    private String username;
    // No tasks - problem solved!
}
```

**Your DTOs don't need `@JsonIgnore` if designed properly!**

---

## Common DTO Patterns

### 1. Summary DTOs for Lists

```java
@GetMapping
public List<AppUserSummaryDto> getAllUsers() {
    return userService.findAll()
        .stream()
        .map(AppUserSummaryDto::new)
        .toList();
}

public class AppUserSummaryDto {
    private Long id;
    private String username;
    // Minimal fields for list views
}
```

---

### 2. Detail DTOs for Single Resources

```java
@GetMapping("/{id}")
public AppUserDetailDto getUser(@PathVariable Long id) {
    AppUser user = userService.getById(id);
    return new AppUserDetailDto(user);
}

public class AppUserDetailDto {
    private Long id;
    private String username;
    private String email;
    private LocalDateTime createdAt;
    private int totalTasks;
    // More fields for detail view
}
```

---

### 3. Nested DTOs (Controlled)

```java
public class ProjectDetailDto {
    private Long id;
    private String name;
    private String description;

    // One-way nesting (no circular ref)
    private AppUserSummaryDto owner;
    private List<TaskSummaryDto> tasks;
}
```

**Key:** Use summary DTOs to prevent deep nesting.

---

## Best Practices

### ✅ Do's

1. **Separate DTOs by purpose** (create, update, response)
2. **Never expose passwords** in response DTOs
3. **Use IDs for relationships**, not nested objects
4. **Validate DTOs** with `@Valid` and Jakarta Validation
5. **Keep DTOs in a separate package** (`dto/appuser/`, `dto/task/`)
6. **Use builder pattern** for entity creation from DTOs
7. **Document DTO fields** with JavaDoc or OpenAPI annotations

### ❌ Don'ts

1. **Don't expose entities directly** to controllers
2. **Don't use nested entity relationships** in DTOs
3. **Don't reuse entities as DTOs** (even with `@JsonIgnore`)
4. **Don't include business logic** in DTOs (they're just data)
5. **Don't use DTOs in the repository layer** (repositories work with entities)
6. **Don't create circular DTO references**

---

## Real-World Examples

### GitHub API
```
POST /users      → UserCreateRequest
PATCH /users/:id → UserUpdateRequest
GET /users/:id   → User (response)
```

### Stripe API
```
POST /customers      → CustomerCreateParams
POST /customers/:id  → CustomerUpdateParams
GET /customers/:id   → Customer (response)
```

### Spring Petclinic (Official Demo)
- `OwnerCreateRequest`
- `OwnerUpdateRequest`
- `OwnerResponse`

---

## Task Manager DTO Structure

```
dto/
├── appuser/
│   ├── AppUserCreateDto.java
│   ├── AppUserUpdateDto.java
│   └── AppUserResponseDto.java
├── task/
│   ├── TaskCreateDto.java
│   ├── TaskUpdateDto.java
│   └── TaskResponseDto.java
└── project/
    ├── ProjectCreateDto.java
    ├── ProjectUpdateDto.java
    └── ProjectResponseDto.java
```

---

## Testing DTOs

### Unit Test: DTO Conversion

```java
@Test
void testDtoToEntityConversion() {
    // Arrange
    AppUserCreateDto dto = new AppUserCreateDto(
        "testuser",
        "test@example.com",
        "password123"
    );

    // Act
    AppUser entity = dto.convert();

    // Assert
    assertThat(entity.getUsername()).isEqualTo("testuser");
    assertThat(entity.getEmail()).isEqualTo("test@example.com");
    assertThat(entity.getPassword()).isEqualTo("password123");
}
```

---

## Summary

**DTOs are essential for:**
- Clean API design
- Security (controlling what's exposed)
- Flexibility (API independent of database schema)
- Preventing circular references
- Validation and documentation

**Key Takeaway:** Use separate DTOs for create, update, and response operations in enterprise applications. Use IDs (not nested objects) for relationships.

---

## References

- Task Manager Implementation: `src/main/java/com/tutorial/taskmanager/dto/`
- Service Layer Usage: `src/main/java/com/tutorial/taskmanager/service/AppUserService.java`
- Entity Definitions: `src/main/java/com/tutorial/taskmanager/model/`

## Next Steps

- [10-bean-validation.md](10-bean-validation.md) - Add validation to DTOs
- [04-rest-controllers.md](04-rest-controllers.md) - Use DTOs in controllers
- [11-exception-handling.md](11-exception-handling.md) - Handle DTO validation errors
