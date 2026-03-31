package com.itineraryledger.kabengosafaris.Vehicle.Enums;

public enum VehicleType {
    LAND_CRUISER("Land Cruiser", "Toyota Land Cruiser or similar 4x4 safari vehicle"),
    SAFARI_VAN("Safari Van", "Extended safari van with pop-up roof"),
    MINIBUS("Minibus", "Minibus for group transfers"),
    COACH("Coach", "Full-size coach for large groups"),
    SUV("SUV", "Sport utility vehicle"),
    PICKUP("Pickup", "Pickup truck for utility or small group transport"),
    OTHER("Other", "Other vehicle type");

    private final String displayName;
    private final String description;

    VehicleType(String displayName, String description) {
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
