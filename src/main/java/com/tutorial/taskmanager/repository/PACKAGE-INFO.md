# Package: repository

## Overview

This package contains **Spring Data JPA repository interfaces** that provide database access for the Task Manager application. Repositories act as the **Data Access Layer (DAL)**, abstracting away SQL queries and JDBC boilerplate.

### What is Spring Data JPA?

Spring Data JPA is a framework that:
- **Eliminates boilerplate code** - no need to write SQL or JDBC
- **Generates implementations at runtime** - you write interfaces, Spring creates the implementation
- **Provides query derivation** - method names are parsed to generate queries
- **Supports pagination and sorting** - built-in support for large datasets
- **Offers type-safe queries** - compile-time checking instead of runtime SQL errors

### Package Structure

```
com.tutorial.taskmanager.repository/
├── AppUserRepository.java      # User data access (authentication, user management)
├── TaskRepository.java         # Task data access (CRUD, filtering, date queries)
├── ProjectRepository.java      # Project data access (appUser queries, status filtering)
└── PACKAGE-INFO.md            # This file - package documentation
```

---

## Repository Hierarchy

```
JpaRepository (Spring Data JPA interface)
    ├── Provides: save, findById, findAll, delete, count, etc.
    ├── Extends: PagingAndSortingRepository + CrudRepository
    └── Generic types: <EntityType, IdType>
        │
        ├─── AppUserRepository extends JpaRepository<AppUser, Long>
        │    ├── findByUsername(String) : Optional<AppUser>
        │    ├── findByEmail(String) : Optional<AppUser>
        │    ├── existsByUsername(String) : boolean
        │    └── existsByEmail(String) : boolean
        │
        ├─── TaskRepository extends JpaRepository<Task, Long>
        │    ├── findByAppUser(AppUser) : List<Task>
        │    ├── findByProject(Project) : List<Task>
        │    ├── findByStatus(TaskStatus) : List<Task>
        │    ├── findByAppUserAndStatus(AppUser, TaskStatus) : List<Task>
        │    ├── findByDueDateBefore(LocalDateTime) : List<Task>
        │    └── findByDueDateBetween(LocalDateTime, LocalDateTime) : List<Task>
        │
        └─── ProjectRepository extends JpaRepository<Project, Long>
             ├── findByAppUser(AppUser) : List<Project>
             ├── findByStatus(ProjectStatus) : List<Project>
             ├── findByAppUserAndStatus(AppUser, ProjectStatus) : List<Project>
             ├── findByName(String) : Optional<Project>
             ├── findByNameContainingIgnoreCase(String) : List<Project>
             └── existsByAppUserAndName(AppUser, String) : boolean
```

---

## File Descriptions

### 1. AppUserRepository.java
**Purpose**: Database operations for user authentication and management

**Key Features**:
- Find users by username or email (for login)
- Check username/email existence (for registration validation)
- Inherits CRUD operations from JpaRepository

**Common Usage**:
```java
// In a service class
Optional<AppUser> user = appUserRepository.findByUsername("john_doe");
boolean exists = appUserRepository.existsByEmail("john@example.com");
appUserRepository.save(newUser);  // Inherited from JpaRepository
```

### 2. TaskRepository.java
**Purpose**: Database operations for task management with advanced filtering

**Key Features**:
- Find tasks by user (all tasks assigned to a user)
- Find tasks by project (all tasks in a project)
- Find tasks by status (TODO, IN_PROGRESS, COMPLETED, etc.)
- Combine filters (e.g., user + status)
- Date-based queries (overdue tasks, tasks due this week)

**Common Usage**:
```java
// In a service class
List<Task> userTasks = taskRepository.findByAppUser(currentUser);
List<Task> todoTasks = taskRepository.findByStatus(TaskStatus.TODO);
List<Task> overdueTasks = taskRepository.findByDueDateBefore(LocalDateTime.now());
```

### 3. ProjectRepository.java
**Purpose**: Database operations for project management

