package com.itineraryledger.kabengosafaris.User.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A role as it appears on a user: enough to name it and link to it, nothing more.
 *
 * Deliberately not RoleDTO. The user list carries these for every row, and a role's
 * description runs to a paragraph — sending a hundred of those to draw a column of
 * badges is bandwidth spent on text nobody sees.
 *
 * `active` is here because an inactive role grants nothing: the user's authorities
 * skip it entirely. A row showing "Finance Officer" with no hint that the role is
 * switched off would explain neither what they can do nor why they cannot do it.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserRoleRefDTO {
    private String id;
    private String name;
    private String displayName;
    private Boolean active;
    private Boolean isSystemRole;
}
