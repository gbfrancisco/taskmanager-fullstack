# Unit Testing with JUnit 5 and Mockito

## What is Unit Testing?

**Unit testing** tests a single component (class or method) in isolation from its dependencies. In Spring Boot applications, we typically unit test **service layer** classes by mocking their dependencies (repositories, other services).

### Testing Pyramid

```
        /\
       /  \  E2E Tests (Few)
      /----\
     / IT  \ Integration Tests (Some)
    /------\
   /  Unit  \ Unit Tests (Many)
  /----------\
```

**Unit tests should be:**
- Fast (milliseconds)
- Isolated (no database, no network)
- Reliable (deterministic)
- Easy to write and maintain

---

## JUnit 5 Basics

### Test Class Structure

```java
@ExtendWith(MockitoExtension.class)  // Enables Mockito
@DisplayName("AppUserService Unit Tests")
class AppUserServiceTest {

    @Mock                  // Creates a mock
    private AppUserRepository repository;

    @InjectMocks          // Injects mocks into this
    private AppUserService service;

    @BeforeEach           // Runs before each test
    void setUp() {
        // Setup test data
    }

    @Test                 // Marks as a test method
    @DisplayName("Should create user successfully")
    void createUser_Success() {
        // Test implementation
    }
}
```

### Key Annotations

| Annotation | Purpose |
|------------|---------|
| `@Test` | Marks a method as a test |
| `@BeforeEach` | Runs before each test method |
| `@AfterEach` | Runs after each test method |
| `@BeforeAll` | Runs once before all tests (static method) |
| `@AfterAll` | Runs once after all tests (static method) |
| `@DisplayName` | Custom test name (appears in reports) |
| `@Disabled` | Skips a test |
| `@ExtendWith` | Registers extensions (e.g., Mockito) |

---

## Mockito Fundamentals

### What is Mocking?

**Mocking** replaces real dependencies with fake objects that you control. This allows you to:
- Test in isolation
- Control return values
- Verify interactions
- Avoid slow operations (database, network)

### Mockito Annotations

```java
@Mock
private AppUserRepository repository;  // Creates a mock

@InjectMocks
private AppUserService service;        // Injects mocks into service

@Spy
private AppUserService service;        // Partial mock (real object with some mocked methods)

@Captor
private ArgumentCaptor<AppUser> userCaptor;  // Captures method arguments
```

---

## The AAA Pattern (Arrange-Act-Assert)

Every test should follow this structure:

```java
@Test
void createUser_Success() {
    // ARRANGE: Setup test data and mock behavior
    AppUserCreateDto dto = new AppUserCreateDto("user", "email@test.com", "pass");
    AppUser savedUser = AppUser.builder()
        .username("user")
        .email("email@test.com")
        .build();

    when(repository.existsByUsername("user")).thenReturn(false);
    when(repository.existsByEmail("email@test.com")).thenReturn(false);
    when(repository.save(any(AppUser.class))).thenReturn(savedUser);

    // ACT: Call the method under test
    AppUser result = service.createAppUser(dto);

    // ASSERT: Verify the result and interactions
    assertThat(result).isNotNull();
    assertThat(result.getUsername()).isEqualTo("user");
    verify(repository).save(any(AppUser.class));
}
```

---

## Mocking with Mockito

### Stubbing Return Values

```java
// Return a value
when(repository.findById(1L)).thenReturn(Optional.of(user));

// Return different values on successive calls
when(repository.findAll())
    .thenReturn(List.of(user1))
    .thenReturn(List.of(user1, user2));

// Throw an exception
when(repository.findById(999L))
    .thenThrow(new NoSuchElementException("Not found"));

// Return based on argument
when(repository.findByUsername(anyString()))
    .thenAnswer(invocation -> {
        String username = invocation.getArgument(0);
        return username.equals("admin") ? Optional.of(adminUser) : Optional.empty();
    });
```

### Argument Matchers

```java
// Exact value
when(repository.findById(1L)).thenReturn(Optional.of(user));

// Any value of type
when(repository.save(any(AppUser.class))).thenReturn(user);

// Any string
when(repository.findByUsername(anyString())).thenReturn(Optional.of(user));

// Any long
when(repository.findById(anyLong())).thenReturn(Optional.of(user));

// Null value
when(repository.findById(isNull())).thenThrow(IllegalArgumentException.class);

// Custom matcher
when(repository.findByEmail(argThat(email -> email.endsWith("@test.com"))))
    .thenReturn(Optional.of(user));
```

**Important:** Don't mix matchers and exact values!
```java
// WRONG - mixing matcher and exact value
when(repository.someMethod(anyString(), 1L));

// CORRECT - all matchers
when(repository.someMethod(anyString(), anyLong()));

// CORRECT - all exact values
when(repository.someMethod("test", 1L));
```

