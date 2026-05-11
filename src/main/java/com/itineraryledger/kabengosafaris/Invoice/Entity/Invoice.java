package com.itineraryledger.kabengosafaris.Invoice.Entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.itineraryledger.kabengosafaris.Customer.Entity.Customer;
import com.itineraryledger.kabengosafaris.Invoice.Enums.InvoiceStatus;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
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
 * Invoice entity representing a billing invoice for safari services.
 *
 * An Invoice is linked to a Customer and Safari, containing itemized billing
 * in multiple currencies to accommodate different payment methods.
 *
 * Key features:
 * - Multi-currency support for subtotals, taxes, discounts, and grand totals
 * - Payment tracking with due dates
 * - Workflow management with status tracking
 * - Comprehensive audit trail
 */
@Entity
@Table(name = "invoices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique invoice code/number for reference
     * Example: INV-000001, INV-000002
     */
    @Column(nullable = false, unique = true, length = 50)
    private String invoiceCode;

    /**
     * Invoice title/name
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * Optional description or summary of the invoice
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    // =====================================================================
    // RELATIONSHIPS
    // =====================================================================

    /**
     * The customer this invoice is for (nullable)
     * Foreign key constraint configured with ON DELETE SET NULL at database level
     * If Customer is deleted, this field will be automatically set to NULL
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = true)
    private Customer customer;

    /**
     * The safari this invoice is for (nullable, unique — one invoice per safari).
     * If Safari is deleted, this field will be automatically set to NULL.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "safari_id", nullable = true, unique = true)
    private Safari safari;

    /**
     * Invoice line items (services, products, fees, etc.)
     */
    @OneToMany(mappedBy = "invoice", fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<InvoiceLineItem> lineItems = new ArrayList<>();

    // =====================================================================
    // MULTI-CURRENCY TOTALS
    // =====================================================================

    /**
     * Subtotals by currency (before taxes and discounts)
     * Each Price object represents: {currency, quantity: 1, unitPrice: subtotal, totalPrice: subtotal}
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "invoice_subtotals", joinColumns = @JoinColumn(name = "invoice_id"))
    @Builder.Default
    private List<Price> subtotals = new ArrayList<>();

    /**
     * Tax amounts by currency
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "invoice_taxes", joinColumns = @JoinColumn(name = "invoice_id"))
    @Builder.Default
    private List<Price> taxes = new ArrayList<>();

    /**
     * Discount amounts by currency
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "invoice_discounts", joinColumns = @JoinColumn(name = "invoice_id"))
    @Builder.Default
    private List<Price> discounts = new ArrayList<>();

    /**
     * Grand totals by currency (subtotal + taxes - discounts)
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "invoice_grand_totals", joinColumns = @JoinColumn(name = "invoice_id"))
    @Builder.Default
    private List<Price> grandTotals = new ArrayList<>();

    // =====================================================================
    // PRICING DETAILS
    // =====================================================================

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
    // MARKUP (bakes into per-line-item unit prices, not a separate line)
    // =====================================================================

    /**
     * Agent commission percentage — pays off the referring agent.
     * Applied (with marginUplift) as a multiplier on each line item's unit
     * price before discount/tax, so the customer sees the inflated price as
     * the line total. P&L expense, not retained profit.
     */
    @Column(name = "agent_commission_percentage", precision = 5, scale = 2)
    private BigDecimal agentCommissionPercentage;

    @Column(name = "agent_commission_reason", length = 500)
    private String agentCommissionReason;

    /**
     * Margin uplift percentage — extra profit retained in-house. Applied
     * together with agentCommissionPercentage as a per-line-item multiplier.
     */
    @Column(name = "margin_uplift_percentage", precision = 5, scale = 2)
    private BigDecimal marginUpliftPercentage;

    @Column(name = "margin_uplift_reason", length = 500)
    private String marginUpliftReason;

    // =====================================================================
    // INVOICE DATES
    // =====================================================================

    /**
     * Date the invoice was issued
     */
    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    /**
     * Payment due date
     */
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    /**
     * Date the invoice was sent to customer
     */
    @Column(name = "sent_date")
    private LocalDate sentDate;

    /**
     * Date the invoice was paid
     */
    @Column(name = "paid_date")
    private LocalDate paidDate;

    // =====================================================================
    // PAYMENT TRACKING
    // =====================================================================
    //
    // Per-currency amountsPaid and balances are NOT stored on this entity.
    // They are derived on demand by InvoicePaymentAggregationService from
    // the payments table + grandTotals, so they never drift from truth.
    // Payment status is tracked via the 'status' field (PARTIALLY_PAID, PAID, etc.)

    // =====================================================================
    // WORKFLOW AND STATUS
    // =====================================================================

    /**
     * Current status of the invoice
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.DRAFT;

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
     * Payment terms description
     */
    @Column(columnDefinition = "TEXT")
    private String paymentTerms;

    /**
     * Whether this invoice is active/enabled
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // =====================================================================
    // AUDIT FIELDS
    // =====================================================================

    /**
     * User who created this invoice
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    /**
     * User who last updated this invoice
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_id")
    private User updatedBy;

    /**
     * Timestamp when this invoice was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when this invoice was last updated
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Generate invoice code based on ID
     * Format: INV-{6-digit padded (ID + 100)}
     * Example: INV-000101
     */
    @Transient
    public String generateCode() {
        if (id == null) {
            return null;
        }
        return String.format("INV-%06d", id + 100);
    }

    /**
     * Check if invoice is overdue
     * An invoice is overdue if it's past the due date and not yet fully paid or refunded
     */
    @Transient
    public boolean isOverdue() {
        if (status == InvoiceStatus.PAID || status == InvoiceStatus.CANCELLED) {
            return false;
        }
        return LocalDate.now().isAfter(dueDate);
    }

    /**
     * Check if invoice can be edited
     * Delegates to InvoiceStatus.isEditable()
     */
    @Transient
    public boolean isEditable() {
        return status != null && status.isEditable();
    }

    /**
     * Check if invoice can be deleted
     * Delegates to InvoiceStatus.isDeletable()
     */
    @Transient
    public boolean isDeletable() {
        return status != null && status.isDeletable();
    }
}
