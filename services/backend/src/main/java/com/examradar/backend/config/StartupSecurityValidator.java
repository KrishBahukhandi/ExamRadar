package com.examradar.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates that all required environment secrets are present before the application
 * starts serving traffic. The server will refuse to start if any critical secret
 * is missing or still set to a known placeholder value.
 */
@Configuration
@Profile("!test")
@ConditionalOnProperty(name = "examradar.security.validation.enabled", havingValue = "true", matchIfMissing = true)
public class StartupSecurityValidator {

    private static final Logger log = LoggerFactory.getLogger(StartupSecurityValidator.class);

    /**
     * Known placeholder values that must never appear in production.
     * If any secret resolves to one of these, startup is aborted.
     */
    private static final List<String> FORBIDDEN_PLACEHOLDERS = List.of(
            "test-jwt-secret-placeholder-minimum-256-bits-length",
            "test-admin-secret-key-32-chars-minimum",
            "Krish@ExamRadar",
            "your-supabase-jwt-secret-placeholder",
            "your-secure-db-password-placeholder",
            "admin-secret-key-placeholder-min-32-chars",
            "placeholder",
            "changeme",
            "secret"
    );

    @Value("${examradar.security.jwt.secret:}")
    private String jwtSecret;

    @Value("${examradar.security.admin-api-key:}")
    private String adminApiKey;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Bean
    public Object validateSecrets() {
        List<String> violations = new ArrayList<>();

        // Validate JWT secret
        if (isNullOrBlank(jwtSecret)) {
            violations.add("SUPABASE_JWT_SECRET is not set");
        } else if (jwtSecret.length() < 32) {
            violations.add("SUPABASE_JWT_SECRET is too short (minimum 32 characters required for HMAC-SHA256)");
        } else if (isForbiddenPlaceholder(jwtSecret)) {
            violations.add("SUPABASE_JWT_SECRET is set to a known test placeholder value — use your real Supabase JWT secret");
        }

        // Validate admin API key
        if (isNullOrBlank(adminApiKey)) {
            violations.add("ADMIN_API_KEY is not set");
        } else if (adminApiKey.length() < 32) {
            violations.add("ADMIN_API_KEY is too short (minimum 32 characters required)");
        } else if (isForbiddenPlaceholder(adminApiKey)) {
            violations.add("ADMIN_API_KEY is set to a known test placeholder value");
        }

        // Validate database password
        if (isNullOrBlank(dbPassword)) {
            violations.add("DB_PASSWORD is not set");
        } else if (isForbiddenPlaceholder(dbPassword)) {
            violations.add("DB_PASSWORD is set to a known placeholder value — use your real database password");
        }

        if (!violations.isEmpty()) {
            log.error("=================================================================");
            log.error("STARTUP ABORTED: Required environment secrets are missing or unsafe.");
            log.error("=================================================================");
            for (String violation : violations) {
                log.error("  ✗ {}", violation);
            }
            log.error("=================================================================");
            log.error("Set the following environment variables before starting the server:");
            log.error("  DB_PASSWORD, SUPABASE_JWT_SECRET, ADMIN_API_KEY");
            log.error("=================================================================");
            throw new IllegalStateException(
                "Server startup aborted: " + violations.size() + " required secret(s) are missing or unsafe. " +
                "Check the logs above for details."
            );
        }

        log.info("Security validation passed: all required secrets are present and non-trivial.");
        return new Object();
    }

    private boolean isNullOrBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isForbiddenPlaceholder(String value) {
        String lower = value.toLowerCase();
        return FORBIDDEN_PLACEHOLDERS.stream().anyMatch(p -> lower.contains(p.toLowerCase()));
    }
}
