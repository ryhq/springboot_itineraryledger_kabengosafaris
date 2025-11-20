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
 * PermissionActionType Entity - Database-driven permission action definitions
 * Replaces the hardcoded PermissionAction enum.
 * Allows runtime definition and modification of action types without recompilation.
 *
 * Examples of action types:
 *   - CREATE: Create new records
 *   - READ: View and read records
 *   - UPDATE: Edit and update records
 *   - DELETE: Delete records
 *   - EXECUTE: Execute actions and workflows
 *   - SUBMIT: Submit documents for approval
 *   - AMEND: Amend submitted documents
 *   - CANCEL: Cancel submitted documents
 *   - EXPORT: Export data to external formats
 *   - PRINT: Print documents
 *
 * System actions (CREATE, READ, UPDATE, DELETE) cannot be deleted.
 * Custom actions can be added/removed at runtime via PermissionActionService.
 */
@Entity
@Table(name = "permission_action_types")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionActionType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique action code identifier
     * Examples: "create", "read", "update", "delete", "execute", "submit", etc.
     * Used in permissions and @RequirePermission annotations
     */
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    /**
     * Human-readable description of what this action allows
     * Examples: "Create new records", "View and read records"
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * Whether this action type is active and can be used
     * Inactive actions cannot be assigned to new permissions
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    /**
     * System action types cannot be deleted
     * System actions like CREATE, READ, UPDATE, DELETE should always be available
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isSystem = true;

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
        return "PermissionActionType{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", description='" + description + '\'' +
                ", active=" + active +
                ", isSystem=" + isSystem +
                '}';
    }
}
