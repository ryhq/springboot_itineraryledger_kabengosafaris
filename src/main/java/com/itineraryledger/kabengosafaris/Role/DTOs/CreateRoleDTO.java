package com.itineraryledger.kabengosafaris.Role.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * CreateRoleDTO - Request DTO for creating new roles
 *
 * Validates all required fields for role creation:
 * - Role identification (name, displayName)
 * - Role configuration (description, active status)
 * - System role flag
 * - Associated permissions
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRoleDTO {

    /**
     * Role name - unique identifier for the role
     * Examples: "booking_manager", "finance_officer"
     * Should be lowercase with underscores
     */
    private String name;

    /**
     * Human-readable role display name
     * Examples: "Booking Manager", "Finance Officer"
     */
    @NotBlank(message = "Display name is required")
    private String displayName;

    /**
     * Detailed description of what this role is for
     */
    private String description;

    /**
     * Whether this role is active/enabled
     * Default: true
     */
    @NotNull(message = "Active status is required")
    private Boolean active;

}
