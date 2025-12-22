package com.tutorial.taskmanager.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for the application.
 *
 * <p>Configures CORS (Cross-Origin Resource Sharing) to allow the frontend
 * to make requests to this backend API.
 *
 * <p><strong>Why CORS?</strong>
 * Browsers block requests from one origin (e.g., localhost:5173) to another
 * origin (e.g., localhost:8080) by default. This is a security feature.
 * CORS headers tell the browser it's okay to allow these cross-origin requests.
 *
 * <p><strong>Configuration:</strong>
 * Allowed origins are configured in application.yml under {@code app.cors.allowed-origins}.
 * We use {@code @ConfigurationProperties} via {@link CorsProperties} for type-safe binding.
 *
 * @see CorsProperties
 */
@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class WebConfig implements WebMvcConfigurer {

    private final CorsProperties corsProperties;

    /**
     * Constructor injection of CORS properties.
     *
     * <p>Spring automatically creates and injects the CorsProperties bean
     * because we enabled it with @EnableConfigurationProperties.
     */
    public WebConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(corsProperties.getAllowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
