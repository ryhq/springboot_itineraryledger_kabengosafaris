package com.itineraryledger.kabengosafaris.PaxNationCategory;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * PaxNationCategory Entity - Manages passenger nationality categories for pricing
 *
 * Represents nationality-based pricing categories (e.g., Resident, East African, Non-Resident)
 * Used by accommodations, activities, and parks to define nationality-specific pricing.
 *
 * Features:
 * - Priority factor for cost calculation (higher = higher priority in group bookings)
 * - System protection (system categories cannot be deleted)
 * - Active status for soft enable/disable
 * - Unique priority factor per category
 *
 * Priority Factor Usage:
 * - When ChargingBasis is PER_VEHICLE or PER_GROUP, the passenger with highest
 *   priorityFactor determines the rate category for the entire group
 * - Example: If a vehicle has both Residents and Non-Residents, the Non-Resident
 *   rate (higher priority) would apply to the vehicle charge
 */
@Entity
@Table(name = "pax_nation_category")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaxNationCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name; // e.g., "Resident", "East African", "Non-Resident"

    @Enumerated(EnumType.STRING)
    @Column(name = "category_type", length = 50)
    private CategoryType categoryType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "priority_factor", nullable = false)
    private Integer priorityFactor; // Higher value = higher priority for rate selection

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private Boolean isSystem = false; // System categories cannot be deleted

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Check if this is a system-protected category
     */
    @Transient
    public boolean isSystemCategory() {
        return isSystem != null && isSystem;
    }

    /**
     * Get formatted display string for priority
     * e.g., "Priority 1", "Priority 2"
     */
    @Transient
    public String getPriorityDisplay() {
        return "Priority " + priorityFactor;
    }

    /**
     * Pre-persist validation
     */
    @PrePersist
    @PreUpdate
    private void validate() {
        if (priorityFactor != null && priorityFactor < 1) {
            throw new IllegalArgumentException("Priority factor must be at least 1");
        }
    }

    /**
     * Predefined category types for standardization
     */
    public enum CategoryType {
        RESIDENT("Resident", "Tanzanian citizens and permanent residents"),
        EXPATRIATE("Expatriate", "Foreign nationals residing in Tanzania with work/residence permits"),
        EAST_AFRICAN("East African", "Citizens of East African Community member states"),
        NON_RESIDENT("Non-Resident", "International visitors from outside East Africa"),
        CUSTOM("Custom", "Custom nationality category");

        private final String displayName;
        private final String description;

        CategoryType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }
}
