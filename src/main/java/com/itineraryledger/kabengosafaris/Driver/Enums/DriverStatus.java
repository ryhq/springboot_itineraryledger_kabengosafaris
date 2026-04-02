package com.itineraryledger.kabengosafaris.Driver.Enums;

public enum DriverStatus {
    AVAILABLE("Available", "Driver is available for assignments"),
    ON_DUTY("On Duty", "Driver is currently on an assignment"),
    ON_LEAVE("On Leave", "Driver is on leave"),
    INACTIVE("Inactive", "Driver is no longer active");

    private final String displayName;
    private final String description;

    DriverStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
