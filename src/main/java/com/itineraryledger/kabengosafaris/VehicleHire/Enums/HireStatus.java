package com.itineraryledger.kabengosafaris.VehicleHire.Enums;

public enum HireStatus {
    PENDING("Pending", "Hire request pending"),
    CONFIRMED("Confirmed", "Hire confirmed"),
    IN_PROGRESS("In Progress", "Vehicle currently hired out"),
    COMPLETED("Completed", "Hire completed"),
    CANCELLED("Cancelled", "Hire cancelled");

    private final String displayName;
    private final String description;

    HireStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
