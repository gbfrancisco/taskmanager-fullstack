package com.tutorial.taskmanager.config;

import com.tutorial.taskmanager.enums.ProjectStatus;
import com.tutorial.taskmanager.enums.TaskStatus;
import com.tutorial.taskmanager.model.AppUser;
import com.tutorial.taskmanager.model.Project;
import com.tutorial.taskmanager.model.Task;
import com.tutorial.taskmanager.repository.AppUserRepository;
import com.tutorial.taskmanager.repository.ProjectRepository;
import com.tutorial.taskmanager.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Seeds the database with sample data on application startup.
 *
 * <p>This component runs only in the "dev" profile to avoid polluting
 * production databases. It creates sample users, projects, and tasks
 * for development and testing purposes.
 *
 * <p><strong>Sample Users:</strong>
 * <ul>
 *   <li>john / password123 - Has 2 projects with tasks</li>
 *   <li>jane / password123 - Has 1 project with tasks</li>
 * </ul>
 *
 * <p>To enable: Run with {@code -Dspring.profiles.active=dev} or set
 * {@code spring.profiles.active=dev} in application.yml
 */
@Component
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    private final Logger LOGGER = LoggerFactory.getLogger(DataSeeder.class);
    private final AppUserRepository appUserRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(
        AppUserRepository appUserRepository,
        ProjectRepository projectRepository,
        TaskRepository taskRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.appUserRepository = appUserRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Skip if data already exists (avoid duplicates on hot reload)
        if (appUserRepository.count() > 0) {
            LOGGER.info("Database already seeded, skipping...");
            return;
        }

        LOGGER.info("Seeding database with sample data...");

        // Create users
        AppUser john = createUser("john", "john@example.com", "password123");
        AppUser jane = createUser("jane", "jane@example.com", "password123");

        // Create projects for John
        Project webApp = createProject("Web Application", "Main web app project", ProjectStatus.ACTIVE, john);
        Project mobileApp = createProject("Mobile App", "iOS and Android app", ProjectStatus.PLANNING, john);

        // Create projects for Jane
        Project apiProject = createProject("API Development", "REST API backend", ProjectStatus.ACTIVE, jane);

        // Create tasks for John's Web Application project
        createTask("Setup React project", "Initialize with Vite and TypeScript", TaskStatus.COMPLETED,
            LocalDateTime.now().minusDays(5), john, webApp);
        createTask("Implement authentication", "Add login and registration", TaskStatus.IN_PROGRESS,
            LocalDateTime.now().plusDays(2), john, webApp);
        createTask("Create dashboard", "Build main dashboard UI", TaskStatus.TODO,
            LocalDateTime.now().plusDays(7), john, webApp);
        createTask("Add dark mode", "Implement theme switching", TaskStatus.TODO,
            LocalDateTime.now().plusDays(14), john, webApp);

        // Create tasks for John's Mobile App project
        createTask("Research frameworks", "Compare React Native vs Flutter", TaskStatus.COMPLETED,
            LocalDateTime.now().minusDays(3), john, mobileApp);
        createTask("Setup development environment", null, TaskStatus.TODO,
            LocalDateTime.now().plusDays(5), john, mobileApp);

        // Create tasks for Jane's API project
        createTask("Design API endpoints", "Create OpenAPI spec", TaskStatus.COMPLETED,
            LocalDateTime.now().minusDays(7), jane, apiProject);
        createTask("Implement user service", "CRUD operations for users", TaskStatus.COMPLETED,
            LocalDateTime.now().minusDays(2), jane, apiProject);
        createTask("Add JWT authentication", "Secure API with tokens", TaskStatus.IN_PROGRESS,
            LocalDateTime.now().plusDays(1), jane, apiProject);
        createTask("Write integration tests", null, TaskStatus.TODO,
            LocalDateTime.now().plusDays(10), jane, apiProject);

        // Create some tasks without projects (standalone)
        createTask("Read Spring Boot docs", "Study security chapter", TaskStatus.TODO,
            LocalDateTime.now().plusDays(3), john, null);
        createTask("Update resume", null, TaskStatus.TODO, null, jane, null);

        LOGGER.info("Database seeded successfully!");
        LOGGER.info("Sample users: john/password123, jane/password123");
    }

    private AppUser createUser(String username, String email, String rawPassword) {
        AppUser user = AppUser.builder()
            .username(username)
            .email(email)
            .password(passwordEncoder.encode(rawPassword))
            .build();
        return appUserRepository.save(user);
    }

    private Project createProject(String name, String description, ProjectStatus status, AppUser owner) {
        Project project = Project.builder()
            .name(name)
            .description(description)
            .status(status)
            .appUser(owner)
            .build();
        return projectRepository.save(project);
    }

    private Task createTask(String title, String description, TaskStatus status,
                           LocalDateTime dueDate, AppUser user, Project project) {
        Task task = Task.builder()
            .title(title)
            .description(description)
            .status(status)
            .dueDate(dueDate)
            .appUser(user)
            .project(project)
            .build();
        return taskRepository.save(task);
    }
}
