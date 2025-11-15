# Spring Boot 3 Cookbook

A comprehensive, hands-on cookbook for learning Spring Boot 3 from fundamentals to enterprise-level features. This project demonstrates best practices through a fully functional Task Manager application.

## What is This?

This repository serves as both a learning resource and a reference implementation for Spring Boot 3. Each feature is:
- Implemented in working code following industry standards
- Documented with explanations in the `docs/` folder
- Organized in a clean, maintainable structure

## Project Structure

```
spring-boot-3-tutorial/
├── docs/                          # Feature documentation and guides
├── src/
│   ├── main/
│   │   ├── java/com/tutorial/taskmanager/
│   │   │   ├── config/           # Configuration classes
│   │   │   ├── controller/       # REST controllers
│   │   │   ├── dto/              # Data Transfer Objects
│   │   │   ├── exception/        # Exception handling
│   │   │   ├── model/            # JPA entities
│   │   │   ├── repository/       # Data access layer
│   │   │   ├── security/         # Security configuration
│   │   │   ├── service/          # Business logic
│   │   │   └── util/             # Utility classes
│   │   └── resources/
│   │       └── application.yml   # Application configuration
│   └── test/                      # Comprehensive test suite
└── pom.xml                        # Maven dependencies
```

## Technology Stack

- **Java 21** - Latest LTS version
- **Spring Boot 3.2.0** - Latest stable release
- **Spring Data JPA** - Database abstraction
- **Spring Security** - Authentication & authorization
- **H2 Database** - In-memory database (will evolve to support multiple DBs)
- **SpringDoc OpenAPI** - API documentation
- **Lombok** - Reduce boilerplate code
- **JUnit 5 & Mockito** - Testing framework

## Getting Started

### Prerequisites

- Java 21 or higher
- Maven 3.6+
- Your favorite IDE (IntelliJ IDEA, Eclipse, VS Code)

### Running the Application

```bash
# Clone the repository
git clone <repository-url>
cd spring-boot-3-tutorial

# Run with Maven
./mvnw spring-boot:run

# Or run tests
./mvnw test
```

The application will start on `http://localhost:8080`

### Exploring the Application

Once running, you can access:
- **API Documentation**: http://localhost:8080/swagger-ui.html
- **H2 Database Console**: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:taskdb`
  - Username: `sa`
  - Password: (leave empty)

## Learning Path

This cookbook is organized into phases. Start with Phase 1 and progress sequentially:

### Phase 1: Fundamentals ✅
Core Spring Boot concepts needed for any application:
- Getting Started & Project Structure
- Dependency Injection & IoC
- REST Controllers & Web Layer
- Spring Data JPA & Database Access
- Bean Validation
- Exception Handling
- Spring Security Basics
- Testing Strategies

See [docs/README.md](docs/README.md) for the complete list of topics.

### Phase 2: Intermediate (Coming Soon)
Advanced features for production applications:
- Custom Configuration Properties
- Actuator & Monitoring
- Caching
- Async Processing
- Scheduled Tasks

### Phase 3: Enterprise-Level (Future)
Production-ready, scalable patterns:
- Messaging (Kafka/RabbitMQ)
- Event-Driven Architecture
- Multiple Database Support
- Distributed Tracing
- Custom Spring Boot Starters
- Reactive Programming

## Contributing to Your Learning

This is a hands-on cookbook - **you write the code**!

For each topic:
1. Read the documentation in `docs/`
2. Implement the feature yourself
3. Refer to best practices and patterns
4. Run tests to validate your implementation

## Code Standards

This project follows industry-standard practices:
- Clean code principles
- SOLID design patterns
- Comprehensive testing (unit, integration, e2e)
- Proper exception handling
- API versioning and documentation
- Security best practices

## Next Steps

1. Create your main application class: `TaskManagerApplication.java`
2. Start with the domain model (entities)
3. Follow along with the documentation in `docs/`

Happy coding! 🚀