**Key Features**:
- Find projects by appUser (all projects belonging to a user)
- Find projects by status (ACTIVE, COMPLETED, etc.)
- Search projects by name (exact or partial match)
- Existence checks (prevent duplicate names per user)

**Common Usage**:
```java
// In a service class
List<Project> myProjects = projectRepository.findByAppUser(currentUser);
List<Project> results = projectRepository.findByNameContainingIgnoreCase("mobile");
boolean exists = projectRepository.existsByAppUserAndName(user, "New Project");
```

---

## How Spring Data JPA Works

### 1. Repository Scanning
At application startup:
1. Spring scans for interfaces extending `Repository` or its subinterfaces
2. For each interface, Spring creates a **proxy implementation** at runtime
3. The `@Repository` annotation (optional but recommended) enables:
   - Exception translation (SQLException → DataAccessException)
   - Component scanning (makes it a Spring bean)

### 2. Query Derivation
Spring parses method names to generate queries:

```java
// Method name: findByUsername
// Spring generates: SELECT * FROM app_users WHERE username = ?

// Method name: findByUserAndStatus
// Spring generates: SELECT * FROM tasks WHERE user_id = ? AND status = ?

// Method name: findByNameContainingIgnoreCase
// Spring generates: SELECT * FROM projects WHERE LOWER(name) LIKE LOWER('%' || ? || '%')
```

### 3. Runtime Proxy Creation
```
Your code:                         Spring Data JPA:
UserService                        UserRepository (interface)
    |                                     |
    | Constructor injection               | At startup, Spring creates:
    v                                     v
userRepository.findByUsername("john")   → SimpleJpaRepository (proxy implementation)
                                              |
                                              v
                                          EntityManager (JPA)
                                              |
                                              v
                                          JDBC → Database
```

---

## Query Method Keywords

### Comparison Operators
| Keyword | Example | Generated SQL |
|---------|---------|---------------|
| `findBy{Field}` | `findByUsername(String)` | `WHERE username = ?` |
| `findBy{Field}Not` | `findByStatusNot(TaskStatus)` | `WHERE status != ?` |
| `findBy{Field}LessThan` | `findByIdLessThan(Long)` | `WHERE id < ?` |
| `findBy{Field}GreaterThan` | `findByIdGreaterThan(Long)` | `WHERE id > ?` |
| `findBy{Field}Before` | `findByDueDateBefore(LocalDate)` | `WHERE due_date < ?` |
| `findBy{Field}After` | `findByDueDateAfter(LocalDate)` | `WHERE due_date > ?` |
| `findBy{Field}Between` | `findByDueDateBetween(LocalDate, LocalDate)` | `WHERE due_date BETWEEN ? AND ?` |

### String Matching
| Keyword | Example | Generated SQL |
|---------|---------|---------------|
| `findBy{Field}Containing` | `findByNameContaining(String)` | `WHERE name LIKE '%'` || ? || '%'` |
| `findBy{Field}StartingWith` | `findByNameStartingWith(String)` | `WHERE name LIKE ?` || '%'` |
| `findBy{Field}EndingWith` | `findByNameEndingWith(String)` | `WHERE name LIKE '%'` || ?` |
| `findBy{Field}IgnoreCase` | `findByUsernameIgnoreCase(String)` | `WHERE LOWER(username) = LOWER(?)` |

### Logical Operators
| Keyword | Example | Generated SQL |
|---------|---------|---------------|
| `findBy{Field1}And{Field2}` | `findByUserAndStatus(AppUser, TaskStatus)` | `WHERE user_id = ? AND status = ?` |
| `findBy{Field1}Or{Field2}` | `findByStatusOrPriority(...)` | `WHERE status = ? OR priority = ?` |

### Null Checks
| Keyword | Example | Generated SQL |
|---------|---------|---------------|
| `findBy{Field}IsNull` | `findByDueDateIsNull()` | `WHERE due_date IS NULL` |
| `findBy{Field}IsNotNull` | `findByDueDateIsNotNull()` | `WHERE due_date IS NOT NULL` |

### Collections
| Keyword | Example | Generated SQL |
|---------|---------|---------------|
| `findBy{Field}In` | `findByStatusIn(Collection<TaskStatus>)` | `WHERE status IN (?, ?, ?)` |
| `findBy{Field}NotIn` | `findByStatusNotIn(Collection<TaskStatus>)` | `WHERE status NOT IN (?, ?, ?)` |

### Other Operations
| Keyword | Return Type | Description |
|---------|-------------|-------------|
| `existsBy{Field}` | `boolean` | Returns true if entity exists (generates COUNT query) |
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
List<Task> findByUser(AppUser user);

// Set - ensures uniqueness (rare, use only when needed)
Set<Task> findByStatus(TaskStatus status);

// Stream - for processing large datasets efficiently
Stream<Task> streamByUser(AppUser user);  // Remember to close!
```

