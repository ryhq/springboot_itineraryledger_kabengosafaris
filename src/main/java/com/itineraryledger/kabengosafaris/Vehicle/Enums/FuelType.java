package com.itineraryledger.kabengosafaris.Vehicle.Enums;

public enum FuelType {
    DIESEL("Diesel", "Diesel fuel"),
    PETROL("Petrol", "Petrol/gasoline fuel"),
    ELECTRIC("Electric", "Electric vehicle"),
    HYBRID("Hybrid", "Hybrid electric/fuel vehicle");

    private final String displayName;
    private final String description;

    FuelType(String displayName, String description) {
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
