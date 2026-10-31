package com.examradar.backend.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Custom Flyway migration strategy that performs auto-repair before migration.
 * Cleans up any failed migration attempts, aligns checksums, and ensures zero-downtime
 * restarts across cloud deployments without manual database intervention.
 */
@Configuration
public class FlywayConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayConfig.class);

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            log.info("Executing Flyway repair to heal any failed migrations or metadata inconsistencies...");
            try {
                flyway.repair();
                log.info("Flyway repair completed successfully.");
            } catch (Exception e) {
                log.warn("Flyway repair encountered non-fatal warning: {}", e.getMessage());
            }

            log.info("Executing Flyway migrate...");
            flyway.migrate();
            log.info("Flyway migration completed successfully.");
        };
    }
}

