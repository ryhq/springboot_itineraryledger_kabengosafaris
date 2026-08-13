package com.itineraryledger.kabengosafaris.Role;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Response.BulkFlags;
import com.itineraryledger.kabengosafaris.Role.DTOs.CreateRoleDTO;
import com.itineraryledger.kabengosafaris.Role.DTOs.RoleUserAssignmentDTO;
import com.itineraryledger.kabengosafaris.Role.DTOs.UpdateRoleDTO;
import com.itineraryledger.kabengosafaris.Role.DTOs.UpdateRolePermissionsDTO;
import com.itineraryledger.kabengosafaris.Role.Services.CreateRoleService;
import com.itineraryledger.kabengosafaris.Role.Services.RoleDeleteService;
import com.itineraryledger.kabengosafaris.Role.Services.RoleGetService;
import com.itineraryledger.kabengosafaris.Role.Services.RoleListService;
import com.itineraryledger.kabengosafaris.Role.Services.RolePermissionService;
import com.itineraryledger.kabengosafaris.Role.Services.RoleUpdateService;
import com.itineraryledger.kabengosafaris.Role.Services.RoleUserAssignmentService;
import com.itineraryledger.kabengosafaris.Role.Specifications.RoleFilter;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * REST Controller for Role management endpoints
 *
 * Provides endpoints for:
 * - Creating new roles with validation
 * - Retrieving roles with pagination and filtering
 * - Updating roles with partial updates
 * - Deleting roles (with system role protection)
 * - Searching roles by various criteria
 */
@RestController
@RequestMapping("/api/roles")
public class RoleController {

    @Autowired
    private RoleGetService roleGetService;

    @Autowired
    private CreateRoleService createRoleService;

    @Autowired
    private RoleUpdateService roleUpdateService;

    @Autowired
    private RoleDeleteService roleDeleteService;

    @Autowired
    private RolePermissionService rolePermissionService;

    @Autowired
    private RoleUserAssignmentService roleUserAssignmentService;

    @Autowired
    private RoleListService roleListService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private BulkFlags bulkFlags;

    /**
     * Create a new role
     *
     * @param createDTO The request DTO with role details
     * @return ResponseEntity with ApiResponse containing created role details
     *
     * Example request:
     * POST /api/roles
     * {
     *   "displayName": "Booking Manager",
     *   "description": "Manages all booking operations",
     *   "active": true
     * }
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_ROLE')")
    public ResponseEntity<ApiResponse<?>> createRole(
        @Valid
        @RequestBody CreateRoleDTO createDTO
    ) {
        return createRoleService.createRole(createDTO);
    }

    /**
     * Update an existing role with partial updates
     *
     * @param id The obfuscated role ID
     * @param updateDTO The request DTO with fields to update (only provided fields will be updated)
     * @return ResponseEntity with ApiResponse containing updated role details
     *
     * Example request:
     * PUT /api/roles/{id}
     * {
     *   "displayName": "Senior Booking Manager",
     *   "description": "Updated description",
     *   "active": true
     * }
     *
     * Note: All fields are optional. Only provided fields will be updated.
     * System roles cannot be updated.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ROLE')")
    public ResponseEntity<ApiResponse<?>> updateRole(
        @PathVariable String id,
        @Valid @RequestBody UpdateRoleDTO updateDTO
    ) {
        return roleUpdateService.updateRole(id, updateDTO);
    }

    /**
     * Get all roles: the rows, the counters and the sort, in one response.
     *
     * The filter arrives as a @ModelAttribute so every dimension is a query param and
     * the list's whole state lives in the URL. The older discrete params (name,
     * displayName, active, isSystemRole) still bind, so existing callers are unaffected.
     *
     * Example: GET /api/roles?statuses=active&kinds=custom&qualities=noUsers
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_ROLE')")
    public ResponseEntity<?> getAllRoles(
        @ModelAttribute RoleFilter filter,
        @RequestParam(required = false) Boolean includeStats,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection
    ) {
        return roleListService.list(filter, includeStats, page, size, sortBy, sortDirection);
    }

    /**
     * Get a single role, plus where it sits in the caller's filtered set.
     *
     * The filter comes along so prev/next walks the same list the user was looking at
     * rather than every role in id order.
     *
     * Example: GET /api/roles/{id}?kinds=custom
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_ROLE')")
    public ResponseEntity<ApiResponse<?>> getRole(
        @PathVariable String id,
        @ModelAttribute RoleFilter filter,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection
    ) {
        return roleListService.getOne(id, filter, sortBy, sortDirection);
    }

    /** Whether this role can be deleted, and what is stopping it. */
    @GetMapping("/{id}/deletable")
    @PreAuthorize("hasAuthority('PERM_DELETE_ROLE')")
    public ResponseEntity<ApiResponse<?>> deletable(@PathVariable String id) {
        return roleDeleteService.checkDeletable(id);
    }

