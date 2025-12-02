# 07 - Spring Data JPA

This guide covers JPA (Java Persistence API) entities, relationships, and the foundational patterns used in the Task Manager application.

---

## Table of Contents
1. [What is JPA?](#what-is-jpa)
2. [Entities Overview](#entities-overview)
3. [BaseEntity Pattern](#baseentity-pattern)
4. [Entity Relationships](#entity-relationships)
5. [JPA Annotations Reference](#jpa-annotations-reference)
6. [Common Patterns](#common-patterns)
   - [Hibernate 6 Proxy Behavior and ID Access](#5-hibernate-6-proxy-behavior-and-id-access)
7. [Best Practices](#best-practices)
8. [Common Pitfalls](#common-pitfalls)

---

## What is JPA?

**JPA (Java Persistence API)** is a specification for object-relational mapping (ORM) in Java. It allows you to:
- Map Java classes to database tables
- Map Java fields to database columns
- Define relationships between entities
- Query databases using objects instead of SQL

**Hibernate** is the most popular JPA implementation (used by Spring Boot by default).

### Why Use JPA?

**Without JPA (raw JDBC):**
```java
String sql = "INSERT INTO users (username, email) VALUES (?, ?)";
PreparedStatement stmt = connection.prepareStatement(sql);
stmt.setString(1, "john");
stmt.setString(2, "john@example.com");
stmt.executeUpdate();
```

**With JPA:**
```java
User user = new User();
user.setUsername("john");
user.setEmail("john@example.com");
userRepository.save(user); // That's it!
```

**Benefits:**
- Less boilerplate code
- Database independence (works with MySQL, PostgreSQL, H2, etc.)
- Type safety
- Automatic SQL generation
- Built-in caching

---

## Entities Overview

Our Task Manager application has three core entities:

### Entity Relationship Diagram

```
┌─────────────────┐
│    AppUser      │
│─────────────────│
│ id (PK)         │
│ username        │
│ email           │
│ password        │
└────────┬────────┘
         │ 1
         │
         │ owns
         │
         │ *
    ┌────┴─────────────────┐
    │                      │
    ▼                      ▼
┌─────────────────┐   ┌─────────────────┐
│    Project      │   │      Task       │
│─────────────────│   │─────────────────│
│ id (PK)         │   │ id (PK)         │
│ name            │ 1 │ title           │
│ description     ├───┤ description     │
│ status          │ * │ status          │
│ appUser (FK)    │   │ dueDate         │
└─────────────────┘   │ appUser (FK)    │
                      │ project (FK)    │
                      └─────────────────┘
```

### File Locations

- **Entities**: `src/main/java/com/tutorial/taskmanager/model/`
  - `BaseEntity.java` - Base class with common fields
  - `AppUser.java` - User entity
  - `Project.java` - Project entity
  - `Task.java` - Task entity

- **Enums**: `src/main/java/com/tutorial/taskmanager/enums/`
  - `TaskStatus.java`
  - `ProjectStatus.java`

- **Constants**: `src/main/java/com/tutorial/taskmanager/constants/`
  - `DatabaseTableConstants.java`

---

## BaseEntity Pattern

### Purpose

The `BaseEntity` pattern is an enterprise-level approach to avoid repeating common fields across all entities.

### Implementation

**`BaseEntity.java`:**
```java
@Getter
@Setter
@MappedSuperclass
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class BaseEntity {
    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdTimestamp;

    @Column(nullable = false)
    private LocalDateTime updatedTimestamp;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdTimestamp = now;
        this.updatedTimestamp = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedTimestamp = LocalDateTime.now();
    }
}
```

### Key Annotations Explained

#### `@MappedSuperclass`
- Tells JPA this is a base class for entities
- Fields are inherited by child entities
- **No separate table** is created for `BaseEntity`
- Each child entity's table includes these fields

**Example:**
```sql
-- No BaseEntity table!

-- Task table includes inherited fields:
CREATE TABLE task (
    task_id BIGINT PRIMARY KEY,
    title VARCHAR(200),
    created_timestamp TIMESTAMP,  -- from BaseEntity
    updated_timestamp TIMESTAMP   -- from BaseEntity
);
```

#### `@PrePersist` and `@PreUpdate`
JPA lifecycle callbacks that execute automatically:

- `@PrePersist`: Called **before** entity is first saved to database
- `@PreUpdate`: Called **before** entity is updated in database

**Usage:**
```java
Task task = new Task();
task.setTitle("My Task");
repository.save(task);
// @PrePersist fires automatically
// createdTimestamp and updatedTimestamp are set
```

#### `@EqualsAndHashCode(onlyExplicitlyIncluded = true)`
Configures Lombok to only use fields marked with `@EqualsAndHashCode.Include`.

**Why ID-only equality?**
- Two entities with the same ID are the **same entity**
- Prevents lazy loading issues (accessing relationships triggers DB queries)
- Follows JPA best practices

**Example:**
```java
Task t1 = new Task();
t1.setId(1L);
t1.setTitle("Task 1");

Task t2 = new Task();
t2.setId(1L);
t2.setTitle("Different Title");

t1.equals(t2); // TRUE - same ID = same entity
```

### `@AttributeOverride` Pattern

Child entities customize the inherited `id` column name:

**`Task.java`:**
```java
@Entity
@Table(name = DatabaseTableConstants.TASK_TABLE)
@AttributeOverride(
    name = "id",  // Field name in BaseEntity
    column = @Column(name = DatabaseTableConstants.TASK_ID_COLUMN)
)
public class Task extends BaseEntity {
    // Now the ID column is named "task_id" instead of "id"
}
```

**Result:**
```sql
CREATE TABLE task (
    task_id BIGINT PRIMARY KEY,  -- customized via @AttributeOverride
    title VARCHAR(200),
    ...
);
```

**Benefits:**
- Generic `id` field in BaseEntity
- Custom column names per table (`task_id`, `project_id`, `app_user_id`)
- Maintains clarity in database schema

---

## Entity Relationships

### One-to-Many: AppUser → Tasks

**Owner side (AppUser):**
```java
@OneToMany(mappedBy = "appUser", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Task> tasks = new ArrayList<>();
```

**Inverse side (Task):**
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = DatabaseTableConstants.APP_USER_ID_COLUMN)
private AppUser appUser;
```

#### Bidirectional Relationship Explained

**`mappedBy = "appUser"`:**
- Indicates this is the **inverse** side of the relationship
- The `Task.appUser` field is the **owner** (has the foreign key)
- AppUser just "mirrors" the relationship for convenience

**`@JoinColumn(name = "APP_USER_ID")`:**
- Specifies the foreign key column name in the `task` table
- This side **owns** the relationship (manages the FK)

**Database schema:**
```sql
CREATE TABLE task (
    task_id BIGINT PRIMARY KEY,
    app_user_id BIGINT,  -- Foreign key
    FOREIGN KEY (app_user_id) REFERENCES app_user(app_user_id)
);
```

### Cascade Operations

**`cascade = CascadeType.ALL`:**
Propagates operations from parent to children:

| Operation | What Happens |
|-----------|--------------|
| `PERSIST` | Saving user also saves their tasks |
| `MERGE` | Updating user also updates their tasks |
| `REMOVE` | **Deleting user deletes all their tasks** |
| `REFRESH` | Refreshing user refreshes their tasks |
| `DETACH` | Detaching user detaches their tasks |
| `ALL` | All of the above |

**Example:**
```java
AppUser user = new AppUser();
user.setUsername("john");

Task task = new Task();
task.setTitle("Task 1");
task.setAppUser(user);
user.getTasks().add(task);

userRepository.save(user);
// Both user AND task are saved (cascade = PERSIST)

userRepository.delete(user);
// Both user AND all tasks are deleted (cascade = REMOVE)
```

**`orphanRemoval = true`:**
Deletes child entities when removed from the collection:

```java
AppUser user = userRepository.findById(1L);
Task task = user.getTasks().get(0);

user.getTasks().remove(task); // Remove from collection
userRepository.save(user);
// Task is deleted from database (orphanRemoval)
```

**Warning:** Consider if you want this behavior! In real-world apps, you might want to keep tasks for audit/history purposes.

### Fetch Strategies

#### `FetchType.LAZY` (Default for @ManyToOne)
```java
@ManyToOne(fetch = FetchType.LAZY)
private AppUser appUser;
```

**How it works:**
- Entity is loaded as a **proxy** (placeholder)
- Actual data is fetched **only when accessed**
- Prevents unnecessary database queries

**Example:**
```java
Task task = taskRepository.findById(1L);
// SQL: SELECT * FROM task WHERE task_id = 1
// appUser is NOT loaded yet (just a proxy)

String username = task.getAppUser().getUsername();
// NOW it fetches: SELECT * FROM app_user WHERE app_user_id = ?
```

**Benefits:**
- Better performance (don't load what you don't need)
- Avoids N+1 query problem in some cases

**Pitfall: LazyInitializationException**
```java
Task task = taskRepository.findById(1L);
// Hibernate session closes here

String username = task.getAppUser().getUsername();
// Exception! Session is closed, can't fetch lazy data
```

**Solutions:**
1. Use `@Transactional` to keep session open
2. Fetch join in query: `JOIN FETCH t.appUser`
3. Use DTOs and load only what you need

#### `FetchType.EAGER` (Default for @ManyToOne, @OneToOne)
```java
@ManyToOne(fetch = FetchType.EAGER)
private AppUser appUser;
```

**How it works:**
- Always loads the relationship immediately
- Single query with JOIN

**When to use:**
- Relationship is almost always needed
- Small, frequently-accessed data

**Warning:** Can cause performance issues if overused (loads too much data).

---

## JPA Annotations Reference

### Entity-Level Annotations

| Annotation | Purpose | Example |
|------------|---------|---------|
| `@Entity` | Marks class as JPA entity | `@Entity public class Task {}` |
| `@Table(name = "...")` | Specifies table name | `@Table(name = "TASK")` |
| `@MappedSuperclass` | Base class for entities (no table) | Used in `BaseEntity` |

### Field-Level Annotations

| Annotation | Purpose | Example |
|------------|---------|---------|
| `@Id` | Primary key | `@Id private Long id;` |
| `@GeneratedValue` | Auto-generate PK value | `@GeneratedValue(strategy = IDENTITY)` |
| `@Column` | Customize column mapping | `@Column(nullable = false, length = 100)` |
| `@Enumerated` | Map enum to DB | `@Enumerated(EnumType.STRING)` |
| `@Transient` | Exclude field from DB | `@Transient private String temp;` |

### Relationship Annotations

| Annotation | Cardinality | Example |
|------------|-------------|---------|
| `@OneToOne` | 1:1 | User ↔ UserProfile |
| `@OneToMany` | 1:* | User → Tasks |
| `@ManyToOne` | *:1 | Task → User |
| `@ManyToMany` | *:* | Student ↔ Course |

### Lifecycle Callbacks

| Annotation | When Called |
|------------|-------------|
| `@PrePersist` | Before INSERT |
| `@PostPersist` | After INSERT |
| `@PreUpdate` | Before UPDATE |
| `@PostUpdate` | After UPDATE |
| `@PreRemove` | Before DELETE |
| `@PostRemove` | After DELETE |
| `@PostLoad` | After entity loaded from DB |

---

## Common Patterns

### 1. Enum Storage

**Always use `EnumType.STRING`:**
```java
@Enumerated(EnumType.STRING)
@Column(nullable = false)
private TaskStatus status = TaskStatus.TODO;
```

**Database:**
```sql
-- Stored as: 'TODO', 'IN_PROGRESS', 'COMPLETED'
-- NOT as: 0, 1, 2 (EnumType.ORDINAL)
```

**Why STRING over ORDINAL?**
- **Readable** in database
- **Safe** - adding enum values in the middle doesn't break data
- **Migration-friendly**

### 2. Constants for Table/Column Names

**`DatabaseTableConstants.java`:**
```java
@UtilityClass
public class DatabaseTableConstants {
    public static final String TASK_TABLE = "TASK";
    public static final String TASK_ID_COLUMN = "TASK_ID";
}
```

**Benefits:**
- Single source of truth
- Refactoring-friendly
- Type-safe (no typos)

### 3. Default Values

**With `@Builder`:**
```java
@Builder.Default
@Enumerated(EnumType.STRING)
private TaskStatus status = TaskStatus.TODO;
```

**Without `@Builder.Default`:**
```java
Task task = Task.builder().title("My Task").build();
task.getStatus(); // null! Default value ignored by builder
```

**With `@Builder.Default`:**
```java
Task task = Task.builder().title("My Task").build();
task.getStatus(); // TaskStatus.TODO ✓
```

### 4. List Initialization

**Always initialize collections:**
```java
@OneToMany(mappedBy = "appUser")
private List<Task> tasks = new ArrayList<>();
```

**Why?**
```java
AppUser user = new AppUser();
user.getTasks().add(task); // NullPointerException without initialization!
```

### 5. Hibernate 6 Proxy Behavior and ID Access

When working with lazy relationships, you often need to compare entities by their owner (e.g., "does this task belong to the same user as this project?"). Understanding Hibernate's proxy behavior is crucial for performance.

#### The Problem

```java
// In TaskService.assignToProject()
if (!Objects.equals(task.getAppUser(), project.getAppUser())) {
    throw new ValidationException("Project does not belong to the same user");
}
```

This code compares two `AppUser` entities. But both are **lazy proxies** - does this trigger database queries?

#### Hibernate Behavior (5.2.12+ and 6.x)

**Good news:** Since Hibernate 5.2.12 (and all of Hibernate 6.x), calling `getId()` on a proxy does **NOT** trigger initialization by default - even when using field access.

```java
// These do NOT trigger SQL queries (Hibernate 5.2.12+):
Long userId1 = task.getAppUser().getId();      // No SQL
Long userId2 = project.getAppUser().getId();   // No SQL

// This DOES trigger SQL (accessing non-ID property):
String username = task.getAppUser().getUsername();  // SELECT * FROM app_user...
```

**Why?** The proxy already holds the ID value (from the foreign key column). Modern Hibernate is smart enough to return it without loading the full entity.

**Historical note:** Before Hibernate 5.2.12, with field access (annotations on fields like `@Id private Long id`), calling `getId()` **would** trigger initialization. This was fixed in [HHH-3718](https://hibernate.atlassian.net/browse/HHH-3718).

#### The Catch: `equals()` Still Triggers Loading

Even though `getId()` is safe, comparing entities with `equals()` or `Objects.equals()` triggers initialization:

```java
// This TRIGGERS lazy loading (calls proxy's equals method):
Objects.equals(task.getAppUser(), project.getAppUser())  // 2 SQL queries!

// This does NOT trigger loading (compares IDs only):
Objects.equals(task.getAppUser().getId(), project.getAppUser().getId())  // No SQL!
```

#### Best Practice: Compare IDs, Not Entities

```java
// ❌ AVOID - triggers lazy loading
if (!Objects.equals(task.getAppUser(), project.getAppUser())) {
    throw new ValidationException("...");
}

// ✅ PREFERRED - no lazy loading
if (!Objects.equals(task.getAppUser().getId(), project.getAppUser().getId())) {
    throw new ValidationException("...");
}
```

#### Alternative: Read-Only ID Field

Some codebases add a separate read-only ID field for the foreign key:

```java
@Entity
public class Task extends BaseEntity {

    // Read-only FK column (same column, mapped twice)
    @Column(name = "APP_USER_ID", insertable = false, updatable = false)
    private Long appUserId;

    // The actual relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "APP_USER_ID")
    private AppUser appUser;

    // Getter for direct ID access
    public Long getAppUserId() {
        return appUserId;
    }
}
```

**Usage:**
```java
// Direct ID access - guaranteed no proxy interaction
if (!Objects.equals(task.getAppUserId(), project.getAppUserId())) {
    throw new ValidationException("...");
}
```

**Pros:**
- Explicit and clear intent
- Works regardless of Hibernate version
- No proxy interaction at all

**Cons:**
- Duplication (two fields for same data)
- MapStruct may need `@Mapping(target = "appUserId", ignore = true)` to avoid conflicts
- More fields to maintain

**Recommendation:** In Hibernate 5.2.12+ or Hibernate 6 (Spring Boot 2.1+ or 3.x), prefer the simpler approach of calling `proxy.getId()`. Only use the read-only ID field pattern if you need backwards compatibility with very old Hibernate versions (pre-5.2.12) or want to be extra explicit.

#### Verifying Proxy Behavior

You can test this behavior in your project:

```java
@DataJpaTest
class HibernateProxyBehaviorTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    void getIdOnProxyShouldNotTriggerInitialization() {
        // Create test data...
        entityManager.flush();
        entityManager.clear();

        // Get a proxy
        AppUser proxy = entityManager.getReference(AppUser.class, userId);
        assertThat(Hibernate.isInitialized(proxy)).isFalse();

        // Access ID
        Long id = proxy.getId();

        // Proxy should STILL be uninitialized
        assertThat(Hibernate.isInitialized(proxy)).isFalse();  // Passes in Hibernate 5.2.12+!
    }
}
```

See `src/test/java/com/tutorial/taskmanager/proxy/HibernateProxyBehaviorTest.java` for complete test examples.

#### Configuration Note

This behavior is controlled by `hibernate.jpa.proxy.compliance`:
- `false` (default): `getId()` does NOT initialize proxy (Hibernate's traditional behavior)
- `true`: `getId()` DOES initialize proxy (strict JPA compliance, not recommended)

Spring Boot 3 with Hibernate 6 uses the default (`false`), so you get the optimized behavior out of the box.

---

## Best Practices

### 1. ID-Only Equality for Entities

**✅ CORRECT:**
```java
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@MappedSuperclass
public abstract class BaseEntity {
    @Id
    @EqualsAndHashCode.Include
    private Long id;
}
```

**❌ WRONG:**
```java
@EqualsAndHashCode // Uses ALL fields
public class Task {
    @ManyToOne(fetch = LAZY)
    private AppUser appUser; // Triggers lazy loading in equals()!
}
```

### 2. Prefer LAZY Fetching

**✅ CORRECT:**
```java
@ManyToOne(fetch = FetchType.LAZY)
private AppUser appUser;
```

**❌ AVOID:**
```java
@ManyToOne(fetch = FetchType.EAGER) // Loads everything always
private AppUser appUser;
```

### 3. Compare Proxy IDs, Not Entities

When comparing lazy relationships, compare IDs to avoid unnecessary database queries:

**✅ CORRECT:**
```java
// No lazy loading - getId() is safe in Hibernate 6
if (!Objects.equals(task.getAppUser().getId(), project.getAppUser().getId())) {
    throw new ValidationException("...");
}
```

**❌ AVOID:**
```java
// Triggers lazy loading via equals()
if (!Objects.equals(task.getAppUser(), project.getAppUser())) {
    throw new ValidationException("...");
}
```

See [Hibernate 6 Proxy Behavior and ID Access](#5-hibernate-6-proxy-behavior-and-id-access) for details.

### 4. Use Validation Constraints

```java
@Column(nullable = false, unique = true, length = 50)
private String username;

@Column(columnDefinition = "TEXT")
private String description;
```

### 5. Timestamp Management

**Use lifecycle callbacks:**
```java
@PrePersist
public void prePersist() {
    LocalDateTime now = LocalDateTime.now();
    this.createdTimestamp = now;
    this.updatedTimestamp = now;
}
```

**Why?** Ensures consistency and removes manual timestamp management.

### 6. Avoid Bidirectional Relationships Unless Needed

**Only add the reverse side if you actually need it:**
```java
// If you only navigate Task → User, don't add User → tasks
```

### 7. Be Careful with Cascade

**Think about the business logic:**
- Should deleting a user delete all their tasks?
- Or should tasks be reassigned/archived?

```java
// Aggressive cascading (use with caution)
@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
private List<Task> tasks;

// Conservative approach (handle in service layer)
@OneToMany(mappedBy = "appUser")
private List<Task> tasks;
```

---

## Common Pitfalls

### 1. LazyInitializationException

**Problem:**
```java
Task task = taskRepository.findById(1L);
// Transaction/session ends here

String username = task.getAppUser().getUsername();
// Exception! Can't load lazy relationship outside transaction
```

**Solutions:**

**A. Use `@Transactional` in service:**
```java
@Service
public class TaskService {
    @Transactional
    public Task getTaskWithUser(Long id) {
        Task task = taskRepository.findById(id).orElseThrow();
        task.getAppUser().getUsername(); // Forces load within transaction
        return task;
    }
}
```

**B. Fetch join in repository:**
```java
@Query("SELECT t FROM Task t JOIN FETCH t.appUser WHERE t.id = :id")
Task findByIdWithUser(@Param("id") Long id);
```

**C. Use DTOs:**
```java
@Query("SELECT new com.tutorial.taskmanager.dto.TaskDTO(t.id, t.title, u.username) " +
       "FROM Task t JOIN t.appUser u WHERE t.id = :id")
TaskDTO findTaskDTO(@Param("id") Long id);
```

### 2. N+1 Query Problem

**Problem:**
```java
List<Task> tasks = taskRepository.findAll();
for (Task task : tasks) {
    System.out.println(task.getAppUser().getUsername());
    // Each iteration triggers a SELECT! (N+1 queries)
}
```

**SQL executed:**
```sql
SELECT * FROM task;           -- 1 query
SELECT * FROM app_user WHERE id = 1;  -- query 2
SELECT * FROM app_user WHERE id = 2;  -- query 3
SELECT * FROM app_user WHERE id = 3;  -- query 4
-- ... N more queries!
```

**Solution - Fetch Join:**
```java
@Query("SELECT t FROM Task t JOIN FETCH t.appUser")
List<Task> findAllWithUsers();
```

**SQL executed:**
```sql
SELECT * FROM task t
JOIN app_user u ON t.app_user_id = u.app_user_id;  -- Single query!
```

### 3. Modifying Collections Without Setting Owner

**Problem:**
```java
AppUser user = new AppUser();
Task task = new Task();

user.getTasks().add(task);
// task.appUser is still null!

userRepository.save(user);
// Relationship NOT saved to database
```

**Solution - Helper methods:**
```java
public class AppUser {
    public void addTask(Task task) {
        tasks.add(task);
        task.setAppUser(this); // Set both sides
    }

    public void removeTask(Task task) {
        tasks.remove(task);
        task.setAppUser(null);
    }
}
```

**Usage:**
```java
user.addTask(task); // Sets both sides correctly
```

### 4. Using `@Data` on Entities

**❌ AVOID:**
```java
@Data // Generates equals/hashCode with ALL fields
@Entity
public class Task {
    @ManyToOne(fetch = LAZY)
    private AppUser appUser; // Lazy loading in equals()!
}
```

**✅ USE:**
```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Task {
    // Manual control over equals/hashCode via BaseEntity
}
```

### 5. Forgetting `@Builder.Default`

**Problem:**
```java
@Builder
public class Task {
    private TaskStatus status = TaskStatus.TODO; // Ignored by @Builder!
}

Task task = Task.builder().title("Task").build();
task.getStatus(); // null
```

**Solution:**
```java
@Builder.Default
private TaskStatus status = TaskStatus.TODO;
```

---

## Next Steps

Now that you understand JPA entities and relationships:

1. **Create repositories** - `@Repository` interfaces extending `JpaRepository`
2. **Write query methods** - Learn derived queries and JPQL
3. **Test repositories** - Use `@DataJpaTest` for repository tests
4. **Create service layer** - Business logic using repositories

See:
- [08-database-configuration.md](08-database-configuration.md) - Database setup
- [09-query-methods.md](09-query-methods.md) - Repository queries
- [19-data-layer-testing.md](19-data-layer-testing.md) - Testing repositories

---

## Related Files

- `src/main/java/com/tutorial/taskmanager/model/` - Entity classes
  - `BaseEntity.java` - Base entity with common fields
  - `AppUser.java` - User entity
  - `Task.java` - Task entity
  - `Project.java` - Project entity
- `src/main/java/com/tutorial/taskmanager/enums/` - Enum types
- `src/main/java/com/tutorial/taskmanager/constants/` - Constants

---

## Additional Resources

- [Spring Data JPA Documentation](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [Hibernate ORM Documentation](https://hibernate.org/orm/documentation/)
- [JPA 2.2 Specification](https://download.oracle.com/otndocs/jcp/persistence-2_2-mrel-spec/)
- [Baeldung - JPA/Hibernate Guide](https://www.baeldung.com/learn-jpa-hibernate)
- [Vlad Mihalcea's Blog](https://vladmihalcea.com/) - JPA/Hibernate expert

---

**Last Updated:** 2025-12-01
**Status:** ✅ Implemented
