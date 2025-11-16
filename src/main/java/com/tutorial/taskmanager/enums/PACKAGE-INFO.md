# Package: com.tutorial.taskmanager.enums

This package contains **enum types** used throughout the Task Manager application for representing fixed sets of values.

---

## Table of Contents
1. [Package Overview](#package-overview)
2. [Why Enums?](#why-enums)
3. [Enum Files](#enum-files)
4. [JPA Integration](#jpa-integration)
5. [Best Practices](#best-practices)
6. [Usage Examples](#usage-examples)
7. [Common Questions](#common-questions)

---

## Package Overview

The `enums` package contains Java enums that represent **domain-specific constants** with a fixed set of allowed values.

**Package Structure:**
```
com.tutorial.taskmanager.enums/
├── TaskStatus.java      # Task lifecycle states
└── ProjectStatus.java   # Project lifecycle states
```

**Why a separate package?**
- Clear separation of concerns
- Easy to locate and manage all enums
- Can be reused across layers (entities, DTOs, services)
- Follows enterprise project structure conventions

---

## Why Enums?

### Problem: Using Strings

**❌ String-based approach:**
```java
public class Task {
    private String status;  // What are the valid values?
}

// Code using it:
task.setStatus("TODO");
task.setStatus("In Progress");  // Typo! No compiler check
task.setStatus("INVALID");      // Runtime bug
```

**Issues:**
- No compile-time safety
- Typos cause runtime errors
- No IDE auto-completion
- Hard to find all valid values
- Validation needed everywhere

---

### Solution: Using Enums

**✅ Enum-based approach:**
```java
public enum TaskStatus {
    TODO, IN_PROGRESS, COMPLETED, CANCELLED
}

public class Task {
    private TaskStatus status;
}

// Code using it:
task.setStatus(TaskStatus.TODO);
task.setStatus(TaskStatus.IN_PROGRESS);  // IDE auto-completes!
task.setStatus("INVALID");  // Compiler error! ✓
```

**Benefits:**
- **Type safety** - Compiler prevents invalid values
- **IDE support** - Auto-completion shows all options
- **Self-documenting** - Clear what values are allowed
- **Refactor-friendly** - Rename enum value updates all usages
- **Validation-free** - Can't assign invalid values

---

## Enum Files

### `TaskStatus.java`

**Purpose:** Represents the lifecycle states of a task.

```java
public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
```

**State Meanings:**

| State | Meaning | Typical Transitions |
|-------|---------|---------------------|
| `TODO` | Task created but not started | → `IN_PROGRESS`, `CANCELLED` |
| `IN_PROGRESS` | Task is actively being worked on | → `COMPLETED`, `CANCELLED` |
| `COMPLETED` | Task is finished | (terminal state) |
| `CANCELLED` | Task was cancelled/abandoned | (terminal state) |

**State Diagram:**
```
┌──────────┐
│   TODO   │ (default)
└────┬─────┘
     │
     │ start
     ▼
┌──────────────┐
│ IN_PROGRESS  │
└────┬─────────┘
     │
     ├───────────────┐
     │               │
     │ complete      │ cancel
     ▼               ▼
┌──────────┐    ┌──────────┐
│COMPLETED │    │CANCELLED │
└──────────┘    └──────────┘
```

**Usage in Entity:**
```java
@Entity
public class Task extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TaskStatus status = TaskStatus.TODO;
}
```

---

### `ProjectStatus.java`

**Purpose:** Represents the lifecycle states of a project.

```java
public enum ProjectStatus {
    PLANNING,
    ACTIVE,
    ON_HOLD,
    COMPLETED,
    CANCELLED
}
```

**State Meanings:**

| State | Meaning | Typical Transitions |
|-------|---------|---------------------|
| `PLANNING` | Project in planning phase | → `ACTIVE`, `CANCELLED` |
| `ACTIVE` | Project is actively being worked on | → `ON_HOLD`, `COMPLETED`, `CANCELLED` |
| `ON_HOLD` | Project temporarily paused | → `ACTIVE`, `CANCELLED` |
| `COMPLETED` | Project successfully finished | (terminal state) |
| `CANCELLED` | Project was cancelled | (terminal state) |

**State Diagram:**
```
┌──────────┐
│ PLANNING │ (default)
└────┬─────┘
     │
     │ activate
     ▼
┌──────────┐     pause     ┌──────────┐
│  ACTIVE  │◄──────────────┤ ON_HOLD  │
└────┬─────┘───────────────►└──────────┘
     │          resume
     │
     ├───────────────┐
     │               │
     │ complete      │ cancel
     ▼               ▼
┌──────────┐    ┌──────────┐
│COMPLETED │    │CANCELLED │
└──────────┘    └──────────┘
```

**Usage in Entity:**
```java
@Entity
public class Project extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ProjectStatus status = ProjectStatus.PLANNING;
}
```

---

## JPA Integration

### Storing Enums in Database

JPA provides two strategies for storing enums:

#### 1. `EnumType.STRING` ✅ RECOMMENDED

**Annotation:**
```java
@Enumerated(EnumType.STRING)
private TaskStatus status;
```

**Database Storage:**
```sql
CREATE TABLE task (
    ...
    status VARCHAR(20)  -- Stores "TODO", "IN_PROGRESS", etc.
);

-- Example data:
INSERT INTO task (status) VALUES ('TODO');
INSERT INTO task (status) VALUES ('IN_PROGRESS');
```

**Benefits:**
- **Human-readable** in database queries
- **Migration-safe** - Adding enums in the middle doesn't break data
- **Debugging-friendly** - Easy to understand in SQL queries
- **Database-independent** - Works with any SQL database

**Example Query:**
```sql
SELECT * FROM task WHERE status = 'TODO';  -- Clear and readable
```

---

#### 2. `EnumType.ORDINAL` ❌ AVOID

**Annotation:**
```java
@Enumerated(EnumType.ORDINAL)  // DON'T USE!
private TaskStatus status;
```

**Database Storage:**
```sql
CREATE TABLE task (
    status INTEGER  -- Stores 0, 1, 2, 3
);

-- Example data:
-- TODO = 0, IN_PROGRESS = 1, COMPLETED = 2, CANCELLED = 3
INSERT INTO task (status) VALUES (0);
INSERT INTO task (status) VALUES (1);
```

**Why AVOID?**

**Problem 1: Adding enum in middle breaks data**
```java
// Original enum
public enum TaskStatus {
    TODO,         // 0
    IN_PROGRESS,  // 1
    COMPLETED,    // 2
    CANCELLED     // 3
}

// You add BLOCKED in the middle
public enum TaskStatus {
    TODO,         // 0
    BLOCKED,      // 1 ← NEW!
    IN_PROGRESS,  // 2 ← Was 1!
    COMPLETED,    // 3 ← Was 2!
    CANCELLED     // 4 ← Was 3!
}

// Database has:
// task_id | status
// --------+-------
//    1    |   1     ← Was IN_PROGRESS, now reads as BLOCKED!
//    2    |   2     ← Was COMPLETED, now reads as IN_PROGRESS!
```

**Problem 2: Not readable**
```sql
SELECT * FROM task WHERE status = 1;  -- What does 1 mean?
```

**Problem 3: Database-dependent**
- Some databases store ordinals differently
- Migration issues when switching databases

---

### Default Values with `@Builder.Default`

**Problem without `@Builder.Default`:**
```java
@Builder
public class Task {
    @Enumerated(EnumType.STRING)
    private TaskStatus status = TaskStatus.TODO;  // IGNORED by @Builder!
}

Task task = Task.builder().title("My Task").build();
task.getStatus();  // null ❌
```

**Solution with `@Builder.Default`:**
```java
@Builder
public class Task {
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private TaskStatus status = TaskStatus.TODO;
}

Task task = Task.builder().title("My Task").build();
task.getStatus();  // TaskStatus.TODO ✓
```

**Why needed?**
- Lombok's `@Builder` creates a separate builder class
- Field initializers are bypassed by the builder
- `@Builder.Default` tells Lombok to respect the default value

---

## Best Practices

### 1. Always Use `EnumType.STRING`

**✅ DO:**
```java
@Enumerated(EnumType.STRING)
private TaskStatus status;
```

**❌ DON'T:**
```java
@Enumerated(EnumType.ORDINAL)  // Fragile and unreadable
private TaskStatus status;

@Enumerated  // Defaults to ORDINAL!
private TaskStatus status;
```

---

### 2. Make Enum Columns NOT NULL

**✅ DO:**
```java
@Enumerated(EnumType.STRING)
@Column(nullable = false)  // Enforce a value
private TaskStatus status;
```

**Why?**
- Every task/project should have a status
- Prevents null checks everywhere
- Clearer business logic

---

### 3. Provide Default Values

**✅ DO:**
```java
@Builder.Default
@Enumerated(EnumType.STRING)
@Column(nullable = false)
private TaskStatus status = TaskStatus.TODO;
```

**Why?**
- New entities start in a valid state
- No need to manually set initial status
- Works with both constructors and builders

---

### 4. Name Enum Values Clearly

**✅ DO:**
```java
public enum TaskStatus {
    TODO,          // Clear meaning
    IN_PROGRESS,   // Descriptive
    COMPLETED      // Unambiguous
}
```

**❌ DON'T:**
```java
public enum TaskStatus {
    S1,    // What does S1 mean?
    S2,    // Unclear
    S3     // Non-descriptive
}
```

---

### 5. Consider Adding Helper Methods (Optional)

**Advanced pattern:**
```java
public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }

    public boolean canTransitionTo(TaskStatus newStatus) {
        return switch (this) {
            case TODO -> newStatus == IN_PROGRESS || newStatus == CANCELLED;
            case IN_PROGRESS -> newStatus == COMPLETED || newStatus == CANCELLED;
            case COMPLETED, CANCELLED -> false;  // Terminal states
        };
    }
}
```

**Usage:**
```java
if (task.getStatus().isTerminal()) {
    throw new IllegalStateException("Cannot modify completed task");
}

if (!task.getStatus().canTransitionTo(TaskStatus.COMPLETED)) {
    throw new IllegalStateException("Invalid status transition");
}
```

---

## Usage Examples

### Creating Entities with Enums

**Using constructor:**
```java
Task task = new Task();
task.setStatus(TaskStatus.TODO);  // Explicit
```

**Using builder with default:**
```java
Task task = Task.builder()
    .title("Implement feature")
    .build();
// status is TaskStatus.TODO (from @Builder.Default)
```

**Using builder with custom value:**
```java
Task task = Task.builder()
    .title("Ongoing task")
    .status(TaskStatus.IN_PROGRESS)  // Override default
    .build();
```

---

### Updating Status

```java
Task task = taskRepository.findById(1L).orElseThrow();

task.setStatus(TaskStatus.IN_PROGRESS);
taskRepository.save(task);
// Database: status = 'IN_PROGRESS'

task.setStatus(TaskStatus.COMPLETED);
taskRepository.save(task);
// Database: status = 'COMPLETED'
```

---

### Querying by Enum

**Repository method:**
```java
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByStatus(TaskStatus status);
}
```

**Usage:**
```java
List<Task> todoTasks = taskRepository.findByStatus(TaskStatus.TODO);
List<Task> completedTasks = taskRepository.findByStatus(TaskStatus.COMPLETED);
```

**Generated SQL:**
```sql
SELECT * FROM task WHERE status = 'TODO';
SELECT * FROM task WHERE status = 'COMPLETED';
```

---

### Switch Statements with Enums

**Modern switch expression (Java 14+):**
```java
String message = switch (task.getStatus()) {
    case TODO -> "Task not started";
    case IN_PROGRESS -> "Task in progress";
    case COMPLETED -> "Task completed";
    case CANCELLED -> "Task cancelled";
};
```

**Traditional switch:**
```java
switch (task.getStatus()) {
    case TODO:
        System.out.println("Task not started");
        break;
    case IN_PROGRESS:
        System.out.println("Task in progress");
        break;
    case COMPLETED:
        System.out.println("Task completed");
        break;
    case CANCELLED:
        System.out.println("Task cancelled");
        break;
}
```

**Benefit:** Compiler ensures all enum values are handled!

---

### Validation with Enums

**DTOs with enum validation:**
```java
public class TaskCreateDTO {
    @NotNull
    private String title;

    @NotNull
    private TaskStatus status;  // Automatically validated by type
}
```

**Controller:**
```java
@PostMapping("/tasks")
public Task createTask(@Valid @RequestBody TaskCreateDTO dto) {
    // If request contains "status": "INVALID",
    // Jackson throws exception automatically
    return taskService.create(dto);
}
```

---

## Common Questions

### Q1: Can I add methods to enums?

**A:** Yes! Enums can have fields, constructors, and methods.

**Example:**
```java
public enum TaskStatus {
    TODO("To Do", false),
    IN_PROGRESS("In Progress", false),
    COMPLETED("Completed", true),
    CANCELLED("Cancelled", true);

    private final String displayName;
    private final boolean terminal;

    TaskStatus(String displayName, boolean terminal) {
        this.displayName = displayName;
        this.terminal = terminal;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isTerminal() {
        return terminal;
    }
}
```

**Usage:**
```java
TaskStatus.TODO.getDisplayName();      // "To Do"
TaskStatus.COMPLETED.isTerminal();     // true
```

---

### Q2: Can I add new enum values later?

**A:** Yes, with `EnumType.STRING` it's safe!

**Safe with STRING:**
```java
// Original
public enum TaskStatus {
    TODO, IN_PROGRESS, COMPLETED
}

// Add new value
public enum TaskStatus {
    TODO, IN_PROGRESS, COMPLETED, CANCELLED  // ✓ Safe!
}
```

**Existing data is unaffected:**
```sql
-- Existing rows still valid:
SELECT * FROM task WHERE status = 'TODO';  -- Still works
```

**Unsafe with ORDINAL:**
```java
// Adding BLOCKED in middle changes all ordinals!
public enum TaskStatus {
    TODO,         // 0
    BLOCKED,      // 1 ← NEW! Was IN_PROGRESS's ordinal
    IN_PROGRESS,  // 2 ← Changed from 1!
    COMPLETED     // 3 ← Changed from 2!
}
// Existing data (1 = IN_PROGRESS) now reads as BLOCKED!
```

---

### Q3: How do I convert String to Enum?

**A:** Use `valueOf()` or custom logic.

**Basic conversion:**
```java
String statusString = "TODO";
TaskStatus status = TaskStatus.valueOf(statusString);  // TaskStatus.TODO
```

**Case-insensitive (safer):**
```java
String statusString = "todo";  // lowercase
TaskStatus status = TaskStatus.valueOf(statusString.toUpperCase());
```

**With error handling:**
```java
try {
    TaskStatus status = TaskStatus.valueOf(statusString);
} catch (IllegalArgumentException e) {
    // Invalid status string
}
```

**Spring automatically converts in REST controllers:**
```java
@GetMapping("/tasks")
public List<Task> getTasks(@RequestParam TaskStatus status) {
    // Spring converts "?status=TODO" to TaskStatus.TODO automatically
}
```

---

### Q4: Why separate `TaskStatus` and `ProjectStatus` instead of one `Status` enum?

**A:** Different entities may have different valid statuses in the future.

**Current:**
```java
TaskStatus:    TODO, IN_PROGRESS, COMPLETED, CANCELLED
ProjectStatus: PLANNING, ACTIVE, ON_HOLD, COMPLETED, CANCELLED
```

**Future scenarios:**
- Tasks might need `BLOCKED`, `WAITING_FOR_REVIEW`
- Projects might need `ARCHIVED`, `SUSPENDED`
- Different business rules for transitions

**Separate enums provide:**
- **Flexibility** - Evolve independently
- **Clarity** - Clear what applies to what
- **Type safety** - Can't assign `ProjectStatus` to `Task.status`

---

### Q5: Can enums be used in DTOs?

**A:** Absolutely! They work great in DTOs.

**Example DTO:**
```java
public class TaskResponseDTO {
    private Long id;
    private String title;
    private TaskStatus status;  // Enum in DTO
}
```

**JSON serialization (automatic with Jackson):**
```json
{
  "id": 1,
  "title": "My Task",
  "status": "TODO"
}
```

**Benefits:**
- Type safety in API contracts
- Automatic validation
- Self-documenting (API consumers see valid values)

---

### Q6: What if I need to rename an enum value?

**With `EnumType.STRING`, you have options:**

**Option 1: Rename and migrate data**
```java
// Before
public enum TaskStatus {
    TODO, IN_PROGRESS, DONE  // DONE
}

// After
public enum TaskStatus {
    TODO, IN_PROGRESS, COMPLETED  // COMPLETED
}

// SQL migration:
UPDATE task SET status = 'COMPLETED' WHERE status = 'DONE';
```

**Option 2: Use `@JsonProperty` for API compatibility**
```java
public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    @JsonProperty("DONE")  // Accept "DONE" from API
    COMPLETED  // But store as "COMPLETED"
}
```

---

## Next Steps

Now that you understand enums:

1. **Use enums in validation** - Add Bean Validation to DTOs
2. **Test enum persistence** - Write `@DataJpaTest` tests
3. **Add business logic** - Status transition rules in services
4. **Document API** - Use enums in OpenAPI/Swagger docs

---

## Related Documentation

- [../../docs/07-spring-data-jpa.md](../../docs/07-spring-data-jpa.md) - JPA entities guide
- [../model/PACKAGE-INFO.md](../model/PACKAGE-INFO.md) - Entity documentation
- [../constants/PACKAGE-INFO.md](../constants/PACKAGE-INFO.md) - Constants documentation

---

**Last Updated:** 2025-11-15
**Package Status:** ✅ Complete and production-ready