    /**
     * Activating or deactivating a selection in one request.
     *
     * An inactive role grants nothing — the authorities of everybody holding it drop
     * the permissions immediately — so this is the switch that suspends access without
     * unpicking assignments.
     */
    @PatchMapping("/bulk")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ROLE')")
    public ResponseEntity<?> bulkUpdate(@RequestBody BulkFlags.Request request) {
        return bulkFlags.apply("role", roleRepository, request, role -> {
            if (request.getIsActive() != null) role.setActive(request.getIsActive());
        });
    }

    /**
     * Delete a single role by obfuscated ID
     *
     * @param id The obfuscated role ID
     * @return ResponseEntity with ApiResponse containing success message
     *
     * Validation:
     * - Cannot delete system roles
     * - Returns 400 error if role is a system role
     * - Returns 404 if role not found
     *
     * Response format:
     * {
     *   "success": true,
     *   "statusCode": 200,
     *   "message": "1 role(s) deleted successfully",
     *   "data": null
     * }
     *
     * Example request:
     * DELETE /api/roles/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DELETE_ROLE')")
    public ResponseEntity<ApiResponse<?>> deleteRole(@PathVariable String id) {
        List<String> idList = new ArrayList<>();
        idList.add(id);
        return roleDeleteService.deleteRoles(idList);
    }

    /**
     * Delete multiple roles by batch request
     *
     * @param idList List of obfuscated role IDs to delete
     * @return ResponseEntity with ApiResponse containing success message
     *
     * Validation:
     * - If ANY role in the list is a system role, NO roles will be deleted
     * - Returns 400 error if any role is a system role
     *
     * Response format (success):
     * {
     *   "success": true,
     *   "statusCode": 200,
     *   "message": "2 role(s) deleted successfully",
     *   "data": null
     * }
     *
     * Response format (error - has system roles):
     * {
     *   "success": false,
     *   "statusCode": 400,
     *   "message": "Cannot delete system roles. System roles cannot be deleted.",
     *   "errorCode": "CANNOT_DELETE_SYSTEM_ROLES"
     * }
     *
     * Example request:
     * DELETE /api/roles
     * [
     *   "encoded_id_1",
     *   "encoded_id_2",
     *   "encoded_id_3"
     * ]
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_ROLE')")
    public ResponseEntity<ApiResponse<?>> deleteRolesBatch(@RequestBody List<String> idList) {
        return roleDeleteService.deleteRoles(idList);
    }

    /**
     * Get all entities with permission summary for a role
     *
     * Returns a list of all entities (USER, ROLE, EMAIL_ACCOUNT, etc.) with:
     * - Total permissions for each entity
     * - How many permissions are assigned to the role
     * - How many permissions are not assigned
     * - Assignment percentage
     *
     * @param roleId The obfuscated role ID
     * @return ResponseEntity with list of entities and their permission counts
     *
     * Example request:
     * GET /api/roles/{roleId}/entities
     *
     * Example response:
     * {
     *   "success": true,
     *   "statusCode": 200,
     *   "message": "Entities retrieved successfully",
     *   "data": [
     *     {
     *       "entity": "USER",
     *       "entityDisplayName": "User",
     *       "totalPermissions": 4,
     *       "assignedPermissions": 2,
     *       "unassignedPermissions": 2,
     *       "assignmentPercentage": 50.0
     *     },
     *     {
     *       "entity": "ROLE",
     *       "entityDisplayName": "Role",
     *       "totalPermissions": 4,
     *       "assignedPermissions": 1,
     *       "unassignedPermissions": 3,
     *       "assignmentPercentage": 25.0
     *     }
     *   ]
     * }
     */
    @GetMapping("/{roleId}/entities")
    @PreAuthorize("hasAuthority('PERM_READ_ROLE')")
    public ResponseEntity<?> getEntitiesForRole(@PathVariable String roleId) {
        return rolePermissionService.getEntitiesForRole(roleId);
    }

