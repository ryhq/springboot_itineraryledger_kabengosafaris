package com.itineraryledger.kabengosafaris.Invoice.Enums;

import lombok.Getter;

/**
 * Payment status enum for Invoice payment tracking
 */
@Getter
public enum PaymentStatus {
    UNPAID("Unpaid", "No payment received"),
    PARTIALLY_PAID("Partially Paid", "Partial payment received"),
    PAID("Paid", "Fully paid"),
    REFUNDED("Refunded", "Payment refunded"),
    OVERDUE("Overdue", "Payment is overdue");

    private final String displayName;
    private final String description;

    PaymentStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
