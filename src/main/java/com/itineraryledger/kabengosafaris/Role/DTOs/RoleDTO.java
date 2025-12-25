package com.itineraryledger.kabengosafaris.Role.DTOs;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RoleDTO - Data Transfer Object for Roles
 *
 * Transfers role information to clients with:
 * - Role identification and configuration
 * - Associated permissions
 * - Audit trail information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoleDTO {

    /**
     * Role ID
     */
    private String id;

    /**
     * Role name - unique identifier for the role
     * Examples: "booking_manager", "finance_officer"
     */
    private String name;

    /**
     * Human-readable role display name
     * Examples: "Booking Manager", "Finance Officer"
     */
    private String displayName;

    /**
     * Detailed description of what this role is for
     */
    private String description;

    /**
     * Whether this role is active/enabled
     */
    private Boolean active;

    /**
     * System roles cannot be deleted (like ADMIN, USER)
     * Custom roles can be deleted
     */
    private Boolean isSystemRole;

    /**
     * Timestamp when this role was created
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when this role was last updated
     */
    private LocalDateTime updatedAt;

}
