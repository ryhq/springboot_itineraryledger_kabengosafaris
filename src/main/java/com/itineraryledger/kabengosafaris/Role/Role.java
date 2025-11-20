package com.itineraryledger.kabengosafaris.Role;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.itineraryledger.kabengosafaris.Permission.Permission;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Role Entity - Represents a role that contains multiple permissions
 * Similar to Frappe ERPNext Role system
 *
 * Roles are collections of permissions that define what users with that role can do
 * Example: "Safari Guide", "Booking Manager", "Finance Officer"
 */
@Entity
@Table(name = "roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Role name - unique identifier for the role
     * Examples: "booking_manager", "finance_officer"
     */
    @Column(nullable = false, unique = true)
    private String name;

    /**
     * Human-readable role display name
     * Examples: "Booking Manager", "Finance Officer"
     */
    @Column(nullable = false)
    private String displayName;

    /**
     * Detailed description of what this role is for
     */
    @Lob
    @Column(length = 2000)
    private String description;

    /**
     * Whether this role is active/enabled
     */
    @Column(nullable = false)
    private Boolean active;

    /**
     * System roles cannot be deleted (like ADMIN, USER)
     * Custom roles can be deleted
     */
    @Column(nullable = false)
    private Boolean isSystemRole;

    /**
     * Relationships
     */
    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.DETACH)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();

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
        if (isSystemRole == null) {
            isSystemRole = false;
        }
    }

    /**
     * Check if this role has a specific permission
     *
     * @param permissionName name of the permission
     * @return true if role has the permission
     */
    public boolean hasPermission(String permissionName) {
        return permissions.stream().anyMatch(p -> p.getName().equals(permissionName) && p.getActive());
    }

    /**
     * Check if this role has a specific action on a resource
     * Database-driven: looks up action type from database
     *
     * @param actionCode action code to check (e.g., "create", "read", "update")
     * @param resource resource/document type
     * @return true if role has the permission
     */
    public boolean hasPermission(String actionCode, String resource) {
        return permissions.stream()
            .anyMatch(
                p -> p.getActionType() != null &&
                     p.getActionType().getCode().equalsIgnoreCase(actionCode) &&
                     p.getResource().equalsIgnoreCase(resource) &&
                     p.getActive()
            );
    }

    /**
     * Add a permission to this role
     *
     * @param permission permission to add
     */
    public void addPermission(Permission permission) {
        this.permissions.add(permission);
    }

    /**
     * Remove a permission from this role
     *
     * @param permission permission to remove
     */
    public void removePermission(Permission permission) {
        this.permissions.remove(permission);
    }

    /**
     * Get all permissions for a specific action code
     * Database-driven: uses action code instead of enum
     *
     * @param actionCode action code (e.g., "create", "read", "update")
     * @return set of resources this role can perform the action on
     */
    public Set<String> getResourcesForAction(String actionCode) {
        Set<String> resources = new HashSet<>();
        permissions.stream()
                .filter(p -> p.getActionType() != null &&
                           p.getActionType().getCode().equalsIgnoreCase(actionCode) &&
                           p.getActive())
                .forEach(p -> resources.add(p.getResource()));
        return resources;
    }

    @Override
    public String toString() {
        return "Role{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", displayName='" + displayName + '\'' +
                ", active=" + active +
                ", isSystemRole=" + isSystemRole +
                ", permissionCount=" + permissions.size() +
                '}';
    }
    
}
