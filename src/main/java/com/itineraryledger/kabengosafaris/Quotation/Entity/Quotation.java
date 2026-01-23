package com.itineraryledger.kabengosafaris.Quotation.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.itineraryledger.kabengosafaris.Customer.Entity.Customer;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Quotation.Enums.DiscountType;
import com.itineraryledger.kabengosafaris.Quotation.Enums.QuotationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Quotation Entity - Represents a cost estimate/proposal for a safari
 *
 * Quotations are generated from Itinerary templates and sent to customers for approval.
 * Upon acceptance, a quotation can be converted into a Safari booking.
 *
 * Supports versioning for revisions and tracks full lifecycle from draft to acceptance/rejection.
 */
@Entity
@Table(name = "quotations",
    indexes = {
        @Index(name = "idx_quotation_code", columnList = "code"),
        @Index(name = "idx_quotation_customer", columnList = "customer_id"),
        @Index(name = "idx_quotation_itinerary", columnList = "itinerary_id"),
        @Index(name = "idx_quotation_status", columnList = "status"),
        @Index(name = "idx_quotation_start_date", columnList = "start_date"),
        @Index(name = "idx_quotation_valid_until", columnList = "valid_until"),
        @Index(name = "idx_quotation_assigned_to", columnList = "assigned_to"),
        @Index(name = "idx_quotation_created_at", columnList = "created_at"),
        @Index(name = "idx_quotation_parent", columnList = "parent_quotation_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_quotation_code", columnNames = {"code"})
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========================
    // IDENTIFICATION
    // ========================

    /**
     * Auto-generated unique code (e.g., QUO-00010126)
     * Format: QUO-{8-digit padded ID + 10000}
     */
    @NotBlank(message = "Quotation code is required")
    @Column(length = 50, unique = true, nullable = false)
    private String code;

    /**
     * Quotation name/title for easy identification
     */
    @NotBlank(message = "Quotation name is required")
    @Column(length = 200, nullable = false)
    private String name;

    // ========================
    // RELATIONSHIPS
    // ========================

    @NotNull(message = "Customer is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnore
    private Customer customer;

    /**
     * Reference to the source itinerary template
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_id")
    @JsonIgnore
    private Itinerary itinerary;

    /**
     * Self-reference for revision tracking
     * When a quotation is revised, the new version points to the original
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_quotation_id")
    @JsonIgnore
    private Quotation parentQuotation;

    // ========================
    // STATUS & VERSIONING
    // ========================

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private QuotationStatus status = QuotationStatus.DRAFT;

    /**
     * Revision version number (starts at 1)
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    // ========================
    // TRIP DETAILS
    // ========================

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "total_days")
    private Integer totalDays;

    @Column(name = "total_nights")
    private Integer totalNights;

    @Column(name = "total_pax")
    private Integer totalPax;

    // ========================
    // PRICING
    // ========================

    /**
     * Quotation currency (ISO code)
     */
    @Column(length = 10)
    @Builder.Default
    private String currency = "USD";

    /**
     * Exchange rate to base currency (USD) at time of quotation
     */
    @Column(name = "exchange_rate", precision = 15, scale = 6)
    @Builder.Default
    private BigDecimal exchangeRate = BigDecimal.ONE;

    /**
     * Sum of all line items before discount and tax
     */
    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", length = 20)
    @Builder.Default
    private DiscountType discountType = DiscountType.NONE;

    /**
     * Discount value (percentage or fixed amount based on discountType)
     */
    @Column(name = "discount_value", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal discountValue = BigDecimal.ZERO;

    @Lob
    @Column(name = "discount_reason", columnDefinition = "TEXT")
    private String discountReason;

    /**
     * Tax percentage (e.g., 18.00 for 18%)
     */
    @Column(name = "tax_rate", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal taxRate = BigDecimal.ZERO;

    /**
     * Calculated tax amount
     */
    @Column(name = "tax_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    /**
     * Final total after discount and tax
     */
    @Column(name = "total_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    // ========================
    // DEPOSIT
    // ========================

    /**
     * Required deposit amount
     */
    @Column(name = "deposit_required", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal depositRequired = BigDecimal.ZERO;

    /**
     * Deposit as percentage of total
     */
    @Column(name = "deposit_percentage", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal depositPercentage = new BigDecimal("50.00");

    // ========================
    // VALIDITY
    // ========================

    /**
     * Date until which the quotation is valid
     */
    @Column(name = "valid_until")
    private LocalDate validUntil;

    // ========================
    // LIFECYCLE TIMESTAMPS
    // ========================

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "viewed_at")
    private LocalDateTime viewedAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Lob
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // ========================
    // CONTENT
    // ========================

    @Lob
    @Column(name = "terms_and_conditions", columnDefinition = "TEXT")
    private String termsAndConditions;

    /**
     * What's included in the price (JSON array)
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String inclusions;

    /**
     * What's NOT included in the price (JSON array)
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String exclusions;

    @Lob
    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;

    @Lob
    @Column(name = "customer_notes", columnDefinition = "TEXT")
    private String customerNotes;

    // ========================
    // METADATA
    // ========================

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "assigned_to")
    private Long assignedTo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ========================
    // CHILD ENTITIES
    // ========================

    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @Builder.Default
    private List<QuotationPax> paxList = new ArrayList<>();

    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayNumber ASC, sortOrder ASC")
    @Builder.Default
    private List<QuotationLineItem> lineItems = new ArrayList<>();

    // ========================
    // HELPER METHODS
    // ========================

    /**
     * Add a pax configuration to this quotation
     */
    public void addPax(QuotationPax pax) {
        paxList.add(pax);
        pax.setQuotation(this);
    }

    /**
     * Remove a pax configuration from this quotation
     */
    public void removePax(QuotationPax pax) {
        paxList.remove(pax);
        pax.setQuotation(null);
    }

    /**
     * Add a line item to this quotation
     */
    public void addLineItem(QuotationLineItem lineItem) {
        lineItems.add(lineItem);
        lineItem.setQuotation(this);
    }

    /**
     * Remove a line item from this quotation
     */
    public void removeLineItem(QuotationLineItem lineItem) {
        lineItems.remove(lineItem);
        lineItem.setQuotation(null);
    }

    /**
     * Generate quotation code based on ID
     * Format: QUO-{8-digit padded (ID + 10000)}
     * Example: ID=1 -> QUO-00010001, ID=126 -> QUO-00010126
     */
    @Transient
    public String generateCode() {
        if (id == null) {
            return null;
        }
        return String.format("QUO-%08d", id + 10000);
    }

    /**
     * Calculate the total pax count from pax list
     */
    @Transient
    public int calculateTotalPax() {
        if (paxList == null || paxList.isEmpty()) {
            return totalPax != null ? totalPax : 0;
        }
        return paxList.stream()
            .mapToInt(p -> p.getCount() != null ? p.getCount() : 0)
            .sum();
    }

    /**
     * Calculate deposit required based on percentage
     */
    public void calculateDeposit() {
        if (totalAmount != null && depositPercentage != null) {
            this.depositRequired = totalAmount.multiply(depositPercentage)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }
    }

    /**
     * Calculate totals from line items
     */
    public void calculateTotals() {
        // Calculate subtotal from line items
        if (lineItems != null && !lineItems.isEmpty()) {
            this.subtotal = lineItems.stream()
                .map(li -> li.getTotalPrice() != null ? li.getTotalPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // Calculate discount amount
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (discountType == DiscountType.PERCENTAGE && discountValue != null) {
            discountAmount = subtotal.multiply(discountValue)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        } else if (discountType == DiscountType.FIXED && discountValue != null) {
            discountAmount = discountValue;
        }

        // Calculate amount after discount
        BigDecimal afterDiscount = subtotal.subtract(discountAmount);

        // Calculate tax
        if (taxRate != null && taxRate.compareTo(BigDecimal.ZERO) > 0) {
            this.taxAmount = afterDiscount.multiply(taxRate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        } else {
            this.taxAmount = BigDecimal.ZERO;
        }

        // Calculate total
        this.totalAmount = afterDiscount.add(taxAmount);

        // Calculate deposit
        calculateDeposit();
    }

    /**
     * Check if the quotation is expired
     */
    @Transient
    public boolean isExpired() {
        if (validUntil == null) {
            return false;
        }
        return LocalDate.now().isAfter(validUntil);
    }

    /**
     * Check if the quotation can be sent
     */
    @Transient
    public boolean canSend() {
        return status == QuotationStatus.DRAFT && customer != null && !isExpired();
    }

    /**
     * Check if the quotation can be revised
     */
    @Transient
    public boolean canRevise() {
        return status == QuotationStatus.SENT ||
               status == QuotationStatus.VIEWED ||
               status == QuotationStatus.REJECTED;
    }

    /**
     * Check if the quotation can be accepted
     */
    @Transient
    public boolean canAccept() {
        return (status == QuotationStatus.SENT || status == QuotationStatus.VIEWED) && !isExpired();
    }

    /**
     * Check if the quotation can be converted to Safari
     */
    @Transient
    public boolean canConvertToSafari() {
        return status == QuotationStatus.ACCEPTED;
    }

    /**
     * Get days/nights display string
     */
    @Transient
    public String getDaysNightsDisplay() {
        int days = totalDays != null ? totalDays : 0;
        int nights = totalNights != null ? totalNights : 0;
        return days + " Days / " + nights + " Nights";
    }

    /**
     * Get per person cost
     */
    @Transient
    public BigDecimal getPerPersonCost() {
        int pax = calculateTotalPax();
        if (pax == 0 || totalAmount == null) {
            return BigDecimal.ZERO;
        }
        return totalAmount.divide(new BigDecimal(pax), 2, RoundingMode.HALF_UP);
    }
}
