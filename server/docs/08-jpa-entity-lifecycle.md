# 08 - JPA Entity Lifecycle

Understanding JPA entity states is **critical** for working effectively with Hibernate and Spring Data JPA. This guide provides a deep dive into how entities move through different states and why it matters for your code.

---

## Table of Contents
1. [The Four Entity States](#the-four-entity-states)
2. [Persistence Context (EntityManager)](#persistence-context-entitymanager)
3. [State Transitions](#state-transitions)
4. [Deep Dive: How Hibernate Tracks Changes](#deep-dive-how-hibernate-tracks-changes)
5. [Spring Data JPA: save() Explained](#spring-data-jpa-save-explained)
6. [Real-World Implications](#real-world-implications)
7. [Best Practices](#best-practices)
8. [Common Pitfalls](#common-pitfalls)

---

## The Four Entity States

Every JPA entity exists in one of four states at any given time:

```
┌─────────────────────────────────────────────────────────────────┐
│                     JPA ENTITY STATES                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   ┌───────────┐                          ┌───────────┐          │
│   │ TRANSIENT │                          │  REMOVED  │          │
│   │           │                          │           │          │
│   │ new Task()│                          │ Marked for│          │
│   │ No ID yet │                          │ deletion  │          │
│   └───────────┘                          └───────────┘          │
│         │                                      ▲                │
│         │ persist()                   remove() │                │
│         ▼                                      │                │
│   ┌─────────────────────────────────────────────┐               │
│   │              MANAGED / PERSISTENT           │               │
│   │                                             │               │
│   │  • In persistence context (first-level      │               │
│   │    cache)                                   │               │
│   │  • Changes are automatically tracked        │               │
│   │  • Will be synchronized with DB at flush    │               │
│   └─────────────────────────────────────────────┘               │
│         │                                      ▲                │
│         │ detach()/clear()/close()     merge() │                │
│         ▼                                      │                │
│   ┌───────────┐                                │                │
│   │ DETACHED  │────────────────────────────────┘                │
│   │           │                                                 │
│   │ Was       │                                                 │
│   │ managed,  │                                                 │
│   │ session   │                                                 │
│   │ closed    │                                                 │
│   └───────────┘                                                 │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 1. Transient

A **transient** entity is a plain Java object that:
- Was created using `new`
- Has **no** association with a persistence context
- Has **no** database representation (no ID, not in any table)

```java
// This task is TRANSIENT
Task task = new Task();
task.setTitle("Learn JPA");
task.setStatus(TaskStatus.TODO);
// At this point, task exists only in memory
// No database row exists for it
```

**Key characteristics:**
- Not tracked by Hibernate
- Garbage collected when no references exist
- Has no ID (or ID is null for `@GeneratedValue`)

### 2. Managed (Persistent)

A **managed** (or persistent) entity:
- Is associated with a persistence context
- Has a database representation (has an ID)
- Changes are **automatically tracked** (dirty checking)
- Will be synchronized with the database at flush time

```java
@Transactional
public void updateTask(Long taskId) {
    // After this line, task is MANAGED
    Task task = taskRepository.findById(taskId).orElseThrow();

    // This change is automatically tracked!
    task.setTitle("Updated Title");

    // No explicit save() needed - Hibernate detects the change
    // and will UPDATE the database at transaction commit
}
```

**Key characteristics:**
- Any changes to the entity are automatically detected
- No need to call `save()` for updates (though it's not wrong to do so)
- Lazy relationships can be loaded (session is open)
- Entity is cached in the persistence context

### 3. Detached

A **detached** entity:
- Was previously managed but is no longer associated with a persistence context
- Still has an ID (database row exists)
- Changes are **NOT** tracked

```java
// Inside @Transactional - task is MANAGED
Task task = taskRepository.findById(1L).orElseThrow();

// Transaction ends - task becomes DETACHED
// (or explicit detach/clear/close)

// Outside transaction - task is now DETACHED
task.setTitle("New Title"); // This change is NOT tracked!
// The database still has the old title
```

**How entities become detached:**
- Transaction ends (`@Transactional` method returns)
- `entityManager.detach(entity)` is called
- `entityManager.clear()` is called (detaches all entities)
- Session/EntityManager is closed
- Entity is serialized (e.g., sent over network, stored in HTTP session)

### 4. Removed

A **removed** entity:
- Is scheduled for deletion from the database
- Still associated with persistence context until flush
- Will be deleted at flush/commit time

```java
@Transactional
public void deleteTask(Long taskId) {
    Task task = taskRepository.findById(taskId).orElseThrow();
    // task is MANAGED

    taskRepository.delete(task);
    // task is now REMOVED (scheduled for deletion)

    // At transaction commit, DELETE SQL executes
}
```

---

## Persistence Context (EntityManager)

The **persistence context** is the key to understanding entity states. Think of it as:

1. **A first-level cache** - Entities loaded from the database are cached here
2. **A unit of work** - Tracks all changes to managed entities
3. **An identity map** - Ensures only one instance per entity identity

### What Is It?

```
┌─────────────────────────────────────────────────────────────────┐
│                    PERSISTENCE CONTEXT                          │
│                   (First-Level Cache)                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Entity Map (Identity Map):                                    │
│   ┌─────────────┬────────────────────────────────────┐          │
│   │ Entity Type │ ID → Entity Instance               │          │
│   ├─────────────┼────────────────────────────────────┤          │
│   │ Task        │ 1L → Task@abc123                   │          │
│   │ Task        │ 2L → Task@def456                   │          │
│   │ AppUser     │ 1L → AppUser@ghi789                │          │
│   └─────────────┴────────────────────────────────────┘          │
│                                                                 │
│   Snapshot Storage (for dirty checking):                        │
│   ┌─────────────┬────────────────────────────────────┐          │
│   │ Entity      │ Original Field Values              │          │
│   ├─────────────┼────────────────────────────────────┤          │
│   │ Task@abc123 │ {title:"Old", status:TODO, ...}    │          │
│   │ Task@def456 │ {title:"Task 2", status:DONE, ...} │          │
│   └─────────────┴────────────────────────────────────┘          │
│                                                                 │
│   Action Queue:                                                 │
│   ┌─────────────────────────────────────────────────┐           │
│   │ INSERT Task@xyz...                              │           │
│   │ UPDATE Task@abc123 (title changed)              │           │
│   │ DELETE Project@old...                           │           │
│   └─────────────────────────────────────────────────┘           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### EntityManager vs Session

- **EntityManager** - JPA standard interface
- **Session** - Hibernate-specific interface (extends EntityManager functionality)

Spring uses `EntityManager` by default, but you can unwrap to `Session` if needed:

```java
@PersistenceContext
private EntityManager entityManager;

// Unwrap to Hibernate Session if needed
Session session = entityManager.unwrap(Session.class);
```

### Scope: One Per Transaction

By default, Spring creates a new persistence context for each `@Transactional` method:

```java
@Service
public class TaskService {

    @Transactional
    public void method1() {
        // Persistence Context A is created
        Task task = taskRepository.findById(1L).orElseThrow();
        // task is MANAGED in Context A
    } // Context A is closed, task becomes DETACHED

    @Transactional
    public void method2() {
        // Persistence Context B is created (different from A!)
        Task task = taskRepository.findById(1L).orElseThrow();
        // This is a DIFFERENT Task instance than method1!
    }
}
```

### Identity Guarantee

The persistence context guarantees that within a single context, you always get the **same instance** for the same entity:

```java
@Transactional
public void demonstrateIdentity() {
    Task task1 = taskRepository.findById(1L).orElseThrow();
    Task task2 = taskRepository.findById(1L).orElseThrow();

    // These are the SAME object instance!
    System.out.println(task1 == task2); // true

    // Second findById() hits the cache, not the database
}
```

---

## State Transitions

### Transition Diagram

```
                           ┌──────────────┐
           new Task()      │   TRANSIENT  │
          ────────────────►│              │
                           └──────┬───────┘
                                  │
                      persist()   │
                    ──────────────┼──────────────────────────────┐
                                  ▼                              │
   ┌──────────────┐        ┌──────────────┐        ┌───────────┐│
   │   DETACHED   │◄───────│   MANAGED    │───────►│  REMOVED  ││
   │              │detach()│              │remove()│           ││
   └──────┬───────┘clear() └──────▲───────┘        └───────────┘│
          │        close()        │                              │
          │                       │ find()                       │
          │     merge()           │ query                        │
          └───────────────────────┴──────────────────────────────┘
```

### Detailed Transitions

#### Transient → Managed: `persist()`

```java
@Transactional
public Task createTask(TaskCreateDto dto) {
    Task task = new Task();         // TRANSIENT
    task.setTitle(dto.getTitle());
    task.setStatus(TaskStatus.TODO);

    entityManager.persist(task);    // Now MANAGED
    // INSERT is queued (not executed yet)

    return task;
}
```

**Important:** `persist()` does NOT execute INSERT immediately. It queues the INSERT for flush time.

#### Database → Managed: `find()` / Query

```java
@Transactional
public void loadEntities() {
    // Direct lookup - entity becomes MANAGED
    Task task = entityManager.find(Task.class, 1L);

    // Query result - all results are MANAGED
    List<Task> tasks = entityManager
        .createQuery("SELECT t FROM Task t", Task.class)
        .getResultList();
}
```

#### Managed → Detached: `detach()` / `clear()` / Transaction End

```java
@Transactional
public Task getTask(Long id) {
    Task task = taskRepository.findById(id).orElseThrow();
    // task is MANAGED
    return task;
} // Transaction ends, task becomes DETACHED

// In controller (no transaction):
// task is DETACHED here
```

Explicit detachment:

```java
@Transactional
public void explicitDetach() {
    Task task = taskRepository.findById(1L).orElseThrow();

    entityManager.detach(task);  // task is now DETACHED
    // or
    entityManager.clear();       // ALL entities become DETACHED
}
```

#### Detached → Managed: `merge()`

```java
@Transactional
public Task updateTask(Task detachedTask) {
    // detachedTask is DETACHED (came from outside transaction)

    Task managedTask = entityManager.merge(detachedTask);
    // managedTask is MANAGED
    // detachedTask is STILL DETACHED!

    // IMPORTANT: merge() returns a NEW managed instance!
    System.out.println(detachedTask == managedTask); // false

    return managedTask;
}
```

**Critical:** `merge()` returns a **new** managed instance. The original detached entity remains detached!

#### Managed → Removed: `remove()`

```java
@Transactional
public void deleteTask(Long id) {
    Task task = taskRepository.findById(id).orElseThrow();
    // task is MANAGED

    entityManager.remove(task);
    // task is now REMOVED
    // DELETE is queued for flush
}
```

---

## Deep Dive: How Hibernate Tracks Changes

### Dirty Checking Mechanism

When an entity becomes managed, Hibernate takes a **snapshot** of its current state. At flush time, it compares the current state to the snapshot:

```
┌─────────────────────────────────────────────────────────────────┐
│                     DIRTY CHECKING                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. Entity Loaded (becomes MANAGED)                             │
│     ┌──────────────────┐      ┌──────────────────┐              │
│     │ Entity Instance  │      │ Snapshot         │              │
│     ├──────────────────┤      ├──────────────────┤              │
│     │ title: "Task 1"  │      │ title: "Task 1"  │              │
│     │ status: TODO     │      │ status: TODO     │              │
│     └──────────────────┘      └──────────────────┘              │
│                                                                 │
│  2. Entity Modified                                             │
│     ┌──────────────────┐      ┌──────────────────┐              │
│     │ Entity Instance  │      │ Snapshot         │              │
│     ├──────────────────┤      ├──────────────────┤              │
│     │ title: "Updated" │ ───► │ title: "Task 1"  │ ← Different! │
│     │ status: DONE     │      │ status: TODO     │ ← Different! │
│     └──────────────────┘      └──────────────────┘              │
│                                                                 │
│  3. At Flush: Compare & Generate UPDATE                         │
│     UPDATE tasks SET title = 'Updated', status = 'DONE'         │
│     WHERE id = 1                                                │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### When Does Flush Happen?

Flush synchronizes the persistence context with the database. It happens:

1. **Before transaction commit** (automatic)
2. **Before a query executes** (to ensure query sees latest changes)
3. **When explicitly called** via `entityManager.flush()`

```java
@Transactional
public void flushExample() {
    Task task = taskRepository.findById(1L).orElseThrow();
    task.setTitle("Updated");

    // No SQL executed yet!

    // This query triggers a flush to ensure consistency
    List<Task> tasks = taskRepository.findByTitle("Updated");
    // UPDATE executed before SELECT

    // Or explicit flush:
    entityManager.flush();
}
```

### Flush Modes

```java
// AUTO (default) - Flush before queries and at commit
entityManager.setFlushMode(FlushModeType.AUTO);

// COMMIT - Only flush at commit (not before queries)
entityManager.setFlushMode(FlushModeType.COMMIT);
```

**FlushModeType.AUTO** (default):
- Flushes before any query that might be affected by pending changes
- Ensures queries always see the latest state
- Most intuitive behavior

**FlushModeType.COMMIT**:
- Only flushes at transaction commit
- Better performance (fewer flushes)
- But queries might not see pending changes!

### First-Level Cache Behavior

The persistence context acts as a cache:

```java
@Transactional
public void cacheDemo() {
    // First call - hits database
    Task task1 = taskRepository.findById(1L).orElseThrow();
    // SQL: SELECT * FROM tasks WHERE id = 1

    // Second call - hits CACHE, no SQL!
    Task task2 = taskRepository.findById(1L).orElseThrow();
    // No SQL executed

    System.out.println(task1 == task2); // true - same instance
}
```

**Note:** JPQL/HQL queries always hit the database but return cached instances:

```java
@Transactional
public void queryVsFind() {
    Task task1 = taskRepository.findById(1L).orElseThrow();
    // SQL: SELECT * FROM tasks WHERE id = 1

    // Query hits database but returns same cached instance
    Task task2 = taskRepository.findByTitle("Task 1").get(0);
    // SQL: SELECT * FROM tasks WHERE title = 'Task 1'
    // But task2 is the SAME instance as task1 (from cache)

    System.out.println(task1 == task2); // true
}
```

---

## Spring Data JPA: save() Explained

Spring Data JPA's `save()` method is a convenience that handles both insert and update:

### How save() Works

```java
// Simplified implementation from SimpleJpaRepository
@Transactional
public <S extends T> S save(S entity) {
    if (entityInformation.isNew(entity)) {
        entityManager.persist(entity);
        return entity;
    } else {
        return entityManager.merge(entity);
    }
}
```

### The isNew() Check

How does Spring determine if an entity is "new"?

1. **@Id with @GeneratedValue**: Entity is new if ID is `null`
2. **@Id without @GeneratedValue**: Entity is new if ID is `null` (or check `Persistable` interface)
3. **Implements Persistable**: Uses `isNew()` method you define

```java
// With @GeneratedValue - most common case
@Entity
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // null = new, not-null = existing
}

// Creating new entity
Task newTask = new Task();
newTask.setTitle("New Task");
taskRepository.save(newTask);  // persist() called - INSERT

// Updating existing (if detached)
Task detachedTask = ...;  // Has ID = 1L
taskRepository.save(detachedTask);  // merge() called - SELECT + UPDATE
```

### Why save() Return Value Matters

For new entities (persist), the returned entity is the same instance:

```java
Task newTask = new Task();
Task savedTask = taskRepository.save(newTask);
System.out.println(newTask == savedTask); // true
```

For detached entities (merge), the returned entity is a **NEW managed instance**:

```java
Task detached = getDetachedTask();  // ID = 1L
Task merged = taskRepository.save(detached);

System.out.println(detached == merged); // FALSE!

// Use merged, not detached!
detached.setTitle("Won't be saved");  // WRONG - detached is still detached
merged.setTitle("Will be saved");     // CORRECT - merged is managed
```

### When You Don't Need save()

If the entity is already managed, you don't need to call `save()`:

```java
@Transactional
public void updateTask(Long id, String newTitle) {
    Task task = taskRepository.findById(id).orElseThrow();
    // task is MANAGED

    task.setTitle(newTitle);  // Change is tracked!

    // No need to call save() - dirty checking will handle it
    // But calling save() is not wrong, it just does nothing extra
}
```

---

## Real-World Implications

### Why Updates "Just Work" on Managed Entities

```java
@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;

    @Transactional
    public TaskResponseDto updateTask(Long id, TaskUpdateDto dto) {
        // 1. Load entity - it's now MANAGED
        Task task = taskRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Task", id));

        // 2. Modify it - changes are automatically tracked
        taskMapper.updateEntityFromDto(dto, task);

        // 3. No explicit save needed! At commit time:
        //    - Hibernate compares current state to snapshot
        //    - Detects changes
        //    - Generates and executes UPDATE SQL

        return taskMapper.toResponseDto(task);
    }
}
```

### Why LazyInitializationException Happens

This exception occurs when you access a lazy-loaded relationship on a **detached** entity:

```java
@Service
public class TaskService {

    @Transactional
    public Task getTask(Long id) {
        return taskRepository.findById(id).orElseThrow();
    } // Transaction ends, task becomes DETACHED
}

@RestController
public class TaskController {

    @GetMapping("/tasks/{id}")
    public TaskDto getTask(@PathVariable Long id) {
        Task task = taskService.getTask(id);
        // task is DETACHED here (transaction ended in service)

        // This FAILS with LazyInitializationException!
        String projectName = task.getProject().getName();
        // Because project wasn't loaded and session is closed
    }
}
```

**Solutions:**

1. **Load relationships within transaction:**
```java
@Transactional
public TaskDto getTaskWithProject(Long id) {
    Task task = taskRepository.findById(id).orElseThrow();
    // Force initialization while session is open
    Hibernate.initialize(task.getProject());
    return taskMapper.toResponseDto(task);
}
```

2. **Use JOIN FETCH in query:**
```java
@Query("SELECT t FROM Task t JOIN FETCH t.project WHERE t.id = :id")
Optional<Task> findByIdWithProject(@Param("id") Long id);
```

3. **Use DTOs (recommended):** Convert to DTO within transaction, only include needed data.

### Why @Transactional Matters

Without `@Transactional`, each repository call creates its own persistence context:

```java
// BAD: No @Transactional
public void updateTask(Long id, String title) {
    Task task = taskRepository.findById(id).orElseThrow();
    // Transaction ended! task is DETACHED

    task.setTitle(title);  // Not tracked!

    taskRepository.save(task);  // Creates NEW transaction
    // merge() is called - extra SELECT + UPDATE
}

// GOOD: With @Transactional
@Transactional
public void updateTask(Long id, String title) {
    Task task = taskRepository.findById(id).orElseThrow();
    // task is MANAGED

    task.setTitle(title);  // Tracked by dirty checking

    // No save() needed - UPDATE at commit
}
```

### @Transactional(readOnly = true) for Read Operations

For methods that only read data, use `readOnly = true` as an optimization hint:

```java
@Service
@Transactional  // Default for all methods - read-write
public class TaskService {

    @Transactional(readOnly = true)  // Override for read-only methods
    public Optional<TaskResponseDto> findById(Long id) {
        return taskRepository.findById(id)
            .map(taskMapper::toResponseDto);
    }

    @Transactional(readOnly = true)
    public List<TaskResponseDto> findAll() {
        return taskMapper.toResponseDtoList(taskRepository.findAll());
    }

    @Transactional(readOnly = true)
    public List<TaskResponseDto> findByStatus(TaskStatus status) {
        return taskMapper.toResponseDtoList(taskRepository.findByStatus(status));
    }

    // Write methods inherit class-level @Transactional (read-write)
    public TaskResponseDto createTask(TaskCreateDto dto) { ... }
    public TaskResponseDto updateTask(Long id, TaskUpdateDto dto) { ... }
    public void deleteTask(Long id) { ... }
}
```

#### Benefits of readOnly = true

| Benefit | Explanation |
|---------|-------------|
| **Performance** | Hibernate skips dirty checking (no snapshot comparison at flush) |
| **Consistency** | Multiple reads share the same persistence context - consistent view of data |
| **Safety** | Accidental entity modifications won't be persisted |
| **DB Optimization** | Some databases/connection pools can route to read replicas |

#### When to Use readOnly = true

- **Use it for:** Any method that only reads data, even with multiple repository calls
- **Don't use it for:** Methods that create, update, or delete entities

```java
// Multiple reads - still use readOnly = true
@Transactional(readOnly = true)
public DashboardDto getDashboard(Long userId) {
    AppUser user = userRepository.findById(userId).orElseThrow();
    List<Task> tasks = taskRepository.findByAppUserId(userId);
    List<Project> projects = projectRepository.findByAppUserId(userId);
    return buildDashboardDto(user, tasks, projects);
}
```

#### Class-Level vs Method-Level

**Recommended pattern:** Class-level `@Transactional` + method-level `readOnly = true` for reads:

```java
@Service
@Transactional  // All methods are transactional by default
public class TaskService {

    @Transactional(readOnly = true)  // Override for reads
    public TaskResponseDto getById(Long id) { ... }

    // Inherits class-level @Transactional (read-write)
    public TaskResponseDto createTask(TaskCreateDto dto) { ... }
}
```

**Alternative:** Class-level `readOnly = true` + method-level override for writes:

```java
@Service
@Transactional(readOnly = true)  // Default to read-only
public class TaskService {

    // Inherits class-level readOnly = true
    public TaskResponseDto getById(Long id) { ... }

    @Transactional  // Override for writes (removes readOnly)
    public TaskResponseDto createTask(TaskCreateDto dto) { ... }
}
```

Both patterns are valid. Choose based on whether your service has more reads or writes.

### How DTOs Help

DTOs are plain objects - they don't have JPA state:

```java
@Transactional
public TaskResponseDto getTask(Long id) {
    Task task = taskRepository.findById(id).orElseThrow();
    // task is MANAGED

    // Convert to DTO while session is open
    TaskResponseDto dto = taskMapper.toResponseDto(task);
    // DTO captures all data we need

    return dto;
} // task becomes detached, but we have the DTO

// Controller receives a plain DTO, no JPA concerns
```

Benefits:
- No LazyInitializationException (DTO has no lazy fields)
- Clear boundary between persistence and API layers
- Entity never crosses transaction boundary

---

## Best Practices

### 1. Keep Operations Within @Transactional Boundaries

```java
// GOOD
@Transactional
public void businessOperation() {
    Task task = taskRepository.findById(1L).orElseThrow();
    task.setTitle("Updated");
    // Automatic dirty checking handles the update
}

// BAD
public void businessOperation() {
    Task task = taskRepository.findById(1L).orElseThrow();
    // task is DETACHED
    task.setTitle("Updated");  // Lost!
}
```

### 2. Always Use the Returned Entity from save() When Merging

```java
// GOOD
Task detached = getDetachedTask();
Task managed = taskRepository.save(detached);
managed.setStatus(TaskStatus.DONE);  // This change is tracked

// BAD
Task detached = getDetachedTask();
taskRepository.save(detached);
detached.setStatus(TaskStatus.DONE);  // NOT tracked! detached is still detached
```

### 3. Load Lazy Relationships Before Leaving Transaction

```java
@Transactional
public TaskDto getTaskWithDetails(Long id) {
    Task task = taskRepository.findById(id).orElseThrow();

    // Initialize lazy relationships NOW
    task.getProject().getName();  // Forces load
    // Or use Hibernate.initialize(task.getProject());

    return taskMapper.toResponseDto(task);
}
```

### 4. Use DTOs for Crossing Boundaries

```java
// Service layer - works with entities
@Transactional
public TaskResponseDto createTask(TaskCreateDto dto) {
    Task task = taskMapper.toEntity(dto);
    Task saved = taskRepository.save(task);
    return taskMapper.toResponseDto(saved);  // Return DTO, not entity
}

// Controller layer - works with DTOs only
@PostMapping("/tasks")
public TaskResponseDto create(@RequestBody TaskCreateDto dto) {
    return taskService.createTask(dto);  // Receives DTO
}
```

### 5. Understand Your Entity's State

Ask yourself: "Is this entity managed or detached?"

```java
public void process(Task task) {
    // Where did this task come from?
    // - From findById() in current @Transactional? → MANAGED
    // - Returned from another method? → Probably DETACHED
    // - Passed from controller? → DETACHED
    // - Deserialized from JSON/session? → DETACHED
}
```

---

## Common Pitfalls

### 1. Modifying Detached Entities and Expecting Auto-Save

```java
// WRONG
public Task updateTask(Task task) {  // task is DETACHED (from controller)
    task.setTitle("New Title");      // NOT tracked
    return task;                     // Database unchanged!
}

// CORRECT
@Transactional
public Task updateTask(Task task) {
    Task managed = taskRepository.findById(task.getId()).orElseThrow();
    managed.setTitle(task.getTitle());
    return managed;  // Changes tracked, will be saved
}
```

### 2. Forgetting merge() Returns a New Instance

```java
// WRONG
Task detached = getDetachedTask();
entityManager.merge(detached);
detached.setStatus(TaskStatus.DONE);  // detached is STILL detached!

// CORRECT
Task detached = getDetachedTask();
Task managed = entityManager.merge(detached);
managed.setStatus(TaskStatus.DONE);  // managed is tracked
```

### 3. Merge Overwrites ALL Fields (Including Nulls!)

This is a **critical difference** between updating managed vs detached entities:

```java
// MANAGED UPDATE - Only specified fields change
@Transactional
public void updateTitle(Long id, String newTitle) {
    Task task = taskRepository.findById(id).orElseThrow();
    // task has: title="Old", status=IN_PROGRESS, dueDate=2024-01-15

    task.setTitle(newTitle);  // Only title changes

    // Result: title="New Title", status=IN_PROGRESS, dueDate=2024-01-15
    // Other fields PRESERVED
}

// MERGE (DETACHED) - ALL fields are copied, including nulls!
@Transactional
public void updateFromDetached(Task detached) {
    // detached has: id=1, title="New Title", status=null, dueDate=null
    // (maybe only title was set, rest are null)

    Task merged = entityManager.merge(detached);

    // Result: title="New Title", status=NULL, dueDate=NULL
    // Existing data OVERWRITTEN with nulls!
}
```

**Why this happens:** `merge()` copies ALL field values from the detached entity to the managed entity. It doesn't know which fields you "intended" to update vs which are just null because they weren't set.

**The safe pattern - fetch then update:**

```java
@Transactional
public TaskResponseDto updateTask(Long id, TaskUpdateDto dto) {
    // 1. Fetch the managed entity (has all current values)
    Task task = taskRepository.findById(id).orElseThrow();

    // 2. Update ONLY the fields that should change
    if (dto.getTitle() != null) {
        task.setTitle(dto.getTitle());
    }
    if (dto.getStatus() != null) {
        task.setStatus(dto.getStatus());
    }
    // Fields not in DTO remain unchanged

    // 3. Dirty checking handles the rest
    return taskMapper.toResponseDto(task);
}
```

**Or use MapStruct with null-handling:**

```java
@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TaskMapper {
    // With IGNORE strategy, null values in DTO won't overwrite entity fields
    void updateEntityFromDto(TaskUpdateDto dto, @MappingTarget Task entity);
}
```

### 4. LazyInitializationException from Detached Entities

```java
// WRONG
public String getProjectName(Long taskId) {
    Task task = taskService.getTask(taskId);  // Returns detached task
    return task.getProject().getName();        // BOOM! LazyInitializationException
}

// CORRECT - Option 1: Load in transaction
@Transactional
public String getProjectName(Long taskId) {
    Task task = taskRepository.findById(taskId).orElseThrow();
    return task.getProject().getName();  // Works - session is open
}

// CORRECT - Option 2: Use DTO
public TaskWithProjectDto getTaskWithProject(Long taskId) {
    return taskService.getTaskWithProjectDto(taskId);  // DTO has project name
}
```

### 5. N+1 Queries from Lazy Loading

```java
// BAD - N+1 queries
@Transactional
public List<String> getAllProjectNames() {
    List<Task> tasks = taskRepository.findAll();  // 1 query
    return tasks.stream()
        .map(t -> t.getProject().getName())       // N queries!
        .toList();
}

// GOOD - JOIN FETCH
@Query("SELECT t FROM Task t JOIN FETCH t.project")
List<Task> findAllWithProjects();  // 1 query with JOIN
```

### 6. Cascade Operations in Unexpected States

```java
// Entity with CascadeType.ALL
@OneToMany(mappedBy = "appUser", cascade = CascadeType.ALL)
private List<Task> tasks;

// PITFALL
@Transactional
public void addTaskToUser(Long userId, Task newTask) {
    AppUser user = userRepository.findById(userId).orElseThrow();
    user.getTasks().add(newTask);  // newTask is TRANSIENT
    // CascadeType.ALL means newTask will be persisted automatically
    // But only if you understand this cascade behavior!
}
```

### 7. Assuming Entity Stays Managed Across Web Request

```java
// Controller
@GetMapping("/tasks/{id}/edit")
public String editForm(@PathVariable Long id, Model model) {
    Task task = taskService.getTask(id);  // task is DETACHED
    model.addAttribute("task", task);
    return "edit-form";
}

@PostMapping("/tasks/{id}")
public String save(@ModelAttribute Task task) {  // Still DETACHED (rebound from form)
    task.setTitle("Updated");  // NOT tracked
    // Need to merge or fetch fresh
}
```

---

## Summary

| State | In Persistence Context? | Has DB Row? | Changes Tracked? |
|-------|------------------------|-------------|------------------|
| Transient | No | No | No |
| Managed | Yes | Yes | Yes |
| Detached | No | Yes | No |
| Removed | Yes (until flush) | Yes (until flush) | N/A |

**Key Takeaways:**

1. **Understand entity state** - Know whether your entity is managed or detached
2. **Use @Transactional** - Keep business operations within transaction boundaries
3. **Dirty checking is automatic** - No need to save() managed entities
4. **merge() returns a new instance** - Always use the returned value
5. **DTOs solve boundary problems** - Convert to DTO before leaving transaction
6. **Load lazy relationships within transaction** - Or use JOIN FETCH

---

**Next Steps:**
- See [07-jpa-entities.md](07-jpa-entities.md) for entity structure and relationships
- See [09-jpa-query-methods.md](09-jpa-query-methods.md) for query method patterns
