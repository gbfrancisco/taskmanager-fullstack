# Getting Started with Spring Boot 3

This guide covers the fundamentals of Spring Boot, auto-configuration, and the application bootstrap process.

---

## Table of Contents
1. [What is Spring Boot?](#what-is-spring-boot)
2. [Spring vs Spring Boot](#spring-vs-spring-boot)
3. [The Main Application Class](#the-main-application-class)
4. [Understanding @SpringBootApplication](#understanding-springbootapplication)
5. [Application Startup Process](#application-startup-process)
6. [Application Events](#application-events)
7. [Accessing Configuration Properties](#accessing-configuration-properties)
8. [Running the Application](#running-the-application)

---

## What is Spring Boot?

**Spring Boot** is an opinionated framework built on top of the Spring Framework that simplifies the development of production-ready applications.

### Key Features

| Feature | Description | Benefit |
|---------|-------------|---------|
| **Auto-configuration** | Automatically configures beans based on classpath | Less boilerplate configuration |
| **Starter dependencies** | Curated dependency bundles | Simplified dependency management |
| **Embedded server** | Built-in Tomcat/Jetty/Undertow | No need for separate deployment |
| **Production-ready features** | Actuator, metrics, health checks | Operations support out-of-the-box |
| **Opinionated defaults** | Sensible defaults for common scenarios | Faster development |

### The "Convention over Configuration" Philosophy

Spring Boot makes assumptions about what you want to do:
- See `spring-boot-starter-web` → Configure embedded Tomcat, Spring MVC, Jackson for JSON
- See `spring-boot-starter-data-jpa` → Configure DataSource, EntityManager, transaction management
- See H2 on classpath → Configure in-memory database

You can always override these defaults when needed.

---

## Spring vs Spring Boot

### Spring Framework (The Foundation)

```java
// Traditional Spring - lots of manual configuration
@Configuration
@EnableWebMvc
@ComponentScan("com.tutorial.taskmanager")
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public ViewResolver viewResolver() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setPrefix("/WEB-INF/views/");
        resolver.setSuffix(".jsp");
        return resolver;
    }

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:testdb");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    // ... many more beans
}
```

### Spring Boot (Simplified)

```java
// Spring Boot - auto-configuration does the heavy lifting
@SpringBootApplication
public class TaskManagerApplication {
    public static void main(String[] args) {
        SpringApplication.run(TaskManagerApplication.class, args);
    }
}
```

**What Spring Boot does automatically:**
- Configures embedded Tomcat
- Sets up Spring MVC with sensible defaults
- Configures DataSource based on properties
- Enables JSON serialization with Jackson
- Sets up exception handling
- Configures logging

**All from just `@SpringBootApplication` and dependencies in `pom.xml`!**

---

## The Main Application Class

**Location:** `src/main/java/com/tutorial/taskmanager/TaskManagerApplication.java`

```java
package com.tutorial.taskmanager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Task Manager Spring Boot application.
 */
@SpringBootApplication
public class TaskManagerApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskManagerApplication.class);

    public static void main(String[] args) {
        LOGGER.info("Starting TaskManagerApplication...");
        SpringApplication.run(TaskManagerApplication.class, args);
    }
}
```

### Key Points

1. **Package Location Matters**:
   - This class is in the root package (`com.tutorial.taskmanager`)
   - Component scanning starts from this package and scans all sub-packages
   - Don't put this in a deeply nested package

2. **Standard main() Method**:
   - Entry point like any Java application
   - Delegates to `SpringApplication.run()`

3. **SpringApplication.run()**:
   - Creates the Application Context
   - Performs auto-configuration
   - Starts the embedded web server

---

## Understanding @SpringBootApplication

The `@SpringBootApplication` annotation is a **meta-annotation** that combines three essential annotations:

```java
@SpringBootApplication
// Is equivalent to:
@Configuration
@EnableAutoConfiguration
@ComponentScan
```

### Breaking It Down

#### 1. @Configuration

Marks the class as a source of bean definitions.

```java
@Configuration
public class TaskManagerApplication {

    @Bean  // You can define beans here if needed
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
```

**What it does:**
- Allows you to define `@Bean` methods
- Registers this class in the Application Context
- Enables Java-based configuration (vs XML)

#### 2. @EnableAutoConfiguration

**This is the magic of Spring Boot!**

```java
@EnableAutoConfiguration
```

**What it does:**
1. Looks at your classpath dependencies
2. Reads auto-configuration classes from `spring-boot-autoconfigure` JAR
3. Conditionally configures beans based on what it finds

**Example auto-configuration logic:**

```java
@Configuration
@ConditionalOnClass(DataSource.class)  // Only if DataSource is on classpath
@ConditionalOnMissingBean(DataSource.class)  // Only if user hasn't defined one
public class DataSourceAutoConfiguration {

    @Bean
    public DataSource dataSource() {
        // Create DataSource based on application.properties
    }
}
```

**Common auto-configurations:**
- `DataSourceAutoConfiguration` - Configures database connection
- `JpaRepositoriesAutoConfiguration` - Enables Spring Data JPA repositories
- `WebMvcAutoConfiguration` - Configures Spring MVC
- `SecurityAutoConfiguration` - Sets up basic security
- `JacksonAutoConfiguration` - Configures JSON processing

#### 3. @ComponentScan

Scans for Spring components starting from the current package.

```java
@ComponentScan  // Defaults to the package of this class
```

**What it scans for:**
- `@Component`
- `@Service`
- `@Repository`
- `@Controller` / `@RestController`
- `@Configuration`

**Scanning behavior:**

```
com.tutorial.taskmanager/              ← @SpringBootApplication here
├── TaskManagerApplication.java        ← Scans this package and below
├── controller/                        ✅ SCANNED
│   └── TaskController.java
├── service/                           ✅ SCANNED
│   └── TaskService.java
├── repository/                        ✅ SCANNED
│   └── TaskRepository.java
└── model/                             ✅ SCANNED
    └── Task.java

com.other.package/                     ❌ NOT SCANNED (different root)
└── SomeService.java
```

**To scan additional packages:**

```java
@SpringBootApplication(scanBasePackages = {
    "com.tutorial.taskmanager",
    "com.other.package"
})
```

---

## Application Startup Process

When you run `SpringApplication.run(TaskManagerApplication.class, args)`, here's what happens:

### Step-by-Step Startup

```
┌─────────────────────────────────────────────────────────────┐
│ 1. Create SpringApplication Instance                        │
│    Determines application type (Servlet, Reactive, etc.)    │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. Prepare Environment                                      │
│    Loads application.properties/yml                         │
│    Processes command-line arguments                         │
│    Resolves active profiles                                 │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. Create Application Context                               │
│    The IoC container that will hold all beans               │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. Component Scanning                                       │
│    Scans for @Component, @Service, @Repository, etc.        │
│    Discovers all classes that should be beans               │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 5. Auto-Configuration                                       │
│    Examines classpath and existing beans                    │
│    Conditionally creates auto-configured beans              │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 6. Bean Registration                                        │
│    Registers all discovered and configured beans            │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 7. Dependency Resolution                                    │
│    Analyzes bean dependencies                               │
│    Determines creation order                                │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 8. Bean Creation & Injection                                │
│    Creates beans in dependency order                        │
│    Injects dependencies via constructors/setters            │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 9. Post-Processing                                          │
│    Runs BeanPostProcessors                                  │
│    Applies AOP proxies                                      │
│    Registers event listeners                                │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 10. Start Embedded Server                                   │
│    Starts Tomcat/Jetty/Undertow                             │
│    Registers servlets and filters                           │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 11. Application Ready                                       │
│    Publishes ApplicationReadyEvent                          │
│    Application is now serving requests                      │
└─────────────────────────────────────────────────────────────┘
```

### Startup Logging

Spring Boot logs key information during startup:

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.0)

2025-11-15 ... : Starting TaskManagerApplication using Java 21
2025-11-15 ... : No active profile set, falling back to 1 default profile: "default"
2025-11-15 ... : Bootstrapping Spring Data JPA repositories
2025-11-15 ... : Finished Spring Data repository scanning
2025-11-15 ... : Tomcat initialized with port 8080 (http)
2025-11-15 ... : Starting service [Tomcat]
2025-11-15 ... : Starting Servlet engine: [Apache Tomcat/10.1.15]
2025-11-15 ... : Initializing Spring embedded WebApplicationContext
2025-11-15 ... : Root WebApplicationContext: initialization completed in 842 ms
2025-11-15 ... : HikariPool-1 - Starting...
2025-11-15 ... : HikariPool-1 - Added connection conn0: url=jdbc:h2:mem:taskdb
2025-11-15 ... : HikariPool-1 - Start completed.
2025-11-15 ... : H2 console available at '/h2-console'
2025-11-15 ... : Tomcat started on port 8080 (http)
2025-11-15 ... : Started TaskManagerApplication in 2.156 seconds
```

**What to look for:**
- Active profiles
- Port numbers
- Database connections
- Startup time
- Any warnings or errors

---

## Application Events

Spring Boot publishes events during the application lifecycle. You can listen to these events to perform actions at specific times.

### Common Application Events

| Event | When It Fires | Use Case |
|-------|---------------|----------|
| `ApplicationStartingEvent` | Very early, before any processing | Early initialization |
| `ApplicationEnvironmentPreparedEvent` | Environment ready, context not created | Environment customization |
| `ApplicationContextInitializedEvent` | Context created, sources not loaded | Context initialization |
| `ApplicationPreparedEvent` | Context loaded, not refreshed | Pre-refresh operations |
| `ApplicationStartedEvent` | Context refreshed, before runners | Post-startup initialization |
| `ApplicationReadyEvent` | **Application ready to serve requests** | **Most commonly used** |
| `ApplicationFailedEvent` | Startup fails | Error handling |

### Listening to Events

**Using @EventListener (Recommended):**

```java
@Component
class ApplicationStartupLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationStartupLogger.class);

    private final Environment environment;

    public ApplicationStartupLogger(Environment environment) {
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logApplicationStartup() {
        String port = environment.getProperty("server.port", "8080");

        LOGGER.info("=".repeat(80));
        LOGGER.info("Task Manager Application is RUNNING!");
        LOGGER.info("=".repeat(80));
        LOGGER.info("Local URL:          http://localhost:{}", port);
        LOGGER.info("H2 Console:         http://localhost:{}/h2-console", port);
        LOGGER.info("API Documentation:  http://localhost:{}/swagger-ui.html", port);
        LOGGER.info("=".repeat(80));
    }
}
```

**Why this works:**
1. `@Component` makes it a Spring bean → discovered by component scanning
2. `@EventListener` registers the method to be called when event fires
3. `Environment` is injected → provides access to configuration properties
4. When app is ready → Spring publishes `ApplicationReadyEvent` → method called

**Output when running:**
```
================================================================================
Task Manager Application is RUNNING!
================================================================================
Local URL:          http://localhost:8080
H2 Console:         http://localhost:8080/h2-console
API Documentation:  http://localhost:8080/swagger-ui.html
================================================================================
```

---

## Accessing Configuration Properties

Spring Boot provides the `Environment` abstraction to access properties from:
- `application.properties` / `application.yml`
- Command-line arguments
- Environment variables
- System properties

### Using Environment Bean

```java
@Component
public class MyComponent {

    private final Environment env;

    public MyComponent(Environment environment) {
        this.env = environment;
    }

    public void printConfig() {
        String port = env.getProperty("server.port", "8080");
        String appName = env.getProperty("spring.application.name", "app");
        String[] profiles = env.getActiveProfiles();

        System.out.println("Port: " + port);
        System.out.println("App Name: " + appName);
        System.out.println("Profiles: " + String.join(", ", profiles));
    }
}
```

### Using @Value Annotation

```java
@Component
public class MyComponent {

    @Value("${server.port:8080}")  // Default value after colon
    private String port;

    @Value("${spring.application.name}")
    private String appName;
}
```

### Configuration Properties Files

**application.yml:**
```yaml
server:
  port: 8080

spring:
  application:
    name: task-manager
  datasource:
    url: jdbc:h2:mem:taskdb
    username: sa
    password:
  h2:
    console:
      enabled: true
```

---

## Running the Application

### Using Maven

```bash
# Run the application
./mvnw spring-boot:run

# Run with specific profile
./mvnw spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=dev

# Run with debug logging
./mvnw spring-boot:run -Dspring-boot.run.arguments=--debug
```

### Using Java

```bash
# Build the JAR
./mvnw clean package

# Run the JAR
java -jar target/task-manager-0.0.1-SNAPSHOT.jar

# Run with profile
java -jar target/task-manager-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### IDE Integration

Most IDEs recognize Spring Boot applications:
- **IntelliJ IDEA**: Right-click → Run 'TaskManagerApplication'
- **Eclipse**: Right-click → Run As → Spring Boot App
- **VS Code**: Spring Boot Dashboard

### Verifying the Application is Running

Once started, verify:

1. **Console output**: Look for "Started TaskManagerApplication in X seconds"
2. **Port listening**: Check that Tomcat started on port 8080
3. **HTTP request**: Open http://localhost:8080 (may show error page if no controllers yet)
4. **H2 Console**: http://localhost:8080/h2-console

---

## Debugging Auto-Configuration

Want to see what Spring Boot is auto-configuring?

### Enable Debug Mode

**Option 1: Command line**
```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--debug
```

**Option 2: application.yml**
```yaml
debug: true
```

### Output

```
============================
CONDITIONS EVALUATION REPORT
============================

Positive matches:
-----------------

   DataSourceAutoConfiguration matched:
      - @ConditionalOnClass found required classes 'javax.sql.DataSource', 'org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType' (OnClassCondition)

   JpaRepositoriesAutoConfiguration matched:
      - @ConditionalOnClass found required class 'org.springframework.data.jpa.repository.JpaRepository' (OnClassCondition)

Negative matches:
-----------------

   MongoAutoConfiguration:
      Did not match:
         - @ConditionalOnClass did not find required class 'com.mongodb.client.MongoClient' (OnClassCondition)
```

**Interpretation:**
- **Positive matches**: Auto-configurations that were applied
- **Negative matches**: Auto-configurations that were skipped (and why)

---

## Summary

### Key Takeaways

✅ **Spring Boot simplifies Spring development** with auto-configuration and starter dependencies

✅ **@SpringBootApplication** combines `@Configuration`, `@EnableAutoConfiguration`, and `@ComponentScan`

✅ **Component scanning** discovers beans in the application package and sub-packages

✅ **SpringApplication.run()** bootstraps the entire application in one method call

✅ **Application events** allow you to hook into the application lifecycle

✅ **Environment bean** provides access to configuration properties

✅ **Auto-configuration** is conditional and can be customized or disabled

### Next Steps

Now that you understand how Spring Boot applications start up, you're ready to:

1. **Learn Dependency Injection** ([02-dependency-injection.md](02-dependency-injection.md))
2. **Create domain models** with JPA entities
3. **Build REST controllers** to handle HTTP requests
4. **Configure databases** and data access

---

## Related Files

- `src/main/java/com/tutorial/taskmanager/TaskManagerApplication.java` - Main application class
- `src/main/java/com/tutorial/taskmanager/PACKAGE-INFO.md` - Detailed package documentation
- `src/main/resources/application.yml` - Application configuration
- `pom.xml` - Maven dependencies and build configuration

---

## Additional Resources

- [Spring Boot Reference - Getting Started](https://docs.spring.io/spring-boot/docs/current/reference/html/getting-started.html)
- [Spring Boot Reference - Auto-configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/using.html#using.auto-configuration)
- [Spring Boot Reference - Application Events](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.spring-application.application-events-and-listeners)
- [Baeldung - Spring Boot Tutorial](https://www.baeldung.com/spring-boot)

---

**Last Updated:** 2025-11-15
