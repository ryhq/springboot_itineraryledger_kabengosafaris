package com.itineraryledger.kabengosafaris.Configurations;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Permission.PermissionActionService;
import com.itineraryledger.kabengosafaris.Role.RoleService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * DataInitializationService - Initializes system RBAC data on application startup
 *
 * This service runs automatically when the application starts (after Spring context is ready)
 * and ensures that all required system data is initialized:
 * 1. System action types (CREATE, READ, UPDATE, DELETE, etc.)
 * 2. System roles (ADMIN, USER, etc.)
 *
 * This ensures the application has a consistent base state for RBAC operations.
 * The initialization is idempotent - running it multiple times is safe and will not duplicate data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataInitializationService {

    private final PermissionActionService permissionActionService;
    private final RoleService roleService;

    /**
     * Initialize system RBAC data on application ready event
     * This method is called automatically after Spring context is initialized
     *
     * Initialization order is important:
     * 1. Action types must be created first (permissions depend on them)
     * 2. Roles are created next (can be assigned to users later)
     * 3. Permissions can be created after roles (if needed)
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeSystemData() {
        System.out.println("\n\n\n");
        log.info("=========================================");
        log.info("Starting system RBAC data initialization...");
        log.info("=========================================");
        System.out.println("\n");

        try {
            // Step 1: Initialize system action types
            log.info("Step 1: Initializing system action types...");
            permissionActionService.initializeSystemActions();
            log.info("✓ System action types initialized");

            // Step 2: Initialize system roles
            log.info("Step 2: Initializing system roles...");
            roleService.initializeSystemRoles();
            log.info("✓ System roles initialized");

            // Step 3: Additional initialization (e.g., default permissions) can go here
            log.info("Step 3: Additional initialization...");
            initializeDefaultPermissions();
            log.info("✓ Additional initialization complete");

            log.info("=========================================");
            log.info("✓ System RBAC data initialization complete");
            log.info("=========================================");

        } catch (Exception e) {
            log.error("Error during system RBAC initialization", e);
            // Don't throw - allow application to start even if initialization fails
            // This allows for manual recovery/correction
        }
    }

    /**
     * Initialize default permissions (optional)
     * This method can be used to create default permissions for core features
     *
     * Note: This is optional and can be expanded as needed for your application
     * For example:
     * - permissionService.createPermission("create_booking", "Create new bookings", "Booking", "create", "Booking");
     * - permissionService.createPermission("view_dashboard", "View dashboard", "Dashboard", "read", "Dashboard");
     */
    private void initializeDefaultPermissions() {
        log.debug("Default permissions initialization skipped (none configured)");
        // Uncomment and modify as needed:
        /*
        try {
            PermissionService permissionService = ...; // Inject via constructor if needed

            // Create default permissions for core features
            if (!permissionService.permissionExists("create_booking")) {
                permissionService.createPermission(
                    "create_booking",
                    "Create new bookings",
                    "Booking",
                    "create",
                    "Booking"
                );
            }

            if (!permissionService.permissionExists("view_dashboard")) {
                permissionService.createPermission(
                    "view_dashboard",
                    "View dashboard",
                    "Dashboard",
                    "read",
                    "Dashboard"
                );
            }

            log.info("Default permissions initialized");
        } catch (Exception e) {
            log.error("Error initializing default permissions", e);
        }
        */
    }
}
