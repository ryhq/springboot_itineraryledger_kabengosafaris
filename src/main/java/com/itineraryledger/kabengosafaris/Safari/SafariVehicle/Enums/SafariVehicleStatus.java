package com.itineraryledger.kabengosafaris.Safari.SafariVehicle.Enums;

public enum SafariVehicleStatus {
    ASSIGNED("Assigned", "Vehicle assigned to safari"),
    CONFIRMED("Confirmed", "Assignment confirmed"),
    IN_USE("In Use", "Vehicle currently in use for safari"),
    COMPLETED("Completed", "Safari trip completed"),
    CANCELLED("Cancelled", "Assignment cancelled");

    private final String displayName;
    private final String description;

    SafariVehicleStatus(String displayName, String description) {
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
