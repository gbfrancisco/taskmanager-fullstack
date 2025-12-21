# Package: com.tutorial.taskmanager

This is the **root package** of the Task Manager application. This document explains key Spring Boot concepts demonstrated in this package.

---

## Table of Contents
1. [TaskManagerApplication.java](#taskmangerapplicationjava)
2. [ApplicationStartupLogger Component](#applicationstartuplogger-component)
3. [Spring Boot Fundamentals](#spring-boot-fundamentals)
4. [Common Questions](#common-questions)

---

## TaskManagerApplication.java

### Purpose
The main entry point that bootstraps the entire Spring Boot application.

### Key Annotation: `@SpringBootApplication`

This is a **meta-annotation** (annotation that combines other annotations):

```java
@SpringBootApplication
// Is equivalent to:
@Configuration
@EnableAutoConfiguration
@ComponentScan
```

#### What Each Does:

| Annotation | Purpose | Example |
|------------|---------|---------|
| `@Configuration` | Marks this class as a source of bean definitions | Can define `@Bean` methods |
| `@EnableAutoConfiguration` | Auto-configures beans based on classpath | Sees `spring-boot-starter-data-jpa` → configures DataSource, EntityManager, etc. |
| `@ComponentScan` | Scans for stereotype annotations (`@Component`, `@Service`, etc.) | Finds all classes in `com.tutorial.taskmanager.*` |

### The main() Method

```java
public static void main(String[] args) {
    SpringApplication.run(TaskManagerApplication.class, args);
}
```

**What happens when you call `SpringApplication.run()`:**

1. **Creates Application Context**: The IoC (Inversion of Control) container
2. **Scans for Components**: Finds all `@Component`, `@Service`, `@Repository`, `@Controller` classes
3. **Registers Beans**: Adds discovered components to the Application Context
4. **Resolves Dependencies**: Figures out what each bean needs (dependency injection)
5. **Auto-Configuration**: Configures beans based on classpath and properties
6. **Starts Embedded Server**: Launches Tomcat (or Jetty/Undertow)
7. **Publishes Events**: Fires `ApplicationReadyEvent`, etc.

---

## ApplicationStartupLogger Component

### The "Magic" Explained

**Question:** How does `ApplicationStartupLogger` work without manually creating it?

**Answer:** Spring's **Dependency Injection** and **Component Scanning**.

### Step-by-Step Breakdown

#### 1. Component Discovery
```java
@Component
class ApplicationStartupLogger {
    // ...
}
```

- `@Component` tells Spring: "This is a bean - manage it for me"
- During startup, `@ComponentScan` finds this class
- Spring registers it in the Application Context

#### 2. Bean Creation & Constructor Injection
```java
public ApplicationStartupLogger(Environment environment) {
    this.environment = environment;
}
```

**The Injection Process:**
1. Spring sees `ApplicationStartupLogger` needs an `Environment`
2. Spring **already has** an `Environment` bean (auto-configured)
3. Spring calls: `new ApplicationStartupLogger(environmentBean)`
4. You **never** manually call `new` or pass the `Environment` - Spring does it!

**This is Dependency Injection (DI):**
- You declare what you need (constructor parameters)
- Spring provides it automatically
- Your class doesn't create its own dependencies

#### 3. Event Listener Registration
```java
@EventListener(ApplicationReadyEvent.class)
public void logApplicationStartup() {
    // Logs startup info
}
```

- Spring scans all beans for `@EventListener` methods
- Registers them to be called when events are published
- When app is fully started → `ApplicationReadyEvent` fires → method called

### Visual Flow

```
┌─────────────────────────────────────────────────────────────┐
│ 1. Application Starts                                       │
│    ./mvnw spring-boot:run                                   │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. Component Scanning                                       │
│    @ComponentScan finds @Component classes                  │
│    → Discovers: ApplicationStartupLogger                    │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. Bean Registration                                        │
│    Spring registers ApplicationStartupLogger in context     │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. Dependency Resolution                                    │
│    Constructor needs: Environment                           │
│    Spring has: Environment bean (auto-configured)           │
│    → Inject Environment into constructor                    │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 5. Bean Creation                                            │
│    new ApplicationStartupLogger(environmentBean)            │
│    Bean is now fully initialized and ready                  │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 6. Event Listener Registration                              │
│    Spring finds @EventListener method                       │
│    Registers it for ApplicationReadyEvent                   │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 7. Application Fully Started                                │
│    Embedded Tomcat running, all beans initialized           │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 8. ApplicationReadyEvent Published                          │
│    Spring publishes event to all registered listeners       │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 9. Listener Method Called                                   │
│    logApplicationStartup() executes                         │
│    Logs appear in console with URLs and info                │
└─────────────────────────────────────────────────────────────┘
```

---

## Spring Boot Fundamentals

### The Spring Container (Application Context)

Think of the Application Context as a **container** that:
- **Holds** all your application objects (beans)
- **Manages** their lifecycle (creation, initialization, destruction)
- **Injects** dependencies automatically
- **Handles** configuration

### Inversion of Control (IoC)

**Traditional approach:**
```java
public class MyService {
    private MyRepository repository = new MyRepository(); // You create it
}
```

**Spring approach (IoC):**
```java
@Repository  // ← IMPORTANT: MyRepository must be a Spring bean!
public class MyRepository {
    // Repository implementation
}

@Service  // ← This class is also a Spring bean
public class MyService {
    private final MyRepository repository;

    // Spring provides the repository bean automatically
    public MyService(MyRepository repository) {
        this.repository = repository;
    }
}
```

**Critical Rule: You can only inject Spring beans!**
- Both `MyRepository` and `MyService` must be Spring-managed beans
- Spring can **only inject** objects it knows about (in the Application Context)
- If `MyRepository` wasn't annotated with `@Repository` (or `@Component`), Spring wouldn't know about it
- Result: `NoSuchBeanDefinitionException` at startup

**Why IoC?**
- Loose coupling (easier to change implementations)
- Easier testing (can inject mocks)
- Better separation of concerns
- Spring manages complexity for you

### Dependency Injection (DI)

**Three types of DI in Spring:**

#### 1. Constructor Injection (RECOMMENDED)
```java
@Repository  // ← TaskRepository must be a bean
public interface TaskRepository extends JpaRepository<Task, Long> {
}

@Component  // ← TaskService must be a bean
public class TaskService {
    private final TaskRepository repository;

    // Spring calls this constructor and provides the TaskRepository bean
    // REQUIREMENT: TaskRepository must exist in the Application Context
    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }
}
```

**How it works:**
1. Spring scans and finds both `TaskRepository` and `TaskService`
2. Spring creates `TaskRepository` bean first (dependency)
3. Spring creates `TaskService` bean and injects `TaskRepository` into constructor
4. If `TaskRepository` wasn't a bean → `NoSuchBeanDefinitionException`

**Benefits:**
- Makes dependencies explicit and required
- Enables immutability (`final` fields)
- Easier to test (can pass mocks in tests)
- Prevents `NullPointerException`

#### 2. Field Injection (NOT RECOMMENDED)
```java
@Component
public class TaskService {
    @Autowired
    private TaskRepository repository; // Spring injects directly
}
```

**Why avoid:**
- Harder to test (need Spring context)
- Can't make fields `final`
- Hides dependencies

#### 3. Setter Injection (RARELY USED)
```java
@Component
public class TaskService {
    private TaskRepository repository;

    @Autowired
    public void setRepository(TaskRepository repository) {
        this.repository = repository;
    }
}
```

**When to use:**
- Optional dependencies
- Changing dependencies at runtime

### Stereotype Annotations

**Stereotype annotations are how you create beans!**

When you annotate a class with a stereotype annotation, you're telling Spring:
- **"This class is a bean - manage it in the Application Context"**
- Spring will create an instance of this class
- Spring will make it available for dependency injection

| Annotation | Purpose | Example Use | Bean? |
|------------|---------|-------------|-------|
| `@Component` | Generic Spring-managed bean | Utilities, helpers | ✅ YES |
| `@Service` | Business logic layer | `TaskService`, `UserService` | ✅ YES |
| `@Repository` | Data access layer | `TaskRepository` (with JPA) | ✅ YES |
| `@Controller` | MVC controller (returns views) | Web pages with Thymeleaf | ✅ YES |
| `@RestController` | REST API controller (returns JSON/XML) | `TaskController` API | ✅ YES |
| `@Configuration` | Bean configuration class | `SecurityConfig`, `DatabaseConfig` | ✅ YES |

**Key Point:** All stereotype annotations create beans!
- `@Component` is the **base** stereotype
- `@Service`, `@Repository`, `@Controller` are **specializations** of `@Component`
- They all register the class as a Spring bean
- The difference is **semantic** (clarifies role) and **behavior** (role-specific features)

**Example:**
```java
@Service  // ← This creates a Spring bean of type TaskService
public class TaskService {
    // Spring will create an instance and manage it
}

@Repository  // ← This creates a Spring bean of type TaskRepository
public interface TaskRepository extends JpaRepository<Task, Long> {
    // Spring Data JPA creates the implementation AND registers it as a bean
}
```

**Without a stereotype annotation:**
```java
// NO annotation = NOT a bean
public class MyClass {
    // Spring doesn't know about this
    // Cannot be injected
    // You must use 'new MyClass()' manually
}
```

**Role-Specific Behavior:**
- `@Repository`: Adds automatic exception translation (SQL exceptions → Spring's DataAccessException)
- `@Controller`/`@RestController`: Enables request mapping and HTTP handling
- `@Service`: No special behavior, just semantic (clarifies it's business logic)
- `@Component`: Generic, no special behavior

### Bean Scopes

Beans can have different lifecycles:

| Scope | Description | When to Use |
|-------|-------------|-------------|
| `singleton` (default) | One instance per Application Context | Services, repositories (stateless) |
| `prototype` | New instance every time requested | Objects with state |
| `request` | One per HTTP request | Web apps (user-specific data) |
| `session` | One per HTTP session | Shopping cart, user session |

```java
@Component
@Scope("prototype") // New instance each time
public class TaskBuilder {
    // ...
}
```

---

## Common Questions

### Q1: Why don't I see `new ApplicationStartupLogger()` anywhere?

**A:** Spring calls `new` for you! When you annotate a class with `@Component`, Spring:
1. Scans and finds it during startup
2. Creates an instance using reflection
3. Manages it in the Application Context

You **could** manually create it, but then:
- You'd have to manually pass `Environment`
- You'd have to manually register the event listener
- You'd lose all Spring features

### Q2: How does Spring know to inject `Environment`?

**A:** Spring uses **type-based autowiring** by default:
1. Sees constructor: `ApplicationStartupLogger(Environment environment)`
2. Looks in Application Context for a bean of type `Environment`
3. Finds one (Spring creates it automatically)
4. Injects it

**Key Point:** Spring can **only inject beans** that exist in the Application Context!
- `Environment` is a bean that Spring Boot auto-configures
- If you try to inject a non-bean class, you get `NoSuchBeanDefinitionException`
- Both the class being injected **and** the class receiving the injection must be beans

If there are **multiple beans** of the same type:
- Use `@Qualifier("beanName")` to specify which one
- Or use `@Primary` on the preferred bean

### Q2a: What if I want to inject a regular Java class (not a bean)?

**A:** You have two options:

**Option 1: Make it a bean**
```java
@Component  // Now Spring manages it
public class MyUtility {
    // ...
}
```

**Option 2: Create it manually and don't rely on injection**
```java
@Service
public class MyService {
    private final MyUtility utility = new MyUtility(); // Manual creation

    // No constructor injection needed
}
```

**Option 3: Define it as a bean in a @Configuration class**
```java
@Configuration
public class AppConfig {

    @Bean  // Now MyUtility is a Spring bean
    public MyUtility myUtility() {
        return new MyUtility();
    }
}

@Service
public class MyService {
    private final MyUtility utility;

    // Now Spring can inject it!
    public MyService(MyUtility utility) {
        this.utility = utility;
    }
}
```

### Q3: What if I want to create a bean manually?

You can use `@Bean` in a `@Configuration` class:

```java
@Configuration
public class AppConfig {

    @Bean
    public MyService myService() {
        return new MyService(); // You control creation
    }
}
```

### Q4: When should I use @Component vs @Bean?

**Both create beans, but in different ways:**

**@Component (and @Service, @Repository, etc.):**
- Annotate the **class itself**
- Spring automatically discovers it via component scanning
- Spring creates the bean using the class constructor
- Use for **your own classes** that you have source code for

```java
@Service  // ← Creates a bean automatically
public class TaskService {
    // Spring calls constructor
}
```

**@Bean:**
- Annotate a **method** in a `@Configuration` class
- You manually control bean creation
- The method **returns** the bean instance
- Use for **third-party classes** or complex creation logic

```java
@Configuration
public class AppConfig {

    @Bean  // ← Creates a bean by calling this method
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(/* custom config */);
        return mapper;  // This instance becomes the bean
    }
}
```

**Decision Table:**

| Use Case | Use This | Why |
|----------|----------|-----|
| Your own classes (you control the source) | `@Component` | Simpler, automatic discovery |
| Third-party classes (no source access) | `@Bean` | Can't modify their source to add `@Component` |
| Complex creation logic needed | `@Bean` | You control the instantiation |
| Need multiple beans of same type | `@Bean` | Create multiple `@Bean` methods |
| Simple classes | `@Component` | Less code, more automatic |

**Remember:** Both approaches create beans that can be injected!

### Q5: What's the difference between Spring and Spring Boot?

**Spring Framework:**
- Provides DI, IoC, and many modules (Web, Data, Security)
- Requires **manual configuration** (lots of XML or Java config)

**Spring Boot:**
- Built **on top of** Spring Framework
- **Auto-configuration** - sensible defaults based on classpath
- **Embedded server** - no need to deploy to Tomcat
- **Starter dependencies** - `spring-boot-starter-web` includes everything for web apps
- **Opinionated** - makes decisions for you (can override)

### Q6: How do I debug Spring's auto-configuration?

Run with debug logging:
```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--debug
```

Or in `application.yml`:
```yaml
logging:
  level:
    org.springframework.boot.autoconfigure: DEBUG
```

This shows:
- Which auto-configurations are applied
- Which are skipped (and why)
- What beans are created

---

## Next Steps

Now that you understand how the application bootstraps:

1. **Create domain models** (`@Entity` classes in `model/` package)
2. **Create repositories** (`@Repository` interfaces in `repository/`)
3. **Create services** (`@Service` classes in `service/`)
4. **Create controllers** (`@RestController` classes in `controller/`)

Each of these will be discovered by component scanning and managed by Spring!

---

## Additional Resources

- [Spring Boot Reference - Auto-configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/using.html#using.auto-configuration)
- [Spring Framework - IoC Container](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#beans)
- [Spring Boot - Dependency Injection](https://docs.spring.io/spring-boot/docs/current/reference/html/using.html#using.spring-beans-and-dependency-injection)
- [Baeldung - Spring Component Scanning](https://www.baeldung.com/spring-component-scanning)
- [Documentation Index](../../../../../docs/README.md)

---

**Last Updated:** 2025-11-15
**Related Files:**
- `TaskManagerApplication.java` - Main application class
- `docs/01-getting-started.md` - Getting started guide
- `docs/02-dependency-injection.md` - DI deep dive
