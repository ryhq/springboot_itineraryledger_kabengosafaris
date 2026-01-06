package com.itineraryledger.kabengosafaris.Permission;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Permission Entity - Represents granular permissions in the system
 *
 * Each permission defines what action can be performed on what entity
 * Format: {ACTION}_{ENTITY}
 * Examples: "CREATE_USER", "READ_USER", "UPDATE_ROLE", "DELETE_EMAIL_ACCOUNT"
 *
 * This simplified approach uses:
 * - PermissionAction enum for actions (CREATE, READ, UPDATE, DELETE, etc.)
 * - Entity name for the resource being protected
 * - Permission name for use in @PreAuthorize annotations
 */
@Entity
@Table(name = "permissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Permission name - unique identifier for the permission
     * Format: {ACTION}_{ENTITY} (uppercase with underscores)
     * Examples: "CREATE_USER", "READ_ROLE", "UPDATE_EMAIL_ACCOUNT", "DELETE_BOOKING"
     *
     * Used in @PreAuthorize("hasPermission('CREATE_USER')") annotations
     */
    @Column(nullable = false, unique = true)
    private String name;

    /**
     * Human-readable description of what this permission allows
     * Example: "Allows creating new users in the system"
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * Action type from PermissionAction enum
     * Examples: CREATE, READ, UPDATE, DELETE, EXECUTE, SUBMIT, etc.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PermissionAction action;

    /**
     * Entity name this permission applies to
     * Examples: "USER", "ROLE", "EMAIL_ACCOUNT", "BOOKING", "SAFARI_PACKAGE"
     *
     * Used for filtering and grouping permissions by entity
     */
    @Column(nullable = false)
    private String entity;

    /**
     * Whether this permission is active/enabled
     */
    @Column(nullable = false)
    private Boolean active;

    /**
     * Timestamp fields
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (active == null) {
            active = true;
        }
    }
}
