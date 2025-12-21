package com.tutorial.taskmanager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Main entry point for the Task Manager Spring Boot application.
 *
 * <h2>@SpringBootApplication Annotation</h2>
 * This is a convenience annotation that combines three key annotations:
 * <ul>
 *   <li><b>@Configuration</b>: Marks this class as a source of bean definitions</li>
 *   <li><b>@EnableAutoConfiguration</b>: Tells Spring Boot to automatically configure
 *      beans based on classpath dependencies (e.g., H2, JPA, Web)</li>
 *   <li><b>@ComponentScan</b>: Scans this package and sub-packages for @Component, @Service, @Repository, @Controller
 *      classes</li>
 * </ul>
 *
 * <h2>Component Scanning</h2>
 * By default, Spring Boot scans all packages under the base package (com.tutorial.taskmanager).
 * This means all classes annotated with stereotype annotations will be automatically discovered
 * and registered as Spring beans.
 *
 * @author Spring Boot 3 Tutorial
 * @version 1.0
 */
@SpringBootApplication
public class TaskManagerApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskManagerApplication.class);

    /**
     * Main method - the application entry point.
     *
     * <p>SpringApplication.run() does several things:
     * <ol>
     *   <li>Creates the Spring ApplicationContext</li>
     *   <li>Registers command-line arguments as beans</li>
     *   <li>Triggers auto-configuration based on classpath</li>
     *   <li>Starts the embedded web server (Tomcat by default)</li>
     *   <li>Publishes application events (like ApplicationReadyEvent)</li>
     * </ol>
     *
     * @param args command-line arguments (can be used to override properties)
     */
    public static void main(String[] args) {
        LOGGER.info("Starting TaskManagerApplication...");
        SpringApplication.run(TaskManagerApplication.class, args);
    }
}

/**
 * Component that logs application startup information.
 *
 * <p>This demonstrates:
 * <ul>
 *   <li><b>@Component</b>: Marks this as a Spring-managed bean</li>
 *   <li><b>@EventListener</b>: Listens for ApplicationReadyEvent (fired when app is ready to serve requests)</li>
 *   <li><b>Environment injection</b>: Shows how to access application properties</li>
 * </ul>
 */
@Component
class ApplicationStartupLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationStartupLogger.class);

    private final Environment environment;

    /**
     * Constructor injection - Spring automatically provides the Environment bean.
     *
     * @param environment Spring's Environment abstraction for accessing properties
     */
    public ApplicationStartupLogger(Environment environment) {
        this.environment = environment;
    }

    /**
     * Logs application details once the application is fully started and ready.
     *
     * <p>ApplicationReadyEvent is published when:
     * <ul>
     *   <li>All beans are initialized</li>
     *   <li>The web server is started and ready to accept requests</li>
     *   <li>The application is fully configured and operational</li>
     * </ul>
     */
    @EventListener(ApplicationReadyEvent.class)
    public void logApplicationStartup() {
        String port = environment.getProperty("server.port", "8080");
        String contextPath = environment.getProperty("server.servlet.context-path", "");

        LOGGER.info("=".repeat(80));
        LOGGER.info("Task Manager Application is RUNNING!");
        LOGGER.info("=".repeat(80));
        LOGGER.info("Local URL:          http://localhost:{}{}", port, contextPath);
        LOGGER.info("H2 Console:         http://localhost:{}{}/h2-console", port, contextPath);
        LOGGER.info("API Documentation:  http://localhost:{}{}/swagger-ui.html", port, contextPath);
        LOGGER.info("Profile(s):         {}", String.join(", ", environment.getActiveProfiles().length > 0
            ? environment.getActiveProfiles()
            : new String[]{"default"}));
        LOGGER.info("=".repeat(80));
    }
}
