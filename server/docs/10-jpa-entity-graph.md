# JPA Entity Graphs

Entity graphs provide a declarative way to control which relationships are fetched when loading entities. They solve the N+1 query problem without hardcoding fetch behavior in your entities.

---

## Table of Contents

1. [The Problem: N+1 Queries](#the-problem-n1-queries)
2. [What is an Entity Graph?](#what-is-an-entity-graph)
3. [Entity Graph vs JOIN FETCH](#entity-graph-vs-join-fetch)
4. [Defining Entity Graphs](#defining-entity-graphs)
5. [Using Entity Graphs with Spring Data](#using-entity-graphs-with-spring-data)
6. [Fetching Multiple Relationships](#fetching-multiple-relationships)
7. [Graph Types: FETCH vs LOAD](#graph-types-fetch-vs-load)
8. [Best Practices](#best-practices)
9. [Common Pitfalls](#common-pitfalls)
10. [Implementation Reference](#implementation-reference)

---

## The Problem: N+1 Queries

When entities have lazy relationships, accessing them triggers additional queries:

```java
// Entity with lazy relationships
@Entity
public class Task {
    @ManyToOne(fetch = FetchType.LAZY)
    private AppUser appUser;

    @ManyToOne(fetch = FetchType.LAZY)
    private Project project;
}
```

**The N+1 problem in action:**

```java
// Service method
public List<TaskResponseDto> getAllTasks() {
    List<Task> tasks = taskRepository.findAll();  // 1 query

    return tasks.stream()
        .map(task -> {
            // Each access triggers a lazy load query!
            String username = task.getAppUser().getUsername();  // N queries
            String projectName = task.getProject().getName();   // N more queries
            return new TaskResponseDto(task, username, projectName);
        })
        .toList();
}
// Total: 1 + N + N = 1 + 2N queries for N tasks!
```

With 100 tasks, that's **201 queries** instead of 1.

---

## What is an Entity Graph?

An **Entity Graph** is a template that defines which attributes (including relationships) should be fetched when loading an entity. Think of it as a "shopping list" for the JPA provider.

**Key characteristics:**

| Feature | Description |
|---------|-------------|
| **Declarative** | Define once, reuse across queries |
| **Query-time** | Override fetch behavior per query, not globally |
| **Composable** | Can include nested relationships |
| **Standard** | Part of JPA 2.1 specification |

**How it solves N+1:**

```java
// With Entity Graph - single query with JOINs
@EntityGraph(attributePaths = {"appUser", "project"})
List<Task> findAll();
// Generates: SELECT t.*, u.*, p.* FROM task t
//            LEFT JOIN app_user u ON t.app_user_id = u.id
//            LEFT JOIN project p ON t.project_id = p.id
```

---

## Entity Graph vs JOIN FETCH

Both solve N+1, but have different trade-offs:

### JOIN FETCH (JPQL)

```java
@Query("SELECT t FROM Task t " +
       "JOIN FETCH t.appUser " +
       "JOIN FETCH t.project " +
       "WHERE t.status = :status")
List<Task> findByStatusWithRelationships(@Param("status") TaskStatus status);
```

**Pros:**
- Explicit and visible in the query
- Full control over JOIN type (INNER vs LEFT)
- Can combine with complex WHERE clauses

**Cons:**
- Hardcoded in each query method
- Duplicated across similar queries
- Mixes fetch strategy with query logic

### Entity Graph

```java
@EntityGraph(attributePaths = {"appUser", "project"})
List<Task> findByStatus(TaskStatus status);
```

**Pros:**
- Declarative and reusable
- Separates "what to fetch" from "what to filter"
- Works with derived query methods
- Can be defined once and applied to multiple queries

**Cons:**
- Always uses LEFT JOIN (can't specify INNER)
- Less visible than explicit JPQL
- Named graphs require entity annotations

### When to Use Which?

| Scenario | Recommendation |
|----------|----------------|
| Simple relationship fetching | Entity Graph |
| Complex queries with JOINs in WHERE | JOIN FETCH |
| Reusable fetch patterns | Named Entity Graph |
| One-off specific queries | JOIN FETCH or ad-hoc graph |
| Standard CRUD with relationships | Entity Graph |

---

## Defining Entity Graphs

### Option 1: Ad-hoc with `attributePaths` (Recommended for most cases)

Define the graph directly on the repository method:

```java
public interface TaskRepository extends JpaRepository<Task, Long> {

    // Fetch appUser and project with all tasks
    @EntityGraph(attributePaths = {"appUser", "project"})
    List<Task> findAll();

    // Fetch only appUser
    @EntityGraph(attributePaths = {"appUser"})
    List<Task> findByStatus(TaskStatus status);

    // Fetch nested relationship
    @EntityGraph(attributePaths = {"project.appUser"})
    Optional<Task> findById(Long id);
}
```

**Nested paths:** Use dot notation for nested relationships:
- `"project"` - fetch the project
- `"project.appUser"` - fetch the project AND its owner

### Option 2: Named Entity Graph (Reusable across queries)

Define on the entity class:

```java
@Entity
@NamedEntityGraph(
    name = "Task.withUserAndProject",
    attributeNodes = {
        @NamedAttributeNode("appUser"),
        @NamedAttributeNode("project")
    }
)
@NamedEntityGraph(
    name = "Task.withFullProject",
    attributeNodes = {
        @NamedAttributeNode("appUser"),
        @NamedAttributeNode(value = "project", subgraph = "project-subgraph")
    },
    subgraphs = {
        @NamedSubgraph(
            name = "project-subgraph",
            attributeNodes = {
                @NamedAttributeNode("appUser")  // Project's owner
            }
        )
    }
)
public class Task {
    // ...
}
```

Use in repository:

```java
public interface TaskRepository extends JpaRepository<Task, Long> {

    @EntityGraph(value = "Task.withUserAndProject")
    List<Task> findAll();

    @EntityGraph(value = "Task.withFullProject")
    Optional<Task> findById(Long id);
}
```

### When to Use Named vs Ad-hoc

| Named Entity Graph | Ad-hoc (`attributePaths`) |
|--------------------|---------------------------|
| Complex nested graphs | Simple 1-2 level fetching |
| Reused across many methods | One-off queries |
| Self-documenting entity | Quick prototyping |
| Team convention | Simpler repositories |

---

## Using Entity Graphs with Spring Data

### With Derived Query Methods

Entity graphs work seamlessly with Spring Data's query derivation:

```java
public interface TaskRepository extends JpaRepository<Task, Long> {

    // Derived query + entity graph
    @EntityGraph(attributePaths = {"appUser", "project"})
    List<Task> findByStatus(TaskStatus status);

    @EntityGraph(attributePaths = {"appUser"})
    List<Task> findByAppUserId(Long userId);

    @EntityGraph(attributePaths = {"project"})
    List<Task> findByDueDateBefore(LocalDateTime date);

    // Override inherited findById
    @EntityGraph(attributePaths = {"appUser", "project"})
    Optional<Task> findById(Long id);
}
```

### With Custom JPQL Queries

Combine with `@Query`:

```java
@EntityGraph(attributePaths = {"appUser", "project"})
@Query("SELECT t FROM Task t WHERE t.status IN :statuses")
List<Task> findByStatusIn(@Param("statuses") List<TaskStatus> statuses);
```

### With Pagination

Works with Spring Data pagination:

```java
@EntityGraph(attributePaths = {"appUser", "project"})
Page<Task> findByStatus(TaskStatus status, Pageable pageable);
```

**Warning:** Be cautious with pagination and entity graphs on collections (OneToMany). The "MultipleBagFetchException" and incorrect counts can occur. For ToMany relationships with pagination, consider alternative approaches.

---

## Fetching Multiple Relationships

### Same Level (Siblings)

Fetch multiple relationships at the same level:

```java
// Task has appUser and project at the same level
@EntityGraph(attributePaths = {"appUser", "project"})
List<Task> findAll();
```

Generated SQL:
```sql
SELECT t.*, u.*, p.*
FROM task t
LEFT JOIN app_user u ON t.app_user_id = u.id
LEFT JOIN project p ON t.project_id = p.id
```

### Nested Relationships

Fetch relationships of relationships:

```java
// Task -> Project -> AppUser (project owner)
@EntityGraph(attributePaths = {"appUser", "project", "project.appUser"})
List<Task> findAll();
```

**Important:** When fetching nested paths, you must include the intermediate:
- `"project.appUser"` requires `"project"` (implicit or explicit)

### Complex Example

```java
// Fetch task with:
// - task's assigned user
// - task's project
// - project's owner (different from task's assignee)
@EntityGraph(attributePaths = {
    "appUser",           // Task's assignee
    "project",           // Task's project
    "project.appUser"    // Project's owner
})
Optional<Task> findDetailedById(Long id);
```

---

## Graph Types: FETCH vs LOAD

Entity graphs have two types that determine how unspecified attributes are handled:

### FETCH (Default)

Attributes in the graph are fetched eagerly. **Attributes NOT in the graph use their entity-defined fetch type.**

```java
@EntityGraph(
    attributePaths = {"appUser"},
    type = EntityGraph.EntityGraphType.FETCH  // default
)
List<Task> findAll();
// appUser: EAGER (in graph)
// project: uses @ManyToOne default (EAGER) or entity annotation (LAZY)
```

### LOAD

Attributes in the graph are fetched eagerly. **Attributes NOT in the graph are loaded lazily, regardless of entity annotation.**

```java
@EntityGraph(
    attributePaths = {"appUser"},
    type = EntityGraph.EntityGraphType.LOAD
)
List<Task> findAll();
// appUser: EAGER (in graph)
// project: LAZY (not in graph, forced lazy)
```

### Which to Use?

| Type | Use When |
|------|----------|
| **FETCH** | You want entity defaults for unspecified attributes |
| **LOAD** | You want to ensure only specified attributes are eager |

**Recommendation:** Use `FETCH` (default) for most cases. Use `LOAD` when you need strict control to prevent accidental eager loading.

---

## Best Practices

### 1. Define Graphs Based on Use Cases

Create specific graphs for specific use cases:

```java
public interface TaskRepository extends JpaRepository<Task, Long> {

    // For list views - minimal data
    @EntityGraph(attributePaths = {"appUser"})
    List<Task> findAllForList();

    // For detail views - full data
    @EntityGraph(attributePaths = {"appUser", "project", "project.appUser"})
    Optional<Task> findDetailById(Long id);

    // For reports - just task and project
    @EntityGraph(attributePaths = {"project"})
    List<Task> findAllForReport();
}
```

### 2. Use Separate Methods for Different Fetch Needs

Don't overload the same method with different graphs:

```java
// BAD - confusing, can't have two findAll() with different graphs
// (Actually won't compile)

// GOOD - clear intent
List<Task> findAllForList();      // with appUser
List<Task> findAllForDetail();    // with appUser and project
List<Task> findAllForExport();    // with everything
```

### 3. Use Descriptive Method Names Matching Fields

Name your methods to clearly indicate which relationships are fetched:

```java
// IMPORTANT: For methods WITHOUT query criteria (like findAll variants),
// you must add @Query because Spring Data can't derive a query from
// names like "findAllWithAppUser".

@EntityGraph(attributePaths = {"appUser", "project"})
@Query("SELECT t FROM Task t")  // Required! Spring Data can't parse "WithAppUserAndProject"
List<Task> findAllWithAppUserAndProject();

// Methods WITH criteria work because Spring Data understands "ByStatus"
@EntityGraph(attributePaths = {"appUser", "project"})
List<Task> findWithAppUserAndProjectByStatus(TaskStatus status);

@EntityGraph(attributePaths = {"appUser", "project"})
Optional<Task> findWithAppUserAndProjectById(Long id);
```

**Why the @Query is needed:** Spring Data JPA derives queries from method names. For `findAllWithAppUser`, it tries to parse "WithAppUser" as a property path on the entity. Since there's no such property, it fails. The `@Query` annotation tells Spring Data exactly what query to run.

### 4. Document Complex Graphs

```java
/**
 * Fetches task with full context for detail view.
 *
 * Includes:
 * - appUser: task assignee
 * - project: task's project
 * - project.appUser: project owner
 *
 * Use for: Task detail page, task editing
 */
@EntityGraph(attributePaths = {"appUser", "project", "project.appUser"})
Optional<Task> findDetailById(Long id);
```

### 5. Keep Graphs Minimal

Only fetch what you need:

```java
// BAD - fetching everything "just in case"
@EntityGraph(attributePaths = {"appUser", "project", "project.appUser", "project.tasks"})
List<Task> findAll();

// GOOD - fetch only what this use case needs
@EntityGraph(attributePaths = {"appUser"})
List<Task> findAllForListView();
```

---

## Common Pitfalls

### 1. MultipleBagFetchException

**Problem:** Fetching multiple `@OneToMany` collections:

```java
@Entity
public class Project {
    @OneToMany(mappedBy = "project")
    private List<Task> tasks;

    @OneToMany(mappedBy = "project")
    private List<Comment> comments;
}

// This will fail!
@EntityGraph(attributePaths = {"tasks", "comments"})
List<Project> findAll();
// ERROR: MultipleBagFetchException
```

**Solution:** Use `Set` instead of `List`, or fetch one collection per query:

```java
// Option 1: Use Set
@OneToMany(mappedBy = "project")
private Set<Task> tasks;

// Option 2: Separate queries
List<Project> projects = projectRepo.findAllWithTasks();
// Then fetch comments separately if needed
```

### 2. Cartesian Product with Collections

**Problem:** Fetching `@OneToMany` creates cartesian product:

```java
// Project has 10 tasks
@EntityGraph(attributePaths = {"tasks"})
List<Project> findAll();
// Returns: Project duplicated 10 times in result list!
```

**Solution:** Use `DISTINCT`:

```java
@EntityGraph(attributePaths = {"tasks"})
@Query("SELECT DISTINCT p FROM Project p")
List<Project> findAllWithTasks();
```

### 3. Pagination with Collections

**Problem:** Pagination + collection fetch = in-memory pagination:

```java
@EntityGraph(attributePaths = {"tasks"})
Page<Project> findAll(Pageable pageable);
// WARNING: HHH90003004: firstResult/maxResults specified with collection fetch
// Hibernate fetches ALL rows, then paginates in memory!
```

**Solution:** Don't use entity graphs for `@OneToMany` with pagination. Use separate queries or DTOs.

### 4. Overriding `findById` Doesn't Always Work

Spring Data's `findById` from `CrudRepository` may not pick up your `@EntityGraph` if there's a method signature conflict.

**Solution:** Use a differently named method:

```java
// May not work as expected
@EntityGraph(attributePaths = {"appUser"})
Optional<Task> findById(Long id);

// Definitely works
@EntityGraph(attributePaths = {"appUser"})
Optional<Task> findDetailById(Long id);
```

### 5. N+1 in Service Layer After Fetch

**Problem:** Fetching relationship, but then accessing something not in the graph:

```java
@EntityGraph(attributePaths = {"appUser"})  // Only appUser
List<Task> findAll();

// Service
List<Task> tasks = repo.findAll();
tasks.forEach(t -> {
    t.getAppUser();   // OK - was fetched
    t.getProject();   // N+1! Not in the graph, triggers lazy load
});
```

**Solution:** Include all needed relationships in the graph.

---

## Implementation Reference

### Task Manager Files

| File | Usage |
|------|-------|
| `TaskRepository.java` | Entity graph annotations for task fetching |
| `ProjectRepository.java` | Entity graph annotations for project fetching |
| `Task.java` | Optional: `@NamedEntityGraph` definitions |
| `Project.java` | Optional: `@NamedEntityGraph` definitions |

### Example: Task Repository with Entity Graphs

```java
public interface TaskRepository extends JpaRepository<Task, Long> {

    // Standard findAll - no relationships
    // (inherited from JpaRepository)

    // For "findAll" variants, @Query is REQUIRED because Spring Data
    // can't derive a query from method names like "findAllWithAppUser"
    @EntityGraph(attributePaths = {"appUser", "project"})
    @Query("SELECT t FROM Task t")
    List<Task> findAllWithAppUserAndProject();

    // Detail view - with assignee and project
    // No @Query needed - "ById" is a valid Spring Data derived query
    @EntityGraph(attributePaths = {"appUser", "project"})
    Optional<Task> findWithAppUserAndProjectById(Long id);

    // Filter queries with graphs - work without @Query
    @EntityGraph(attributePaths = {"appUser", "project"})
    List<Task> findWithAppUserAndProjectByStatus(TaskStatus status);

    @EntityGraph(attributePaths = {"appUser", "project"})
    List<Task> findWithAppUserAndProjectByAppUserId(Long userId);
}
```

### Example: Service Layer Usage

```java
@Service
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;

    // List - fetches all with relationships
    public List<TaskResponseDto> getAllTasks() {
        return taskMapper.toResponseDtoList(
            taskRepository.findAllWithAppUserAndProject()  // Single query with JOINs
        );
    }

    // Detail - full fetch
    public TaskResponseDto getTaskById(Long id) {
        return taskRepository.findWithAppUserAndProjectById(id)  // Single query
            .map(taskMapper::toResponseDto)
            .orElseThrow(() -> new ResourceNotFoundException("Task", id));
    }

    // Filter by status - with relationships
    public List<TaskResponseDto> getTasksByStatus(TaskStatus status) {
        return taskMapper.toResponseDtoList(
            taskRepository.findWithAppUserAndProjectByStatus(status)
        );
    }
}
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **Entity Graph** | Declarative fetch plan for relationships |
| **`attributePaths`** | Ad-hoc graph definition on repository method |
| **`@NamedEntityGraph`** | Reusable graph defined on entity |
| **FETCH type** | Specified = eager, others = entity default |
| **LOAD type** | Specified = eager, others = lazy |
| **N+1 Solution** | Fetch related entities in single query |
| **Best for** | `@ManyToOne`, `@OneToOne` relationships |
| **Careful with** | `@OneToMany` + pagination |

---

**Next Steps:**
- See [07-jpa-entities.md](./07-jpa-entities.md) for relationship definitions
- See [08-jpa-entity-lifecycle.md](./08-jpa-entity-lifecycle.md) for lazy loading context
- See [17-mapstruct.md](./17-mapstruct.md) for mapping fetched entities to DTOs
