package com.itineraryledger.kabengosafaris.Initializers;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * One-time schema migration: drop NOT NULL on email_messages.file_name.
 *
 * Resend-API-sent messages legitimately have no .eml file on disk, so
 * file_name must be nullable. The entity column constraint has been
 * relaxed, but ddl-auto=update does not drop existing NOT NULL clauses,
 * so we issue an explicit ALTER TABLE if the running column is still
 * defined as NOT NULL.
 *
 * Idempotent: only ALTERs when the column is currently NOT NULL.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailMessageFileNameNullableMigrationInitializer implements ApplicationRunner, Ordered {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public int getOrder() {
        return -100;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            String isNullable = jdbcTemplate.queryForObject(
                    "SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE TABLE_SCHEMA = DATABASE() " +
                    "  AND TABLE_NAME = 'email_messages' " +
                    "  AND COLUMN_NAME = 'file_name'",
                    String.class);

            if (isNullable == null) {
                return;
            }

            if ("NO".equalsIgnoreCase(isNullable)) {
                log.info("Migrating email_messages.file_name to nullable...");
                jdbcTemplate.execute(
                        "ALTER TABLE email_messages MODIFY COLUMN file_name VARCHAR(255) NULL");
                log.info("email_messages.file_name is now nullable");
            }
        } catch (Exception e) {
            log.warn("Could not migrate email_messages.file_name nullability: {}", e.getMessage());
        }
    }
}
