package com.itineraryledger.kabengosafaris.Invoice.Enums;

import lombok.Getter;

/**
 * Invoice workflow states - simplified 6-state system
 *
 * Core Journey:
 *   DRAFT → SENT → PARTIALLY_PAID → PAID
 *                        ↕
 *                     OVERDUE
 *
 * Exception State:
 *   CANCELLED (from any non-PAID state)
 */
@Getter
public enum InvoiceStatus {
    // ========================
    // CORE JOURNEY
    // ========================

    DRAFT("Draft", "Invoice is being prepared"),
    SENT("Sent", "Invoice sent to customer"),
    PARTIALLY_PAID("Partially Paid", "Partial payment received"),
    PAID("Paid", "Fully paid"),
    OVERDUE("Overdue", "Payment is overdue"),

    // ========================
    // EXCEPTION STATE
    // ========================

    CANCELLED("Cancelled", "Invoice cancelled");

    private final String displayName;
    private final String description;

    InvoiceStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public boolean isEditable() {
        return this == DRAFT;
    }

    public boolean isDeletable() {
        return this == DRAFT;
    }

    public boolean isPaymentState() {
        return this == PARTIALLY_PAID || this == PAID || this == OVERDUE;
    }

    public boolean isFinalState() {
        return this == PAID || this == CANCELLED;
    }

    public boolean canTransitionTo(InvoiceStatus targetState) {
        switch (this) {
            case DRAFT:
                return targetState == SENT || targetState == CANCELLED;
            case SENT:
                return targetState == PARTIALLY_PAID || targetState == PAID ||
                       targetState == OVERDUE || targetState == CANCELLED;
            case PARTIALLY_PAID:
                return targetState == PAID || targetState == OVERDUE || targetState == CANCELLED;
            case OVERDUE:
                return targetState == PARTIALLY_PAID || targetState == PAID || targetState == CANCELLED;
            case PAID:
            case CANCELLED:
                return false;
            default:
                return false;
        }
    }

    public boolean isUnpaid() {
        return this == DRAFT || this == SENT || this == PARTIALLY_PAID || this == OVERDUE;
    }
}
