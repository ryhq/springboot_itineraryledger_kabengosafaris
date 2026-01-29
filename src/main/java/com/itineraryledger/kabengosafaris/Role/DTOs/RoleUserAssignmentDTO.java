package com.itineraryledger.kabengosafaris.Role.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for assigning or removing a user from a role.
 *
 * Use assign=true to assign the user to the role.
 * Use assign=false to remove the user from the role.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleUserAssignmentDTO {

    /**
     * The obfuscated user ID.
     */
    @NotBlank(message = "User ID is required")
    private String userId;

    /**
     * The obfuscated role ID.
     */
    @NotBlank(message = "Role ID is required")
    private String roleId;

    /**
     * Whether to assign (true) or remove (false) the user from the role.
     */
    @NotNull(message = "Assign flag is required")
    private Boolean assign;
}
