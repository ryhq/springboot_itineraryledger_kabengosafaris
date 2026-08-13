package com.itineraryledger.kabengosafaris.Permission.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Permission.PermissionAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * PermissionDTO - Data Transfer Object for Permission
 *
 * Used for transferring permission information to clients
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PermissionDTO {

    /**
     * Permission ID (obfuscated)
     */
    private String id;

    /**
     * Permission name - unique identifier
     * Format: {ACTION}_{ENTITY}
     * Examples: "CREATE_USER", "READ_ROLE", "UPDATE_EMAIL_ACCOUNT"
     */
    private String name;

    /**
     * Human-readable description
     */
    private String description;

    /**
     * Permission action (CREATE, READ, UPDATE, DELETE, etc.)
     */
    private PermissionAction action;

    /**
     * Permission action display name
     */
    private String actionDisplayName;

    /**
     * Entity this permission applies to
     * Examples: "USER", "ROLE", "EMAIL_ACCOUNT"
     */
    private String entity;

    /**
     * Whether this permission is active
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

    /**
     * How many roles grant this permission.
     *
     * Zero is the finding: nobody in the company can do the thing, and the endpoint
     * behind it refuses every person who tries.
     */
    private Integer roleCount;
}