### Void Methods

```java
// No exception (default)
doNothing().when(repository).deleteById(1L);

// Throw exception
doThrow(new RuntimeException("Error"))
    .when(repository).deleteById(999L);

// Custom behavior
doAnswer(invocation -> {
    Long id = invocation.getArgument(0);
    System.out.println("Deleting: " + id);
    return null;
}).when(repository).deleteById(anyLong());
```

---

## Verifying Interactions

### Basic Verification

```java
// Verify method was called once
verify(repository).save(any(AppUser.class));

// Verify method was called with exact arguments
verify(repository).findById(1L);

// Verify method was never called
verify(repository, never()).deleteById(anyLong());

// Verify method was called N times
verify(repository, times(2)).save(any(AppUser.class));

// Verify method was called at least once
verify(repository, atLeastOnce()).findAll();

// Verify method was called at most N times
verify(repository, atMost(3)).findById(anyLong());

// Verify no interactions at all
verifyNoInteractions(repository);

// Verify no more interactions (after other verifications)
verify(repository).save(any(AppUser.class));
verifyNoMoreInteractions(repository);
```

### Verification Order

```java
@Test
void testOperationOrder() {
    // Arrange
    InOrder inOrder = inOrder(repository);

    // Act
    service.createUser(dto);

    // Assert - verify calls happened in this order
    inOrder.verify(repository).existsByUsername("user");
    inOrder.verify(repository).existsByEmail("email@test.com");
    inOrder.verify(repository).save(any(AppUser.class));
}
```

---

## AssertJ Assertions

We use **AssertJ** for fluent, readable assertions.

### Basic Assertions

```java
// Null checks
assertThat(result).isNotNull();
assertThat(result).isNull();

// Equality
assertThat(result.getId()).isEqualTo(1L);
assertThat(result.getUsername()).isEqualTo("testuser");

// Boolean
assertThat(service.existsByUsername("user")).isTrue();
assertThat(service.existsByUsername("unknown")).isFalse();

// Strings
assertThat(user.getEmail()).contains("@example.com");
assertThat(user.getEmail()).startsWith("test");
assertThat(user.getEmail()).endsWith(".com");
assertThat(user.getEmail()).isEqualToIgnoringCase("TEST@EXAMPLE.COM");

// Numbers
assertThat(users.size()).isGreaterThan(0);
assertThat(users.size()).isLessThanOrEqualTo(10);
assertThat(count).isBetween(5, 15);
```

### Collection Assertions

```java
// Size
assertThat(users).hasSize(3);
assertThat(users).isEmpty();
assertThat(users).isNotEmpty();

// Contains
assertThat(users).contains(user1, user2);
assertThat(users).containsOnly(user1, user2);
assertThat(users).containsExactly(user1, user2);  // Order matters
assertThat(users).doesNotContain(user3);

// Extracting fields
assertThat(users)
    .extracting(AppUser::getUsername)
    .containsExactly("user1", "user2", "user3");

// Filtering
assertThat(users)
    .filteredOn(user -> user.getEmail().endsWith("@test.com"))
    .hasSize(2);
```

### Exception Assertions

```java
// Exception is thrown
assertThatThrownBy(() -> service.getById(999L))
    .isInstanceOf(NoSuchElementException.class)
    .hasMessage("appUser with id '999' not found");

// More specific
assertThatThrownBy(() -> service.createUser(null))
    .isInstanceOf(IllegalArgumentException.class)
    .hasMessageContaining("cannot be null");

// Exception is NOT thrown
assertThatNoException().isThrownBy(() -> service.getById(1L));

// Alternative syntax
assertThatCode(() -> service.getById(1L)).doesNotThrowAnyException();
```

### Optional Assertions

```java
// Present
Optional<AppUser> result = service.findById(1L);
assertThat(result).isPresent();
assertThat(result).contains(expectedUser);
assertThat(result.get().getUsername()).isEqualTo("testuser");

// Empty
Optional<AppUser> result = service.findById(999L);
assertThat(result).isEmpty();
assertThat(result).isNotPresent();
```

---

## Common Test Scenarios

### Testing Service CRUD Operations

#### Create Operation

```java
@Test
@DisplayName("createUser - should create user successfully")
void createUser_Success() {
    // Arrange
    AppUserCreateDto dto = new AppUserCreateDto("user", "email@test.com", "pass");
    when(repository.existsByUsername("user")).thenReturn(false);
    when(repository.existsByEmail("email@test.com")).thenReturn(false);
    when(repository.save(any(AppUser.class))).thenReturn(testUser);

    // Act
    AppUser result = service.createAppUser(dto);

    // Assert
    assertThat(result).isNotNull();
    verify(repository).save(any(AppUser.class));
}

@Test
@DisplayName("createUser - should throw exception when username exists")
void createUser_DuplicateUsername_ThrowsException() {
    // Arrange
    AppUserCreateDto dto = new AppUserCreateDto("user", "email@test.com", "pass");
    when(repository.existsByUsername("user")).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> service.createAppUser(dto))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("username already exists");

    verify(repository, never()).save(any(AppUser.class));
}
```