### Counts and Existence
```java
// Count
long countByStatus(TaskStatus status);

// Existence check (more efficient than find + isPresent)
boolean existsByUsername(String username);
```

---

## Design Patterns

### 1. Repository Pattern
**What**: Separates data access logic from business logic
**Why**:
- Testability (easy to mock repositories)
- Maintainability (change database without affecting services)
- Single Responsibility Principle (repositories only handle data access)

**Example**:
```java
@Service
public class TaskService {
    private final TaskRepository taskRepository;

    // Constructor injection (recommended - no @Autowired needed)
    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public List<Task> getOverdueTasks() {
        return taskRepository.findByDueDateBefore(LocalDate.now());
    }
}
```

### 2. Interface Segregation
**What**: Each repository is specific to one entity
**Why**:
- Clear separation of concerns
- Easier to understand and maintain
- Type safety (can't accidentally query wrong entity)

### 3. Query Derivation
**What**: Method names are parsed to generate queries
**Why**:
- No SQL/JPQL needed for simple queries
- Compile-time checking (typos in method names cause errors)
- Refactoring support (rename fields → method names update)

---

## Common Patterns

### 1. Relationship Navigation
```java
// Task has @ManyToOne AppUser user
List<Task> findByUser(AppUser user);

// Navigate through relationship to user's field
List<Task> findByUser_Username(String username);  // Underscore for nested property
```

### 2. Combining Multiple Criteria
```java
// AND condition
List<Task> findByUserAndStatus(AppUser user, TaskStatus status);

// OR condition
List<Task> findByStatusOrPriority(TaskStatus status, Priority priority);
```

### 3. Sorting Results
```java
// Static sorting (part of method name)
List<Task> findByUserOrderByDueDateAsc(AppUser user);

// Dynamic sorting (parameter)
List<Task> findByUser(AppUser user, Sort sort);
// Call with: Sort.by(Sort.Direction.DESC, "createdTimestamp")
```

### 4. Pagination
```java
// Pageable parameter (includes sorting + pagination)
Page<Task> findByUser(AppUser user, Pageable pageable);

// Call with:
Pageable pageable = PageRequest.of(0, 10, Sort.by("dueDate"));
Page<Task> page = taskRepository.findByUser(user, pageable);
```

---

## Best Practices

### ✅ DO

1. **Use Optional for single results**
   ```java
   Optional<AppUser> findByUsername(String username);
   ```

2. **Use existence checks instead of find + isPresent**
   ```java
   // Good
   boolean exists = userRepository.existsByUsername("john");

   // Wasteful (fetches entire entity just to check existence)
   boolean exists = userRepository.findByUsername("john").isPresent();
   ```

3. **Keep method names readable**
   ```java
   // Good
   findByUserAndStatus(AppUser user, TaskStatus status)

   // Too complex - use @Query instead
   findByUserAndStatusAndDueDateBetweenAndProjectNotNull(...)
   ```

4. **Use appropriate return types**
   - `List<T>` for multiple results (even if 0 expected)
   - `Optional<T>` for single result that might not exist
   - `boolean` for existence checks
   - `long` for counts

### ❌ DON'T

1. **Don't use repositories directly in controllers**
   ```java
   // Bad - skips service layer
   @RestController
   public class TaskController {
       private final TaskRepository taskRepository;

       public TaskController(TaskRepository taskRepository) {
           this.taskRepository = taskRepository;  // ❌ Skip service layer
       }
   }

   // Good - uses service layer
   @RestController
   public class TaskController {
       private final TaskService taskService;

       public TaskController(TaskService taskService) {
           this.taskService = taskService;  // ✅ Use service layer
       }
   }
   ```

2. **Don't over-complicate query methods**
   ```java
   // Too complex - use @Query with JPQL instead
   findByUserAndStatusAndDueDateBetweenAndProjectNotNullOrderByDueDateAsc(...)
   ```

3. **Don't use field names that don't exist**
   ```java
   // ❌ Error: Task entity has no field "userName"
   List<Task> findByUserName(String name);

   // ✅ Correct: Navigate through relationship
   List<Task> findByUser_Username(String username);
   ```

4. **Don't forget to close Streams**
   ```java
   // ❌ Resource leak
   Stream<Task> stream = taskRepository.streamByUser(user);

   // ✅ Use try-with-resources
   try (Stream<Task> stream = taskRepository.streamByUser(user)) {
       // Process stream
   }
   ```

---

## Common Questions

### Q1: Do I need to implement repository interfaces?
**A**: No! Spring Data JPA creates the implementation at runtime. You only write the interface.

### Q2: What's the difference between `@Repository` and no annotation?
**A**: Spring Data JPA works without `@Repository`, but it's recommended because:
- Enables exception translation (SQLException → DataAccessException)
- Makes the intent clear (this is a repository)
- Supports component scanning in some configurations

### Q3: When should I use `@Query` instead of method names?
**A**: Use `@Query` when:
- Query is too complex for method names
- You need custom JPQL or native SQL
- You need JOINs, GROUP BY, or aggregate functions
- You want to optimize queries (e.g., JOIN FETCH)

**Example**:
```java
@Query("SELECT t FROM Task t JOIN FETCH t.user WHERE t.status = :status")
List<Task> findByStatusWithUser(@Param("status") TaskStatus status);
```

### Q4: What's the difference between `find`, `get`, and `read`?
**A**: All work the same way in Spring Data JPA:
- `findByUsername` - most common, returns Optional
- `getByUsername` - same behavior, different naming preference
- `readByUsername` - same behavior, less common

### Q5: How do I handle relationships (LAZY loading)?
**A**: Three approaches:
1. **Accept lazy loading** - access relationships in transaction
2. **Use @EntityGraph** - fetch relationships eagerly for specific queries
3. **Use @Query with JOIN FETCH** - control exactly what's loaded

**Example**:
```java
@EntityGraph(attributePaths = {"user", "project"})
List<Task> findByStatus(TaskStatus status);
```

### Q6: Can I use query methods in transactions?
**A**: Yes! Repository methods are transactional by default (read-only for queries).

### Q7: How do I debug generated queries?
**A**: Enable SQL logging in `application.yml`:
```yaml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

---

## Key Takeaways

1. **Repositories are interfaces** - Spring creates implementations at runtime
2. **Method names matter** - they're parsed to generate SQL queries
3. **Type safety** - compile-time checking prevents many bugs
4. **No boilerplate** - no SQL, no JDBC, no manual mapping
5. **Use services** - don't inject repositories directly into controllers
6. **Start simple** - use query derivation first, `@Query` only when needed
7. **Test repositories** - use `@DataJpaTest` for repository slice tests

---

**Related Documentation**:
- Entity model: `../model/PACKAGE-INFO.md`
- Spring Data JPA guide: `../../docs/07-spring-data-jpa.md`
- Service layer: `../service/PACKAGE-INFO.md` (coming next)

**Next Steps**:
1. Implement the TODO methods in each repository interface
2. Write repository tests with `@DataJpaTest`
3. Move on to creating the service layer
