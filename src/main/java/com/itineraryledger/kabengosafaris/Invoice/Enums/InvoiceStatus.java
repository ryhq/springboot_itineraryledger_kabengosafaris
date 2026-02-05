package com.itineraryledger.kabengosafaris.Invoice.Enums;

import lombok.Getter;

/**
 * Status enum for Invoice workflow states
 */
@Getter
public enum InvoiceStatus {
    DRAFT("Draft", "Invoice is being prepared"),
    SENT("Sent", "Invoice sent to customer"),
    VIEWED("Viewed", "Customer has viewed the invoice"),
    PARTIALLY_PAID("Partially Paid", "Partial payment received"),
    PAID("Paid", "Fully paid"),
    OVERDUE("Overdue", "Payment is overdue"),
    CANCELLED("Cancelled", "Invoice cancelled"),
    REFUNDED("Refunded", "Invoice refunded");

    private final String displayName;
    private final String description;

    InvoiceStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
