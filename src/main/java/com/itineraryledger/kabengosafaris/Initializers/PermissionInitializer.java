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
            "ITINERARY_DAY_ACTIVITY",
            "ITINERARY_DAY_PARK",
            "ITINERARY_DAY_PARK_ACTIVITY",
            "ITINERARY_DAY_PARK_TARIFF",
            "ITINERARY_DAY_ACCOMMODATION",
            // Safari Module Entities
            "SAFARI",
            "SAFARI_PAX",
            "SAFARI_DAY",
            "SAFARI_DAY_ACTIVITY",
            "SAFARI_DAY_PARK",
            "SAFARI_DAY_ACCOMMODATION",
            // PDF Module Entities
            "PDF_DOCUMENT",
            "PDF_TEMPLATE",
            // Translation Module Entities
            "TRANSLATION_SETTING",
            "TRANSLATION_CACHE",
            // Customer Module Entities
            "CUSTOMER",
            "CUSTOMER_EMAIL",
            "CUSTOMER_PHONE",
            "CUSTOMER_DOCUMENT",
            "CUSTOMER_NOTE",
            // Quotation Module Entities
            "QUOTATION",
            "QUOTATION_PAX",
            "QUOTATION_LINE_ITEM",
            // Image Settings Entity
            "IMAGE_SETTING",
            // File Settings Entity
            "FILE_SETTING"
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

            // ========================
            // SAFARI STATE TRANSITION PERMISSIONS
            // ========================

            // Booking State Transitions
            {
                "SUBMIT_SAFARI_FOR_APPROVAL",
                "UPDATE",
                "SAFARI",
                "Allows submitting a draft safari for management approval (DRAFT -> PENDING_APPROVAL)"
            },
            {
                "APPROVE_SAFARI",
                "UPDATE",
                "SAFARI",
                "Allows approving a safari booking (PENDING_APPROVAL -> APPROVED)"
            },
            {
                "REJECT_SAFARI",
                "UPDATE",
                "SAFARI",
                "Allows rejecting a safari booking (PENDING_APPROVAL -> DRAFT)"
            },
            {
                "CONFIRM_SAFARI",
                "UPDATE",
                "SAFARI",
                "Allows confirming a safari with client and suppliers (APPROVED -> CONFIRMED)"
            },

            // Payment State Transitions
            {
                "REQUEST_SAFARI_DEPOSIT",
                "UPDATE",
                "SAFARI",
                "Allows transitioning safari to pending deposit state (CONFIRMED -> PENDING_DEPOSIT)"
            },
            {
                "RECORD_SAFARI_DEPOSIT",
                "UPDATE",
                "SAFARI",
                "Allows recording deposit payment received (PENDING_DEPOSIT -> DEPOSIT_PAID)"
            },
            {
                "RECORD_SAFARI_FULL_PAYMENT",
                "UPDATE",
                "SAFARI",
                "Allows recording full payment received (DEPOSIT_PAID -> FULLY_PAID)"
            },

            // Operational State Transitions
            {
                "MARK_SAFARI_READY",
                "UPDATE",
                "SAFARI",
                "Allows marking safari as ready to commence (FULLY_PAID -> READY)"
            },
            {
                "START_SAFARI",
                "UPDATE",
                "SAFARI",
                "Allows starting a safari (READY -> IN_PROGRESS)"
            },
            {
                "COMPLETE_SAFARI",
                "UPDATE",
                "SAFARI",
                "Allows marking safari as completed (IN_PROGRESS -> COMPLETED)"
            },

            // Post-Safari State Transitions
            {
                "REQUEST_SAFARI_REVIEW",
                "UPDATE",
                "SAFARI",
                "Allows transitioning safari to pending review (COMPLETED -> PENDING_REVIEW)"
            },
            {
                "CLOSE_SAFARI",
                "UPDATE",
                "SAFARI",
                "Allows closing a safari after all post-trip tasks are done (PENDING_REVIEW/COMPLETED -> CLOSED)"
            },

            // Hold/Pause State Transitions
            {
                "HOLD_SAFARI",
                "UPDATE",
                "SAFARI",
                "Allows putting a safari on hold (-> ON_HOLD)"
            },
            {
                "RELEASE_SAFARI_HOLD",
                "UPDATE",
                "SAFARI",
                "Allows releasing a safari from hold state (ON_HOLD -> previous active state)"
            },
            {
                "MARK_SAFARI_PENDING_DOCUMENTS",
                "UPDATE",
                "SAFARI",
                "Allows marking safari as awaiting documents (-> PENDING_DOCUMENTS)"
            },
            {
                "MARK_SAFARI_DOCUMENTS_RECEIVED",
                "UPDATE",
                "SAFARI",
                "Allows marking that required documents have been received (PENDING_DOCUMENTS -> previous state)"
            },
            {
                "MARK_SAFARI_PENDING_AVAILABILITY",
                "UPDATE",
                "SAFARI",
                "Allows marking safari as awaiting availability confirmation (-> PENDING_AVAILABILITY)"
            },
            {
                "MARK_SAFARI_AVAILABILITY_CONFIRMED",
                "UPDATE",
                "SAFARI",
                "Allows confirming availability (PENDING_AVAILABILITY -> previous state)"
            },

            // Reschedule State Transitions
            {
                "POSTPONE_SAFARI",
                "UPDATE",
                "SAFARI",
                "Allows postponing a safari to a later date (-> POSTPONED)"
            },
            {
                "INITIATE_SAFARI_RESCHEDULE",
                "UPDATE",
                "SAFARI",
                "Allows initiating safari date change process (-> RESCHEDULING)"
            },
            {
                "COMPLETE_SAFARI_RESCHEDULE",
                "UPDATE",
                "SAFARI",
                "Allows completing safari reschedule with new dates (RESCHEDULING -> CONFIRMED)"
            },

            // Cancellation State Transitions
            {
                "REQUEST_SAFARI_CANCELLATION",
                "CANCEL",
                "SAFARI",
                "Allows requesting safari cancellation (-> CANCELLATION_REQUESTED)"
            },
            {
                "CANCEL_SAFARI",
                "CANCEL",
                "SAFARI",
                "Allows cancelling a safari (-> CANCELLED)"
            },
            {
                "CANCEL_SAFARI_BY_CLIENT",
                "CANCEL",
                "SAFARI",
                "Allows recording client-initiated cancellation (-> CANCELLED_BY_CLIENT)"
            },
            {
                "CANCEL_SAFARI_BY_OPERATOR",
                "CANCEL",
                "SAFARI",
                "Allows operator-initiated cancellation (-> CANCELLED_BY_OPERATOR)"
            },
            {
                "CANCEL_SAFARI_FORCE_MAJEURE",
                "CANCEL",
                "SAFARI",
                "Allows force majeure cancellation (-> CANCELLED_FORCE_MAJEURE)"
            },

            // Refund State Transitions
            {
                "INITIATE_SAFARI_REFUND",
                "UPDATE",
                "SAFARI",
                "Allows initiating refund process (CANCELLED_* -> REFUND_PENDING)"
            },
            {
                "RECORD_SAFARI_PARTIAL_REFUND",
                "UPDATE",
                "SAFARI",
                "Allows recording partial refund issued (REFUND_PENDING -> REFUND_PARTIAL)"
            },
            {
                "RECORD_SAFARI_FULL_REFUND",
                "UPDATE",
                "SAFARI",
                "Allows recording full refund issued (REFUND_PENDING/REFUND_PARTIAL -> REFUND_COMPLETE)"
            },

            // Dispute State Transitions
            {
                "MARK_SAFARI_DISPUTED",
                "UPDATE",
                "SAFARI",
                "Allows marking a safari as disputed by client (-> DISPUTED)"
            },
            {
                "INVESTIGATE_SAFARI_DISPUTE",
                "UPDATE",
                "SAFARI",
                "Allows marking dispute as under investigation (DISPUTED -> UNDER_INVESTIGATION)"
            },
            {
                "RESOLVE_SAFARI_DISPUTE",
                "UPDATE",
                "SAFARI",
                "Allows resolving a safari dispute (UNDER_INVESTIGATION/DISPUTED -> appropriate resolution state)"
            },

            // Full Safari View Permission
            {
                "READ_FULL_SAFARI",
                "READ",
                "SAFARI",
                "Allows viewing complete safari with all nested data (days, parks, accommodations, pax)"
            },

            // ========================
            // PDF GENERATION PERMISSIONS
            // ========================
            {
                "GENERATE_PDF",
                "CREATE",
                "PDF_TEMPLATE",
                "Allows generating PDF documents from templates"
            },

            // ========================
            // TRANSLATION PERMISSIONS
            // ========================
            {
                "READ_TRANSLATION_CACHE_STATS",
                "READ",
                "TRANSLATION_SETTING",
                "Allows viewing translation cache statistics"
            },
            {
                "CLEAR_TRANSLATION_CACHE",
                "DELETE",
                "TRANSLATION_SETTING",
                "Allows clearing translation cache entries"
            },
            {
                "TEST_TRANSLATION_SERVICE",
                "EXECUTE",
                "TRANSLATION_SETTING",
                "Allows testing the translation service connection"
            },

            // ========================
            // QUOTATION PERMISSIONS
            // ========================
            {
                "SEND_QUOTATION",
                "UPDATE",
                "QUOTATION",
                "Allows sending a quotation to the customer (DRAFT -> SENT)"
            },
            {
                "CONVERT_QUOTATION_TO_SAFARI",
                "CREATE",
                "SAFARI",
                "Allows converting an accepted quotation into a Safari booking"
            },
            {
                "READ_FULL_QUOTATION",
                "READ",
                "QUOTATION",
                "Allows viewing complete quotation with all nested data (pax list, line items)"
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