    /**
     * Get detailed permissions for a specific entity with assignment status
     *
     * Returns all permissions for the specified entity, showing which ones
     * are assigned to the role and which ones are not.
     *
     * @param roleId The obfuscated role ID
     * @param entity The entity name (e.g., "USER", "ROLE", "EMAIL_ACCOUNT")
     * @return ResponseEntity with detailed permissions for the entity
     *
     * Example request:
     * GET /api/roles/{roleId}/entities/USER/permissions
     *
     * Example response:
     * {
     *   "success": true,
     *   "statusCode": 200,
     *   "message": "Entity permissions retrieved successfully",
     *   "data": {
     *     "entity": "USER",
     *     "entityDisplayName": "User",
     *     "permissions": [
     *       {
     *         "id": "abc123",
     *         "name": "CREATE_USER",
     *         "description": "Allows creating new users",
     *         "action": "CREATE",
     *         "actionDisplayName": "Create",
     *         "entity": "USER",
     *         "assigned": true,
     *         "active": true
     *       },
     *       {
     *         "id": "def456",
     *         "name": "READ_USER",
     *         "description": "Allows viewing user details",
     *         "action": "READ",
     *         "actionDisplayName": "Read",
     *         "entity": "USER",
     *         "assigned": true,
     *         "active": true
     *       },
     *       {
     *         "id": "ghi789",
     *         "name": "UPDATE_USER",
     *         "description": "Allows updating user information",
     *         "action": "UPDATE",
     *         "actionDisplayName": "Update",
     *         "entity": "USER",
     *         "assigned": false,
     *         "active": true
     *       },
     *       {
     *         "id": "jkl012",
     *         "name": "DELETE_USER",
     *         "description": "Allows deleting users",
     *         "action": "DELETE",
     *         "actionDisplayName": "Delete",
     *         "entity": "USER",
     *         "assigned": false,
     *         "active": true
     *       }
     *     ],
     *     "totalPermissions": 4,
     *     "assignedPermissions": 2,
     *     "unassignedPermissions": 2
     *   }
     * }
     */
    @GetMapping("/{roleId}/entities/{entity}/permissions")
    @PreAuthorize("hasAuthority('PERM_READ_ROLE')")
    public ResponseEntity<?> getEntityPermissions(
        @PathVariable String roleId,
        @PathVariable String entity
    ) {
        return rolePermissionService.getEntityPermissions(roleId, entity);
    }

