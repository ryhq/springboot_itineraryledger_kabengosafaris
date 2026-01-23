package com.itineraryledger.kabengosafaris.Quotation.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.itineraryledger.kabengosafaris.Quotation.Enums.LineItemType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * QuotationLineItem Entity - Represents an itemized cost line in a quotation
 *
 * Each line item represents a specific cost component like accommodation,
 * park fees, activities, transport, etc. Line items can be organized by
 * day number for day-by-day breakdowns.
 */
@Entity
@Table(name = "quotation_line_items",
    indexes = {
        @Index(name = "idx_quotation_line_item_quotation_id", columnList = "quotation_id"),
        @Index(name = "idx_quotation_line_item_day_number", columnList = "day_number"),
        @Index(name = "idx_quotation_line_item_type", columnList = "item_type"),
        @Index(name = "idx_quotation_line_item_reference", columnList = "reference_type, reference_id"),
        @Index(name = "idx_quotation_line_item_sort", columnList = "day_number, sort_order")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuotationLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quotation_id", nullable = false)
    @JsonIgnore
    private Quotation quotation;

    // ========================
    // ORGANIZATION
    // ========================

    /**
     * Day number this item belongs to (null for overall items)
     * Day numbers start at 1
     */
    @Column(name = "day_number")
    private Integer dayNumber;

    /**
     * Sort order within the day (or overall section)
     */
    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    // ========================
    // ITEM DETAILS
    // ========================

    @NotNull(message = "Item type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", length = 30, nullable = false)
    private LineItemType itemType;

    /**
     * Human-readable description of the item
     */
    @NotBlank(message = "Item name is required")
    @Column(name = "item_name", length = 500, nullable = false)
    private String itemName;

    /**
     * Additional description or details
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    // ========================
    // REFERENCE TO SOURCE ENTITY
    // ========================

    /**
     * Foreign key to the actual entity (e.g., accommodation_id, park_id)
     * Stored as Long for flexibility
     */
    @Column(name = "reference_id")
    private Long referenceId;

    /**
     * Entity type name for the reference (e.g., "Accommodation", "Park", "Activity")
     */
    @Column(name = "reference_type", length = 100)
    private String referenceType;

    // ========================
    // PRICING
    // ========================

    /**
     * Number of units (rooms, people, vehicles, etc.)
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    /**
     * Unit of measurement (e.g., "night", "person", "vehicle", "group")
     */
    @Column(name = "unit_of_measure", length = 50)
    private String unitOfMeasure;

    /**
     * Price per unit
     */
    @Column(name = "unit_price", precision = 15, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal unitPrice = BigDecimal.ZERO;

    /**
     * Total price for this line item (quantity × unitPrice)
     */
    @Column(name = "total_price", precision = 15, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalPrice = BigDecimal.ZERO;

    /**
     * Currency for this line item (if different from quotation currency)
     */
    @Column(length = 10)
    private String currency;

    // ========================
    // FLAGS
    // ========================

    /**
     * Whether this item is taxable
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean taxable = true;

    /**
     * Whether this item is included in the price or optional
     */
    @Column(name = "is_included", nullable = false)
    @Builder.Default
    private Boolean isIncluded = true;

    /**
     * Whether this is an optional add-on
     */
    @Column(name = "is_optional", nullable = false)
    @Builder.Default
    private Boolean isOptional = false;

    // ========================
    // NOTES
    // ========================

    @Lob
    @Column(columnDefinition = "TEXT")
    private String notes;

    // ========================
    // METADATA
    // ========================

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
     * Calculate total price based on quantity and unit price
     */
    public void calculateTotalPrice() {
        if (quantity != null && unitPrice != null) {
            this.totalPrice = unitPrice.multiply(new BigDecimal(quantity));
        }
    }

    /**
     * Get display string for the item
     * E.g., "Serengeti Serena Lodge - 2 nights @ $350/night = $700"
     */
    @Transient
    public String getDisplayLine() {
        StringBuilder sb = new StringBuilder();
        sb.append(itemName);

        if (quantity != null && quantity > 0) {
            sb.append(" - ").append(quantity);
            if (unitOfMeasure != null && !unitOfMeasure.isEmpty()) {
                sb.append(" ").append(unitOfMeasure);
                if (quantity > 1 && !unitOfMeasure.endsWith("s")) {
                    sb.append("s");
                }
            }
        }

        if (unitPrice != null && unitPrice.compareTo(BigDecimal.ZERO) > 0) {
            sb.append(" @ $").append(unitPrice);
            if (unitOfMeasure != null && !unitOfMeasure.isEmpty()) {
                sb.append("/").append(unitOfMeasure);
            }
        }

        if (totalPrice != null && totalPrice.compareTo(BigDecimal.ZERO) > 0) {
            sb.append(" = $").append(totalPrice);
        }

        return sb.toString();
    }

    /**
     * Get item type display name
     */
    @Transient
    public String getItemTypeDisplayName() {
        return itemType != null ? itemType.getDisplayName() : null;
    }

    /**
     * Check if this is a day-specific item or overall item
     */
    @Transient
    public boolean isDaySpecific() {
        return dayNumber != null && dayNumber > 0;
    }

    @PrePersist
    @PreUpdate
    private void prePersist() {
        calculateTotalPrice();
        if (quantity == null || quantity < 1) {
            quantity = 1;
        }
    }
}