#### Read Operation

```java
@Test
@DisplayName("findById - should return user when found")
void findById_UserExists_ReturnsOptional() {
    // Arrange
    when(repository.findById(1L)).thenReturn(Optional.of(testUser));

    // Act
    Optional<AppUser> result = service.findById(1L);

    // Assert
    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo(1L);
    verify(repository).findById(1L);
}

@Test
@DisplayName("getById - should throw exception when not found")
void getById_UserNotFound_ThrowsException() {
    // Arrange
    when(repository.findById(999L)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> service.getById(999L))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessage("appUser with id '999' not found");
}
```

#### Update Operation

```java
@Test
@DisplayName("updateUser - should update email and password")
void updateUser_Success() {
    // Arrange
    AppUserUpdateDto updateDto = new AppUserUpdateDto();
    updateDto.setEmail("new@example.com");
    updateDto.setPassword("newPass");

    when(repository.findById(1L)).thenReturn(Optional.of(testUser));
    when(repository.existsByEmail("new@example.com")).thenReturn(false);
    when(repository.save(any(AppUser.class))).thenReturn(testUser);

    // Act
    AppUser result = service.updateAppUser(1L, updateDto);

    // Assert
    assertThat(result).isNotNull();
    verify(repository).findById(1L);
    verify(repository).save(testUser);
}

@Test
@DisplayName("updateUser - should allow keeping same email")
void updateUser_SameEmail_Success() {
    // Arrange
    AppUserUpdateDto updateDto = new AppUserUpdateDto();
    updateDto.setEmail("test@example.com");  // Same email

    when(repository.findById(1L)).thenReturn(Optional.of(testUser));
    when(repository.save(any(AppUser.class))).thenReturn(testUser);

    // Act
    AppUser result = service.updateAppUser(1L, updateDto);

    // Assert
    assertThat(result).isNotNull();
    verify(repository, never()).existsByEmail(anyString());  // Skip check
}
```

#### Delete Operation

```java
@Test
@DisplayName("deleteById - should delete user successfully")
void deleteById_Success() {
    // Arrange
    when(repository.existsById(1L)).thenReturn(true);
    doNothing().when(repository).deleteById(1L);

    // Act
    service.deleteById(1L);

    // Assert
    verify(repository).existsById(1L);
    verify(repository).deleteById(1L);
}

@Test
@DisplayName("deleteById - should throw exception when user not found")
void deleteById_UserNotFound_ThrowsException() {
    // Arrange
    when(repository.existsById(999L)).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> service.deleteById(999L))
        .isInstanceOf(NoSuchElementException.class);

    verify(repository, never()).deleteById(anyLong());
}
```

---

## Testing Validation Logic

```java
@Test
@DisplayName("createUser - should throw exception when DTO is null")
void createUser_NullDto_ThrowsException() {
    // Act & Assert
    assertThatThrownBy(() -> service.createAppUser(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("appUserCreateDto cannot be null");

    verifyNoInteractions(repository);
}

@Test
@DisplayName("createUser - should throw exception when username is empty")
void createUser_EmptyUsername_ThrowsException() {
    // Arrange
    AppUserCreateDto dto = new AppUserCreateDto("", "email@test.com", "pass");

    // Act & Assert
    assertThatThrownBy(() -> service.createAppUser(dto))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("username cannot be empty");

    verifyNoInteractions(repository);
}
```

---

## Test Data Setup

### Using @BeforeEach

```java
private AppUser testUser;
private AppUserCreateDto createDto;

@BeforeEach
void setUp() {
    // Reusable test data
    testUser = AppUser.builder()
        .username("testuser")
        .email("test@example.com")
        .password("password123")
        .build();
    testUser.setId(1L);

    createDto = new AppUserCreateDto("newuser", "new@example.com", "pass");
}
```

### Test Data Builders

```java
public class AppUserTestBuilder {
    private String username = "testuser";
    private String email = "test@example.com";
    private String password = "password";

    public AppUserTestBuilder withUsername(String username) {
        this.username = username;
        return this;
    }

    public AppUserTestBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public AppUser build() {
        AppUser user = AppUser.builder()
            .username(username)
            .email(email)
            .password(password)
            .build();
        user.setId(1L);
        return user;
    }
}

// Usage
AppUser user = new AppUserTestBuilder()
    .withUsername("admin")
    .withEmail("admin@test.com")
    .build();
```