    /**
     * Update (upsert) role permissions for a specific entity
     *
     * This endpoint replaces all permissions for the given entity:
     * - Permissions in the request will be ASSIGNED to the role
     * - Permissions NOT in the request will be REMOVED from the role
     *
     * This is a "replace" operation, not an "add" operation.
     *
     * @param roleId The obfuscated role ID
     * @param entity The entity name (e.g., "USER", "ROLE", "EMAIL_ACCOUNT")
     * @param updateDTO The DTO containing permission IDs to assign
     * @return ResponseEntity with updated entity permissions
     *
     * Example request:
     * POST /api/roles/{roleId}/entities/USER/permissions
     * {
     *   "permissionIds": ["perm123", "perm456"]
     * }
     *
     * This example will:
     * - Assign CREATE_USER (perm123) and READ_USER (perm456) to the role
     * - Remove UPDATE_USER and DELETE_USER from the role (if they were assigned)
     *
     * To remove all permissions for an entity, send an empty array:
     * {
     *   "permissionIds": []
     * }
     *
     * Example success response:
     * {
     *   "success": true,
     *   "statusCode": 200,
     *   "message": "Entity permissions retrieved successfully",
     *   "data": {
     *     "entity": "USER",
     *     "entityDisplayName": "User",
     *     "permissions": [
     *       {
     *         "id": "perm123",
     *         "name": "CREATE_USER",
     *         "assigned": true,
     *         ...
     *       },
     *       {
     *         "id": "perm456",
     *         "name": "READ_USER",
     *         "assigned": true,
     *         ...
     *       },
     *       {
     *         "id": "perm789",
     *         "name": "UPDATE_USER",
     *         "assigned": false,
     *         ...
     *       },
     *       {
     *         "id": "perm012",
     *         "name": "DELETE_USER",
     *         "assigned": false,
     *         ...
     *       }
     *     ],
     *     "totalPermissions": 4,
     *     "assignedPermissions": 2,
     *     "unassignedPermissions": 2
     *   }
     * }
     */
    @PostMapping("/{roleId}/entities/{entity}/permissions")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ROLE')")
    public ResponseEntity<?> updateRolePermissions(
        @PathVariable String roleId,
        @PathVariable String entity,
        @Valid @RequestBody UpdateRolePermissionsDTO updateDTO
    ) {
        return rolePermissionService.updateRolePermissions(roleId, entity, updateDTO);
    }

    /**
     * Reset role permissions for a specific entity to system defaults
     *
     * This endpoint resets permissions for a specific role and entity back to system defaults:
     * - SUPERADMIN: CREATE, READ, UPDATE, DELETE
     * - ADMIN: CREATE, READ, UPDATE (no DELETE)
     * - USER: CREATE, READ (no UPDATE, no DELETE)
     * - GUEST: READ only
     *
     * Only works for system roles (SUPERADMIN, ADMIN, USER, GUEST).
     * Custom roles cannot be reset.
     *
     * @param roleId The obfuscated role ID
     * @param entity The entity name (e.g., "USER", "ROLE", "EMAIL_ACCOUNT")
     * @return ResponseEntity with updated entity permissions (same format as getEntityPermissions)
     *
     * Example request:
     * POST /api/roles/{roleId}/entities/USER/permissions/reset
     *
     * Example success response:
     * {
     *   "success": true,
     *   "statusCode": 200,
     *   "message": "Entity permissions retrieved successfully",
     *   "data": {
     *     "entity": "USER",
     *     "entityDisplayName": "User",
     *     "permissions": [
     *       {
     *         "id": "abc123",
     *         "name": "CREATE_USER",
     *         "assigned": true,
     *         ...
     *       },
     *       {
     *         "id": "def456",
     *         "name": "READ_USER",
     *         "assigned": true,
     *         ...
     *       },
     *       {
     *         "id": "ghi789",
     *         "name": "UPDATE_USER",
     *         "assigned": false,
     *         ...
     *       },
     *       {
     *         "id": "jkl012",
     *         "name": "DELETE_USER",
     *         "assigned": false,
     *         ...
     *       }
     *     ],
     *     "totalPermissions": 4,
     *     "assignedPermissions": 2,
     *     "unassignedPermissions": 2
     *   }
     * }
     *
     * Error responses:
     * - 400 if role ID format is invalid
     * - 404 if role not found
     * - 400 if role is not a system role
     * - 404 if entity has no permissions
     */
    @PostMapping("/{roleId}/entities/{entity}/permissions/reset")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ROLE')")
    public ResponseEntity<?> resetRoleEntityPermissions(
        @PathVariable String roleId,
        @PathVariable String entity
    ) {
        return rolePermissionService.resetRoleEntityPermissionsToDefaults(roleId, entity);
    }

    // ==================== User Assignment Endpoints ====================

