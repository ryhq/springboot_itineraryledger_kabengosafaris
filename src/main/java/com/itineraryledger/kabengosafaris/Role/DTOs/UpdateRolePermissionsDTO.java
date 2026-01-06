package com.itineraryledger.kabengosafaris.Role.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * UpdateRolePermissionsDTO - Request DTO for updating role permissions for a specific entity
 *
 * This DTO is used to assign/unassign permissions to a role for a specific entity.
 * The frontend sends a list of permission IDs that should be assigned to the role.
 *
 * Example request:
 * POST /api/roles/{roleId}/entities/USER/permissions
 * {
 *   "permissionIds": ["perm123", "perm456"]
 * }
 *
 * This will:
 * - Assign CREATE_USER (perm123) and READ_USER (perm456) to the role
 * - Remove UPDATE_USER and DELETE_USER from the role (if they were previously assigned)
 *
 * Basically, this is a "replace" operation for the entity:
 * - Any permission ID in the list will be ASSIGNED
 * - Any permission ID NOT in the list will be REMOVED
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateRolePermissionsDTO {

    /**
     * List of permission IDs (obfuscated) that should be assigned to the role for this entity
     *
     * Example: ["abc123", "def456"] means:
     * - Assign permission with ID abc123
     * - Assign permission with ID def456
     * - Remove all other permissions for this entity from the role
     *
     * Empty list [] means: Remove all permissions for this entity from the role
     */
    @NotNull(message = "Permission IDs list cannot be null")
    private List<String> permissionIds;
}
