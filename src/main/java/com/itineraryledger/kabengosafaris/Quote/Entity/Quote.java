package com.itineraryledger.kabengosafaris.Quote.Entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.itineraryledger.kabengosafaris.Customer.Entity.Customer;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import com.itineraryledger.kabengosafaris.Quote.Enums.QuoteStatus;
import com.itineraryledger.kabengosafaris.User.User;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Quote entity representing a price quotation for a safari itinerary.
 *
 * A Quote is linked to an Itinerary and contains itemized pricing in multiple currencies
 * to accommodate passengers with different nationality categories.
 *
 * Key features:
 * - Multi-currency support for subtotals, taxes, discounts, and grand totals
 * - Version control with links to previous/next versions
 * - Workflow management with status tracking
 * - Payment terms and validity period
 * - Approval workflow
 * - Comprehensive audit trail
 */
@Entity
@Table(name = "quotes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique quote code/number for reference
     * Example: QT-2024-001, QUOTE-ABC-123
     */
    @Column(nullable = false, unique = true, length = 50)
    private String quoteCode;

    /**
     * Quote title/name
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * Optional description or summary of the quote
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    // =====================================================================
    // RELATIONSHIPS
    // =====================================================================

    /**
     * The itinerary this quote is for
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_id", nullable = false)
    private Itinerary itinerary;

    /**
     * The customer this quote is for
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    /**
     * Quote items (accommodations, activities, park fees, etc.)
     */
    @OneToMany(mappedBy = "quote", fetch = FetchType.LAZY)
    @Builder.Default
    private List<QuoteItem> items = new ArrayList<>();

    /**
     * Quote documents (PDFs, contracts, itineraries, etc.)
     */
    @OneToMany(mappedBy = "quote", fetch = FetchType.LAZY)
    @Builder.Default
    private List<QuoteDocument> documents = new ArrayList<>();

    // =====================================================================
    // MULTI-CURRENCY TOTALS
    // =====================================================================

    /**
     * Subtotals by currency (before taxes and discounts)
     * Each Price object represents: {currency, quantity: 1, unitPrice: subtotal, totalPrice: subtotal}
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "quote_subtotals", joinColumns = @JoinColumn(name = "quote_id"))
    @Builder.Default
    private List<Price> subtotals = new ArrayList<>();

    /**
     * Tax amounts by currency
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "quote_taxes", joinColumns = @JoinColumn(name = "quote_id"))
    @Builder.Default
    private List<Price> taxes = new ArrayList<>();

    /**
     * Discount amounts by currency
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "quote_discounts", joinColumns = @JoinColumn(name = "quote_id"))
    @Builder.Default
    private List<Price> discounts = new ArrayList<>();

    /**
     * Grand totals by currency (subtotal + taxes - discounts)
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "quote_grand_totals", joinColumns = @JoinColumn(name = "quote_id"))
    @Builder.Default
    private List<Price> grandTotals = new ArrayList<>();

    // =====================================================================
    // PRICING DETAILS
    // =====================================================================

    /**
     * Whether prices shown are STO (System Tour Operator) rates or Rack rates
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isStoRate = true;

    /**
     * Tax percentage applied (if applicable)
     */
    @Column(precision = 5, scale = 2)
    private BigDecimal taxPercentage;

    /**
     * Discount percentage applied (if applicable)
     */
    @Column(precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    /**
     * Optional discount reason/note
     */
    @Column(length = 500)
    private String discountReason;

    // =====================================================================
    // VERSIONING
    // =====================================================================

    /**
     * Version number of this quote (1, 2, 3, etc.)
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    /**
     * Reference to the previous version of this quote
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_version_id")
    private Quote previousVersion;

    /**
     * Reference to the next version of this quote
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_version_id")
    private Quote nextVersion;
    
    /**
     * Notes explaining what changed in this version
     */
    @Column(columnDefinition = "TEXT")
    private String versionNotes;

    // =====================================================================
    // WORKFLOW AND STATUS
    // =====================================================================

    /**
     * Current status of the quote
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private QuoteStatus status = QuoteStatus.DRAFT;

    /**
     * Safari start date used for cost estimation.
     * Determines which season's rates apply. This date becomes the Safari's
     * start date when converting the quote to a booking, ensuring price consistency.
     */
    @Column(name = "safari_start_date")
    private LocalDate safariStartDate;

    /**
     * Date the quote was sent to customer
     */
    @Column(name = "sent_date")
    private LocalDate sentDate;

    /**
     * Quote validity start date
     */
    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    /**
     * Quote validity end date
     */
    @Column(name = "valid_to", nullable = false)
    private LocalDate validTo;

    /**
     * Whether the quote is currently valid (based on validity dates)
     * This should be calculated dynamically or updated by a scheduled job
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isValid = true;

    // =====================================================================
    // PAYMENT TERMS
    // =====================================================================

    /**
     * Deposit percentage required
     */
    @Column(precision = 5, scale = 2)
    private BigDecimal depositPercentage;

    /**
     * Deposit due date
     */
    @Column(name = "deposit_due_date")
    private LocalDate depositDueDate;

    /**
     * Full payment due date
     */
    @Column(name = "full_payment_due_date")
    private LocalDate fullPaymentDueDate;

    // =====================================================================
    // APPROVAL WORKFLOW
    // =====================================================================

    /**
     * User who needs to approve this quote
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id")
    private User approver;

    /**
     * User who approved this quote
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private User approvedBy;

    /**
     * Date and time the quote was approved
     */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /**
     * Approval notes/comments
     */
    @Column(columnDefinition = "TEXT")
    private String approvalNotes;

    // =====================================================================
    // ADDITIONAL FIELDS
    // =====================================================================

    /**
     * Internal notes (not visible to customer)
     */
    @Column(columnDefinition = "TEXT")
    private String internalNotes;

    /**
     * Customer-facing notes/remarks
     */
    @Column(columnDefinition = "TEXT")
    private String customerNotes;

    /**
     * Whether this quote is active/enabled
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // =====================================================================
    // AUDIT FIELDS
    // =====================================================================

    /**
     * User who created this quote
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    /**
     * User who last updated this quote
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_id")
    private User updatedBy;

    /**
     * Timestamp when this quote was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when this quote was last updated
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Generate quote code based on ID, month/year, and version
     * Format: QT-{1000 + ID}-{MM}{YY}-{version}
     * Example: QT-1001-0126-1
     */
    @Transient
    public String generateCode() {
        if (id == null || version == null) {
            return null;
        }
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MMyy");
        String monthYear = LocalDateTime.now().format(formatter);
        return String.format("QT-%d-%s-%d", 1000 + id, monthYear, version);
    }
}
