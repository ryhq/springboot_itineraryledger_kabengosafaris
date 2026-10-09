package com.itineraryledger.kabengosafaris.Invoice.Enums;

import lombok.Getter;

/**
 * Invoice workflow states
 *
 * Core Journey:
 *   DRAFT → SENT → PARTIALLY_PAID → PAID
 *                        ↕
 *                     OVERDUE
 *
 * Correction State:
 *   ON_HOLD (from SENT / PARTIALLY_PAID / OVERDUE / PAID, and back again)
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
    // CORRECTION STATE
    // ========================

    /**
     * Unlocked so a mistake can be corrected on the invoice itself.
     *
     * An invoice the customer has started paying used to be frozen outright, which meant a wrong
     * figure could only be answered with a credit note: a second document, saying the first one was
     * wrong, for a customer who had not yet paid the first one. Holding the invoice lets the figure
     * be put right where the customer will look for it, and the invoice then goes back to whatever
     * its payments say it is.
     */
    ON_HOLD("On Hold", "Unlocked for correction; payments are untouched"),

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
        return this == DRAFT || this == ON_HOLD;
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

    /** A held invoice is one somebody is working on, so it is nobody's idea of a final state. */
    public boolean isHeld() {
        return this == ON_HOLD;
    }

    /** The states a hold can be taken from: it has left the building, so DRAFT is not one of them. */
    public boolean canBeHeld() {
        return this == SENT || this == PARTIALLY_PAID || this == OVERDUE || this == PAID;
    }

    public boolean canTransitionTo(InvoiceStatus targetState) {
        switch (this) {
            case DRAFT:
                return targetState == SENT || targetState == CANCELLED;
            case SENT:
                return targetState == PARTIALLY_PAID || targetState == PAID ||
                       targetState == OVERDUE || targetState == ON_HOLD || targetState == CANCELLED;
            case PARTIALLY_PAID:
                return targetState == PAID || targetState == OVERDUE ||
                       targetState == ON_HOLD || targetState == CANCELLED;
            case OVERDUE:
                return targetState == PARTIALLY_PAID || targetState == PAID ||
                       targetState == ON_HOLD || targetState == CANCELLED;
            case PAID:
                /* a paid invoice is final to everyone except the person correcting it */
                return targetState == ON_HOLD;
            case ON_HOLD:
                /* release puts it back wherever its payments say it belongs */
                return targetState == SENT || targetState == PARTIALLY_PAID ||
                       targetState == PAID || targetState == OVERDUE || targetState == CANCELLED;
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
