package com.itineraryledger.kabengosafaris.Role.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for returning user information with role assignment status.
 *
 * Used by GET /api/roles/{roleId}/users to return all users
 * with a boolean indicating if they are assigned to the role.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleUserDTO {

    /**
     * The obfuscated user ID.
     */
    private String id;

    /**
     * User's username.
     */
    private String username;

    /**
     * User's email address.
     */
    private String email;

    /**
     * User's first name.
     */
    private String firstName;

    /**
     * User's last name.
     */
    private String lastName;

    /**
     * User's full name (firstName + lastName).
     */
    private String fullName;

    /**
     * Whether the user account is enabled.
     */
    private Boolean enabled;

    /**
     * Whether the user is assigned to this role.
     */
    private Boolean assigned;
}
