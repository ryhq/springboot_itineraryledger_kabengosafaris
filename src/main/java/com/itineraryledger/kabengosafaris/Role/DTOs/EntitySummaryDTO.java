package com.itineraryledger.kabengosafaris.Role.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * EntitySummaryDTO - Summary of an entity with permission counts
 *
 * Example response for GET /api/roles/{roleId}/entities:
 * [
 *   {
 *     "entity": "USER",
 *     "entityDisplayName": "User Management",
 *     "totalPermissions": 4,
 *     "assignedPermissions": 2,
 *     "unassignedPermissions": 2
 *   },
 *   {
 *     "entity": "ROLE",
 *     "entityDisplayName": "Role Management",
 *     "totalPermissions": 4,
 *     "assignedPermissions": 1,
 *     "unassignedPermissions": 3
 *   }
 * ]
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EntitySummaryDTO {

    /**
     * Entity name (e.g., "USER", "ROLE", "EMAIL_ACCOUNT")
     */
    private String entity;

    /**
     * Display name for the entity (e.g., "User Management", "Role Management")
     */
    private String entityDisplayName;

    /**
     * Total number of permissions available for this entity
     */
    private Integer totalPermissions;

    /**
     * Number of permissions assigned to the role for this entity
     */
    private Integer assignedPermissions;

    /**
     * Number of permissions not assigned to the role for this entity
     */
    private Integer unassignedPermissions;

    /**
     * Percentage of permissions assigned (0-100)
     */
    private Double assignmentPercentage;
}
