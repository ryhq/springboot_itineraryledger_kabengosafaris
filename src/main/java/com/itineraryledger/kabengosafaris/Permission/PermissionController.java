package com.itineraryledger.kabengosafaris.Permission;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.itineraryledger.kabengosafaris.Permission.Specifications.PermissionFilter;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Response.BulkFlags;
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

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private BulkFlags bulkFlags;

    /**
     * The catalogue: the rows, the counters and the sort, in one response.
     *
     * The filter arrives as a @ModelAttribute so every dimension is a query param and
     * the list's whole state lives in the URL. The older discrete params (name, entity,
     * action, active) still bind, so existing callers are unaffected.
     *
     * Example: GET /api/permissions?entities=USER,ROLE&actions=DELETE&qualities=noRoles
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_PERMISSION')")
    public ResponseEntity<?> getAllPermissions(
        @ModelAttribute PermissionFilter filter,
        @RequestParam(required = false) Boolean includeStats,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection
    ) {
        log.info("GET /api/permissions - Fetching permissions with filters");
        return permissionService.getAllPermissions(filter, includeStats, page, size, sortBy, sortDirection);
    }

    /**
     * The distinct entities in the catalogue, in one call.
     *
     * The filter dropdown needs all hundred and five of them, and paging through the
     * list to collect them would only ever offer the twenty on the current page — a
     * filter that cannot see most of its own options.
     */
    @GetMapping("/entities")
    @PreAuthorize("hasAuthority('PERM_READ_PERMISSION')")
    public ResponseEntity<ApiResponse<?>> getEntities() {
        return permissionService.getDistinctEntities();
    }

    /**
     * Activating or deactivating a selection in one request.
     *
     * Worth knowing what this does: an inactive permission is dropped from the
     * authorities of every role that grants it, immediately and everywhere. It is a
     * system-wide switch, not a per-role one — to take a capability away from one role,
     * edit that role.
     */
    @PatchMapping("/bulk")
    @PreAuthorize("hasAuthority('PERM_UPDATE_PERMISSION')")
    public ResponseEntity<?> bulkUpdate(@RequestBody BulkFlags.Request request) {
        return bulkFlags.apply("permission", permissionRepository, request, permission -> {
            if (request.getIsActive() != null) permission.setActive(request.getIsActive());
        });
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
