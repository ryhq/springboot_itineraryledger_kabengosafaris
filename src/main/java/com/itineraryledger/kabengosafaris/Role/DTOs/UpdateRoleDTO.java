package com.itineraryledger.kabengosafaris.Role.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UpdateRoleDTO - Request DTO for updating existing roles
 *
 * All fields are optional for partial updates:
 * - Role identification (name, displayName)
 * - Role configuration (description, active status)
 * - Only provided fields will be updated
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateRoleDTO {

    /**
     * Role name - unique identifier for the role
     * Examples: "booking_manager", "finance_officer"
     * Should be lowercase with underscores
     * Optional: Only update if provided
     */
    private String name;

    /**
     * Human-readable role display name
     * Examples: "Booking Manager", "Finance Officer"
     * Optional: Only update if provided
     */
    private String displayName;

    /**
     * Detailed description of what this role is for
     * Optional: Only update if provided
     */
    private String description;

    /**
     * Whether this role is active/enabled
     * Optional: Only update if provided
     */
    private Boolean active;
}
