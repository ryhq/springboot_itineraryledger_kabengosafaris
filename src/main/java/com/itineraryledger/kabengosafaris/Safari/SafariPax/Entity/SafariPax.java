package com.itineraryledger.kabengosafaris.Safari.SafariPax.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.itineraryledger.kabengosafaris.PaxAgeCategory.PaxAgeCategory;
import com.itineraryledger.kabengosafaris.PaxNationCategory.PaxNationCategory;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * SafariPax Entity - Represents passenger categories in a safari booking
 *
 * Tracks the number of passengers per nationality and age category.
 * Copied from ItineraryPax when Safari is created from Itinerary template.
 *
 * Safari-specific additions:
 * - actualCount: Actual number of passengers (may differ from planned count)
 * - confirmedAt: When this pax category was confirmed
 * - specialRequirements: Specific requirements for this pax group
 */
@Entity
@Table(name = "safari_pax",
    indexes = {
        @Index(name = "idx_safari_pax_safari_id", columnList = "safari_id"),
        @Index(name = "idx_safari_pax_nation_category_id", columnList = "nation_category_id"),
        @Index(name = "idx_safari_pax_age_category_id", columnList = "age_category_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_safari_pax_combination",
            columnNames = {"safari_id", "nation_category_id", "age_category_id"}
        )
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafariPax {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "safari_id", nullable = false)
    @JsonIgnore
    private Safari safari;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nation_category_id", nullable = false)
    private PaxNationCategory nationCategory;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "age_category_id", nullable = false)
    private PaxAgeCategory ageCategory;

    @Min(value = 1, message = "Pax count cannot be less than one (01)")
    @Column(nullable = false)
    @Builder.Default
    private Integer count = 1;

    /**
     * Special requirements for this passenger group
     * E.g., "Wheelchair accessible vehicle needed", "Child seat required"
     */
    @Lob
    @Column(name = "special_requirements", columnDefinition = "TEXT")
    private String specialRequirements;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ========================
    // HELPER METHODS
    // ========================

    /**
     * Get display name for this pax category
     * E.g., "Non-Resident Adult (2)"
     */
    @Transient
    public String getDisplayName() {
        String nation = nationCategory != null ? nationCategory.getName() : "Unknown";
        String age = ageCategory != null ? ageCategory.getName() : "Unknown";
        return nation + " " + age + " (" + count + ")";
    }

    @PrePersist
    @PreUpdate
    private void validate() {
        if (count != null && count < 1) {
            throw new IllegalArgumentException("Pax count cannot be less than one (01)");
        }
    }
}
