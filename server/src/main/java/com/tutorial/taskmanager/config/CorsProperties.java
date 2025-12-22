package com.tutorial.taskmanager.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for CORS (Cross-Origin Resource Sharing).
 *
 * <p>This class uses {@code @ConfigurationProperties} to bind YAML configuration
 * to a type-safe Java object. This is the recommended approach in Spring Boot
 * for handling complex configuration values like lists.
 *
 * <p><strong>Why @ConfigurationProperties over @Value?</strong>
 * <ul>
 *   <li>Type-safe binding - YAML lists bind directly to Java List</li>
 *   <li>IDE support - autocomplete and refactoring work properly</li>
 *   <li>Validation - can add @Validated with JSR-303 constraints</li>
 *   <li>Immutability - can use constructor binding for immutable config</li>
 *   <li>Documentation - properties are self-documenting in the class</li>
 * </ul>
 *
 * <p><strong>Configuration example:</strong>
 * <pre>
 * app:
 *   cors:
 *     allowed-origins:
 *       - http://localhost:5173
 *       - http://localhost:3000
 * </pre>
 *
 * @see WebConfig
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    /**
     * List of allowed origins for CORS requests.
     *
     * <p>Each origin should be a full URL including protocol and port.
     * Example: http://localhost:5173
     */
    private List<String> allowedOrigins = new ArrayList<>();

}
