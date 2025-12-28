package com.tutorial.taskmanager.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("app.jwt")
public class JwtProperties {

    /**
     * Secret key for signing JWT tokens (Base64 encoded).
     * Must be at least 256 bits (32 bytes) for HS256 algorithm.
     */
    private String secret;

    /**
     * Token expiration time in milliseconds.
     * Default: 86400000 (24 hours)
     */
    private Long expirationMs;

    /**
     * Issuer claim for JWT tokens.
     * Identifies this application as the token issuer.
     */
    private String issuer;
}
