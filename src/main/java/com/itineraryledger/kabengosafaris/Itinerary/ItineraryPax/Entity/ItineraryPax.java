package com.itineraryledger.kabengosafaris.Itinerary.ItineraryPax.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.PaxAgeCategory.PaxAgeCategory;
import com.itineraryledger.kabengosafaris.PaxNationCategory.PaxNationCategory;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * ItineraryPax Entity - Represents passenger categories in an itinerary
 *
 * Tracks the number of passengers per nationality and age category.
 * Used for pricing calculations based on tariff rates.
 */
@Entity
@Table(name = "itinerary_pax",
    indexes = {
        @Index(name = "idx_itinerary_pax_itinerary_id", columnList = "itinerary_id"),
        @Index(name = "idx_itinerary_pax_nation_category_id", columnList = "nation_category_id"),
        @Index(name = "idx_itinerary_pax_age_category_id", columnList = "age_category_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_itinerary_pax_combination",
            columnNames = {"itinerary_id", "nation_category_id", "age_category_id"}
        )
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryPax {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "itinerary_id", nullable = false)
    @JsonIgnore
    private Itinerary itinerary;

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