---

## Best Practices

### ✅ Do's

1. **Test one thing per test** - Each test should verify one behavior
2. **Use descriptive names** - `createUser_DuplicateUsername_ThrowsException`
3. **Follow AAA pattern** - Arrange, Act, Assert
4. **Keep tests independent** - No test should depend on another
5. **Use @DisplayName** - Makes test reports readable
6. **Verify interactions** - Ensure repository methods are called correctly
7. **Test edge cases** - Null values, empty strings, not found scenarios
8. **Use test data builders** - For complex object creation
9. **Clean up mocks** - Mockito does this automatically with `@ExtendWith`

### ❌ Don'ts

1. **Don't test Spring framework** - Trust that Spring works
2. **Don't test getters/setters** - Unless they have logic
3. **Don't use real database** - That's integration testing
4. **Don't make tests depend on each other** - Use @BeforeEach for setup
5. **Don't over-mock** - Only mock direct dependencies
6. **Don't test private methods** - Test through public API
7. **Don't use @SpringBootTest** - That's integration testing (slower)

---

## Common Mistakes

### Mistake 1: Not Verifying Interactions

```java
// BAD - No verification
@Test
void createUser_Success() {
    service.createAppUser(dto);
    // Missing: verify(repository).save(...)
}

// GOOD
@Test
void createUser_Success() {
    service.createAppUser(dto);
    verify(repository).save(any(AppUser.class));
}
```

### Mistake 2: Testing Too Much in One Test

```java
// BAD - Testing multiple scenarios
@Test
void testCreateUser() {
    // Test success
    service.createAppUser(validDto);
    // Test duplicate username
    assertThatThrownBy(() -> service.createAppUser(duplicateDto));
    // Test null DTO
    assertThatThrownBy(() -> service.createAppUser(null));
}

// GOOD - Separate tests
@Test void createUser_Success() { ... }
@Test void createUser_DuplicateUsername_ThrowsException() { ... }
@Test void createUser_NullDto_ThrowsException() { ... }
```

### Mistake 3: Not Resetting Mocks

```java
// BAD - Reusing mock behavior across tests (can cause flaky tests)
@BeforeAll  // WRONG - should be @BeforeEach
static void setUp() {
    when(repository.findById(1L)).thenReturn(Optional.of(testUser));
}

// GOOD - Fresh mocks for each test
@BeforeEach
void setUp() {
    // Setup runs before EACH test
}
```

---

## Running Tests

### Run all tests
```bash
mvn test
```

### Run specific test class
```bash
mvn test -Dtest=AppUserServiceTest
```

### Run specific test method
```bash
mvn test -Dtest=AppUserServiceTest#createUser_Success
```

### With code coverage
```bash
mvn clean test jacoco:report
# Report: target/site/jacoco/index.html
```

---

## Mockito Agent Configuration

To avoid the "self-attaching" warning, configure Maven Surefire plugin:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <argLine>
            @{argLine}
            -javaagent:${settings.localRepository}/org/mockito/mockito-core/${mockito.version}/mockito-core-${mockito.version}.jar
            -XX:+EnableDynamicAgentLoading
        </argLine>
    </configuration>
</plugin>
```

---

## Task Manager Test Suite

### AppUserServiceTest (41 tests)

Coverage:
- ✅ `createAppUser()` - 7 tests
- ✅ `findById()` / `getById()` - 6 tests
- ✅ `findByUsername()` / `getByUsername()` - 6 tests
- ✅ `findByEmail()` - 3 tests
- ✅ `findAll()` - 2 tests
- ✅ `updateAppUser()` - 8 tests
- ✅ `deleteById()` - 3 tests
- ✅ `existsByUsername()` / `existsByEmail()` - 6 tests

**Result:** `Tests run: 41, Failures: 0, Errors: 0`

---

## Summary

**Unit testing verifies:**
- Business logic correctness
- Validation rules
- Error handling
- Edge cases

**Key tools:**
- **JUnit 5**: Test framework
- **Mockito**: Mocking dependencies
- **AssertJ**: Fluent assertions

**Benefits:**
- Fast feedback (runs in seconds)
- Prevents regressions
- Documents behavior
- Improves code design

---

## References

- Test Implementation: `src/test/java/com/tutorial/taskmanager/service/AppUserServiceTest.java`
- Service Under Test: `src/main/java/com/tutorial/taskmanager/service/AppUserService.java`
- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [AssertJ Documentation](https://assertj.github.io/doc/)

## Next Steps

- [17-integration-testing.md](17-integration-testing.md) - Test with real database
- [18-web-layer-testing.md](18-web-layer-testing.md) - Test controllers with @WebMvcTest
- [19-data-layer-testing.md](19-data-layer-testing.md) - Test repositories with @DataJpaTest
