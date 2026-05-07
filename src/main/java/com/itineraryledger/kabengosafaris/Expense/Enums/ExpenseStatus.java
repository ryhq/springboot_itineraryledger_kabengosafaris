package com.itineraryledger.kabengosafaris.Expense.Enums;

import lombok.Getter;

/**
 * Expense workflow — simple record-and-pay (no approval step).
 *
 * Core journey:
 *   DRAFT → RECORDED → PARTIALLY_PAID → PAID
 *
 * Exception state:
 *   CANCELLED (from any non-PAID state)
 *
 * Auto-transitions are driven by ExpensePayment writes/deletes.
 */
@Getter
public enum ExpenseStatus {

    DRAFT("Draft", "Expense is being prepared"),
    RECORDED("Recorded", "Expense recorded, no payments made yet"),
    PARTIALLY_PAID("Partially Paid", "Partial payment made to vendor"),
    PAID("Paid", "Fully settled"),
    CANCELLED("Cancelled", "Expense cancelled");

    private final String displayName;
    private final String description;

    ExpenseStatus(String displayName, String description) {
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
        return this == PARTIALLY_PAID || this == PAID;
    }

    public boolean isFinalState() {
        return this == PAID || this == CANCELLED;
    }

    public boolean isUnpaid() {
        return this == DRAFT || this == RECORDED || this == PARTIALLY_PAID;
    }

    public boolean canTransitionTo(ExpenseStatus target) {
        switch (this) {
            case DRAFT:
                return target == RECORDED || target == CANCELLED;
            case RECORDED:
                return target == PARTIALLY_PAID || target == PAID || target == CANCELLED;
            case PARTIALLY_PAID:
                return target == PAID || target == CANCELLED;
            case PAID:
            case CANCELLED:
                return false;
            default:
                return false;
        }
    }
}
