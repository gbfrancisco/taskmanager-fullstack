# Dependency Injection and Inversion of Control

This guide covers Spring's core concepts: Inversion of Control (IoC), Dependency Injection (DI), beans, and how they all work together.

---

## Table of Contents
1. [What is Inversion of Control (IoC)?](#what-is-inversion-of-control-ioc)
2. [What is Dependency Injection (DI)?](#what-is-dependency-injection-di)
3. [Understanding Beans](#understanding-beans)
4. [The Application Context (IoC Container)](#the-application-context-ioc-container)
5. [Creating Beans with Stereotype Annotations](#creating-beans-with-stereotype-annotations)
6. [Creating Beans with @Bean Methods](#creating-beans-with-bean-methods)
7. [Dependency Injection Methods](#dependency-injection-methods)
8. [Bean Scopes](#bean-scopes)
9. [Autowiring and Qualifiers](#autowiring-and-qualifiers)
10. [Common Pitfalls](#common-pitfalls)

---

## What is Inversion of Control (IoC)?

**Inversion of Control** is a design principle where the control of object creation and dependency management is **inverted** from your code to a framework (Spring).

### Traditional Approach (You Control Everything)

```java
public class TaskService {
    // You create the dependency manually
    private TaskRepository repository = new TaskRepository();

    public void saveTask(Task task) {
        repository.save(task);
    }
}
```

**Problems:**
- Tight coupling (hard to change implementation)
- Hard to test (can't easily mock `TaskRepository`)
- You manage all dependencies manually
- Difficult to maintain as application grows

### IoC Approach (Spring Controls Creation)

```java
@Repository  // ← Spring manages this
public class TaskRepository {
    // Repository implementation
}

@Service  // ← Spring manages this too
public class TaskService {
    private final TaskRepository repository;

    // Spring provides the dependency
    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }

    public void saveTask(Task task) {
        repository.save(task);
    }
}
```

**Benefits:**
- Loose coupling (easy to swap implementations)
- Easy to test (inject mocks)
- Spring manages complexity
- Configuration in one place

### The Inversion

| Aspect | Traditional | IoC (Spring) |
|--------|-------------|--------------|
| **Who creates objects?** | You (`new`) | Spring Container |
| **Who manages lifecycle?** | You | Spring Container |
| **Who injects dependencies?** | You | Spring Container |
| **Where is configuration?** | Scattered in code | Centralized (annotations, config classes) |

**Control is "inverted"** - you don't control object creation anymore, Spring does!

---

## What is Dependency Injection (DI)?

**Dependency Injection** is a pattern where objects receive their dependencies from an external source rather than creating them.

It's the **mechanism** that implements IoC.

### Example Scenario

```java
// TaskService depends on TaskRepository
@Service
public class TaskService {
    private final TaskRepository repository;  // ← This is the dependency

    // Spring INJECTS the dependency via constructor
    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }
}
```

**What happens:**
1. Spring creates `TaskRepository` bean
2. Spring creates `TaskService` bean
3. Spring **injects** `TaskRepository` into `TaskService` constructor
4. You never call `new TaskService(...)` - Spring does it!

### Why "Injection"?

Dependencies are **injected** (provided from outside) rather than created internally.

```java
// NOT injection - object creates its own dependency
public class TaskService {
    private TaskRepository repository = new TaskRepository();
}

// YES injection - dependency provided from outside
public class TaskService {
    public TaskService(TaskRepository repository) {
        this.repository = repository;  // Injected!
    }
}
```

---

## Understanding Beans

A **bean** is an object that is managed by the Spring IoC container.

### What Makes Something a Bean?

**Simple rule:** If Spring manages it, it's a bean. If you create it with `new`, it's not a bean.

```java
// This is a bean - Spring manages it
@Service
public class TaskService {
    // ...
}

// This is NOT a bean - you create it manually
public class MyUtility {
    // ...
}

MyUtility util = new MyUtility();  // Not managed by Spring
```

### Characteristics of Beans

| Characteristic | Description |
|----------------|-------------|
| **Spring-managed** | Created and managed by Spring |
| **In Application Context** | Registered in the IoC container |
| **Injectable** | Can be injected into other beans |
| **Lifecycle-managed** | Spring handles creation, initialization, destruction |
| **Singleton by default** | One instance per Application Context (can be changed) |

### Bean Lifecycle

```
┌─────────────────────────────────────────────────────────────┐
│ 1. Bean Definition                                          │
│    Spring discovers class via @Component or @Bean           │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. Bean Instantiation                                       │
│    Spring creates instance via constructor                  │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. Dependency Injection                                     │
│    Spring injects required dependencies                     │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. Post-Processing                                          │
│    @PostConstruct methods called, AOP proxies applied       │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 5. Bean Ready                                               │
│    Bean is fully initialized and ready to use               │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 6. Bean Destruction (on shutdown)                           │
│    @PreDestroy methods called, resources cleaned up         │
└─────────────────────────────────────────────────────────────┘
```

### Critical Rule: You Can Only Inject Beans!

**This is extremely important:**

```java
@Repository  // ← This is a bean
public class TaskRepository {
    // ...
}

@Service  // ← This is also a bean
public class TaskService {
    private final TaskRepository repository;

    // ✅ WORKS: TaskRepository is a bean, can be injected
    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }
}
```

**What if `TaskRepository` wasn't a bean?**

```java
// NO @Repository annotation = NOT a bean
public class TaskRepository {
    // ...
}

@Service
public class TaskService {
    // ❌ FAILS: TaskRepository is not a bean!
    // Result: NoSuchBeanDefinitionException at startup
    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }
}
```

**Error you'd see:**
```
***************************
APPLICATION FAILED TO START
***************************

Description:

Parameter 0 of constructor in com.tutorial.taskmanager.service.TaskService required a bean of type 'com.tutorial.taskmanager.repository.TaskRepository' that could not be found.

Action:

Consider defining a bean of type 'com.tutorial.taskmanager.repository.TaskRepository' in your configuration.
```

**The fix:** Make `TaskRepository` a bean by adding `@Repository` (or `@Component`).

---

## The Application Context (IoC Container)

The **Application Context** is Spring's IoC container that holds and manages all beans.

### What It Does

```java
┌────────────────────────────────────────────────┐
│           Application Context                  │
│  (The IoC Container / Bean Factory)            │
│                                                 │
│  ┌──────────────────────────────────────┐     │
│  │ Bean: TaskService (singleton)        │     │
│  │ Dependencies: [TaskRepository]       │     │
│  └──────────────────────────────────────┘     │
│                                                 │
│  ┌──────────────────────────────────────┐     │
│  │ Bean: TaskRepository (singleton)     │     │
│  │ Dependencies: [EntityManager]        │     │
│  └──────────────────────────────────────┘     │
│                                                 │
│  ┌──────────────────────────────────────┐     │
│  │ Bean: EntityManager (singleton)      │     │
│  │ Auto-configured by Spring Boot       │     │
│  └──────────────────────────────────────┘     │
│                                                 │
│  ... (hundreds of beans in a typical app)      │
└────────────────────────────────────────────────┘
```

### Responsibilities

1. **Bean Creation**: Instantiates objects
2. **Dependency Resolution**: Figures out what each bean needs
3. **Dependency Injection**: Provides dependencies to beans
4. **Lifecycle Management**: Handles initialization and destruction
5. **Bean Scoping**: Manages singleton vs prototype instances

### Accessing the Application Context (Rarely Needed)

```java
@Component
public class MyComponent {

    private final ApplicationContext context;

    public MyComponent(ApplicationContext context) {
        this.context = context;  // You can inject the context itself!
    }

    public void doSomething() {
        // Programmatically get a bean (not recommended, use injection instead)
        TaskService service = context.getBean(TaskService.class);
    }
}
```

**Note:** You rarely need to access `ApplicationContext` directly. Use dependency injection instead!

---

## Creating Beans with Stereotype Annotations

**Stereotype annotations are the primary way to create beans.**

When you annotate a class with a stereotype annotation, Spring:
1. Discovers it via component scanning
2. Creates an instance
3. Registers it as a bean in the Application Context
4. Makes it available for injection

### Available Stereotype Annotations

| Annotation | Purpose | Layer | Creates Bean? |
|------------|---------|-------|---------------|
| `@Component` | Generic Spring-managed component | Any | ✅ YES |
| `@Service` | Business logic layer | Service | ✅ YES |
| `@Repository` | Data access layer | Persistence | ✅ YES |
| `@Controller` | MVC controller (returns views) | Web | ✅ YES |
| `@RestController` | REST API controller (returns JSON) | Web | ✅ YES |
| `@Configuration` | Bean configuration class | Config | ✅ YES |

### The Hierarchy

```
@Component  ← Base stereotype
    │
    ├── @Service       ← Specialization for business logic
    ├── @Repository    ← Specialization for data access
    └── @Controller    ← Specialization for web layer
            │
            └── @RestController  ← Specialization for REST APIs
```

**Key Point:** All stereotype annotations create beans! They're just specialized versions of `@Component`.

### Examples

#### @Service (Business Logic)

```java
@Service  // ← Creates a bean of type TaskService
public class TaskService {

    private final TaskRepository repository;

    // Constructor injection
    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }

    public Task createTask(String title) {
        Task task = new Task(title);
        return repository.save(task);
    }
}
```

#### @Repository (Data Access)

```java
@Repository  // ← Creates a bean of type TaskRepository
public interface TaskRepository extends JpaRepository<Task, Long> {
    // Spring Data JPA creates the implementation
    // AND registers it as a bean automatically
    List<Task> findByStatus(TaskStatus status);
}
```

**Special behavior:** `@Repository` adds automatic exception translation (SQL exceptions → Spring's `DataAccessException`).

#### @RestController (REST API)

```java
@RestController  // ← Creates a bean AND enables HTTP request handling
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @GetMapping
    public List<Task> getAllTasks() {
        return service.findAll();
    }
}
```

#### @Component (Generic)

```java
@Component  // ← Creates a bean
public class TaskValidator {

    public boolean isValidTitle(String title) {
        return title != null && title.length() >= 3;
    }
}
```

### Without Stereotype Annotations (Not a Bean)

```java
// NO annotation = NOT a bean
public class MyUtility {
    public String format(String s) {
        return s.toUpperCase();
    }
}

// Cannot inject this!
@Service
public class MyService {
    // ❌ This will fail - MyUtility is not a bean
    public MyService(MyUtility utility) {
        // ...
    }
}
```

### Role-Specific Behavior

| Annotation | Additional Behavior |
|------------|---------------------|
| `@Repository` | Exception translation (SQLException → DataAccessException) |
| `@Controller` | Request mapping and view resolution |
| `@RestController` | `@ResponseBody` on all methods (returns JSON/XML) |
| `@Service` | None (purely semantic) |
| `@Component` | None (purely semantic) |

---

## Creating Beans with @Bean Methods

Use `@Bean` when you can't use stereotype annotations (third-party classes, complex creation logic).

### Basic @Bean Usage

```java
@Configuration  // ← Must be in a @Configuration class
public class AppConfig {

    @Bean  // ← This method creates a bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        return mapper;  // This instance becomes the bean
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

**How it works:**
1. Spring scans for `@Configuration` classes
2. Finds `@Bean` methods
3. Calls each method to get the bean instance
4. Registers the returned object as a bean
5. Bean name = method name (by default)

### @Bean vs @Component

| Use Case | Use This | Example |
|----------|----------|---------|
| Your own classes | `@Component` | `TaskService`, `TaskRepository` |
| Third-party classes | `@Bean` | `ObjectMapper`, `RestTemplate` |
| Complex creation logic | `@Bean` | Custom database connection pools |
| Multiple beans of same type | `@Bean` | Multiple `DataSource` beans |

### @Bean with Dependencies

```java
@Configuration
public class DatabaseConfig {

    @Bean
    public DataSource primaryDataSource() {
        // Create first DataSource
        return new HikariDataSource();
    }

    @Bean
    public DataSource secondaryDataSource() {
        // Create second DataSource
        return new HikariDataSource();
    }

    @Bean
    // Spring injects the DataSource bean into this method!
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
```

### Bean Names

```java
@Configuration
public class AppConfig {

    @Bean  // Bean name = "objectMapper"
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean(name = "customMapper")  // Bean name = "customMapper"
    public ObjectMapper customObjectMapper() {
        return new ObjectMapper();
    }

    @Bean({"mapper1", "mapper2"})  // Multiple names
    public ObjectMapper multiNameMapper() {
        return new ObjectMapper();
    }
}
```

---

## Dependency Injection Methods

Spring supports three types of dependency injection. **Constructor injection is strongly recommended.**

### 1. Constructor Injection (RECOMMENDED ✅)

```java
@Repository  // ← Must be a bean
public interface TaskRepository extends JpaRepository<Task, Long> {
}

@Component  // ← Must be a bean
public class TaskValidator {
    // Validation logic
}

@Service  // ← This is also a bean
public class TaskService {
    private final TaskRepository repository;
    private final TaskValidator validator;

    // Spring calls this constructor and injects both dependencies
    // REQUIREMENT: Both TaskRepository and TaskValidator must be beans!
    public TaskService(TaskRepository repository, TaskValidator validator) {
        this.repository = repository;
        this.validator = validator;
    }

    public Task createTask(String title) {
        if (!validator.isValidTitle(title)) {
            throw new IllegalArgumentException("Invalid title");
        }
        return repository.save(new Task(title));
    }
}
```

**How it works:**
1. Spring discovers `TaskService`, `TaskRepository`, and `TaskValidator`
2. Spring creates `TaskRepository` bean first (no dependencies)
3. Spring creates `TaskValidator` bean (no dependencies)
4. Spring creates `TaskService` bean and injects both dependencies
5. If either dependency wasn't a bean → `NoSuchBeanDefinitionException`

**Benefits:**
- ✅ Dependencies are **required** (cannot be null)
- ✅ Fields can be `final` (immutable)
- ✅ Easy to test (pass mocks to constructor)
- ✅ Prevents circular dependencies (fails fast)
- ✅ No need for `@Autowired` (Spring automatically injects)

**Best Practice:** Always use constructor injection for required dependencies.

### 2. Field Injection (NOT RECOMMENDED ❌)

```java
@Service
public class TaskService {

    @Autowired  // Spring injects directly into the field
    private TaskRepository repository;

    @Autowired
    private TaskValidator validator;

    public Task createTask(String title) {
        return repository.save(new Task(title));
    }
}
```

**Why avoid:**
- ❌ Cannot make fields `final`
- ❌ Hard to test (need Spring context)
- ❌ Hides dependencies (not obvious what class needs)
- ❌ Can lead to `NullPointerException` if used before injection
- ❌ Makes code less maintainable

**When it might be acceptable:** Very simple prototypes or learning examples.

### 3. Setter Injection (RARELY USED)

```java
@Service
public class TaskService {
    private TaskRepository repository;

    @Autowired  // Optional dependency
    public void setRepository(TaskRepository repository) {
        this.repository = repository;
    }
}
```

**When to use:**
- Optional dependencies (may or may not be available)
- Reconfiguration at runtime (very rare)

**Most of the time:** Use constructor injection instead.

### Injection Without @Autowired (Spring 4.3+)

If a class has **only one constructor**, `@Autowired` is optional:

```java
@Service
public class TaskService {
    private final TaskRepository repository;

    // No @Autowired needed - Spring automatically uses this constructor
    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }
}
```

**Best Practice:** Omit `@Autowired` on single-constructor classes (cleaner code).

---

## Bean Scopes

Beans can have different lifecycles (scopes).

### Available Scopes

| Scope | Description | Lifetime | Use Case |
|-------|-------------|----------|----------|
| `singleton` **(default)** | One instance per Application Context | Application lifetime | Services, repositories (stateless) |
| `prototype` | New instance every time requested | Per request | Stateful objects |
| `request` | One instance per HTTP request | HTTP request | Web apps (request-specific data) |
| `session` | One instance per HTTP session | HTTP session | Shopping cart, user session |
| `application` | One instance per ServletContext | Application lifetime | Global application state |
| `websocket` | One instance per WebSocket session | WebSocket session | WebSocket handlers |

### Singleton Scope (Default)

```java
@Service  // Singleton by default
public class TaskService {
    // Only ONE instance exists in the Application Context
    // Shared by all classes that inject it
}
```

**Behavior:**
```java
@Component
public class ComponentA {
    private final TaskService service;

    public ComponentA(TaskService service) {
        this.service = service;  // Same instance
    }
}

@Component
public class ComponentB {
    private final TaskService service;

    public ComponentB(TaskService service) {
        this.service = service;  // SAME instance as ComponentA!
    }
}
```

**When to use:** Stateless beans (no mutable state).

### Prototype Scope

```java
@Component
@Scope("prototype")  // New instance every time
public class TaskBuilder {
    private String title;
    private String description;

    public TaskBuilder setTitle(String title) {
        this.title = title;
        return this;
    }

    public Task build() {
        return new Task(title, description);
    }
}
```

**Behavior:**
```java
@Service
public class TaskService {
    private final ApplicationContext context;

    public TaskService(ApplicationContext context) {
        this.context = context;
    }

    public Task createTask() {
        // Each call gets a NEW instance
        TaskBuilder builder1 = context.getBean(TaskBuilder.class);
        TaskBuilder builder2 = context.getBean(TaskBuilder.class);
        // builder1 != builder2 (different instances)
    }
}
```

**When to use:** Stateful objects that should not be shared.

### Request Scope (Web Applications)

```java
@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RequestContext {
    private String requestId = UUID.randomUUID().toString();

    public String getRequestId() {
        return requestId;
    }
}
```

**Behavior:** New instance for each HTTP request, destroyed after response.

---

## Autowiring and Qualifiers

### Type-Based Autowiring (Default)

Spring injects beans by **type** (class or interface):

```java
@Service
public class TaskService {
    // Spring looks for a bean of type TaskRepository
    public TaskService(TaskRepository repository) {
        // ...
    }
}
```

### Multiple Beans of Same Type

**Problem:** What if there are multiple beans of the same type?

```java
@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource primaryDataSource() {
        return new HikariDataSource();
    }

    @Bean
    public DataSource secondaryDataSource() {
        return new HikariDataSource();
    }
}

@Service
public class TaskService {
    // ❌ ERROR: Which DataSource should Spring inject?
    public TaskService(DataSource dataSource) {
        // ...
    }
}
```

**Error:**
```
required a single bean, but 2 were found:
    - primaryDataSource
    - secondaryDataSource
```

### Solution 1: @Primary

```java
@Configuration
public class DataSourceConfig {

    @Bean
    @Primary  // ← Use this one by default
    public DataSource primaryDataSource() {
        return new HikariDataSource();
    }

    @Bean
    public DataSource secondaryDataSource() {
        return new HikariDataSource();
    }
}

@Service
public class TaskService {
    // ✅ Injects primaryDataSource (marked @Primary)
    public TaskService(DataSource dataSource) {
        // ...
    }
}
```

### Solution 2: @Qualifier

```java
@Service
public class TaskService {
    // ✅ Explicitly specify which bean to inject
    public TaskService(@Qualifier("secondaryDataSource") DataSource dataSource) {
        // ...
    }
}
```

### Solution 3: Match Parameter Name to Bean Name

```java
@Service
public class TaskService {
    // ✅ Parameter name matches bean name
    public TaskService(DataSource primaryDataSource) {
        // Spring matches by name: primaryDataSource
    }
}
```

---

## Common Pitfalls

### 1. Injecting Non-Bean Classes

```java
// NO annotation = NOT a bean
public class MyUtility {
    // ...
}

@Service
public class MyService {
    // ❌ FAILS: MyUtility is not a bean!
    public MyService(MyUtility utility) {
        // NoSuchBeanDefinitionException
    }
}
```

**Fix:** Add `@Component` to `MyUtility` or create it with `@Bean`.

### 2. Circular Dependencies

```java
@Service
public class ServiceA {
    public ServiceA(ServiceB serviceB) {
        // ServiceA needs ServiceB
    }
}

@Service
public class ServiceB {
    public ServiceB(ServiceA serviceA) {
        // ServiceB needs ServiceA → CIRCULAR!
    }
}
```

**Error:**
```
The dependencies of some of the beans in the application context form a cycle
```

**Fix:** Refactor to remove circular dependency (usually indicates poor design).

### 3. Field Injection in Tests

```java
@Service
public class TaskService {
    @Autowired
    private TaskRepository repository;  // Field injection
}

// In test:
@Test
void testCreateTask() {
    TaskService service = new TaskService();
    // ❌ repository is null! No Spring context to inject it
}
```

**Fix:** Use constructor injection for easier testing.

### 4. Forgetting @Component on Configuration Classes

```java
// ❌ Missing @Configuration
public class AppConfig {
    @Bean
    public MyBean myBean() {
        return new MyBean();
    }
}
```

**Fix:** Add `@Configuration` so Spring discovers it.

---

## Summary

### Key Takeaways

✅ **IoC** means Spring controls object creation and dependency management

✅ **DI** is the mechanism where dependencies are provided (injected) from outside

✅ **Beans** are objects managed by Spring's Application Context

✅ **Stereotype annotations** (`@Component`, `@Service`, `@Repository`, etc.) **create beans**

✅ **@Bean methods** create beans for third-party classes or complex setup

✅ **Constructor injection** is the recommended approach (required dependencies, immutability)

✅ **You can only inject beans** - both the injecting class and injected class must be beans

✅ **Singleton scope** is default (one instance per context)

### Decision Flowchart

```
Need to create a bean?
    │
    ├─ Is it your own class?
    │   └─ YES → Use @Component, @Service, @Repository, etc.
    │
    └─ Is it a third-party class?
        └─ YES → Use @Bean in @Configuration class

Need to inject a dependency?
    │
    ├─ Is it required?
    │   └─ YES → Constructor injection (recommended)
    │
    └─ Is it optional?
        └─ YES → Setter injection

Multiple beans of same type?
    │
    ├─ One is "default"?
    │   └─ YES → Mark it @Primary
    │
    └─ Need specific one?
        └─ YES → Use @Qualifier
```

---

## Related Files

- `src/main/java/com/tutorial/taskmanager/PACKAGE-INFO.md` - Detailed package documentation with examples
- `src/main/java/com/tutorial/taskmanager/TaskManagerApplication.java` - Main application class
- [01-getting-started.md](01-getting-started.md) - Spring Boot basics

---

## Additional Resources

- [Spring Framework Reference - IoC Container](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#beans)
- [Spring Boot Reference - Dependency Injection](https://docs.spring.io/spring-boot/docs/current/reference/html/using.html#using.spring-beans-and-dependency-injection)
- [Baeldung - Spring Dependency Injection](https://www.baeldung.com/spring-dependency-injection)
- [Baeldung - Spring Bean Scopes](https://www.baeldung.com/spring-bean-scopes)

---

**Last Updated:** 2025-11-15
