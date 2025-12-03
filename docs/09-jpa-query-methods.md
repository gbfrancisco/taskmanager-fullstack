# 09 - Spring Data JPA Query Methods

This guide covers Spring Data JPA repositories, query method derivation, and performance optimization patterns used in the Task Manager application.

---

## Table of Contents
1. [What is Spring Data JPA?](#what-is-spring-data-jpa)
2. [Repository Pattern](#repository-pattern)
3. [Query Method Derivation](#query-method-derivation)
4. [Performance: Object vs ID-Based Queries](#performance-object-vs-id-based-queries)
5. [Query Method Keywords](#query-method-keywords)
6. [Return Types](#return-types)
7. [Parameter Naming](#parameter-naming)
8. [Best Practices](#best-practices)
9. [Common Pitfalls](#common-pitfalls)

---

## What is Spring Data JPA?

**Spring Data JPA** is a framework that eliminates boilerplate data access code by:
- Automatically generating repository implementations at runtime
- Deriving queries from method names
- Providing built-in CRUD operations
- Supporting pagination and sorting

### Without Spring Data JPA

```java
@Repository
public class TaskRepositoryImpl {

    @PersistenceContext
    private EntityManager entityManager;

    public List<Task> findByAppUser(AppUser appUser) {
        return entityManager
            .createQuery("SELECT t FROM Task t WHERE t.appUser = :appUser", Task.class)
            .setParameter("appUser", appUser)
            .getResultList();
    }

    public List<Task> findByStatus(TaskStatus status) {
        return entityManager
            .createQuery("SELECT t FROM Task t WHERE t.status = :status", Task.class)
            .setParameter("status", status)
            .getResultList();
    }

    // ... lots of boilerplate for each query
}
```

### With Spring Data JPA

```java
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // Spring generates implementation automatically!
    List<Task> findByAppUser(AppUser appUser);

    List<Task> findByStatus(TaskStatus status);

    // That's it - no implementation needed!
}
```

**Spring creates the implementation at runtime** - you just write the interface!

---

## Repository Pattern

### Repository Hierarchy

```
Repository (Spring Data interface)
    │
    └── CrudRepository (basic CRUD operations)
            │
            └── PagingAndSortingRepository (adds pagination/sorting)
                    │
                    └── JpaRepository (JPA-specific operations)
                            │
                            └── TaskRepository (your custom interface)
```

### Basic Repository Interface

```java
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    // JpaRepository provides (inherited for free):
    // - save(Task)
    // - findById(Long)
    // - findAll()
    // - delete(Task)
    // - count()
    // - existsById(Long)
    // And many more!
}
```

**Generic types:**
- `Task` - the entity type
- `Long` - the ID type (must match the entity's @Id field type)

---

## Query Method Derivation

Spring Data JPA **parses method names** to generate queries automatically.

### How It Works

```java
List<Task> findByAppUserAndStatus(AppUser appUser, TaskStatus status);
```

**Spring breaks down the method name:**
1. `find` - return results (can also use `get`, `read`, `query`, `stream`)
2. `By` - start of criteria
3. `AppUser` - field name in Task entity
4. `And` - logical operator
5. `Status` - another field name
6. Parameters map **by position** to fields in method name

**Generated SQL:**
```sql
SELECT * FROM TASK WHERE APP_USER_ID = ? AND STATUS = ?
```

### Basic Examples

```java
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // Find by single field
    List<Task> findByStatus(TaskStatus status);

    // Find by relationship
    List<Task> findByAppUser(AppUser appUser);
    List<Task> findByProject(Project project);

    // Combine multiple criteria with AND
    List<Task> findByAppUserAndStatus(AppUser appUser, TaskStatus status);

    // Date comparisons
    List<Task> findByDueDateBefore(LocalDateTime dateTimeToCompare);
    List<Task> findByDueDateBetween(LocalDateTime start, LocalDateTime end);

    // String matching
    List<Project> findByNameContainingIgnoreCase(String keyword);

    // Existence checks
    boolean existsByAppUserAndName(AppUser appUser, String name);
}
```

---

## Performance: Object vs ID-Based Queries

### The Problem

When you only have an ID, fetching the entire entity just to query with it is wasteful:

```java
// ❌ Inefficient - TWO database queries
Long userId = 1L;
AppUser user = appUserRepository.findById(userId).orElseThrow();  // Query 1
List<Task> tasks = taskRepository.findByAppUser(user);            // Query 2
```

### The Solution: ID-Based Queries

Spring Data JPA can navigate to relationship IDs using method names:

```java
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // Object-based (when you already have the AppUser loaded)
    List<Task> findByAppUser(AppUser appUser);

    // ID-based (when you only have the ID - MORE EFFICIENT!)
    List<Task> findByAppUserId(Long appUserId);
}
```

**Both generate the same SQL**, but the ID-based version avoids fetching AppUser first:

```java
// ✅ Efficient - ONE database query
Long userId = 1L;
List<Task> tasks = taskRepository.findByAppUserId(userId);  // Query 1 only!
```

### Naming Convention

Use **camelCase** (no underscores) to navigate to relationship IDs:

```java
// ✅ Correct - camelCase
List<Task> findByAppUserId(Long appUserId);
List<Task> findByProjectId(Long projectId);

// ❌ Don't use underscores (less readable)
List<Task> findByAppUser_Id(Long appUserId);
```

**Spring understands both formats**, but camelCase is cleaner.

### When to Use Each

| Scenario | Use | Example |
|----------|-----|---------|
| Already have entity loaded | Object-based | `findByAppUser(currentUser)` |
| Only have the ID | ID-based | `findByAppUserId(1L)` |
| Working with DTOs containing IDs | ID-based | `findByAppUserId(dto.getUserId())` |
| Need to access entity fields afterward | Object-based | May need entity for other purposes |

### Combined Filters

You can mix ID-based and other criteria:

```java
// ID-based with status filter
List<Task> findByAppUserIdAndStatus(Long appUserId, TaskStatus status);

// ID-based with date range
List<Task> findByProjectIdAndDueDateBetween(
    Long projectId,
    LocalDateTime start,
    LocalDateTime end
);
```

### Alternative: getReferenceById()

Spring Data JPA also provides `getReferenceById()` which returns a **lazy-loaded proxy**:

```java
// Get a proxy without hitting the database
AppUser userProxy = appUserRepository.getReferenceById(1L);  // No query yet
List<Task> tasks = taskRepository.findByAppUser(userProxy);  // Query 1

// vs ID-based (clearer and more explicit)
List<Task> tasks = taskRepository.findByAppUserId(1L);  // Query 1
```

**Recommendation:** Use ID-based methods for clarity and explicitness.

---

## Query Method Keywords

### Comparison Operators

| Keyword | Example | Generated SQL |
|---------|---------|---------------|
| `findBy{Field}` | `findByStatus(TaskStatus)` | `WHERE STATUS = ?` |
| `findBy{Field}Not` | `findByStatusNot(TaskStatus)` | `WHERE STATUS != ?` |
| `findBy{Field}LessThan` | `findByIdLessThan(Long)` | `WHERE ID < ?` |
| `findBy{Field}GreaterThan` | `findByIdGreaterThan(Long)` | `WHERE ID > ?` |
| `findBy{Field}Before` | `findByDueDateBefore(LocalDateTime)` | `WHERE DUE_DATE < ?` |
| `findBy{Field}After` | `findByDueDateAfter(LocalDateTime)` | `WHERE DUE_DATE > ?` |
| `findBy{Field}Between` | `findByDueDateBetween(LocalDateTime, LocalDateTime)` | `WHERE DUE_DATE BETWEEN ? AND ?` |

### String Matching

| Keyword | Example | Generated SQL |
|---------|---------|---------------|
| `{Field}Containing` | `findByNameContaining(String)` | `WHERE NAME LIKE '%' || ? || '%'` |
| `{Field}StartingWith` | `findByNameStartingWith(String)` | `WHERE NAME LIKE ? || '%'` |
| `{Field}EndingWith` | `findByNameEndingWith(String)` | `WHERE NAME LIKE '%' || ?` |
| `{Field}IgnoreCase` | `findByUsernameIgnoreCase(String)` | `WHERE LOWER(USERNAME) = LOWER(?)` |
| `{Field}ContainingIgnoreCase` | `findByNameContainingIgnoreCase(String)` | `WHERE LOWER(NAME) LIKE LOWER('%' || ? || '%')` |

### Logical Operators

| Keyword | Example | Generated SQL |
|---------|---------|---------------|
| `{Field1}And{Field2}` | `findByAppUserAndStatus(AppUser, TaskStatus)` | `WHERE APP_USER_ID = ? AND STATUS = ?` |
| `{Field1}Or{Field2}` | `findByStatusOrPriority(...)` | `WHERE STATUS = ? OR PRIORITY = ?` |

### Null Checks

| Keyword | Example | Generated SQL |
|---------|---------|---------------|
| `{Field}IsNull` | `findByDueDateIsNull()` | `WHERE DUE_DATE IS NULL` |
| `{Field}IsNotNull` | `findByDueDateIsNotNull()` | `WHERE DUE_DATE IS NOT NULL` |

### Collections

| Keyword | Example | Generated SQL |
|---------|---------|---------------|
| `{Field}In` | `findByStatusIn(Collection<TaskStatus>)` | `WHERE STATUS IN (?, ?, ?)` |
| `{Field}NotIn` | `findByStatusNotIn(Collection<TaskStatus>)` | `WHERE STATUS NOT IN (?, ?, ?)` |

### Other Operations

| Keyword | Return Type | Description |
|---------|-------------|-------------|
| `existsBy{Field}` | `boolean` | Returns true if entity exists (COUNT query) |
| `countBy{Field}` | `long` | Counts entities matching criteria |
| `deleteBy{Field}` | `void` or `long` | Deletes entities (returns count if long) |

---

## Return Types

### Single Results

```java
// Optional - recommended for single results that might not exist
Optional<AppUser> findByUsername(String username);

// Entity directly - throws exception if not found
AppUser getByUsername(String username);  // Use "get" prefix instead of "find"
```

### Multiple Results

```java
// List - most common for multiple results
List<Task> findByAppUser(AppUser appUser);

// Set - ensures uniqueness (rare)
Set<Task> findByStatus(TaskStatus status);

// Stream - for processing large datasets (remember to close!)
Stream<Task> streamByAppUser(AppUser appUser);
```

### Counts and Existence

```java
// Count entities
long countByStatus(TaskStatus status);

// Check existence (more efficient than find + isPresent)
boolean existsByUsername(String username);
```

### Pagination and Sorting

```java
// Pageable - combines pagination + sorting
Page<Task> findByAppUser(AppUser appUser, Pageable pageable);

// Usage:
Pageable pageable = PageRequest.of(0, 10, Sort.by("dueDate"));
Page<Task> page = taskRepository.findByAppUser(user, pageable);

// Dynamic sorting only
List<Task> findByAppUser(AppUser appUser, Sort sort);
```

---

## Parameter Naming

### IMPORTANT: Parameter Names Don't Matter!

Spring Data JPA **only looks at method names and parameter positions**, NOT parameter names.

```java
// ✅ All three are IDENTICAL to Spring:
List<Task> findByDueDateBefore(LocalDateTime dateTimeToCompare);
List<Task> findByDueDateBefore(LocalDateTime cutoffDate);
List<Task> findByDueDateBefore(LocalDateTime x);
```

**Spring maps parameters by POSITION:**
1. Method says `findByDueDateBefore` → expects 1 parameter
2. 1st parameter → maps to `dueDate` field
3. Parameter name (`dateTimeToCompare`, `cutoffDate`, `x`) is **ignored** by Spring

### Why Use Descriptive Names?

**For human readability!** While Spring doesn't care, **your teammates do**:

```java
// ❌ Compiles and works, but confusing to read
List<Task> findByAppUserAndDueDateBeforeAndStatusNotIn(
    AppUser a,
    LocalDateTime b,
    Collection<TaskStatus> c
);

// ✅ Clear and self-documenting
List<Task> findByAppUserAndDueDateBeforeAndStatusNotIn(
    AppUser appUser,
    LocalDateTime dateTimeToCompare,
    Collection<TaskStatus> excludedStatuses
);
```

### Parameter Order Matters

```java
// Method name dictates parameter order
List<Task> findByAppUserAndStatus(AppUser appUser, TaskStatus status);

// ❌ Wrong order - won't compile (type mismatch)
List<Task> findByAppUserAndStatus(TaskStatus status, AppUser appUser);
```

**Rule:** Parameter order must match the order of fields in the method name.

---

## Best Practices

### 1. Use Descriptive Method Names

```java
// ✅ Good - clear and specific
List<Task> findByAppUserAndStatusNotIn(AppUser appUser, Collection<TaskStatus> excludedStatuses);

// ❌ Bad - too vague
List<Task> getTasks(AppUser user, Collection<TaskStatus> statuses);
```

### 2. Prefer ID-Based Queries When Possible

```java
// ✅ More efficient when you only have IDs
List<Task> findByAppUserId(Long appUserId);

// ⚠️ Less efficient - requires fetching AppUser first
List<Task> findByAppUser(AppUser appUser);
```

### 3. Use Existence Checks Over Find + IsPresent

```java
// ✅ Efficient - generates COUNT query
boolean exists = userRepository.existsByUsername("john");

// ❌ Wasteful - fetches entire entity
boolean exists = userRepository.findByUsername("john").isPresent();
```

### 4. Keep Method Names Readable

```java
// ✅ Good - readable
List<Task> findByAppUserAndStatus(AppUser appUser, TaskStatus status);

// ❌ Too complex - use @Query instead
List<Task> findByAppUserAndStatusAndDueDateBetweenAndProjectNotNullOrderByDueDateAsc(...);
```

**Rule of thumb:** If method name becomes too long, use `@Query` annotation with JPQL.

### 5. Use Appropriate Return Types

- `Optional<T>` for single result that might not exist
- `List<T>` for multiple results (even if 0 expected)
- `boolean` for existence checks
- `long` for counts

### 6. Type Safety Matters

```java
// ✅ Correct - parameter type matches entity field type
List<Task> findByDueDateBefore(LocalDateTime dateTimeToCompare);

// ❌ Wrong - Task.dueDate is LocalDateTime, not LocalDate
List<Task> findByDueDateBefore(LocalDate date);  // Runtime error!
```

**Rule:** Method parameter types must **exactly match** entity field types.

---

## Common Pitfalls

### 1. Type Mismatch Between Entity Field and Parameter

```java
// Entity field type
@Entity
public class Task {
    private LocalDateTime dueDate;  // LocalDateTime!
}

// ❌ Wrong - type mismatch
List<Task> findByDueDateBefore(LocalDate date);  // Expects LocalDateTime!

// ✅ Correct
List<Task> findByDueDateBefore(LocalDateTime dateTimeToCompare);
```

### 2. Typos in Field Names

```java
// Entity field
@Entity
public class Task {
    private AppUser appUser;  // Field name is "appUser"
}

// ❌ Wrong - field name doesn't match
List<Task> findByUser(AppUser appUser);  // No field named "user"!

// ✅ Correct
List<Task> findByAppUser(AppUser appUser);
```

**Spring is case-sensitive!** Method names must match entity field names exactly.

### 3. Forgetting @Repository Annotation

```java
// ❌ Missing @Repository (works but not recommended)
public interface TaskRepository extends JpaRepository<Task, Long> {
}

// ✅ Include @Repository for clarity and exception translation
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
}
```

While Spring Data JPA scans for `JpaRepository` interfaces automatically, `@Repository` adds:
- Exception translation (SQLException → DataAccessException)
- Clarity of intent

### 4. Using Repositories Directly in Controllers

```java
// ❌ Bad - skip service layer
@RestController
public class TaskController {
    private final TaskRepository taskRepository;

    public TaskController(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;  // Don't do this!
    }
}

// ✅ Good - use service layer
@RestController
public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;  // Proper layering
    }
}
```

**Always use a service layer** between controllers and repositories.

### 5. Not Closing Streams

```java
// ❌ Resource leak
Stream<Task> stream = taskRepository.streamByAppUser(user);
// Process stream...
// Stream never closed!

// ✅ Use try-with-resources
try (Stream<Task> stream = taskRepository.streamByAppUser(user)) {
    // Process stream
    stream.forEach(task -> {
        // ... process task
    });
}  // Stream auto-closed
```

### 6. Assuming Parameter Names Matter

```java
// Common misconception: parameter names must match field names
// ❌ False assumption
List<Task> findByAppUser(AppUser user);  // Works fine, even though param is "user" not "appUser"

// Spring only cares about:
// 1. Method name (appUser field)
// 2. Parameter position (1st param)
// 3. Parameter TYPE (AppUser type)
```

---

## Summary

### Key Takeaways

✅ **Spring Data JPA eliminates boilerplate** - just write interfaces, Spring creates implementations

✅ **Method names are parsed** - Spring derives queries from method names automatically

✅ **ID-based queries are more efficient** - avoid extra queries when you only have IDs

✅ **Parameter types must match exactly** - entity field types and method parameter types must align

✅ **Parameter names don't matter to Spring** - they're for human readability only

✅ **Parameter position matters** - must match order in method name

✅ **Use @Repository** - adds exception translation and clarity

✅ **Keep method names readable** - use `@Query` for complex queries

### Decision Flow

```
Need to query data?
    │
    ├─ Simple query (1-2 fields)?
    │   └─ YES → Use query method derivation
    │
    ├─ Complex query (many fields, joins)?
    │   └─ YES → Use @Query with JPQL
    │
    ├─ Only have entity ID?
    │   └─ YES → Use ID-based method (findBy{Field}Id)
    │
    └─ Already have entity object?
        └─ YES → Use object-based method (findBy{Field})
```

---

## Related Files

- Repository interfaces: `src/main/java/com/tutorial/taskmanager/repository/`
  - `AppUserRepository.java`
  - `TaskRepository.java`
  - `ProjectRepository.java`
  - `PACKAGE-INFO.md` - Detailed repository documentation

- Entity models: `src/main/java/com/tutorial/taskmanager/model/`
  - `AppUser.java`
  - `Task.java`
  - `Project.java`

- Previous docs:
  - [07-jpa-entities.md](07-jpa-entities.md) - JPA entities and relationships
  - [02-dependency-injection.md](02-dependency-injection.md) - Dependency injection patterns

---

**Last Updated:** 2025-11-16
**Current Focus:** Spring Data JPA query method derivation, performance optimization with ID-based queries
