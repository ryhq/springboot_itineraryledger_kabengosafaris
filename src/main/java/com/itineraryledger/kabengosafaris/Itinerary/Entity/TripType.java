package com.itineraryledger.kabengosafaris.Itinerary.Entity;

/**
 * Enumeration of trip types for safari itineraries
 */
public enum TripType {
    PRIVATE("Private", "Exclusive private tour for a single group"),
    GROUP("Group", "Shared tour with other travelers"),
    CUSTOM("Custom", "Customized tour based on client requirements"),
    HONEYMOON("Honeymoon", "Romantic honeymoon package"),
    FAMILY("Family", "Family-friendly tour with activities for all ages"),
    PHOTOGRAPHY("Photography", "Specialized photography safari"),
    ADVENTURE("Adventure", "Active adventure-focused safari");

    private final String displayName;
    private final String description;

    TripType(String displayName, String description) {
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
