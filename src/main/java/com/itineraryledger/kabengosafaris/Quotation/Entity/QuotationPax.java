package com.itineraryledger.kabengosafaris.Quotation.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.itineraryledger.kabengosafaris.PaxAgeCategory.PaxAgeCategory;
import com.itineraryledger.kabengosafaris.PaxNationCategory.PaxNationCategory;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * QuotationPax Entity - Represents passenger categories in a quotation
 *
 * Tracks the number of passengers per nationality and age category along with
 * their pricing. Used for quotation calculations and breakdown display.
 */
@Entity
@Table(name = "quotation_pax",
    indexes = {
        @Index(name = "idx_quotation_pax_quotation_id", columnList = "quotation_id"),
        @Index(name = "idx_quotation_pax_nation_category_id", columnList = "nation_category_id"),
        @Index(name = "idx_quotation_pax_age_category_id", columnList = "age_category_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_quotation_pax_combination",
            columnNames = {"quotation_id", "nation_category_id", "age_category_id"}
        )
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuotationPax {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quotation_id", nullable = false)
    @JsonIgnore
    private Quotation quotation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nation_category_id", nullable = false)
    private PaxNationCategory nationCategory;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "age_category_id", nullable = false)
    private PaxAgeCategory ageCategory;

    @Min(value = 1, message = "Pax count cannot be less than one")
    @Column(nullable = false)
    @Builder.Default
    private Integer count = 1;

    /**
     * Per-person price for this category
     */
    @Column(name = "unit_price", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal unitPrice = BigDecimal.ZERO;

    /**
     * Total price for this category (count × unitPrice)
     */
    @Column(name = "total_price", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalPrice = BigDecimal.ZERO;

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

    /**
     * Calculate total price based on count and unit price
     */
    public void calculateTotalPrice() {
        if (count != null && unitPrice != null) {
            this.totalPrice = unitPrice.multiply(new BigDecimal(count));
        }
    }

    @PrePersist
    @PreUpdate
    private void validate() {
        if (count != null && count < 1) {
            throw new IllegalArgumentException("Pax count cannot be less than one");
        }
        calculateTotalPrice();
    }
}
