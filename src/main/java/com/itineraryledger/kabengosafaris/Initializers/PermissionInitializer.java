package com.itineraryledger.kabengosafaris.Initializers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Permission.Permission;
import com.itineraryledger.kabengosafaris.Permission.PermissionAction;
import com.itineraryledger.kabengosafaris.Permission.PermissionRepository;

/**
 * PermissionInitializer - Initializes permissions in the database at application startup
 *
 * This initializer creates standard permissions for all entities following the pattern:
 * {ACTION}_{ENTITY}
 *
 * Examples:
 * - CREATE_USER, READ_USER, UPDATE_USER, DELETE_USER
 * - CREATE_ROLE, READ_ROLE, UPDATE_ROLE, DELETE_ROLE
 * - CREATE_EMAIL_ACCOUNT, READ_EMAIL_ACCOUNT, UPDATE_EMAIL_ACCOUNT, DELETE_EMAIL_ACCOUNT
 *
 * Permissions are only created if they don't already exist (idempotent).
 * These permissions cannot be modified or deleted via API - they are system permissions.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 3) // Run third
public class PermissionInitializer implements ApplicationRunner {

    private final PermissionRepository permissionRepository;

    /**
     * Runs at application startup to initialize permissions
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        printStartBanner();

        try {
            initializePermissions();
            printEndBanner(true);
        } catch (Exception e) {
            log.error("Error during Permission initialization", e);
            printEndBanner(false);
        }
    }

    private void printStartBanner() {
        log.info("");
        log.info("╔════════════════════════════════════════════════════════════════════╗");
        log.info("║                                                                    ║");
        log.info("║                PERMISSION INITIALIZER - START                      ║");
        log.info("║                                                                    ║");
        log.info("╚════════════════════════════════════════════════════════════════════╝");
        log.info("");
    }

    private void printEndBanner(boolean success) {
        log.info("");
        if (success) {
            log.info("╔════════════════════════════════════════════════════════════════════╗");
            log.info("║                                                                    ║");
            log.info("║             ✓ PERMISSION INITIALIZER - COMPLETED                   ║");
            log.info("║                                                                    ║");
            log.info("╚════════════════════════════════════════════════════════════════════╝");
        } else {
            log.info("╔════════════════════════════════════════════════════════════════════╗");
            log.info("║                                                                    ║");
            log.info("║             ✗ PERMISSION INITIALIZER - FAILED                      ║");
            log.info("║                                                                    ║");
            log.info("╚════════════════════════════════════════════════════════════════════╝");
        }
        log.info("");
    }

    private void initializePermissions() {
        // Define entities that need CRUD permissions
        String[] entities = {
            "USER",
            "ROLE",
            "PERMISSION",
            "EMAIL_ACCOUNT",
            "EMAIL_ACCOUNT_SIGNATURE",
            "EMAIL_EVENT",
            "EMAIL_TEMPLATE",
            "SECURITY_SETTING",
            "AUDIT_LOG_SETTING",
            "AUDIT_LOG",
            "PARK",
            "ACTIVITY",
            "ACCOMMODATION",
            "ACCOMMODATION_EMAIL",
            "ACCOMMODATION_PHONE",
            "ACCOMMODATION_IMAGE",
            "ACCOMMODATION_RATE",
            "ACCOMMODATION_BOARD_TYPE",
            "ACCOMMODATION_ROOM_TYPE",
            "ACCOMMODATION_ROOM_STANDARD",
            "ACCOMMODATION_DOCUMENT",
            "SEASON",
            "SEASON_PERIOD",
        };

        // Define standard actions (CREATE, READ, UPDATE, DELETE)
        PermissionAction[] standardActions = {
            PermissionAction.CREATE,
            PermissionAction.READ,
            PermissionAction.UPDATE,
            PermissionAction.DELETE
        };

        int createdCount = 0;
        int existingCount = 0;

        // Create permissions for each entity
        for (String entity : entities) {
            for (PermissionAction action : standardActions) {
                String permissionName = action.name() + "_" + entity;

                if (!permissionRepository.existsByName(permissionName)) {
                    // Create permission
                    String description = generateDescription(action, entity);

                    Permission permission = Permission.builder()
                        .name(permissionName)
                        .description(description)
                        .action(action)
                        .entity(entity)
                        .active(true)
                        .build();

                    permissionRepository.save(permission);
                    log.debug("Created permission: {}", permissionName);
                    createdCount++;
                } else {
                    log.trace("⊘ Permission already exists: {}", permissionName);
                    existingCount++;
                }
            }
        }

        log.info("Permission initialization complete: {} permissions created, {} already existed",
                createdCount, existingCount);

        // Log summary by entity
        for (String entity : entities) {
            long count = permissionRepository.countByEntity(entity);
            log.debug("Entity '{}' has {} permissions", entity, count);
        }
    }

    /**
     * Generate human-readable description for a permission
     *
     * @param action the permission action
     * @param entity the entity name
     * @return formatted description
     */
    private String generateDescription(PermissionAction action, String entity) {
        String entityFormatted = formatEntityName(entity);

        switch (action) {
            case CREATE:
                return "Allows creating new " + entityFormatted + " records";
            case READ:
                return "Allows viewing and reading " + entityFormatted + " records";
            case UPDATE:
                return "Allows editing and updating " + entityFormatted + " records";
            case DELETE:
                return "Allows deleting " + entityFormatted + " records";
            case EXECUTE:
                return "Allows executing operations on " + entityFormatted;
            case SUBMIT:
                return "Allows submitting " + entityFormatted + " for approval";
            case AMEND:
                return "Allows amending submitted " + entityFormatted;
            case CANCEL:
                return "Allows canceling " + entityFormatted;
            case EXPORT:
                return "Allows exporting " + entityFormatted + " data";
            case PRINT:
                return "Allows printing " + entityFormatted + " documents";
            default:
                return "Permission for " + action.name() + " on " + entityFormatted;
        }
    }

    /**
     * Format entity name for display
     * Converts: USER -> User, EMAIL_ACCOUNT -> Email Account
     *
     * @param entity entity name in uppercase with underscores
     * @return formatted entity name
     */
    private String formatEntityName(String entity) {
        // Split by underscore and capitalize each word
        String[] parts = entity.split("_");
        StringBuilder formatted = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].toLowerCase();
            formatted.append(Character.toUpperCase(part.charAt(0)))
                     .append(part.substring(1));

            if (i < parts.length - 1) {
                formatted.append(" ");
            }
        }

        return formatted.toString();
    }
}
