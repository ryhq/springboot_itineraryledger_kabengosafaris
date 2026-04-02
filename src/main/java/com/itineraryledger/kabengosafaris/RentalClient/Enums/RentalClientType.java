package com.itineraryledger.kabengosafaris.RentalClient.Enums;

public enum RentalClientType {
    INDIVIDUAL("Individual", "Individual client"),
    COMPANY("Company", "Corporate/company client");

    private final String displayName;
    private final String description;

    RentalClientType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
