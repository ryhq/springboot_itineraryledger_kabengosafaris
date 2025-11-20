package com.itineraryledger.kabengosafaris.Permission;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PermissionService - Manages granular permission definitions
 *
 * This service handles all CRUD operations for permissions (e.g., "create_booking", "view_reports").
 * Each permission combines:
 * - Action type (CREATE, READ, UPDATE, DELETE, etc.)
 * - Resource type (Booking, Report, User, etc.)
 * - Category (Booking, Reporting, User Management, etc.)
 *
 * Key Responsibilities:
 * - Create permissions with validation
 * - Query permissions by resource and action
 * - Deactivate permissions
 * - Manage permission lifecycle
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionActionService actionService;

    /**
     * Create a new permission with validation
     * Validates that:
     * - Permission name doesn't already exist
     * - Action code exists in database
     *
     * @param name unique permission identifier (e.g., "create_booking")
     * @param description human-readable description
     * @param category module/entity category (e.g., "Booking", "User")
     * @param actionCode action type code (must exist in permission_action_types table)
     * @param resource resource/document type (e.g., "Booking", "Report")
     * @return the created Permission
     * @throws IllegalArgumentException if name exists or action code invalid
     */
    @Transactional
    @AuditLogAnnotation(action = "CREATE_PERMISSION", entityType = "Permission", description = "Create a new permission")
    public Permission createPermission(String name, String description, String category,
                                       String actionCode, String resource) {
        // Validate permission name doesn't exist
        if (permissionRepository.existsByName(name)) {
            log.warn("Permission already exists: {}", name);
            throw new IllegalArgumentException("Permission name already exists: " + name);
        }

        // Validate action code exists and is active
        PermissionActionType action = actionService.getActionByCode(actionCode)
            .orElseThrow(() -> {
                log.warn("Invalid action code: {}", actionCode);
                return new IllegalArgumentException("Action code not found: " + actionCode);
            });

        if (!action.getActive()) {
            log.warn("Cannot create permission with inactive action: {}", actionCode);
            throw new IllegalArgumentException("Action type is inactive: " + actionCode);
        }

        // Create permission
        Permission permission = Permission.builder()
            .name(name)
            .description(description)
            .category(category)
            .actionType(action)
            .resource(resource)
            .active(true)
            .build();

        Permission saved = permissionRepository.save(permission);
        log.info("Created permission: name={}, action={}, resource={}", name, actionCode, resource);
        return saved;
    }

    /**
     * Get all permissions for a specific resource and action combination
     *
     * @param resource resource type (e.g., "Booking")
     * @param actionCode action code (e.g., "create")
     * @return list of matching active permissions
     */
    @Transactional(readOnly = true)
    public List<Permission> getPermissionsForResourceAndAction(String resource, String actionCode) {
        return permissionRepository.findAll().stream()
            .filter(p -> p.getResource().equalsIgnoreCase(resource) &&
                        p.getActionType() != null &&
                        p.getActionType().getCode().equalsIgnoreCase(actionCode) &&
                        p.getActive())
            .collect(Collectors.toList());
    }

    /**
     * Get all active permissions for a category
     *
     * @param category category name (e.g., "Booking")
     * @return list of active permissions in category
     */
    @Transactional(readOnly = true)
    public List<Permission> getPermissionsByCategory(String category) {
        return permissionRepository.findByCategory(category).stream()
            .filter(Permission::getActive)
            .collect(Collectors.toList());
    }

    /**
     * Get all active permissions for a resource
     *
     * @param resource resource type
     * @return list of active permissions for resource
     */
    @Transactional(readOnly = true)
    public List<Permission> getPermissionsByResource(String resource) {
        return permissionRepository.findByResource(resource).stream()
            .filter(Permission::getActive)
            .collect(Collectors.toList());
    }

    /**
     * Deactivate a permission (soft delete)
     * Inactive permissions can be reactivated later
     *
     * @param id permission ID to deactivate
     * @return the deactivated Permission
     */
    @Transactional
    @AuditLogAnnotation(action = "DEACTIVATE_PERMISSION", entityType = "Permission", entityIdParamName = "id", description = "Deactivate a permission")
    public Permission deactivatePermission(Long id) {
        Permission permission = permissionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Permission not found with ID: " + id));

        permission.setActive(false);
        Permission updated = permissionRepository.save(permission);
        log.info("Deactivated permission: name={}", permission.getName());
        return updated;
    }

    /**
     * Reactivate a permission
     *
     * @param id permission ID to reactivate
     * @return the reactivated Permission
     */
    @Transactional
    @AuditLogAnnotation(action = "REACTIVATE_PERMISSION", entityType = "Permission", entityIdParamName = "id", description = "Reactivate a permission")
    public Permission reactivatePermission(Long id) {
        Permission permission = permissionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Permission not found with ID: " + id));

        permission.setActive(true);
        Permission updated = permissionRepository.save(permission);
        log.info("Reactivated permission: name={}", permission.getName());
        return updated;
    }

    /**
     * Check if a user has a specific permission
     * This is a convenience method - actual checking is done in User.hasPermission()
     *
     * @param permissionName permission name to check
     * @return true if permission exists and is active
     */
    public boolean permissionExists(String permissionName) {
        return permissionRepository.findByName(permissionName)
            .map(Permission::getActive)
            .orElse(false);
    }

    /**
     * Get total count of active permissions
     *
     * @return count of active permissions
     */
    public long countActivePermissions() {
        return permissionRepository.findByActiveTrue().size();
    }

    /**
     * Get all permissions for an action type
     *
     * @param actionCode action code
     * @return list of permissions using this action
     */
    @Transactional(readOnly = true)
    public List<Permission> getPermissionsByActionCode(String actionCode) {
        return permissionRepository.findAll().stream()
            .filter(p -> p.getActionType() != null &&
                        p.getActionType().getCode().equalsIgnoreCase(actionCode) &&
                        p.getActive())
            .collect(Collectors.toList());
    }
}
