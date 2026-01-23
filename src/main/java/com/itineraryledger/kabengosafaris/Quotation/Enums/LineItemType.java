package com.itineraryledger.kabengosafaris.Quotation.Enums;

/**
 * LineItemType - Represents the type of cost item in a quotation
 */
public enum LineItemType {
    ACCOMMODATION("Accommodation", "Lodging and accommodation costs"),
    PARK_FEE("Park Fee", "National park entry and conservation fees"),
    ACTIVITY("Activity", "Safari activities and excursions"),
    TRANSPORT("Transport", "Ground transportation and transfers"),
    GUIDE("Guide", "Guide and driver services"),
    MEAL("Meal", "Meals not included in accommodation"),
    FLIGHT("Flight", "Domestic or charter flight"),
    VISA("Visa", "Visa and travel document fees"),
    INSURANCE("Insurance", "Travel insurance"),
    OTHER("Other", "Miscellaneous items");

    private final String displayName;
    private final String description;

    LineItemType(String displayName, String description) {
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
