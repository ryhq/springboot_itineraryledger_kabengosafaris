package com.itineraryledger.kabengosafaris.Role.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Permission.PermissionAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * RolePermissionItemDTO - Represents a single permission with role assignment status
 *
 * Used in role permission management UI to show:
 * - What the permission is (name, description, action)
 * - Whether this permission is assigned to the selected role
 * - Whether the permission is active in the system
 *
 * Example:
 * {
 *   "id": "x1",
 *   "name": "CREATE_USER",
 *   "description": "Allows creating new users in the system",
 *   "action": "CREATE",
 *   "entity": "USER",
 *   "assigned": true,  // This role HAS this permission
 *   "active": true,
 *   "createdAt": "2025-01-15T10:00:00",
 *   "updatedAt": "2025-01-15T10:00:00"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RolePermissionItemDTO {

    /**
     * Permission ID (obfuscated)
     */
    private String id;

    /**
     * Permission name (e.g., "CREATE_USER", "READ_ROLE")
     */
    private String name;

    /**
     * Human-readable description of what this permission allows
     */
    private String description;

    /**
     * Permission action (CREATE, READ, UPDATE, DELETE, etc.)
     */
    private PermissionAction action;

    /**
     * Action display name for UI (e.g., "Create", "Read", "Update")
     */
    private String actionDisplayName;

    /**
     * Entity this permission applies to (e.g., "USER", "ROLE")
     */
    private String entity;

    /**
     * Whether this permission is currently ASSIGNED to the selected role
     * - true: Role HAS this permission
     * - false: Role DOES NOT have this permission
     */
    private Boolean assigned;

    /**
     * Whether this permission is active in the system
     * Inactive permissions cannot be assigned
     */
    private Boolean active;

    /**
     * Timestamp when this permission was created
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when this permission was last updated
     */
    private LocalDateTime updatedAt;
}
