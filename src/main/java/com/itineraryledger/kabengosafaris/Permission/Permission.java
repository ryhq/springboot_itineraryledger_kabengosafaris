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
 * Each permission defines what action can be performed on what resource
 * Example: "create_safari_booking", "read_user_profile",
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
     * Format: action_resource (e.g., "create_booking", "read_profile", "delete_user")
     */
    @Column(nullable = false, unique = true)
    private String name;

    /**
     * Human-readable description of what this permission allows
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * Category/module/entity this permission belongs to
     * Examples: "Safari", "User", "Booking"
     */
    @Column(nullable = false)
    private String category;

    /**
     * Action type: reference to PermissionActionType (database-driven)
     * Examples: "create", "read", "update", "delete", "execute", "submit", etc.
     * Loaded eagerly since permission checks happen frequently
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "action_type_id", nullable = false)
    private PermissionActionType actionType;

    /**
     * Resource/Document type this permission applies to
     * Examples: "Safari Package", "Booking", "User", "Payment"
     */
    @Column(nullable = false)
    private String resource;

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
