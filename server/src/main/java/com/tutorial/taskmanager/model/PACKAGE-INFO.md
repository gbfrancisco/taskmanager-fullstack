# Package: com.tutorial.taskmanager.model

This package contains the **domain model** (JPA entities) for the Task Manager application.

---

## Table of Contents
1. [Package Overview](#package-overview)
2. [Entity Hierarchy](#entity-hierarchy)
3. [File Descriptions](#file-descriptions)
4. [Design Patterns Used](#design-patterns-used)
5. [Key Concepts](#key-concepts)
6. [Usage Examples](#usage-examples)
7. [Common Questions](#common-questions)

---

## Package Overview

The `model` package contains JPA entities that represent the core domain objects of our application. These classes:
- Are mapped to database tables
- Define relationships between entities
- Contain business data and validation rules
- Are managed by JPA/Hibernate

**Package Structure:**
```
com.tutorial.taskmanager.model/
├── BaseEntity.java       # Abstract base class with common fields
├── AppUser.java          # User entity
├── Project.java          # Project entity
└── Task.java             # Task entity
```

---

## Entity Hierarchy

```
                    BaseEntity (abstract)
                    @MappedSuperclass
                    ┌─────────────────┐
                    │ id              │
                    │ createdTs       │
                    │ updatedTs       │
                    └────────┬────────┘
                             │
                             │ extends
                             │
         ┌───────────────────┼───────────────────┐
         │                   │                   │
         ▼                   ▼                   ▼
    ┌─────────┐         ┌─────────┐        ┌─────────┐
    │ AppUser │         │ Project │        │  Task   │
    │ @Entity │         │ @Entity │        │ @Entity │
    └─────────┘         └─────────┘        └─────────┘
```

**Inheritance Strategy:**
- `BaseEntity` uses `@MappedSuperclass` (not an entity itself)
- Common fields (`id`, timestamps) are **inherited** by child entities
- Each child entity has its own table with inherited columns
- No `BaseEntity` table is created

---

## File Descriptions

### `BaseEntity.java`

**Purpose:** Abstract base class providing common fields and behavior for all entities.

**Key Features:**
- **ID field** - Generic `Long id` primary key
- **Timestamps** - Automatic `createdTimestamp` and `updatedTimestamp`
- **Lifecycle callbacks** - `@PrePersist` and `@PreUpdate` for timestamp management
- **Equality** - ID-based `equals()` and `hashCode()` (best practice for JPA)

**Annotations:**
```java
@MappedSuperclass  // Not an entity, but fields are inherited
@EqualsAndHashCode(onlyExplicitlyIncluded = true)  // Only use ID for equality
```

**Why `@MappedSuperclass`?**
- Allows field inheritance without creating a separate table
- Each entity table includes `id`, `createdTimestamp`, `updatedTimestamp`
- Centralizes common entity logic

**Lifecycle Callbacks:**
```java
@PrePersist  // Called before INSERT
public void prePersist() {
    LocalDateTime now = LocalDateTime.now();
    this.createdTimestamp = now;
    this.updatedTimestamp = now;
}

@PreUpdate  // Called before UPDATE
public void preUpdate() {
    this.updatedTimestamp = LocalDateTime.now();
}
```

**Benefits:**
- Automatic timestamp management (no manual setting required)
- Consistent behavior across all entities
- Single source of truth for common entity logic

---

### `AppUser.java`

**Purpose:** Represents a user who can own tasks and projects.

**Table:** `APP_USER`

**Fields:**
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | `Long` | PK, auto-generated | Inherited from `BaseEntity` (column: `APP_USER_ID`) |
| `username` | `String` | NOT NULL, UNIQUE, max 50 chars, not updatable | User's login name |
| `email` | `String` | NOT NULL, UNIQUE, max 100 chars | User's email |
| `password` | `String` | NOT NULL | User's password (will be encrypted) |
| `tasks` | `List<Task>` | One-to-Many | User's tasks |
| `projects` | `List<Project>` | One-to-Many | User's projects |

**Key Patterns:**

**1. Renamed from `User` to `AppUser`:**
- `USER` is a reserved keyword in many databases
- `APP_USER` avoids potential conflicts

**2. `@AttributeOverride` for custom ID column:**
```java
@AttributeOverride(
    name = "id",  // Field from BaseEntity
    column = @Column(name = "APP_USER_ID")  // Custom column name
)
```
- Inherits generic `id` field from `BaseEntity`
- Customizes database column name to `APP_USER_ID`
- Maintains clarity in database schema

**3. Username is immutable:**
```java
@Column(nullable = false, updatable = false, unique = true)
private String username;
```
- `updatable = false` prevents changes after creation
- Good practice for usernames (users shouldn't change login names)

**4. Cascade and Orphan Removal:**
```java
@OneToMany(mappedBy = "appUser", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Task> tasks = new ArrayList<>();
```
- Deleting a user **deletes all their tasks and projects**
- Removing a task from the list **deletes it from database**
- Consider business implications (do you want this behavior?)

---

### `Project.java`

**Purpose:** Represents a project that groups related tasks.

**Table:** `PROJECT`

**Fields:**
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | `Long` | PK, auto-generated | Inherited from `BaseEntity` (column: `PROJECT_ID`) |
| `name` | `String` | NOT NULL, max 100 chars | Project name |
| `description` | `String` | TEXT | Project description |
| `status` | `ProjectStatus` | NOT NULL, default: PLANNING | Current status (enum) |
| `appUser` | `AppUser` | Many-to-One, FK | Project owner |
| `tasks` | `List<Task>` | One-to-Many | Tasks in this project |

**Key Patterns:**

**1. Enum for status:**
```java
@Enumerated(EnumType.STRING)
@Column(nullable = false)
@Builder.Default
private ProjectStatus status = ProjectStatus.PLANNING;
```
- `EnumType.STRING` stores "PLANNING", "ACTIVE", etc. (not ordinals 0, 1, 2)
- `@Builder.Default` ensures builder respects the default value
- Default value: `PLANNING` (new projects start in planning phase)

**2. Lazy-loaded owner:**
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "APP_USER_ID")
private AppUser appUser;
```
- `LAZY` = owner is loaded only when accessed
- Prevents unnecessary queries
- Foreign key: `APP_USER_ID` in `project` table

**3. Bidirectional relationship with Task:**
```java
@OneToMany(mappedBy = "project")
private List<Task> tasks = new ArrayList<>();
```
- `mappedBy = "project"` indicates inverse side (Task owns the FK)
- Initialized to prevent `NullPointerException`

---

### `Task.java`

**Purpose:** Represents an individual task that can be assigned to a user and belong to a project.

**Table:** `TASK`

**Fields:**
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | `Long` | PK, auto-generated | Inherited from `BaseEntity` (column: `TASK_ID`) |
| `title` | `String` | NOT NULL, max 200 chars | Task title |
| `description` | `String` | TEXT | Task description |
| `status` | `TaskStatus` | NOT NULL, default: TODO | Current status (enum) |
| `dueDate` | `LocalDateTime` | nullable | Task deadline |
| `appUser` | `AppUser` | Many-to-One, FK | Task assignee |
| `project` | `Project` | Many-to-One, FK | Parent project |

**Key Patterns:**

**1. Multiple relationships:**
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "APP_USER_ID")
private AppUser appUser;  // Who is assigned to this task?

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "PROJECT_ID")
private Project project;  // Which project does this belong to?
```
- Task has **two** Many-to-One relationships
- Both are lazy-loaded for performance
- Foreign keys: `APP_USER_ID` and `PROJECT_ID`

**2. Status with default:**
```java
@Enumerated(EnumType.STRING)
@Column(nullable = false)
@Builder.Default
private TaskStatus status = TaskStatus.TODO;
```
- New tasks default to `TODO` status
- `@Builder.Default` ensures builder honors this default

**3. Optional due date:**
```java
private LocalDateTime dueDate;  // No @Column(nullable = false)
```
- Not all tasks have deadlines
- `null` is allowed

---

## Design Patterns Used

### 1. **Base Entity Pattern** ⭐

**Pattern:** Abstract base class with common fields.

**Benefits:**
- **DRY** - Don't Repeat Yourself
- Centralized timestamp management
- Consistent ID strategy across all entities
- Single point for equality/hashCode logic

**Implementation:**
```java
public abstract class BaseEntity {
    private Long id;
    private LocalDateTime createdTimestamp;
    private LocalDateTime updatedTimestamp;
}

public class Task extends BaseEntity {
    // Inherits id and timestamps
}
```

**Result:**
```sql
CREATE TABLE task (
    task_id BIGINT PRIMARY KEY,
    title VARCHAR(200),
    created_timestamp TIMESTAMP,  -- inherited
    updated_timestamp TIMESTAMP   -- inherited
);
```

---

### 2. **AttributeOverride Pattern** ⭐

**Pattern:** Customize inherited field column names per entity.

**Use Case:** Generic `id` field in base class, specific column names in tables.

**Implementation:**
```java
// BaseEntity
private Long id;  // Generic field name

// Task entity
@AttributeOverride(name = "id", column = @Column(name = "TASK_ID"))
public class Task extends BaseEntity {}

// AppUser entity
@AttributeOverride(name = "id", column = @Column(name = "APP_USER_ID"))
public class AppUser extends BaseEntity {}
```

**Database Result:**
```sql
CREATE TABLE task (task_id BIGINT PRIMARY KEY, ...);
CREATE TABLE app_user (app_user_id BIGINT PRIMARY KEY, ...);
```

**Benefits:**
- Clean Java code (simple `id` field)
- Clear database schema (`TASK_ID`, `APP_USER_ID`, etc.)
- Best of both worlds

---

### 3. **Constants for DB Names** ⭐

**Pattern:** Centralize table/column names in constants class.

**Implementation:**
```java
@UtilityClass
public class DatabaseTableConstants {
    public static final String TASK_TABLE = "TASK";
    public static final String TASK_ID_COLUMN = "TASK_ID";
}

@Entity
@Table(name = DatabaseTableConstants.TASK_TABLE)
public class Task {}
```

**Benefits:**
- Single source of truth
- Refactoring-friendly
- No magic strings
- Type-safe

---

### 4. **Enum for Status** ⭐

**Pattern:** Use enums instead of strings for fixed sets of values.

**Implementation:**
```java
public enum TaskStatus {
    TODO, IN_PROGRESS, COMPLETED, CANCELLED
}

@Enumerated(EnumType.STRING)
private TaskStatus status;
```

**Benefits:**
- Type safety (can't assign invalid values)
- IDE auto-completion
- Compiler checks
- Easy validation

---

### 5. **Lifecycle Callbacks** ⭐

**Pattern:** Use JPA callbacks for automatic field management.

**Implementation:**
```java
@PrePersist
public void prePersist() {
    this.createdTimestamp = LocalDateTime.now();
}

@PreUpdate
public void preUpdate() {
    this.updatedTimestamp = LocalDateTime.now();
}
```

**Benefits:**
- No manual timestamp setting
- Consistent behavior
- Can't forget to set timestamps

---

## Key Concepts

### Bidirectional Relationships

**Definition:** Both sides of a relationship reference each other.

**Example:**
```java
// AppUser side (inverse)
@OneToMany(mappedBy = "appUser")
private List<Task> tasks;

// Task side (owner)
@ManyToOne
@JoinColumn(name = "APP_USER_ID")
private AppUser appUser;
```

**Owner vs. Inverse:**
- **Owner side:** Has `@JoinColumn` (manages the foreign key)
- **Inverse side:** Has `mappedBy` (just mirrors the relationship)

**Database:**
```sql
-- Only ONE foreign key in task table
CREATE TABLE task (
    task_id BIGINT PRIMARY KEY,
    app_user_id BIGINT,  -- This is the relationship!
    FOREIGN KEY (app_user_id) REFERENCES app_user(app_user_id)
);
```

---

### Entity Equality (equals/hashCode)

**Why ID-only equality?**

**JPA contract:** Two entities with the same ID are the same entity, period.

**❌ BAD - Field-based equality:**
```java
@Data  // Uses all fields in equals/hashCode
public class Task {
    @ManyToOne(fetch = LAZY)
    private AppUser appUser;  // Triggers lazy loading!
}

task1.equals(task2);  // Accesses appUser → DB query → LazyInitializationException
```

**✅ GOOD - ID-only equality:**
```java
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class BaseEntity {
    @Id
    @EqualsAndHashCode.Include
    private Long id;
}
```

**Benefits:**
- No lazy loading triggered
- Works across sessions
- Consistent with JPA semantics

---

### Cascade Types

**What is cascading?**
Propagating operations from parent to children.

**Example:**
```java
@OneToMany(cascade = CascadeType.ALL)
private List<Task> tasks;

userRepository.save(user);  // Also saves all tasks
userRepository.delete(user); // Also deletes all tasks
```

**Cascade Options:**
| Type | Effect |
|------|--------|
| `PERSIST` | Save parent → save children |
| `MERGE` | Update parent → update children |
| `REMOVE` | Delete parent → delete children |
| `REFRESH` | Reload parent → reload children |
| `DETACH` | Detach parent → detach children |
| `ALL` | All of the above |

**Use with caution!** Consider business logic before cascading deletes.

---

## Usage Examples

### Creating Entities

**Basic creation:**
```java
Task task = new Task();
task.setTitle("Implement feature X");
task.setDescription("Add new authentication feature");
task.setStatus(TaskStatus.TODO);
task.setDueDate(LocalDateTime.now().plusDays(7));
```

**Using builder:**
```java
Task task = Task.builder()
    .title("Implement feature X")
    .description("Add new authentication feature")
    .dueDate(LocalDateTime.now().plusDays(7))
    .build();
// status is TaskStatus.TODO (default via @Builder.Default)
```

---

### Setting Relationships

**❌ WRONG - Only one side:**
```java
AppUser user = new AppUser();
Task task = new Task();

user.getTasks().add(task);
// task.appUser is still null!

userRepository.save(user);
// Relationship NOT saved (FK is null in DB)
```

**✅ CORRECT - Both sides:**
```java
AppUser user = new AppUser();
Task task = new Task();

user.getTasks().add(task);
task.setAppUser(user);  // Set both sides!

userRepository.save(user);
// Relationship saved correctly
```

**Better approach - Helper method:**
```java
public class AppUser {
    public void addTask(Task task) {
        this.tasks.add(task);
        task.setAppUser(this);  // Maintains both sides
    }
}

// Usage
user.addTask(task);  // Clean and correct
```

---

### Timestamps are Automatic

```java
Task task = new Task();
task.setTitle("My Task");

taskRepository.save(task);
// @PrePersist fires automatically
// createdTimestamp and updatedTimestamp are set

System.out.println(task.getCreatedTimestamp());  // e.g., 2025-11-15T10:30:00
System.out.println(task.getUpdatedTimestamp());  // same as createdTimestamp

task.setTitle("Updated Task");
taskRepository.save(task);
// @PreUpdate fires automatically
// updatedTimestamp is updated

System.out.println(task.getUpdatedTimestamp());  // newer timestamp
```

---

## Common Questions

### Q1: Why use `@MappedSuperclass` instead of `@Entity` for BaseEntity?

**A:** `@MappedSuperclass` does NOT create a table.

**With `@Entity`:**
```sql
CREATE TABLE base_entity (id BIGINT, created_ts TIMESTAMP);
CREATE TABLE task (id BIGINT, title VARCHAR(200));
-- Relationship between tables (inheritance mapping)
```

**With `@MappedSuperclass`:**
```sql
-- No base_entity table!
CREATE TABLE task (
    task_id BIGINT,
    title VARCHAR(200),
    created_timestamp TIMESTAMP,  -- inherited field
    updated_timestamp TIMESTAMP   -- inherited field
);
```

`@MappedSuperclass` = **field inheritance without table creation**.

---

### Q2: Why `GenerationType.IDENTITY` instead of `SEQUENCE`?

**A:** H2 database compatibility.

**`IDENTITY`:**
- Auto-increment column (1, 2, 3, ...)
- Works well with H2, MySQL, SQL Server
- Simpler for development

**`SEQUENCE`:**
- Separate database object (sequence)
- Preferred for PostgreSQL, Oracle
- Better performance in some scenarios

**Our approach:**
```java
@GeneratedValue(strategy = GenerationType.IDENTITY) // For H2 now
// Will switch to SEQUENCE when moving to PostgreSQL
```

---

### Q3: Why initialize lists (`= new ArrayList<>()`)?

**A:** Prevent `NullPointerException`.

**Without initialization:**
```java
@OneToMany(mappedBy = "appUser")
private List<Task> tasks;

AppUser user = new AppUser();
user.getTasks().add(task);  // NullPointerException!
```

**With initialization:**
```java
@OneToMany(mappedBy = "appUser")
private List<Task> tasks = new ArrayList<>();

AppUser user = new AppUser();
user.getTasks().add(task);  // Works!
```

---

### Q4: What's the difference between `mappedBy` and `@JoinColumn`?

**A:**
- `mappedBy`: "I'm the **inverse** side, the other side manages the FK"
- `@JoinColumn`: "I'm the **owner** side, I have the FK column"

**Example:**
```java
// AppUser (inverse side)
@OneToMany(mappedBy = "appUser")  // "Task.appUser owns this relationship"
private List<Task> tasks;

// Task (owner side)
@ManyToOne
@JoinColumn(name = "APP_USER_ID")  // "I have the FK column"
private AppUser appUser;
```

**Only ONE foreign key:**
```sql
CREATE TABLE task (
    app_user_id BIGINT,  -- This is the relationship
    FOREIGN KEY (app_user_id) REFERENCES app_user(app_user_id)
);
```

---

### Q5: Should I use `cascade = CascadeType.ALL` everywhere?

**A:** **No!** Think about business logic first.

**Good use case:**
```java
// Order → OrderItems (items can't exist without order)
@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
private List<OrderItem> items;

orderRepository.delete(order);  // Makes sense to delete items
```

**Questionable use case:**
```java
// AppUser → Tasks (maybe tasks should be reassigned or archived?)
@OneToMany(cascade = CascadeType.ALL)
private List<Task> tasks;

userRepository.delete(user);  // Deletes all tasks - is this desired?
```

**Rule of thumb:**
- If child **can't exist** without parent → use `CascadeType.ALL`
- If child **can exist independently** → handle in service layer

---

### Q6: Why `@Builder.Default`?

**A:** Lombok's `@Builder` ignores field initializers by default.

**Without `@Builder.Default`:**
```java
@Builder
public class Task {
    private TaskStatus status = TaskStatus.TODO;  // Ignored!
}

Task task = Task.builder().title("Task").build();
task.getStatus();  // null ❌
```

**With `@Builder.Default`:**
```java
@Builder.Default
private TaskStatus status = TaskStatus.TODO;

Task task = Task.builder().title("Task").build();
task.getStatus();  // TaskStatus.TODO ✓
```

---

## Next Steps

Now that you understand the entity model:

1. **Create repositories** - Extend `JpaRepository` for CRUD operations
2. **Test entities** - Write `@DataJpaTest` tests to verify mappings
3. **Create service layer** - Business logic using entities and repositories
4. **Add validation** - Bean Validation (`@Valid`, `@NotNull`, etc.)

---

## Related Documentation

- [../../docs/07-spring-data-jpa.md](../../docs/07-spring-data-jpa.md) - Comprehensive JPA guide
- [../enums/PACKAGE-INFO.md](../enums/PACKAGE-INFO.md) - Enum documentation
- [../constants/PACKAGE-INFO.md](../constants/PACKAGE-INFO.md) - Constants documentation

---

**Last Updated:** 2025-11-15
**Package Status:** ✅ Complete and production-ready
