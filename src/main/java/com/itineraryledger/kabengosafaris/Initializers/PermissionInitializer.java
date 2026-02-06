package com.itineraryledger.kabengosafaris.Initializers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Permission.Permission;
import com.itineraryledger.kabengosafaris.Permission.PermissionAction;
import com.itineraryledger.kabengosafaris.Permission.PermissionRepository;

import java.io.IOException;
import java.util.List;

/**
 * PermissionInitializer - Initializes permissions in the database at application startup
 *
 * This initializer creates:
 * 1. Standard CRUD permissions for all entities following the pattern: {ACTION}_{ENTITY}
 * 2. Custom permissions for specific use cases (e.g., matrix views, composite operations)
 *
 * Configuration is loaded from JSON files in src/main/resources/permissions/:
 * - entities.json: List of entities that need CRUD permissions
 * - custom-permissions.json: Custom permissions with name, action, entity, and description
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
    private final ObjectMapper objectMapper;

    /**
     * DTO for deserializing entities.json
     */
    @Data
    private static class EntitiesConfig {
        private List<String> entities;
    }

    /**
     * DTO for deserializing custom-permissions.json
     */
    @Data
    private static class CustomPermissionsConfig {
        private List<CustomPermissionDTO> customPermissions;
    }

    @Data
    private static class CustomPermissionDTO {
        private String name;
        private String action;
        private String entity;
        private String description;
    }

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
        // Load entities from JSON file
        List<String> entities = loadEntitiesFromJson();

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

        // Load custom permissions from JSON file
        List<CustomPermissionDTO> customPermissions = loadCustomPermissionsFromJson();

        for (CustomPermissionDTO customPerm : customPermissions) {
            if (!permissionRepository.existsByName(customPerm.getName())) {
                PermissionAction action = PermissionAction.valueOf(customPerm.getAction());

                Permission permission = Permission.builder()
                    .name(customPerm.getName())
                    .description(customPerm.getDescription())
                    .action(action)
                    .entity(customPerm.getEntity())
                    .active(true)
                    .build();

                permissionRepository.save(permission);
                log.debug("Created custom permission: {}", customPerm.getName());
                createdCount++;
            } else {
                log.trace("Custom permission already exists: {}", customPerm.getName());
                existingCount++;
            }
        }

        if (createdCount > 0 || existingCount > 0) {
            log.info("Custom permissions: {} created, {} already existed", createdCount, existingCount);
        }

        return new int[]{createdCount, existingCount};
    }

    /**
     * Load entities from JSON configuration file
     *
     * @return list of entity names
     */
    private List<String> loadEntitiesFromJson() {
        try {
            ClassPathResource resource = new ClassPathResource("permissions/entities.json");
            EntitiesConfig config = objectMapper.readValue(resource.getInputStream(), EntitiesConfig.class);
            log.info("Loaded {} entities from entities.json", config.getEntities().size());
            return config.getEntities();
        } catch (IOException e) {
            log.error("Failed to load entities from entities.json", e);
            throw new RuntimeException("Failed to load entities configuration", e);
        }
    }

    /**
     * Load custom permissions from JSON configuration file
     *
     * @return list of custom permission DTOs
     */
    private List<CustomPermissionDTO> loadCustomPermissionsFromJson() {
        try {
            ClassPathResource resource = new ClassPathResource("permissions/custom-permissions.json");
            CustomPermissionsConfig config = objectMapper.readValue(
                resource.getInputStream(),
                CustomPermissionsConfig.class
            );
            log.info("Loaded {} custom permissions from custom-permissions.json",
                config.getCustomPermissions().size());
            return config.getCustomPermissions();
        } catch (IOException e) {
            log.error("Failed to load custom permissions from custom-permissions.json", e);
            throw new RuntimeException("Failed to load custom permissions configuration", e);
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
