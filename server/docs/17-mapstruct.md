# MapStruct - Object Mapping Framework

## Table of Contents
- [What is MapStruct?](#what-is-mapstruct)
- [Why Use MapStruct?](#why-use-mapstruct)
- [How MapStruct Works](#how-mapstruct-works)
- [MapStruct + Lombok Integration](#mapstruct--lombok-integration)
- [Setting Up MapStruct](#setting-up-mapstruct)
- [Creating Mappers](#creating-mappers)
- [Using Mappers in Services](#using-mappers-in-services)
- [Advanced Mapping Scenarios](#advanced-mapping-scenarios)
- [Partial Updates and Null Handling Strategy](#partial-updates-and-null-handling-strategy)
- [Best Practices](#best-practices)
- [Common Pitfalls](#common-pitfalls)
- [Troubleshooting](#troubleshooting)

---

## What is MapStruct?

**MapStruct** is a code generator for Java that simplifies the implementation of mappings between Java beans (e.g., entities and DTOs). It generates type-safe, performant mapper implementations at **compile-time** based on interface definitions.

### Key Characteristics

- **Compile-Time Generation**: Generates mapper code during compilation, not at runtime
- **Type-Safe**: Compiler verifies mappings between source and target types
- **Zero Runtime Overhead**: Generated code is as fast as hand-written code
- **Minimal Reflection**: Uses direct method calls instead of reflection
- **Convention-over-Configuration**: Automatically maps fields with matching names
- **Annotation-Driven**: Uses simple `@Mapper` annotation to define mappings

### MapStruct vs Manual Mapping

| Aspect | MapStruct | Manual Mapping |
|--------|-----------|----------------|
| **Development Time** | Fast - reduces boilerplate by 70-80% | Slow - must write all conversions |
| **Maintenance** | Low - changes reflected automatically | High - must update manually |
| **Performance** | Excellent - generated code is optimal | Excellent - equivalent when well-written |
| **Type Safety** | Compile-time checked | Runtime errors possible |
| **Debugging** | Can view generated source | Direct code visibility |
| **Learning Curve** | Moderate - requires understanding annotations | Minimal - straightforward |

---

## Why Use MapStruct?

### 1. Eliminates Boilerplate

**Without MapStruct** (manual conversion in DTO):
```java
public class AppUserCreateDto {
    private String username;
    private String email;
    private String password;

    // Violation of Separation of Concerns!
    public AppUser convert() {
        return AppUser.builder()
            .username(username)
            .email(email)
            .password(password)
            .build();
    }
}
```

**With MapStruct**:
```java
public class AppUserCreateDto {
    private String username;
    private String email;
    private String password;
    // Clean! No conversion logic here.
}

@Mapper(componentModel = "spring")
public interface AppUserMapper {
    AppUser toEntity(AppUserCreateDto dto);
}
```

### 2. Maintains Separation of Concerns

- **DTOs** remain pure data containers
- **Mappers** handle conversion logic
- **Services** use mappers without knowing implementation details
- **Entities** don't know about DTOs

### 3. Type Safety

MapStruct catches mapping errors at **compile-time**:

```java
@Mapper(componentModel = "spring")
public interface TaskMapper {
    // If Task doesn't have 'titel' (typo), compile error!
    @Mapping(source = "titel", target = "title")
    TaskResponseDto toDto(Task task);
}
```

### 4. Performance

Generated code looks like this (optimized):

```java
@Component
public class AppUserMapperImpl implements AppUserMapper {
    @Override
    public AppUser toEntity(AppUserCreateDto dto) {
        if (dto == null) return null;

        AppUser appUser = new AppUser();
        appUser.setUsername(dto.getUsername());
        appUser.setEmail(dto.getEmail());
        appUser.setPassword(dto.getPassword());
        return appUser;
    }
}
```

No reflection, no runtime overhead!

---

## How MapStruct Works

### 1. Annotation Processing

```
┌─────────────────────┐
│  Compile Java Code  │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  Lombok Processor   │  ← Generates getters/setters first
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ MapStruct Processor │  ← Generates mapper implementations
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  Generated Classes  │  ← AppUserMapperImpl, TaskMapperImpl, etc.
└─────────────────────┘
```

### 2. Generated Code Location

MapStruct generates implementations in `target/generated-sources/annotations/`:

```
target/
└── generated-sources/
    └── annotations/
        └── com/tutorial/taskmanager/mapper/
            ├── AppUserMapperImpl.java
            ├── TaskMapperImpl.java
            └── ProjectMapperImpl.java
```

You can view these to understand how MapStruct implements your mappings!

### 3. Spring Integration

When you use `componentModel = "spring"`, MapStruct generates `@Component`:

```java
@Component  // ← MapStruct adds this automatically
public class AppUserMapperImpl implements AppUserMapper {
    // Implementation...
}
```

This makes the mapper a Spring bean that can be `@Autowired` or constructor-injected.

---

## MapStruct + Lombok Integration

### The Challenge

Both Lombok and MapStruct are annotation processors that run at compile-time. **Order matters!**

- **Lombok** generates getters, setters, builders (code that MapStruct needs)
- **MapStruct** generates mappers (uses the code Lombok generated)

If MapStruct runs first, it won't see Lombok-generated methods → compilation error.

### The Solution

Configure `maven-compiler-plugin` with correct processor order:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <!-- 1. Lombok MUST come FIRST -->
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
            </path>

            <!-- 2. Binding for compatibility -->
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok-mapstruct-binding</artifactId>
                <version>0.2.0</version>
            </path>

            <!-- 3. MapStruct comes LAST -->
            <path>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct-processor</artifactId>
                <version>${mapstruct.version}</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

### lombok-mapstruct-binding

This binding ensures Lombok and MapStruct work together correctly:
- Prevents duplicate annotations
- Handles `@Builder` integration
- Ensures proper ordering

---

## Setting Up MapStruct

### Step 1: Add Dependencies to `pom.xml`

```xml
<properties>
    <mapstruct.version>1.6.3</mapstruct.version>
    <lombok-mapstruct-binding.version>0.2.0</lombok-mapstruct-binding.version>
</properties>

<dependencies>
    <!-- MapStruct -->
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
        <version>${mapstruct.version}</version>
    </dependency>
</dependencies>
```

### Step 2: Configure Annotation Processors

See the complete configuration in [MapStruct + Lombok Integration](#mapstruct--lombok-integration) above.

### Step 3: Verify Setup

Run `mvn clean compile` and check for:

1. **No compilation errors**
2. **Generated mappers** in `target/generated-sources/annotations/`
3. **Warnings about unmapped properties** (if configured)

---

## Creating Mappers

### Basic Mapper Interface

```java
package com.tutorial.taskmanager.mapper;

import com.tutorial.taskmanager.dto.appuser.*;
import com.tutorial.taskmanager.model.AppUser;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface AppUserMapper {

    // CreateDto → Entity
    AppUser toEntity(AppUserCreateDto dto);

    // Entity → ResponseDto
    AppUserResponseDto toResponseDto(AppUser entity);

    // List conversion (automatically generated)
    List<AppUserResponseDto> toResponseDtoList(List<AppUser> entities);
}
```

### Automatic List Mapping

One of MapStruct's powerful features is **automatic list mapping**. When you define a method that maps a single object, MapStruct can automatically generate list versions:

```java
@Mapper(componentModel = "spring")
public interface AppUserMapper {

    // Define single object mapping
    AppUserResponseDto toResponseDto(AppUser entity);

    // MapStruct automatically implements this using toResponseDto() internally!
    List<AppUserResponseDto> toResponseDtoList(List<AppUser> entities);
}
```

**How it works**: MapStruct sees that:
1. You have `toResponseDto(AppUser)` that returns `AppUserResponseDto`
2. You want `toResponseDtoList(List<AppUser>)` returning `List<AppUserResponseDto>`
3. It generates code that iterates and calls `toResponseDto()` for each item

**Generated code looks like:**
```java
@Override
public List<AppUserResponseDto> toResponseDtoList(List<AppUser> entities) {
    if (entities == null) {
        return null;
    }

    List<AppUserResponseDto> list = new ArrayList<>(entities.size());
    for (AppUser appUser : entities) {
        list.add(toResponseDto(appUser));  // Reuses single-object mapper!
    }
    return list;
}
```

**Key points:**
- You just declare the method signature - MapStruct implements it
- Null input returns null (safe by default)
- Uses `ArrayList` with pre-sized capacity for performance
- Works with any collection type (`List`, `Set`, `Collection`)
- The single-object mapper (`toResponseDto`) must exist

**Usage in service layer:**
```java
public List<TaskResponseDto> findAll() {
    List<Task> tasks = taskRepository.findAll();
    return taskMapper.toResponseDtoList(tasks);  // One line!
}

public List<TaskResponseDto> findByStatus(TaskStatus status) {
    List<Task> tasks = taskRepository.findByStatus(status);
    return taskMapper.toResponseDtoList(tasks);  // Reuse everywhere
}
```

### Configuration Options

| Option | Values | Purpose |
|--------|--------|---------|
| `componentModel` | `"spring"`, `"cdi"`, `"default"` | Makes mapper a Spring/CDI bean |
| `unmappedTargetPolicy` | `ERROR`, `WARN`, `IGNORE` | What to do with unmapped fields |
| `unmappedSourcePolicy` | `ERROR`, `WARN`, `IGNORE` | What to do with unused source fields |

**Recommendation**: Use `WARN` during development to catch missing mappings.

### Mapping with Relationships

When entities have relationships, map to IDs in DTOs:

```java
@Mapper(componentModel = "spring")
public interface TaskMapper {

    // Entity → DTO: Extract IDs from relationships
    @Mapping(source = "appUser.id", target = "appUserId")
    @Mapping(source = "project.id", target = "projectId")
    TaskResponseDto toResponseDto(Task entity);

    // DTO → Entity: Relationships set in service layer
    @Mapping(target = "appUser", ignore = true)
    @Mapping(target = "project", ignore = true)
    Task toEntity(TaskCreateDto dto);
}
```

**Why ignore relationships?**
- MapStruct can't convert `Long id` to `AppUser entity` automatically
- Service layer fetches entities by ID and sets relationships

### Update Mappings

For update operations, use `@MappingTarget`:

```java
@Mapper(componentModel = "spring")
public interface AppUserMapper {

    // Updates existing entity in-place
    @Mapping(target = "username", ignore = true)  // Immutable
    @Mapping(target = "tasks", ignore = true)     // Not in DTO
    @Mapping(target = "projects", ignore = true)  // Not in DTO
    void updateEntityFromDto(
        AppUserUpdateDto dto,
        @MappingTarget AppUser entity
    );
}
```

This modifies the `entity` parameter instead of creating a new object.

### Ignoring Inherited Fields

If your entities extend `BaseEntity`, you don't need to explicitly ignore inherited fields:

```java
// BaseEntity has: id, createdTimestamp, updatedTimestamp

@Mapper(componentModel = "spring")
public interface AppUserMapper {

    // No need to ignore BaseEntity fields!
    // MapStruct skips fields not in the DTO automatically
    @Mapping(target = "tasks", ignore = true)
    @Mapping(target = "projects", ignore = true)
    AppUser toEntity(AppUserCreateDto dto);
}
```

---

## Using Mappers in Services

### Service Layer Pattern

```java
@Service
@RequiredArgsConstructor  // Constructor injection for mapper
public class AppUserService {
    private final AppUserRepository repository;
    private final AppUserMapper mapper;  // ← Injected by Spring

    // Create: DTO → Entity → Save → DTO
    public AppUserResponseDto createAppUser(AppUserCreateDto dto) {
        // Validate...

        AppUser entity = mapper.toEntity(dto);
        entity = repository.save(entity);
        return mapper.toResponseDto(entity);
    }

    // Read: Entity → DTO
    public Optional<AppUserResponseDto> findById(Long id) {
        return repository.findById(id)
            .map(mapper::toResponseDto);
    }

    // Update: Fetch entity, apply DTO changes, save, return DTO
    public AppUserResponseDto updateAppUser(Long id, AppUserUpdateDto dto) {
        AppUser entity = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("appUser", id));

        // Validate...

        mapper.updateEntityFromDto(dto, entity);  // In-place update
        entity = repository.save(entity);
        return mapper.toResponseDto(entity);
    }

    // List: Entities → DTOs
    public List<AppUserResponseDto> findAll() {
        List<AppUser> entities = repository.findAll();
        return mapper.toResponseDtoList(entities);
    }
}
```

### Dual Methods Pattern

Sometimes you need both entity and DTO access:

```java
@Service
@RequiredArgsConstructor
public class AppUserService {
    private final AppUserRepository repository;
    private final AppUserMapper mapper;

    // Public API: Returns DTO
    public AppUserResponseDto getById(Long id) {
        AppUser entity = getEntityById(id);
        return mapper.toResponseDto(entity);
    }

    // Internal API: Returns entity (for other services)
    AppUser getEntityById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("appUser", id));
    }
}
```

**Use case**: `TaskService` needs the actual `AppUser` entity to set relationships:

```java
@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final AppUserService appUserService;  // Other service

    public TaskResponseDto createTask(TaskCreateDto dto) {
        // Get entity (not DTO) from AppUserService
        AppUser appUser = appUserService.getEntityById(dto.getAppUserId());

        // Map DTO → Entity
        Task task = taskMapper.toEntity(dto);

        // Set relationship
        task.setAppUser(appUser);

        // Save and return DTO
        task = taskRepository.save(task);
        return taskMapper.toResponseDto(task);
    }
}
```

---

## Advanced Mapping Scenarios

### Custom Mapping Methods

When automatic mapping isn't enough:

```java
@Mapper(componentModel = "spring")
public interface TaskMapper {

    // Automatic mapping
    TaskResponseDto toResponseDto(Task task);

    // Custom mapping with different field names
    @Mapping(source = "appUser.username", target = "assignedToUsername")
    @Mapping(source = "project.name", target = "projectName")
    TaskDetailDto toDetailDto(Task task);
}
```

### Default Values

```java
@Mapper(componentModel = "spring")
public interface TaskMapper {

    @Mapping(target = "status", defaultValue = "TODO")
    Task toEntity(TaskCreateDto dto);
}
```

If `dto.getStatus()` is null, MapStruct sets `TaskStatus.TODO`.

### Conditional Mapping

Use default methods for complex logic:

```java
@Mapper(componentModel = "spring")
public interface TaskMapper {

    @Mapping(target = "dueDate", source = "dto", qualifiedByName = "mapDueDate")
    Task toEntity(TaskCreateDto dto);

    @Named("mapDueDate")
    default LocalDateTime mapDueDate(TaskCreateDto dto) {
        if (dto.getDueDate() == null) {
            // Default: 7 days from now
            return LocalDateTime.now().plusDays(7);
        }
        return dto.getDueDate();
    }
}
```

### Using Other Mappers

Mappers can depend on other mappers:

```java
@Mapper(
    componentModel = "spring",
    uses = {AppUserMapper.class, ProjectMapper.class}
)
public interface TaskMapper {

    // MapStruct automatically uses AppUserMapper and ProjectMapper
    // to convert nested objects
    TaskDetailDto toDetailDto(Task task);
}
```

### Automatic Nested Object Mapping

One of MapStruct's most powerful features is its ability to **automatically find and use mapping methods** for nested objects. If your target DTO contains a nested object type, MapStruct will look for a suitable mapping method and call it automatically.

#### How It Works

When MapStruct encounters a nested object that needs mapping:

1. **Same Mapper**: First, it looks for a method in the **same mapper interface**
2. **Uses Mappers**: Then, it checks mappers specified in the `uses` attribute
3. **Automatic Conversion**: If found, it calls that method automatically

**Example: Embedding ProjectSummary in TaskResponse**

Suppose you want your `TaskResponseDto` to include project details (not just an ID):

```java
// A lightweight DTO for embedding in other responses
public class ProjectSummaryDto {
    private Long id;
    private String name;
    private ProjectStatus status;
}

// Task response with embedded project summary
public class TaskResponseDto {
    private Long id;
    private String title;
    private TaskStatus status;
    private ProjectSummaryDto project;  // Embedded object, not just projectId!
}
```

**Mapper Implementation:**

```java
@Mapper(componentModel = "spring")
public interface TaskMapper {

    // MapStruct sees that Task has a Project field
    // and TaskResponseDto has a ProjectSummaryDto field
    // It automatically calls toProjectSummary() below!
    TaskResponseDto toResponseDto(Task task);

    // Define the nested mapping in the SAME mapper
    ProjectSummaryDto toProjectSummary(Project project);
}
```

**Generated Code (simplified):**

```java
@Component
public class TaskMapperImpl implements TaskMapper {

    @Override
    public TaskResponseDto toResponseDto(Task task) {
        if (task == null) {
            return null;
        }

        TaskResponseDto dto = new TaskResponseDto();
        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setStatus(task.getStatus());

        // MapStruct automatically calls the nested mapping method!
        dto.setProject(toProjectSummary(task.getProject()));

        return dto;
    }

    @Override
    public ProjectSummaryDto toProjectSummary(Project project) {
        if (project == null) {
            return null;
        }

        ProjectSummaryDto dto = new ProjectSummaryDto();
        dto.setId(project.getId());
        dto.setName(project.getName());
        dto.setStatus(project.getStatus());

        return dto;
    }
}
```

#### Works with Collections Too

The automatic mapping also works for collections:

```java
@Mapper(componentModel = "spring")
public interface ProjectMapper {

    // ProjectResponseDto has List<TaskSummaryDto> tasks
    ProjectResponseDto toResponseDto(Project project);

    // MapStruct uses this for each Task in the list
    TaskSummaryDto toTaskSummary(Task task);
}
```

MapStruct will iterate over the `tasks` list and call `toTaskSummary()` for each item.

#### Same Mapper vs Uses Attribute

| Approach | When to Use |
|----------|-------------|
| **Same Mapper** | Simple, single-mapper scenario. Keeps related mappings together. |
| **`uses` Attribute** | When the nested mapper is complex or reused by multiple mappers. |

**Same Mapper (simpler):**
```java
@Mapper(componentModel = "spring")
public interface TaskMapper {
    TaskResponseDto toResponseDto(Task task);
    ProjectSummaryDto toProjectSummary(Project project);  // In same interface
}
```

**Uses Attribute (for shared mappers):**

When the nested mapping lives in a different mapper, use `uses` to reference it:

```java
// ProjectMapper.java - contains the summary mapping
@Mapper(componentModel = "spring")
public interface ProjectMapper {

    ProjectResponseDto toResponseDto(Project project);

    // This method will be used by other mappers
    ProjectSummaryDto toSummaryDto(Project project);
}
```

```java
// TaskMapper.java - references ProjectMapper
@Mapper(
    componentModel = "spring",
    uses = ProjectMapper.class  // Tell MapStruct to look here for mappings
)
public interface TaskMapper {

    // MapStruct finds ProjectMapper.toSummaryDto() automatically
    TaskResponseDto toResponseDto(Task task);
}
```

**How MapStruct resolves it:**

1. Sees `Task.project` (type `Project`) → `TaskResponseDto.project` (type `ProjectSummaryDto`)
2. Looks in `TaskMapper` for a method `Project → ProjectSummaryDto` — not found
3. Looks in `uses` mappers (`ProjectMapper`) — finds `toSummaryDto(Project): ProjectSummaryDto`
4. Generates code that **injects** `ProjectMapper` and calls it

**Generated code with dependency injection:**

```java
@Component
public class TaskMapperImpl implements TaskMapper {

    @Autowired
    private ProjectMapper projectMapper;  // Injected as Spring bean!

    @Override
    public TaskResponseDto toResponseDto(Task task) {
        if (task == null) {
            return null;
        }

        TaskResponseDto dto = new TaskResponseDto();
        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setStatus(task.getStatus());

        // Delegates to the injected mapper
        dto.setProject(projectMapper.toSummaryDto(task.getProject()));

        return dto;
    }
}
```

**Key insight:** The method signature must match — MapStruct looks for any method in the `uses` mappers that takes `Project` as input and returns `ProjectSummaryDto`. Method names don't matter, only the type signature.

#### Key Points

1. **No Explicit Mapping Needed**: MapStruct finds the method by matching types automatically
2. **Null-Safe**: Generated code always checks for null before calling nested mappings
3. **Field Name Matching**: The source field (`project`) and target field (`project`) must have matching names (or use `@Mapping` to specify)
4. **Priority**: Methods in the same mapper take precedence over `uses` mappers

#### Common Use Cases

| Scenario | Example |
|----------|---------|
| **Embedding related entity** | `TaskResponseDto` contains `ProjectSummaryDto` |
| **User info in response** | `TaskResponseDto` contains `UserSummaryDto` (id, username) |
| **Nested collections** | `ProjectResponseDto` contains `List<TaskSummaryDto>` |
| **Avoiding circular refs** | Use `SummaryDto` (no nested objects) instead of full `ResponseDto` |

#### JPA Lazy Loading Consideration

When mapping nested objects, be aware of **JPA lazy loading**. If the relationship is `FetchType.LAZY`, calling `task.getProject()` in the mapper **triggers a database query**.

| Scenario | What Happens |
|----------|--------------|
| **Within transaction** (entity is managed) | Lazy fetch executes, additional SQL query runs |
| **Outside transaction** (entity is detached) | `LazyInitializationException` is thrown |

**Safe pattern — map inside `@Transactional`:**

```java
@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;

    @Transactional(readOnly = true)
    public TaskResponseDto getById(Long id) {
        Task task = taskRepository.findById(id).orElseThrow();

        // Still within @Transactional, so lazy fetch works
        return taskMapper.toResponseDto(task);  // task.getProject() triggers SQL
    }
}
```

**Problem — mapping after transaction closes:**

```java
// In controller (outside transaction)
@GetMapping("/{id}")
public TaskResponseDto getTask(@PathVariable Long id) {
    Task task = taskService.getEntityById(id);  // Transaction ends here
    return taskMapper.toResponseDto(task);      // LazyInitializationException!
}
```

**Solutions:**

| Solution | Pros | Cons |
|----------|------|------|
| **Map in service layer** (recommended) | Clean, predictable | Must return DTOs from service |
| **Eager fetch** (`FetchType.EAGER`) | Simple | N+1 query problems, loads data you may not need |
| **Join fetch in query** | Efficient, explicit | Custom query per use case |
| **Entity graph** ([see doc](10-jpa-entity-graph.md)) | Declarative, reusable | More complex setup |

**Join fetch example:**

```java
public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("SELECT t FROM Task t JOIN FETCH t.project WHERE t.id = :id")
    Optional<Task> findByIdWithProject(@Param("id") Long id);
}
```

**Best practice:** Always perform mapping inside the `@Transactional` service method. Return DTOs from services, never entities. This ensures lazy loading works and keeps your API boundaries clean.

This automatic behavior is why MapStruct is so powerful - you just define the mapping methods, and it figures out how to compose them together!

### Enums with Different Names

```java
// Source enum
public enum TaskStatus {
    TODO, IN_PROGRESS, COMPLETED
}

// Target enum
public enum TaskStatusDto {
    PENDING, ACTIVE, DONE
}

@Mapper(componentModel = "spring")
public interface TaskMapper {

    @ValueMapping(source = "TODO", target = "PENDING")
    @ValueMapping(source = "IN_PROGRESS", target = "ACTIVE")
    @ValueMapping(source = "COMPLETED", target = "DONE")
    TaskStatusDto mapStatus(TaskStatus status);
}
```

### Partial Updates and Null Handling Strategy

When implementing update operations, you need to handle **partial updates** where only some fields are sent. By default, MapStruct copies ALL values including nulls, which can accidentally overwrite existing data.

#### The Problem

```java
// Default behavior - nulls OVERWRITE existing values!
@Mapping(target = "appUser", ignore = true)
void updateEntityFromDto(TaskUpdateDto dto, @MappingTarget Task entity);
```

```java
// Client sends only title, other fields are null in DTO
TaskUpdateDto dto = new TaskUpdateDto();
dto.setTitle("New Title");
// dto.status = null, dto.description = null, dto.dueDate = null

taskMapper.updateEntityFromDto(dto, task);
// Result: title="New Title", status=NULL!, description=NULL!, dueDate=NULL!
// Existing values were OVERWRITTEN with nulls!
```

#### The Solution: `@BeanMapping` with IGNORE Strategy

Use `@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)` on update methods:

```java
@Mapper(componentModel = "spring")
public interface TaskMapper {

    // Create: nulls are fine (new object)
    Task toEntity(TaskCreateDto dto);

    // Update: IGNORE nulls to preserve existing values
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "appUser", ignore = true)
    @Mapping(target = "project", ignore = true)
    void patchEntityFromDto(TaskUpdateDto dto, @MappingTarget Task entity);
}
```

Now partial updates work correctly:

```java
TaskUpdateDto dto = new TaskUpdateDto();
dto.setTitle("New Title");  // Only this field sent

taskMapper.patchEntityFromDto(dto, task);
// Result: title="New Title", status=PRESERVED, description=PRESERVED, dueDate=PRESERVED
```

#### NullValuePropertyMappingStrategy Options

MapStruct provides different strategies for handling null values:

| Strategy | Behavior | Use Case |
|----------|----------|----------|
| `SET_TO_NULL` (default) | Null values overwrite target fields with null | PUT (full replacement) |
| `IGNORE` | Null values are skipped, target unchanged | PATCH (partial update) |
| `SET_TO_DEFAULT` | Null values set target to default (0, false, empty collection) | Special cases |

#### Supporting Both PUT and PATCH

If your API needs both full replacement (PUT) and partial updates (PATCH), create separate methods:

```java
@Mapper(componentModel = "spring")
public interface TaskMapper {

    // For PATCH - partial updates, nulls ignored
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "appUser", ignore = true)
    @Mapping(target = "project", ignore = true)
    void patchEntityFromDto(TaskUpdateDto dto, @MappingTarget Task entity);

    // For PUT - full replacement, nulls overwrite existing values
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "appUser", ignore = true)
    @Mapping(target = "project", ignore = true)
    void updateEntityFromDto(TaskUpdateDto dto, @MappingTarget Task entity);
}
```

Then in service:

```java
// PATCH /tasks/{id} - partial update
public TaskResponseDto patchTask(Long id, TaskUpdateDto dto) {
    Task task = taskRepository.findById(id).orElseThrow();
    taskMapper.patchEntityFromDto(dto, task);  // Nulls ignored
    return taskMapper.toResponseDto(taskRepository.save(task));
}

// PUT /tasks/{id} - full replacement
public TaskResponseDto updateTask(Long id, TaskUpdateDto dto) {
    Task task = taskRepository.findById(id).orElseThrow();
    taskMapper.updateEntityFromDto(dto, task);  // Nulls overwrite
    return taskMapper.toResponseDto(taskRepository.save(task));
}
```

#### Method-Level vs Mapper-Level Configuration

You can set the null handling strategy at different levels:

**Method-level (recommended):**
```java
@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
void patchEntityFromDto(TaskUpdateDto dto, @MappingTarget Task entity);
```

**Mapper-level (affects ALL methods):**
```java
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE  // All methods!
)
public interface TaskMapper { ... }
```

**Best practice:** Use method-level `@BeanMapping` to have fine-grained control. This allows different strategies for different methods (PATCH vs PUT).

#### Trade-off: Cannot Set Fields to Null

With `IGNORE` strategy, you **cannot** intentionally clear a field to null via the same endpoint:

```json
{"title": "New"}                      // title updated, others preserved ✅
{"title": "New", "description": null} // description NOT cleared ❌ (null ignored)
```

**Common solutions in enterprise applications:**

1. **Use PUT for full replacement, PATCH for partial updates:**
   - `PUT /tasks/{id}` → Send ALL fields, nulls clear values
   - `PATCH /tasks/{id}` → Send only changed fields, nulls ignored

2. **Separate "clear" endpoints:**
   - `DELETE /tasks/{id}/due-date` → Clear the due date

3. **Empty string convention (for Strings):**
   ```java
   // In service, after mapper:
   if ("".equals(dto.getDescription())) {
       task.setDescription(null);
   }
   ```

4. **Accept the limitation:** Most fields shouldn't be nullable anyway. For the few that can be cleared, handle it manually or use a separate endpoint.

**Industry standard:** Most Spring Boot applications use `IGNORE` on update methods and accept the trade-off. If clearing fields is needed, it's handled separately.

---

## Best Practices

### 1. One Mapper Per Entity

Create a dedicated mapper for each entity:

```
mapper/
├── AppUserMapper.java
├── TaskMapper.java
└── ProjectMapper.java
```

### 2. Use componentModel = "spring"

Always integrate with Spring:

```java
@Mapper(componentModel = "spring")
public interface AppUserMapper { }
```

### 3. Configure Reporting Policies

Catch issues early:

```java
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface AppUserMapper { }
```

### 4. Map IDs, Not Nested Objects

**Don't** include nested objects in DTOs:

```java
// ❌ BAD: Causes circular references
public class TaskResponseDto {
    private AppUserResponseDto appUser;  // Nested object
    private ProjectResponseDto project;  // Nested object
}

// ✅ GOOD: Use IDs
public class TaskResponseDto {
    private Long appUserId;    // Just the ID
    private Long projectId;    // Just the ID
}
```

### 5. Keep DTOs Clean

DTOs should be pure data containers:

```java
// ✅ GOOD
public class AppUserCreateDto {
    private String username;
    private String email;
    private String password;
}

// ❌ BAD: Conversion logic in DTO
public class AppUserCreateDto {
    private String username;
    private String email;
    private String password;

    public AppUser convert() { /* ... */ }  // SoC violation!
}
```

### 6. Document Complex Mappings

Add Javadoc to explain non-obvious mappings:

```java
@Mapper(componentModel = "spring")
public interface TaskMapper {

    /**
     * Maps Task entity to detailed DTO.
     *
     * <p><strong>Relationship Handling:</strong>
     * Instead of nested objects, we map appUser.id → appUserId
     * to prevent circular references and lazy loading issues.
     */
    @Mapping(source = "appUser.id", target = "appUserId")
    TaskDetailDto toDetailDto(Task task);
}
```

### 7. Test Your Mappers

Test edge cases:

```java
@ExtendWith(MockitoExtension.class)
class AppUserMapperTest {

    @Autowired
    private AppUserMapper mapper;

    @Test
    void toEntity_withNullDto_returnsNull() {
        AppUser result = mapper.toEntity(null);
        assertThat(result).isNull();
    }

    @Test
    void toEntity_withValidDto_mapsAllFields() {
        AppUserCreateDto dto = AppUserCreateDto.builder()
            .username("testuser")
            .email("test@example.com")
            .password("password123")
            .build();

        AppUser entity = mapper.toEntity(dto);

        assertThat(entity.getUsername()).isEqualTo("testuser");
        assertThat(entity.getEmail()).isEqualTo("test@example.com");
        assertThat(entity.getPassword()).isEqualTo("password123");
    }
}
```

### 8. Mocking Mappers in Service Tests

When unit testing services that use mappers, **mock the mapper** just like you mock repositories:

```java
@ExtendWith(MockitoExtension.class)
class AppUserServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private AppUserMapper appUserMapper;  // Mock the mapper!

    @InjectMocks
    private AppUserService appUserService;

    private AppUser testUser;
    private AppUserResponseDto testUserResponseDto;

    @BeforeEach
    void setUp() {
        testUser = AppUser.builder()
            .username("testuser")
            .email("test@example.com")
            .build();
        testUser.setId(1L);

        // Create response DTO to return from mocked mapper
        testUserResponseDto = AppUserResponseDto.builder()
            .id(1L)
            .username("testuser")
            .email("test@example.com")
            .build();
    }

    @Test
    void shouldCreateUserSuccessfully() {
        // Arrange
        AppUserCreateDto createDto = AppUserCreateDto.builder()
            .username("newuser")
            .email("new@example.com")
            .password("password")
            .build();

        when(appUserRepository.existsByUsername(anyString())).thenReturn(false);
        when(appUserRepository.existsByEmail(anyString())).thenReturn(false);
        when(appUserMapper.toEntity(createDto)).thenReturn(testUser);  // Stub mapper
        when(appUserRepository.save(any())).thenReturn(testUser);
        when(appUserMapper.toResponseDto(testUser)).thenReturn(testUserResponseDto);  // Stub mapper

        // Act
        AppUserResponseDto result = appUserService.createAppUser(createDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");

        // Verify mapper was called
        verify(appUserMapper).toEntity(createDto);
        verify(appUserMapper).toResponseDto(testUser);
    }
}
```

**Key points for mocking mappers:**

| Method | How to Stub | How to Verify |
|--------|-------------|---------------|
| `toEntity(dto)` | `when(mapper.toEntity(dto)).thenReturn(entity)` | `verify(mapper).toEntity(dto)` |
| `toResponseDto(entity)` | `when(mapper.toResponseDto(entity)).thenReturn(dto)` | `verify(mapper).toResponseDto(entity)` |
| `toResponseDtoList(list)` | `when(mapper.toResponseDtoList(list)).thenReturn(dtoList)` | `verify(mapper).toResponseDtoList(list)` |
| `updateEntityFromDto(dto, entity)` | Void method - just verify | `verify(mapper).updateEntityFromDto(dto, entity)` |

**Why mock the mapper?**
- Unit tests should test ONE class in isolation
- Mocking the mapper lets you control exactly what it returns
- You're testing service LOGIC, not mapper logic
- Faster tests (no annotation processor needed)

---

## Common Pitfalls

### 1. Wrong Annotation Processor Order

**Problem**: Compilation errors about missing getters/setters

```
Unknown property "username" in result type AppUser.AppUserBuilder
```

**Solution**: Ensure Lombok comes before MapStruct in `pom.xml`

### 2. Trying to Map IDs to Entities Automatically

**Problem**: MapStruct can't convert `Long` to `AppUser`

```java
// ❌ DOESN'T WORK
@Mapper(componentModel = "spring")
public interface TaskMapper {
    Task toEntity(TaskCreateDto dto);  // dto has appUserId (Long)
}
```

**Solution**: Ignore the relationship, set it in service layer

```java
// ✅ WORKS
@Mapper(componentModel = "spring")
public interface TaskMapper {
    @Mapping(target = "appUser", ignore = true)
    Task toEntity(TaskCreateDto dto);
}

// In service:
Task task = mapper.toEntity(dto);
task.setAppUser(appUserService.getEntityById(dto.getAppUserId()));
```

### 3. Circular References in DTOs

**Problem**: Infinite recursion when serializing to JSON

```java
// ❌ Circular reference
public class TaskResponseDto {
    private AppUserResponseDto appUser;  // Contains tasks
}

public class AppUserResponseDto {
    private List<TaskResponseDto> tasks;  // Contains appUser
}
```

**Solution**: Use IDs, not nested objects

### 4. Forgetting @MappingTarget

**Problem**: Update method creates new object instead of modifying existing

```java
// ❌ Creates new entity, doesn't update existing
AppUser updateEntity(AppUserUpdateDto dto);

// ✅ Updates existing entity in-place
void updateEntityFromDto(AppUserUpdateDto dto, @MappingTarget AppUser entity);
```

### 5. Not Handling Null Values

MapStruct returns `null` if source is `null`:

```java
AppUser entity = mapper.toEntity(null);  // entity is null!
```

Always validate inputs in service layer before mapping.

### 6. Unmapped BaseEntity Fields Warnings

**Problem**: Warnings about `id`, `createdTimestamp`, `updatedTimestamp`

```
Unmapped target properties: "id, createdTimestamp, updatedTimestamp"
```

**Solution**: Either:
1. Ignore the warnings (these fields are managed by JPA)
2. Explicitly ignore them:

```java
@Mapping(target = "id", ignore = true)
@Mapping(target = "createdTimestamp", ignore = true)
@Mapping(target = "updatedTimestamp", ignore = true)
AppUser toEntity(AppUserCreateDto dto);
```

---

## Troubleshooting

### Viewing Generated Code

To understand what MapStruct generates:

1. Run `mvn clean compile`
2. Navigate to `target/generated-sources/annotations/`
3. Open `<YourMapper>Impl.java`

Example:

```java
// Generated by MapStruct
@Component
public class AppUserMapperImpl implements AppUserMapper {

    @Override
    public AppUser toEntity(AppUserCreateDto dto) {
        if (dto == null) {
            return null;
        }

        AppUser appUser = new AppUser();
        appUser.setUsername(dto.getUsername());
        appUser.setEmail(dto.getEmail());
        appUser.setPassword(dto.getPassword());

        return appUser;
    }

    // ... other methods
}
```

### Compilation Errors

**Error**: "cannot find symbol: class AppUserMapperImpl"

**Cause**: MapStruct didn't generate the implementation

**Solution**:
1. Check `pom.xml` for correct dependencies
2. Verify annotation processor configuration
3. Run `mvn clean compile` (not just `mvn compile`)

**Error**: "Unknown property 'xyz' in result type"

**Cause**: Field doesn't exist in target class, or Lombok didn't generate getter/setter

**Solution**:
1. Check field name spelling
2. Verify Lombok is before MapStruct in processor order
3. Ensure target class has `@Getter/@Setter` or `@Data`

### Runtime Errors

**Error**: "No qualifying bean of type 'AppUserMapper'"

**Cause**: Mapper not registered as Spring bean

**Solution**:
1. Ensure `componentModel = "spring"` in `@Mapper`
2. Check that generated class has `@Component` annotation
3. Verify mapper is in component scan package

### Build Performance

MapStruct can slow down compilation for large projects:

**Solution**: Use incremental compilation

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <useIncrementalCompilation>true</useIncrementalCompilation>
    </configuration>
</plugin>
```

---

## Summary

### When to Use MapStruct

✅ **Use MapStruct when:**
- You have many DTOs and entities
- You want type-safe mappings
- You value maintainability over simplicity
- Your team is comfortable with annotation processing
- You're building production applications

❌ **Don't use MapStruct when:**
- You have very few DTOs (1-2)
- Mappings are extremely complex and custom
- Your team prefers explicit code
- You're building quick prototypes

### Key Takeaways

1. **MapStruct generates code at compile-time** - no runtime overhead
2. **Lombok must run before MapStruct** - configure processor order correctly
3. **Use `componentModel = "spring"`** - integrates with Spring's dependency injection
4. **Map relationships to IDs in DTOs** - prevents circular references
5. **Keep DTOs clean** - no conversion logic, just data
6. **Service layer orchestrates** - fetches entities, uses mappers, sets relationships
7. **View generated code** - helps understand and debug mappings

### Resources

- **Official Documentation**: https://mapstruct.org/documentation/
- **Reference Guide**: https://mapstruct.org/documentation/stable/reference/html/
- **GitHub**: https://github.com/mapstruct/mapstruct
- **Examples**: https://github.com/mapstruct/mapstruct-examples

---

**Next Steps**:
- Explore [JPA Entities](07-jpa-entities.md) for database operations
- Learn about [DTOs](06-data-transfer-objects.md) for API design
- Read about [Unit Testing](16-unit-testing.md) for testing mappers
