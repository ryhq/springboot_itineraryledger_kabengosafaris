package com.itineraryledger.kabengosafaris.EndpointPermission;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.itineraryledger.kabengosafaris.Permission.PermissionAction;

/**
 * EndpointPermission Entity - Maps API endpoints to required permissions
 *
 * This entity enables fully database-driven URL-to-Permission mapping.
 * No @RequirePermission annotations needed on controllers.
 * All permission requirements are configured in the database.
 *
 * Examples:
 *   POST /api/users -> CREATE_USER or (PermissionAction.CREATE, "USER")
 *   GET /api/users/{id} -> READ_USER or (PermissionAction.READ, "USER")
 *   PUT /api/users/{id} -> UPDATE_USER or (PermissionAction.UPDATE, "USER")
 *   DELETE /api/users/{id} -> DELETE_USER or (PermissionAction.DELETE, "USER")
 *
 * Features:
 *   - Enable/disable endpoints without code changes
 *   - Change required permissions without code changes
 *   - Support public endpoints (requires_auth = false)
 *   - Soft deactivation (active flag)
 *   - Regex pattern matching for dynamic paths
 *   - Multiple permission models: name-based or action+entity based
 *
 * Permission Lookup Priority:
 *   1. Exact endpoint match (POST /api/users)
 *   2. Pattern match (POST /api/users/.*) using regex
 *   3. If not found, allowed by default (configurable via property)
 */
@Entity
@Table(name = "endpoint_permissions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"http_method", "endpoint"}, name = "uk_endpoint_permission")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EndpointPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * HTTP method: GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS
     */
    @Column(nullable = false, length = 10)
    private String httpMethod;

    /**
     * API endpoint path
     * Can be exact path: "/api/parks"
     * Or regex pattern: "/api/parks/.*" or "/api/parks/{id}"
     * Wildcards: "/api/parks/*" (converted to regex)
     */
    @Column(nullable = false, length = 500)
    private String endpoint;

    /**
     * Description of this endpoint mapping
     * Useful for documentation and understanding
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Model 1: Specific permission name
     * Examples: "CREATE_USER", "READ_ROLE", "DELETE_BOOKING"
     * If provided, requiredPermissionName is checked
     * If null, uses action + entity
     */
    @Column(nullable = true, length = 255)
    private String requiredPermissionName;

    /**
     * Model 2: Action from PermissionAction enum (alternative to permission name)
     * Examples: CREATE, READ, UPDATE, DELETE, EXECUTE
     * Used with entity for action-based permission checking
     * If requiredPermissionName is set, this is ignored
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = 50)
    private PermissionAction action;

    /**
     * Entity name (used with action)
     * Examples: "USER", "ROLE", "EMAIL_ACCOUNT", "BOOKING", "SAFARI_PACKAGE"
     * User must have action permission on this entity
     */
    @Column(nullable = true, length = 100)
    private String entity;

    /**
     * Whether this endpoint requires authentication
     * true: Requires valid user (with or without specific permission)
     * false: Endpoint is public (anyone can access)
     *
     * If false, requiredPermissionName and action are ignored
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean requiresAuth = true;

    /**
     * Whether to use regex pattern matching for endpoint path
     * true: endpoint is treated as regex pattern
     * false: endpoint must match exactly
     *
     * Examples:
     * requiresPatternMatching=true, endpoint="/api/parks/.*" -> matches /api/parks/123, /api/parks/abc
     * requiresPatternMatching=false, endpoint="/api/parks" -> only matches /api/parks exactly
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean requiresPatternMatching = false;

    /**
     * Whether this endpoint permission mapping is active
     * false: Endpoint is effectively disabled (403 Forbidden)
     * true: Endpoint permission mapping is active
     *
     * Soft delete - allows reactivation without code changes
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    /**
     * Role-based access (optional alternative to permission-based)
     * Comma-separated list of role names
     * If set, user must have one of these roles
     * Examples: "admin,manager", "user,staff"
     */
    @Column(nullable = true, length = 500)
    private String allowedRoles;

    /**
     * Notes/comments about this endpoint
     * For internal documentation
     */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * Timestamp fields for audit trail
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Override
    public String toString() {
        String permission = "PUBLIC";
        if (requiredPermissionName != null) {
            permission = requiredPermissionName;
        } else if (action != null && entity != null) {
            permission = action + " on " + entity;
        }
        return String.format("%s %s -> %s %s",
            httpMethod, endpoint,
            permission,
            active ? "" : "[DISABLED]");
    }

    /**
     * Check if this endpoint requires a specific permission
     */
    public boolean requiresPermission() {
        return requiresAuth && (requiredPermissionName != null ||
               (action != null && entity != null));
    }

    /**
     * Check if this endpoint requires a role
     */
    public boolean requiresRole() {
        return requiresAuth && allowedRoles != null && !allowedRoles.isEmpty();
    }

    /**
     * Get allowed roles as list
     */
    public List<String> getAllowedRolesList() {
        if (allowedRoles == null || allowedRoles.isEmpty()) {
            return java.util.List.of();
        }
        return java.util.Arrays.stream(allowedRoles.split(","))
            .map(String::trim)
            .toList();
    }
}
