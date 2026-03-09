package com.itineraryledger.kabengosafaris.Permission;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Role.Services.RoleGetService;

import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for Permission Management
 *
 * Provides endpoints for:
 * - Retrieving permissions with filtering, pagination, and sorting
 * - Getting a single permission by ID
 * - Toggling permission active status
 *
 * All endpoints require appropriate permissions to access.
 */
@RestController
@RequestMapping("/api/permissions")
@Slf4j
public class PermissionController {

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private RoleGetService roleGetService;

    /**
     * Get all permissions with optional filtering, pagination, and sorting
     *
     * @param page Page number (0-based), default: 0
     * @param size Page size, default: 10
     * @param name Filter by permission name (partial match, case-insensitive)
     * @param entity Filter by entity (partial match, case-insensitive)
     * @param action Filter by permission action (CREATE, READ, UPDATE, DELETE, etc.)
     * @param active Filter by active status (true/false)
     * @param sortDirection Sort direction: "asc" or "desc", default: "desc"
     * @return ResponseEntity with paginated permissions
     *
     * Example: GET /api/permissions?page=0&size=10&entity=USER&active=true&sortDirection=asc
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_PERMISSION')")
    public ResponseEntity<?> getAllPermissions(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String entity,
        @RequestParam(required = false) PermissionAction action,
        @RequestParam(required = false) Boolean active,
        @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/permissions - Fetching permissions with filters");
        return permissionService.getAllPermissions(page, size, name, entity, action, active, sortDirection);
    }

    /**
     * Get a single permission by ID
     *
     * @param id Obfuscated permission ID
     * @return ResponseEntity with permission details or error
     *
     * Example: GET /api/permissions/abc123def456
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_PERMISSION')")
    public ResponseEntity<ApiResponse<?>> getPermission(@PathVariable String id) {
        log.info("GET /api/permissions/{} - Fetching permission", id);
        return permissionService.getPermission(id);
    }

    /**
     * Get roles that have access to a specific permission with optional filtering and pagination
     *
     * @param id Obfuscated permission ID
     * @param page Page number (0-based), default: 0
     * @param size Page size, default: 10
     * @param name Filter by role name partial match (optional)
     * @param displayName Filter by display name partial match (optional)
     * @param active Filter by active status (optional)
     * @param isSystemRole Filter by system role status (optional)
     * @param sortDirection Sort direction: "asc" or "desc", default: "desc"
     * @return ResponseEntity with paginated roles that have this permission
     *
     * Example: GET /api/permissions/abc123def456/roles?page=0&size=10&active=true&sortDirection=desc
     */
    @GetMapping("/{id}/roles")
    // Require both permission read and role read authorities
    @PreAuthorize("hasAuthority('PERM_READ_PERMISSION') and hasAuthority('PERM_READ_ROLE')")
    public ResponseEntity<?> getPermissionRoles(
        @PathVariable String id,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String displayName,
        @RequestParam(required = false) Boolean active,
        @RequestParam(required = false) Boolean isSystemRole,
        @RequestParam(required = false) String sortBy,
        @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/permissions/{}/roles - Fetching roles for permission", id);
        return roleGetService.getRolesForPermission(id, page, size, name, displayName, active, isSystemRole, sortBy, sortDirection);
    }

    /**
     * Toggle permission active status
     *
     * Switches the permission between active (true) and inactive (false).
     * When a permission is inactive, users with that permission will not be able to access
     * protected endpoints that require it.
     *
     * @param id Obfuscated permission ID
     * @return ResponseEntity with updated permission or error
     *
     * Example: PATCH /api/permissions/abc123def456/toggle-active
     */
    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasAuthority('PERM_UPDATE_PERMISSION')")
    public ResponseEntity<ApiResponse<?>> togglePermissionActiveStatus(@PathVariable String id) {
        log.info("PATCH /api/permissions/{}/toggle-active - Toggling permission active status", id);
        return permissionService.togglePermissionActiveStatus(id);
    }
}
