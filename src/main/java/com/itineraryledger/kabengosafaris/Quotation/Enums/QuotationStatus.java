package com.itineraryledger.kabengosafaris.Quotation.Enums;

/**
 * QuotationStatus - Represents the lifecycle status of a quotation
 */
public enum QuotationStatus {
    DRAFT("Draft", "Quotation is being prepared and has not been sent to the customer"),
    SENT("Sent", "Quotation has been sent to the customer"),
    VIEWED("Viewed", "Customer has viewed the quotation"),
    ACCEPTED("Accepted", "Customer has accepted the quotation"),
    REJECTED("Rejected", "Customer has rejected the quotation"),
    EXPIRED("Expired", "Quotation validity period has passed"),
    REVISED("Revised", "Quotation has been superseded by a new version"),
    CANCELLED("Cancelled", "Quotation has been cancelled by staff");

    private final String displayName;
    private final String description;

    QuotationStatus(String displayName, String description) {
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
