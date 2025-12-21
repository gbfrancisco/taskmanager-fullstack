# Session 4+ Summary - Service Layer & DTOs

## Date
November 18, 2025

## Session Overview
Continued building the service layer with a focus on AppUserService, implementing comprehensive DTOs, and writing thorough unit tests.

---

## What We Built

### 1. Data Transfer Objects (DTOs) ✅
Created a complete DTO structure following enterprise best practices:

**AppUser DTOs:**
- `AppUserCreateDto` - For creating users (username, email, password)
- `AppUserUpdateDto` - For updating users (email, password only - username is immutable)
- `AppUserResponseDto` - For API responses (excludes password for security)

**Task DTOs:**
- `TaskCreateDto` - For creating tasks
- `TaskUpdateDto` - For updating tasks
- `TaskResponseDto` - For API responses

**Project DTOs:**
- `ProjectCreateDto` - For creating projects
- `ProjectUpdateDto` - For updating projects
- `ProjectResponseDto` - For API responses

**Key Design Decisions:**
- Separate DTOs per operation (not one DTO per entity)
- No nested entity relationships (prevents circular references)
- Each DTO includes conversion methods to/from entities
- Organized in subpackages: `dto/appuser/`, `dto/task/`, `dto/project/`

---

### 2. AppUserService Implementation ✅
Fully implemented with all CRUD operations:

**Methods Implemented:**
- `createAppUser(AppUserCreateDto)` - Create with validation
- `findById(Long)` - Returns Optional<AppUser>
- `getById(Long)` - Throws exception if not found
- `findByUsername(String)` - Returns Optional<AppUser>
- `getByUsername(String)` - Throws exception if not found
- `findByEmail(String)` - Returns Optional<AppUser>
- `findAll()` - Returns all users
- `updateAppUser(Long, AppUserUpdateDto)` - Update with validation
- `deleteById(Long)` - Delete with existence check
- `existsByUsername(String)` - Boolean check
- `existsByEmail(String)` - Boolean check

**Business Logic:**
- Username uniqueness validation
- Email uniqueness validation (case-insensitive)
- Username immutability enforcement
- Proper null/empty string validation
- Uses DTOs for input, returns entities

---

### 3. Comprehensive Unit Tests ✅
Created `AppUserServiceTest` with 41 test cases:

**Test Coverage:**
- createAppUser() - 7 tests
- findById() / getById() - 6 tests
- findByUsername() / getByUsername() - 6 tests
- findByEmail() - 3 tests
- findAll() - 2 tests
- updateAppUser() - 8 tests
- deleteById() - 3 tests
- existsByUsername() / existsByEmail() - 6 tests

**Testing Patterns Demonstrated:**
- Mockito mocking with @Mock and @InjectMocks
- AAA pattern (Arrange-Act-Assert)
- AssertJ fluent assertions
- Verification of repository interactions
- Exception testing with assertThatThrownBy()
- Edge case testing (null, empty, duplicates, not found)

**Result:** All 41 tests passing ✅

---

### 4. Service Skeletons with TODOs 🔄
Added comprehensive TODOs to guide future implementation:

**TaskService:**
- 17 TODOs for CRUD, filtering, date queries, project assignment

**ProjectService:**
- 14 TODOs for CRUD, filtering, search, uniqueness validation

---

### 5. Documentation ✅
Created two comprehensive documentation files:

**docs/06-data-transfer-objects.md:**
- Why use DTOs vs exposing entities
- Single DTO vs multiple DTOs per entity pattern
- Handling relationships without circular references
- DTO naming conventions and best practices
- DTO-entity conversion patterns
- Real-world examples (GitHub, Stripe, Spring Petclinic)
- When to use (and not use) @JsonIgnore

**docs/16-unit-testing.md:**
- JUnit 5 fundamentals and annotations
- Mockito mocking, stubbing, and verification
- AAA pattern explained
- AssertJ assertions (basic, collections, exceptions, Optional)
- Testing CRUD operations
- Testing validation logic
- Test data setup with @BeforeEach
- Best practices and common mistakes
- Mockito agent configuration for JDK 21+

---

## Key Concepts Learned

### 1. DTO Design Patterns
- **Anti-pattern:** Exposing entities directly (security risk, tight coupling)
- **Best practice:** Separate DTOs per operation (create, update, response)
- **Circular references:** Use IDs instead of nested objects
- **Security:** Never include passwords in response DTOs

### 2. Optional vs Null
- **Never return null** - use Optional<T> instead
- Optional makes "might not exist" explicit in the API
- Pattern: `find*()` returns Optional, `get*()` throws exception
- Prevents NullPointerException at compile-time

### 3. Service Layer Responsibilities
- Business logic and validation
- Works with entities internally
- DTOs as input/output boundaries
- Enforces business rules (immutability, uniqueness)
- Clean separation from controllers

### 4. Unit Testing Philosophy
- Test in isolation (mock dependencies)
- Fast, reliable, deterministic
- One test per behavior
- Test edge cases thoroughly
- Verify interactions, not just results

### 5. Mockito Fundamentals
- `@Mock` creates mocks, `@InjectMocks` injects them
- `when().thenReturn()` for stubbing
- `verify()` for interaction verification
- `verifyNoInteractions()` ensures clean tests
- Argument matchers: `any()`, `anyString()`, `anyLong()`

