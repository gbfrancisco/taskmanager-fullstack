package com.tutorial.taskmanager.config;

import com.tutorial.taskmanager.security.JwtAuthenticationEntryPoint;
import com.tutorial.taskmanager.security.JwtAuthenticationFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Security configuration for the Task Manager application.
 *
 * <p>This initial version permits all requests. We'll add JWT authentication
 * in subsequent checkpoints.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final JwtAuthenticationEntryPoint jwtAuthEntryPoint;
    private final UserDetailsService userDetailsService;
    private final CorsProperties corsProperties;

    public SecurityConfig(
        JwtAuthenticationFilter jwtAuthFilter,
        JwtAuthenticationEntryPoint jwtAuthEntryPoint,
        UserDetailsService userDetailsService,
        CorsProperties corsProperties
    ) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.jwtAuthEntryPoint = jwtAuthEntryPoint;
        this.userDetailsService = userDetailsService;
        this.corsProperties = corsProperties;
    }

    /**
     * Configures the security filter chain.
     *
     * <p>Current configuration:
     * <ul>
     *   <li>CSRF disabled (stateless API)</li>
     *   <li>Stateless session management</li>
     *   <li>Public endpoints: /api/auth/**, /swagger-ui/**, /h2-console/**</li>
     *   <li>All other requests require authentication</li>
     *   <li>JWT filter for token validation</li>
     *   <li>Custom 401 handler for unauthenticated requests</li>
     * </ul>
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            // Enable CORS at Security level - ensures error responses (401, 403) include CORS headers
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Disable CSRF - not needed for stateless REST API
            .csrf(AbstractHttpConfigurer::disable)

            // Stateless session - no server-side session storage
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - only login and register
                .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                // Everything else requires authentication (including /api/auth/me)
                .anyRequest().authenticated()
            )

            // Custom 401 handler
            .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthEntryPoint))

            // Add JWT filter before UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

            // Allow H2 console frames (development only)
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
            .build();
    }

    /**
     * Authentication provider that uses our UserDetailsService and PasswordEncoder.
     *
     * <p>Note: Spring Boot autoconfigures this if you have UserDetailsService
     * and PasswordEncoder beans. This explicit bean is useful for debugging.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * AuthenticationManager bean - used by AuthService for login.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Password encoder using BCrypt.
     *
     * <p>BCrypt is the industry standard for password hashing:
     * <ul>
     *   <li>Automatically handles salting</li>
     *   <li>Configurable work factor (default 10 rounds)</li>
     *   <li>Resistant to rainbow table attacks</li>
     * </ul>
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS configuration for Spring Security.
     *
     * <p><strong>Why configure CORS here instead of WebMvcConfigurer?</strong>
     * Spring Security's filter chain runs BEFORE Spring MVC. If Security rejects
     * a request (401/403), the MVC CORS handler never runs, so error responses
     * won't have CORS headers. By configuring CORS at the Security level, ALL
     * responses (including errors) will include proper CORS headers.
     *
     * <p>This uses the same allowed origins from application.yml via CorsProperties.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
