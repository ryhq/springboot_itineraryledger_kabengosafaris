package com.itineraryledger.kabengosafaris.Invoice.Enums;

import lombok.Getter;

/**
 * Invoice workflow states - simplified 8-state system
 *
 * Core Journey (6 states):
 *   DRAFT → SENT → VIEWED → PARTIALLY_PAID → PAID → (OVERDUE if past due)
 *
 * Exception States (2 states):
 *   CANCELLED, REFUNDED
 *
 * This enum replaces the dual PaymentStatus/InvoiceStatus system to eliminate
 * redundancy and confusion. All invoice lifecycle and payment tracking now uses
 * this single source of truth.
 */
@Getter
public enum InvoiceStatus {
    // ========================
    // CORE JOURNEY (6 states)
    // ========================

    /**
     * Invoice being prepared - fully editable, deletable
     */
    DRAFT("Draft", "Invoice is being prepared"),

    /**
     * Invoice sent to customer - read-only
     */
    SENT("Sent", "Invoice sent to customer"),

    /**
     * Customer has viewed the invoice - read-only
     */
    VIEWED("Viewed", "Customer has viewed the invoice"),

    /**
     * Partial payment received - payment tracking active
     */
    PARTIALLY_PAID("Partially Paid", "Partial payment received"),

    /**
     * Fully paid - read-only
     */
    PAID("Paid", "Fully paid"),

    /**
     * Payment is past due date - can transition to PARTIALLY_PAID or PAID
     */
    OVERDUE("Overdue", "Payment is overdue"),

    // ========================
    // EXCEPTION STATES (2 states)
    // ========================

    /**
     * Invoice cancelled - read-only
     */
    CANCELLED("Cancelled", "Invoice cancelled"),

    /**
     * Invoice refunded (partial or full) - read-only
     */
    REFUNDED("Refunded", "Invoice refunded");

    private final String displayName;
    private final String description;

    InvoiceStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Check if invoice is editable in this state
     * @return true if invoice can be edited
     */
    public boolean isEditable() {
        return this == DRAFT || this == SENT || this == VIEWED;
    }

    /**
     * Check if invoice is deletable in this state
     * @return true if invoice can be deleted
     */
    public boolean isDeletable() {
        return this == DRAFT;
    }

    /**
     * Check if this is a payment-related state
     * @return true if state represents payment status
     */
    public boolean isPaymentState() {
        return this == PARTIALLY_PAID || this == PAID || this == OVERDUE;
    }

    /**
     * Check if this is a final state (no further transitions expected)
     * @return true if invoice is in a terminal state
     */
    public boolean isFinalState() {
        return this == PAID || this == CANCELLED || this == REFUNDED;
    }

    /**
     * Check if invoice can transition to the target state
     * @param targetState The desired target state
     * @return true if transition is allowed
     */
    public boolean canTransitionTo(InvoiceStatus targetState) {
        switch (this) {
            case DRAFT:
                return targetState == SENT || targetState == CANCELLED;
            case SENT:
                return targetState == VIEWED || targetState == PARTIALLY_PAID ||
                       targetState == PAID || targetState == OVERDUE || targetState == CANCELLED;
            case VIEWED:
                return targetState == PARTIALLY_PAID || targetState == PAID ||
                       targetState == OVERDUE || targetState == CANCELLED;
            case PARTIALLY_PAID:
                return targetState == PAID || targetState == OVERDUE || targetState == REFUNDED;
            case OVERDUE:
                return targetState == PARTIALLY_PAID || targetState == PAID ||
                       targetState == CANCELLED || targetState == REFUNDED;
            case PAID:
                return targetState == REFUNDED;
            case CANCELLED:
                return targetState == REFUNDED;
            case REFUNDED:
                return false; // Terminal state
            default:
                return false;
        }
    }

    /**
     * Get all states that represent unpaid invoices
     * @return true if invoice is not yet fully paid
     */
    public boolean isUnpaid() {
        return this == DRAFT || this == SENT || this == VIEWED ||
               this == PARTIALLY_PAID || this == OVERDUE;
    }
}
