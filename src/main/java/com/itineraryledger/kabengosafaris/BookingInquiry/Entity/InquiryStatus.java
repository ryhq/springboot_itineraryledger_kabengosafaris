package com.itineraryledger.kabengosafaris.BookingInquiry.Entity;

public enum InquiryStatus {
    NEW("New", "Inquiry received, not yet reviewed"),
    CONTACTED("Contacted", "Admin has reached out to the customer"),
    QUOTED("Quoted", "A quote has been sent to the customer"),
    CONVERTED("Converted", "Inquiry converted to a booking"),
    LOST("Lost", "Customer did not proceed");

    private final String displayName;
    private final String description;

    InquiryStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
