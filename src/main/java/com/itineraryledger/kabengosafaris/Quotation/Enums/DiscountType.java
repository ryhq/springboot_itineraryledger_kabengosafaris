package com.itineraryledger.kabengosafaris.Quotation.Enums;

/**
 * DiscountType - Represents how discounts are applied to quotations
 */
public enum DiscountType {
    NONE("None", "No discount applied"),
    PERCENTAGE("Percentage", "Discount as a percentage of subtotal"),
    FIXED("Fixed Amount", "Discount as a fixed monetary amount");

    private final String displayName;
    private final String description;

    DiscountType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
