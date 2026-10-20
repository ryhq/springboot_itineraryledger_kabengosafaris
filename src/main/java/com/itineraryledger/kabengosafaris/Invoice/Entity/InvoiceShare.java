package com.itineraryledger.kabengosafaris.Invoice.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.itineraryledger.kabengosafaris.Customer.Entity.Customer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One person's share of an invoice that several people are paying.
 *
 * <p>A group trip is organised by one person and paid for by six. Until now the only way to
 * handle that was for the five others to send their money to the organiser and the organiser to
 * send one lump to us, which makes a private individual into an unpaid, unprotected escrow agent
 * for several thousand dollars. The alternative the system allowed was six payments against one
 * invoice, which works for the bank and not for anybody else: {@code Payment} has no payer, so
 * all six receipts come out in the organiser's name and go to the organiser's inbox, and nothing
 * anywhere records who still owes what.
 *
 * <p>A share is the missing noun. The invoice stays whole and stays addressed to the lead
 * traveller, which keeps the safari, the quote and the status machine exactly as they were; the
 * shares sit underneath it saying who is paying which part of it. Each one can be sent its own
 * request and gets its own receipt in its own name.
 *
 * <p><strong>Nothing here is a second invoice.</strong> One invoice per safari is enforced by a
 * unique key on {@code invoices.safari_id}, and the quote is recovered later by
 * (itinerary, customer), so a second customer on the booking would break the lookup that builds
 * the invoice in the first place. Shares deliberately leave all of that alone.
 *
 * <p>{@code amountPaid}, {@code balance} and the settled flag are NOT columns, for the same
 * reason the invoice's are not: a stored total is a second copy of the truth that drifts from the
 * payments the moment anything is corrected. They are summed from {@code payments} on demand by
 * {@link com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceServices.InvoicePaymentAggregationService}.
 */
@Entity
@Table(name = "invoice_shares", indexes = {
    @Index(name = "idx_invoice_share_invoice", columnList = "invoice_id"),
    @Index(name = "idx_invoice_share_customer", columnList = "customer_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    /**
     * Who is paying this share, as they should appear on their own receipt.
     *
     * <p>Free text and required, because the people splitting a family booking are usually not
     * customers in their own right and forcing a Customer record for each would mean creating
     * five half-empty ones to collect five bank transfers.
     */
    @Column(name = "payer_name", nullable = false, length = 200)
    private String payerName;

    /** Where this share's request and receipt go. Null falls back to the invoice's customer. */
    @Column(name = "payer_email", length = 255)
    private String payerEmail;

    /**
     * The payer as a customer record, when they happen to be one.
     *
     * <p>Optional on purpose. It is here so a returning client's share can be tied to their
     * history, not so that every payer has to be registered before they can send money.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    /**
     * What this payer should quote when they transfer, so an unattributed arrival in the bank
     * can be matched without asking the organiser who sent it.
     */
    @Column(name = "share_reference", length = 100)
    private String shareReference;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (isActive == null) {
            isActive = true;
        }
        if (displayOrder == null) {
            displayOrder = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /** The name to put on this share's paperwork, falling back to the invoice's customer. */
    public String resolvedPayerName() {
        if (payerName != null && !payerName.isBlank()) {
            return payerName;
        }
        if (customer != null) {
            return customer.getDisplayName();
        }
        return invoice != null && invoice.getCustomer() != null
            ? invoice.getCustomer().getDisplayName()
            : null;
    }

    /**
     * Where this share's receipt should go.
     *
     * <p>The explicit address wins, then the linked customer's, then the invoice's customer. The
     * last fallback is what stops a receipt vanishing when somebody records a payment against a
     * share nobody gave an address for: it lands where it would have landed before shares
     * existed, which is wrong but visible, rather than nowhere.
     */
    public String resolvedPayerEmail() {
        if (payerEmail != null && !payerEmail.isBlank()) {
            return payerEmail;
        }
        if (customer != null && customer.getPrimaryEmail() != null) {
            return customer.getPrimaryEmail();
        }
        return invoice != null && invoice.getCustomer() != null
            ? invoice.getCustomer().getPrimaryEmail()
            : null;
    }
}