    /**
     * Get all users with their assignment status for a specific role.
     *
     * Returns a paginated list of all users, each with a boolean 'assigned' field
     * indicating whether the user is assigned to this role.
     *
     * @param roleId The obfuscated role ID
     * @param page Page number (0-based), default: 0
     * @param size Page size, default: 10
     * @param search Optional search keyword (searches username, email, firstName, lastName)
     * @param sortDirection Sort direction: "asc" or "desc", default: "asc"
     * @return ResponseEntity with paginated users and their assignment status
     *
     * Example request:
     * GET /api/roles/{roleId}/users?page=0&size=20&search=john
     *
     * Example response:
     * {
     *   "success": true,
     *   "statusCode": 200,
     *   "message": "Users retrieved successfully",
     *   "data": {
     *     "role": {
     *       "id": "abc123",
     *       "name": "ADMIN",
     *       "displayName": "Administrator"
     *     },
     *     "users": [
     *       {
     *         "id": "user123",
     *         "username": "john.doe",
     *         "email": "john@example.com",
     *         "firstName": "John",
     *         "lastName": "Doe",
     *         "fullName": "John Doe",
     *         "enabled": true,
     *         "assigned": true
     *       },
     *       {
     *         "id": "user456",
     *         "username": "jane.smith",
     *         "email": "jane@example.com",
     *         "firstName": "Jane",
     *         "lastName": "Smith",
     *         "fullName": "Jane Smith",
     *         "enabled": true,
     *         "assigned": false
     *       }
     *     ],
     *     "currentPage": 0,
     *     "totalItems": 50,
     *     "totalPages": 3,
     *     "assignedCount": 5
     *   }
     * }
     */
    @GetMapping("/{roleId}/users")
    @PreAuthorize("hasAuthority('PERM_READ_ROLE')")
    public ResponseEntity<?> getUsersForRole(
        @PathVariable String roleId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String search,
        @RequestParam(defaultValue = "asc") String sortDirection
    ) {
        return roleUserAssignmentService.getUsersForRole(roleId, page, size, search, sortDirection);
    }

    /**
     * Batch assign or remove users from roles.
     *
     * Accepts a list of assignment requests. Each request specifies:
     * - userId: The obfuscated user ID
     * - roleId: The obfuscated role ID
     * - assign: true to assign, false to remove
     *
     * @param requests List of assignment requests
     * @return ResponseEntity with results for each operation
     *
     * Example request:
     * POST /api/roles/user-assignments
     * [
     *   { "userId": "user123", "roleId": "role456", "assign": true },
     *   { "userId": "user789", "roleId": "role456", "assign": false }
     * ]
     *
     * Example response:
     * {
     *   "success": true,
     *   "statusCode": 200,
     *   "message": "Processed 2 assignments: 2 succeeded, 0 failed",
     *   "data": {
     *     "results": [
     *       {
     *         "userId": "user123",
     *         "roleId": "role456",
     *         "requestedAction": "assign",
     *         "success": true,
     *         "message": "Role assigned successfully",
     *         "action": "assigned",
     *         "roleName": "ADMIN",
     *         "roleDisplayName": "Administrator",
     *         "userName": "john.doe",
     *         "userFullName": "John Doe"
     *       },
     *       {
     *         "userId": "user789",
     *         "roleId": "role456",
     *         "requestedAction": "remove",
     *         "success": true,
     *         "message": "Role removed successfully",
     *         "action": "removed",
     *         "roleName": "ADMIN",
     *         "roleDisplayName": "Administrator",
     *         "userName": "jane.smith",
     *         "userFullName": "Jane Smith"
     *       }
     *     ],
     *     "totalProcessed": 2,
     *     "successCount": 2,
     *     "failCount": 0
     *   }
     * }
     */
    @PostMapping("/user-assignments")
    @PreAuthorize("hasAuthority('PERM_UPDATE_USER')")
    public ResponseEntity<?> assignUserRoles(
        @Valid @RequestBody List<RoleUserAssignmentDTO> requests
    ) {
        return roleUserAssignmentService.assignUserRoles(requests);
    }
}
