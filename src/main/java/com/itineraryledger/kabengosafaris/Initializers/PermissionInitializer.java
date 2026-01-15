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
 * This initializer creates:
 * 1. Standard CRUD permissions for all entities following the pattern: {ACTION}_{ENTITY}
 * 2. Custom permissions for specific use cases (e.g., matrix views, composite operations)
 *
 * Standard Permission Examples:
 * - CREATE_USER, READ_USER, UPDATE_USER, DELETE_USER
 * - CREATE_ROLE, READ_ROLE, UPDATE_ROLE, DELETE_ROLE
 *
 * Custom Permission Examples:
 * - READ_ACTIVITY_TARIFF_RATE_MATRIX - View rate matrix with related lookup data
 * - READ_PARK_TARIFF_RATE_MATRIX - View park rate matrix with related lookup data
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
            "PAX_AGE_CATEGORY",
            "PAX_NATION_CATEGORY",
            "TARIFF",
            "ACTIVITY_TARIFF_RATE",
            "PARK_TARIFF_RATE",
            // Itinerary Module Entities
            "ITINERARY",
            "ITINERARY_DAY",
            "ITINERARY_PAX",
            "ITINERARY_DAY_PARK",
            "ITINERARY_DAY_ACCOMMODATION",
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

        // Initialize custom permissions
        int[] customCounts = initializeCustomPermissions();
        createdCount += customCounts[0];
        existingCount += customCounts[1];

        log.info("Permission initialization complete: {} permissions created, {} already existed",
                createdCount, existingCount);

        // Log summary by entity
        for (String entity : entities) {
            long count = permissionRepository.countByEntity(entity);
            log.debug("Entity '{}' has {} permissions", entity, count);
        }
    }

    /**
     * Initialize custom permissions that don't follow the standard CRUD pattern.
     * These are task-based permissions for specific use cases like matrix views,
     * composite operations, or special access patterns.
     *
     * @return array with [createdCount, existingCount]
     */
    private int[] initializeCustomPermissions() {
        int createdCount = 0;
        int existingCount = 0;

        // Define custom permissions: [name, action, entity, description]
        String[][] customPermissions = {
            // Activity Tariff Rate Matrix - allows viewing rate matrix with related lookup data
            {
                "READ_ACTIVITY_TARIFF_RATE_MATRIX",
                "READ",
                "ACTIVITY_TARIFF_RATE",
                "Allows viewing the activity tariff rate matrix including seasons, age categories, and nation categories"
            },
            // Park Tariff Rate Matrix - allows viewing park rate matrix with related lookup data
            {
                "READ_PARK_TARIFF_RATE_MATRIX",
                "READ",
                "PARK_TARIFF_RATE",
                "Allows viewing the park tariff rate matrix including seasons, age categories, and nation categories"
            },
            // Accommodation Rate Matrix - allows viewing accommodation rate matrix with related lookup data
            {
                "READ_ACCOMMODATION_RATE_MATRIX",
                "READ",
                "ACCOMMODATION_RATE",
                "Allows viewing the accommodation rate matrix including seasons, room types, room standards, and board types"
            },
            // Itinerary Custom Permissions
            {
                "PUBLISH_ITINERARY",
                "UPDATE",
                "ITINERARY",
                "Allows publishing an itinerary to make it available for booking, that is, creating itinerary safari from it"
            },
            {
                "UNPUBLISH_ITINERARY",
                "UPDATE",
                "ITINERARY",
                "Allows unpublishing an itinerary to revert it from published status"
            },
            {
                "ARCHIVE_ITINERARY",
                "UPDATE",
                "ITINERARY",
                "Allows archiving an itinerary to mark it as no longer in use"
            },
            {
                "UNARCHIVE_ITINERARY",
                "UPDATE",
                "ITINERARY",
                "Allows unarchiving an itinerary to restore it from archived status"
            },
            {
                "READ_FULL_ITINERARY",
                "READ",
                "ITINERARY",
                "Allows viewing complete itinerary with all nested data (days, parks, accommodations, pax)"
            },
            {
                "CLONE_ITINERARY",
                "CREATE",
                "ITINERARY",
                "Allows duplicating an existing itinerary as a new template"
            },
        };

        for (String[] customPerm : customPermissions) {
            String permissionName = customPerm[0];
            String actionStr = customPerm[1];
            String entity = customPerm[2];
            String description = customPerm[3];

            if (!permissionRepository.existsByName(permissionName)) {
                PermissionAction action = PermissionAction.valueOf(actionStr);

                Permission permission = Permission.builder()
                    .name(permissionName)
                    .description(description)
                    .action(action)
                    .entity(entity)
                    .active(true)
                    .build();

                permissionRepository.save(permission);
                log.debug("Created custom permission: {}", permissionName);
                createdCount++;
            } else {
                log.trace("Custom permission already exists: {}", permissionName);
                existingCount++;
            }
        }

        if (createdCount > 0 || existingCount > 0) {
            log.info("Custom permissions: {} created, {} already existed", createdCount, existingCount);
        }

        return new int[]{createdCount, existingCount};
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
