# Task Manager Fullstack Tutorial

A comprehensive fullstack learning resource demonstrating modern web development practices. Build a complete Task Manager application from backend to frontend.

## Project Structure

```
taskmanager-fullstack/
├── server/          # Spring Boot backend (REST API)
└── client/          # Frontend (coming soon)
```

## Components

### Server (Spring Boot)

A REST API built with Spring Boot 3, demonstrating:
- RESTful CRUD operations
- Spring Data JPA with H2/PostgreSQL
- Authentication & authorization
- Comprehensive testing (unit, integration, slice)
- Industry best practices

**Tech Stack:** Java 21, Spring Boot 3.2, Spring Data JPA, Spring Security

[View Server Documentation](./server/README.md)

### Client (Coming Soon)

Frontend application for the Task Manager.

## Getting Started

### Running the Server

```bash
cd server
./mvnw spring-boot:run
```

Server runs at http://localhost:8080

### API Documentation

Once the server is running:
- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 Console: http://localhost:8080/h2-console

## Learning Approach

This project emphasizes **hands-on learning**:
1. Read the documentation
2. Write the code yourself
3. Understand the patterns and best practices
4. Run tests to validate your implementation

Each component has detailed documentation in its respective `docs/` folder.
