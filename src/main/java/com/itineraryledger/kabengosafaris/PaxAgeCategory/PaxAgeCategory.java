package com.itineraryledger.kabengosafaris.PaxAgeCategory;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * PaxAgeCategory Entity - Manages passenger age categories for pricing
 *
 * Represents age-based pricing categories (Child, Youth, Adult, etc.)
 * Used by accommodations, activities, and parks to define age-specific pricing.
 *
 * Features:
 * - Age range validation (minAge must be less than maxAge)
 * - System protection (system categories cannot be deleted)
 * - Active status for soft enable/disable
 * - Category type enum for standardization
 * - Helper methods for age range checking
 */
@Entity
@Table(name = "pax_age_categories", indexes = {
    @Index(name = "idx_pax_age_category_name", columnList = "name"),
    @Index(name = "idx_pax_age_category_type", columnList = "category_type"),
    @Index(name = "idx_pax_age_category_is_active", columnList = "is_active"),
    @Index(name = "idx_pax_age_category_is_system", columnList = "is_system"),
    @Index(name = "idx_pax_age_category_min_age", columnList = "min_age"),
    @Index(name = "idx_pax_age_category_max_age", columnList = "max_age")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaxAgeCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name; // e.g., "Child", "Youth", "Adult"

    @Enumerated(EnumType.STRING)
    @Column(name = "category_type", length = 50)
    private CategoryType categoryType;

    @Column(name = "min_age", nullable = false)
    private Integer minAge;

    @Column(name = "max_age", nullable = false)
    private Integer maxAge;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * Indicates if this is a system category (created by initializer)
     * TRUE = System category (protected from deletion)
     * FALSE = User-created category (can be deleted)
     */
    @Builder.Default
    @Column(name = "is_system", nullable = false)
    private Boolean isSystem = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Check if a given age falls within this category's range
     *
     * @param age The age to check
     * @return true if age is within [minAge, maxAge] inclusive
     */
    @Transient
    public boolean isAgeInRange(int age) {
        return age >= minAge && age <= maxAge;
    }

    /**
     * Check if this is a system category (protected from deletion)
     */
    @Transient
    public boolean isSystemCategory() {
        return isSystem != null && isSystem;
    }

    /**
     * Get the age range as a formatted string
     * e.g., "0-5 years", "6-14 years", "15+ years"
     */
    @Transient
    public String getAgeRangeDisplay() {
        if (maxAge >= 150) { // Treat 150+ as "no upper limit"
            return minAge + "+ years";
        }
        return minAge + "-" + maxAge + " years";
    }

    /**
     * Lifecycle hook to validate age range
     */
    @PrePersist
    @PreUpdate
    protected void validateAgeRange() {
        if (minAge != null && maxAge != null && minAge > maxAge) {
            throw new IllegalArgumentException("minAge cannot be greater than maxAge");
        }
        if (minAge != null && minAge < 0) {
            throw new IllegalArgumentException("minAge cannot be negative");
        }
        if (maxAge != null && maxAge < 0) {
            throw new IllegalArgumentException("maxAge cannot be negative");
        }
    }

    /**
     * Predefined category types for standardization
     */
    public enum CategoryType {
        CHILD("Child", "Typically 0-5 years old"),
        YOUTH("Youth", "Typically 6-14 years old"),
        ADULT("Adult", "Typically 15 years and above"),
        CUSTOM("Custom", "Custom age category");

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
