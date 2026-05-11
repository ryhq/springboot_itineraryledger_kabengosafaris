package com.itineraryledger.kabengosafaris.Quote.QuotePax.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.itineraryledger.kabengosafaris.PaxAgeCategory.PaxAgeCategory;
import com.itineraryledger.kabengosafaris.PaxNationCategory.PaxNationCategory;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * QuotePax - per-customer pax mix on a quote.
 * Snapshotted from ItineraryPax; copied forward into SafariPax on conversion.
 * specialRequirements lives on SafariPax (operational), not here.
 */
@Entity
@Table(name = "quote_pax",
    indexes = {
        @Index(name = "idx_quote_pax_quote_id", columnList = "quote_id"),
        @Index(name = "idx_quote_pax_nation_category_id", columnList = "nation_category_id"),
        @Index(name = "idx_quote_pax_age_category_id", columnList = "age_category_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_quote_pax_combination",
            columnNames = {"quote_id", "nation_category_id", "age_category_id"}
        )
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuotePax {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_id", nullable = false)
    @JsonIgnore
    private Quote quote;

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