---

## Code Review Highlights

### Issues Fixed:
1. **Critical bug in updateAppUser():** Saving wrong entity (updatedAppUser instead of existingAppUser)
2. **Inconsistent exception types:** Changed to NoSuchElementException for "not found" cases
3. **Return type improvements:** Changed from null returns to Optional<>
4. **Email uniqueness logic:** Added case-insensitive comparison, skip check if same email

### Good Practices Followed:
- Constructor injection (final fields)
- Input validation (null checks, empty string checks)
- Comprehensive error messages
- Consistent naming conventions
- Clean separation of concerns

---

## Configuration Updates

### pom.xml
Added Surefire plugin configuration to resolve Mockito warning:

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

This eliminates the "Mockito is currently self-attaching" warning for JDK 21+.

---

## Questions Answered

### Q: Should I return null or Optional<>?
**A:** Always use Optional<>. It's explicit, prevents NPE, and is the Java best practice.

### Q: Why separate `id` parameter in `updateAppUser(Long id, AppUser updatedUser)`?
**A:** Security - prevents malicious ID changes in request body. The ID from the URL path is trusted.

### Q: Is using one DTO per entity acceptable?
**A:** Yes, it exists in small projects, but separate DTOs per operation is enterprise best practice for security, clarity, and flexibility.

### Q: How to handle circular references in DTOs?
**A:** Use IDs instead of nested objects. Don't use @JsonIgnore on entities - use proper DTOs instead.

### Q: Is @JsonIgnore commonly used for circular dependencies?
**A:** Yes in tutorials/small projects, but it's a code smell. Proper solution is using DTOs with IDs.

---

## Files Created/Modified

### Created:
- `src/main/java/com/tutorial/taskmanager/dto/appuser/AppUserCreateDto.java`
- `src/main/java/com/tutorial/taskmanager/dto/appuser/AppUserUpdateDto.java`
- `src/main/java/com/tutorial/taskmanager/dto/appuser/AppUserResponseDto.java`
- `src/main/java/com/tutorial/taskmanager/dto/task/TaskCreateDto.java`
- `src/main/java/com/tutorial/taskmanager/dto/task/TaskUpdateDto.java`
- `src/main/java/com/tutorial/taskmanager/dto/task/TaskResponseDto.java`
- `src/main/java/com/tutorial/taskmanager/dto/project/ProjectCreateDto.java`
- `src/main/java/com/tutorial/taskmanager/dto/project/ProjectUpdateDto.java`
- `src/main/java/com/tutorial/taskmanager/dto/project/ProjectResponseDto.java`
- `src/test/java/com/tutorial/taskmanager/service/AppUserServiceTest.java`
- `docs/06-data-transfer-objects.md`
- `docs/16-unit-testing.md`

### Modified:
- `src/main/java/com/tutorial/taskmanager/service/AppUserService.java` (fully implemented)
- `src/main/java/com/tutorial/taskmanager/service/TaskService.java` (added TODOs)
- `src/main/java/com/tutorial/taskmanager/service/ProjectService.java` (added TODOs)
- `pom.xml` (added Surefire plugin configuration)
- `CLAUDE.md` (updated session progress)
- `docs/README.md` (marked completed topics)

---

## Test Results

```
Tests run: 41, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Time elapsed: ~1.3 seconds
```

**Coverage:** 100% of AppUserService methods

---

## Next Session Goals

1. **Implement TaskService**
   - Follow AppUserService pattern
   - Handle task-project relationships
   - Date-based queries (overdue, due between)
   - Write comprehensive unit tests

2. **Implement ProjectService**
   - Project name uniqueness per user
   - Status management
   - Search functionality
   - Write comprehensive unit tests

3. **Update Task/Project DTOs**
   - Add relationship fields (appUserId, projectId)
   - Consider summary DTOs for nested responses

4. **Consider Mapper Pattern**
   - Evaluate if mapper classes are needed
   - Look into MapStruct for auto-generation

---

## Takeaways

### What Went Well ✅
- Comprehensive DTO structure following enterprise patterns
- AppUserService fully implemented with excellent test coverage
- Thorough documentation created for future reference
- Learned industry-standard testing patterns
- Fixed Mockito configuration issues

### Challenges Faced 🔧
- Understanding why separate DTOs per operation (initially seemed redundant)
- Circular reference concept and solutions
- Mockito agent configuration for JDK 21+
- Update method logic (ensuring email uniqueness while allowing same email)

### Best Practices Reinforced 🎯
- Never expose entities directly to API
- Always use Optional instead of null
- Test edge cases thoroughly
- One test per behavior
- Clear, descriptive naming (methods and tests)
- Documentation as you build (not after)

---

## Resources Referenced

- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [AssertJ Documentation](https://assertj.github.io/doc/)
- Spring Petclinic (official Spring demo project)
- RESTful Web Services Cookbook (Richardson & Ruby)

---

**Session Duration:** Extended session (multiple topics covered)
**Lines of Code Written:** ~1,500+ (including tests and DTOs)
**Tests Written:** 41 (all passing)
**Documentation Pages:** 2 comprehensive guides
