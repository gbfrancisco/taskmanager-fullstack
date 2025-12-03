# Spring Boot 3 Cookbook - Documentation Index

This cookbook demonstrates Spring Boot 3 features through a Task Manager application, progressing from fundamentals to advanced enterprise patterns.

## Phase 1: Fundamentals (Getting Started)

### Core Concepts
- [x] **[01-getting-started.md](01-getting-started.md)** - Spring Boot basics, auto-configuration, and project structure
- [x] **[02-dependency-injection.md](02-dependency-injection.md)** - IoC container, beans, and dependency injection patterns
- [ ] **03-application-properties.md** - Configuration management and Spring profiles

### Web Layer
- [ ] **04-rest-controllers.md** - Building RESTful APIs with @RestController
- [ ] **05-request-mapping.md** - HTTP methods, path variables, and request parameters
- [x] **[06-data-transfer-objects.md](06-data-transfer-objects.md)** - DTOs, separation of concerns, avoiding circular references

### Data Layer
- [x] **[07-jpa-entities.md](07-jpa-entities.md)** - JPA entities, relationships, and repositories
- [x] **[08-jpa-entity-lifecycle.md](08-jpa-entity-lifecycle.md)** - Entity states, persistence context, and state transitions
- [x] **[09-jpa-query-methods.md](09-jpa-query-methods.md)** - Query method derivation and performance optimization
- [ ] **database-configuration.md** - DataSource setup, connection pools, H2 console (future)

### Validation & Error Handling
- [ ] **10-bean-validation.md** - Input validation with Jakarta Validation
- [ ] **11-exception-handling.md** - @ControllerAdvice and global error handling
- [ ] **12-custom-validators.md** - Creating custom validation annotations

### Security
- [ ] **13-spring-security-basics.md** - Security fundamentals and configuration
- [ ] **14-authentication.md** - User authentication and password encoding
- [ ] **15-authorization.md** - Role-based access control and method security

### Testing
- [x] **[16-unit-testing.md](16-unit-testing.md)** - JUnit 5, Mockito, and testing services
- [ ] **17-integration-testing.md** - @SpringBootTest and testing the full stack
- [ ] **18-web-layer-testing.md** - @WebMvcTest and testing controllers
- [ ] **19-data-layer-testing.md** - @DataJpaTest and repository testing

### API Documentation
- [ ] **20-openapi-documentation.md** - SpringDoc OpenAPI and Swagger UI

## Phase 2: Intermediate Features (To Be Added)

- [ ] Custom configuration properties with @ConfigurationProperties
- [ ] Actuator and application monitoring
- [ ] Caching with Spring Cache abstraction
- [ ] Async processing with @Async
- [ ] Scheduled tasks with @Scheduled
- [ ] File upload/download handling

## Phase 3: Advanced/Enterprise (Future)

- [ ] Messaging with Kafka/RabbitMQ
- [ ] Event-driven architecture with Spring Events
- [ ] Multiple database support
- [ ] Distributed tracing and observability
- [ ] Custom Spring Boot starters
- [ ] Reactive programming with WebFlux

---

**Note**: Each document will include:
- Concept explanation
- Code examples from the task manager application
- Best practices and common pitfalls
- References to actual implementation files
