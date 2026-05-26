package com.itineraryledger.kabengosafaris.Initializers;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * One-time schema migration: drop the legacy {@code vehicle_hires.client_name}
 * column.
 *
 * The VehicleHire entity used to denormalise the renting party's name into a
 * NOT NULL {@code client_name} column. It was refactored to a {@code
 * rental_client_id} FK referencing RentalClient, and the field was removed
 * from the entity. {@code ddl-auto=update} does not drop columns, so MySQL
 * still rejects every INSERT with "Field 'client_name' doesn't have a
 * default value".
 *
 * Idempotent: only drops the column when it is still present in the running
 * schema.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VehicleHireClientNameDropMigrationInitializer implements ApplicationRunner, Ordered {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public int getOrder() {
        return -100;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            Integer columnExists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE TABLE_SCHEMA = DATABASE() " +
                    "  AND TABLE_NAME = 'vehicle_hires' " +
                    "  AND COLUMN_NAME = 'client_name'",
                    Integer.class);

            if (columnExists != null && columnExists > 0) {
                log.info("Dropping legacy vehicle_hires.client_name column (replaced by rental_client_id FK)...");
                jdbcTemplate.execute("ALTER TABLE vehicle_hires DROP COLUMN client_name");
                log.info("vehicle_hires.client_name dropped");
            }
        } catch (Exception e) {
            log.warn("Could not drop vehicle_hires.client_name: {}", e.getMessage());
        }
    }
}
