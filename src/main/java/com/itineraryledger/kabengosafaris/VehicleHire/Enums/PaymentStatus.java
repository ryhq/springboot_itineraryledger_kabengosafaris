package com.itineraryledger.kabengosafaris.VehicleHire.Enums;

public enum PaymentStatus {
    UNPAID("Unpaid", "Payment not received"),
    PARTIAL("Partial", "Partial payment received"),
    PAID("Paid", "Full payment received"),
    REFUNDED("Refunded", "Payment refunded");

    private final String displayName;
    private final String description;

    PaymentStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
