package com.itineraryledger.kabengosafaris.CreditNote.Entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.itineraryledger.kabengosafaris.CreditNote.Enums.ConsumptionMethod;
import com.itineraryledger.kabengosafaris.CreditNote.Enums.CreditNoteStatus;
import com.itineraryledger.kabengosafaris.Customer.Entity.Customer;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.User.User;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CreditNote entity representing a credit/refund document against an Invoice.
 *
 * A credit note is issued when money needs to be returned or re-applied:
 * - Customer cancels part of a safari
 * - Service quality issue
 * - Overcharge correction
 *
 * One Invoice can have many CreditNotes.
 * Workflow: DRAFT → CONFIRMED → SENT → CONSUMED
 */
@Entity
@Table(name = "credit_notes", indexes = {
    @Index(name = "idx_cn_invoice_id", columnList = "invoice_id"),
    @Index(name = "idx_cn_customer_id", columnList = "customer_id"),
    @Index(name = "idx_cn_status", columnList = "status"),
    @Index(name = "idx_cn_code", columnList = "credit_note_code")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique credit note code
     * Format: CN-{6-digit padded (ID + 100)}
     */
    @Column(name = "credit_note_code", nullable = false, unique = true, length = 50)
    private String creditNoteCode;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    // =====================================================================
    // RELATIONSHIPS
    // =====================================================================

    /**
     * The original invoice this credit note is against
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    /**
     * The customer (derived from invoice, stored for direct access)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    /**
     * Credit note line items
     */
    @OneToMany(mappedBy = "creditNote", fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<CreditNoteLineItem> lineItems = new ArrayList<>();

    // =====================================================================
    // MULTI-CURRENCY TOTALS
    // =====================================================================
    //
    // Per-currency subtotals, taxes and totals are NOT stored on this entity.
    // They are derived on demand by CreditNoteTotalsAggregationService from
    // the active line items + taxPercentage, so they never drift from truth.

    // =====================================================================
    // PRICING DETAILS
    // =====================================================================

    @Column(precision = 5, scale = 2)
    private BigDecimal taxPercentage;

    // =====================================================================
    // DATES
    // =====================================================================

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "sent_date")
    private LocalDate sentDate;

    @Column(name = "consumed_date")
    private LocalDate consumedDate;

    // =====================================================================
    // WORKFLOW AND STATUS
    // =====================================================================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private CreditNoteStatus status = CreditNoteStatus.DRAFT;

    // =====================================================================
    // CONSUMPTION DETAILS
    // =====================================================================

    /**
     * How the credit was consumed (set when status → CONSUMED)
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private ConsumptionMethod consumptionMethod;

    /**
     * Notes about the consumption (e.g., "Applied to invoice INV-000123")
     */
    @Column(columnDefinition = "TEXT")
    private String consumptionNotes;

    // =====================================================================
    // ADDITIONAL FIELDS
    // =====================================================================

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String internalNotes;

    @Column(columnDefinition = "TEXT")
    private String customerNotes;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // =====================================================================
    // AUDIT FIELDS
    // =====================================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_id")
    private User updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    public String generateCode() {
        if (id == null) return null;
        return String.format("CN-%06d", id + 100);
    }

    @Transient
    public boolean isEditable() {
        return status != null && status.isEditable();
    }

    @Transient
    public boolean isDeletable() {
        return status != null && status.isDeletable();
    }
}
