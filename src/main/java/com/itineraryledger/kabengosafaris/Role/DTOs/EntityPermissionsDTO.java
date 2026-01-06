package com.itineraryledger.kabengosafaris.Role.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * EntityPermissionsDTO - Shows all permissions for an entity with role assignment status
 *
 * This DTO is designed for a role permission management UI where:
 * 1. User selects a Role
 * 2. System loads all Entities (USER, ROLE, EMAIL_ACCOUNT, etc.)
 * 3. User selects an Entity
 * 4. System shows all permissions for that entity, marked as assigned/not assigned to the role
 *
 * Example structure:
 * {
 *   "entity": "USER",
 *   "entityDisplayName": "User Management",
 *   "permissions": [
 *     { "id": "x1", "name": "CREATE_USER", "description": "...", "action": "CREATE", "assigned": true },
 *     { "id": "x2", "name": "READ_USER", "description": "...", "action": "READ", "assigned": true },
 *     { "id": "x3", "name": "UPDATE_USER", "description": "...", "action": "UPDATE", "assigned": false },
 *     { "id": "x4", "name": "DELETE_USER", "description": "...", "action": "DELETE", "assigned": false }
 *   ],
 *   "totalPermissions": 4,
 *   "assignedPermissions": 2
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EntityPermissionsDTO {

    /**
     * Entity name (e.g., "USER", "ROLE", "EMAIL_ACCOUNT")
     */
    private String entity;

    /**
     * Display name for the entity (e.g., "User Management", "Role Management")
     */
    private String entityDisplayName;

    /**
     * All permissions for this entity with assignment status
     */
    private List<RolePermissionItemDTO> permissions;

    /**
     * Total number of permissions available for this entity
     */
    private Integer totalPermissions;

    /**
     * Number of permissions assigned to the role
     */
    private Integer assignedPermissions;

    /**
     * Number of permissions not assigned to the role
     */
    private Integer unassignedPermissions;
}
