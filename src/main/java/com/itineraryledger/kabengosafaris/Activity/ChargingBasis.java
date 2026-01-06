package com.itineraryledger.kabengosafaris.Activity;

/**
 * Enum for different charging basis types for activities
 */
public enum ChargingBasis {
    PER_PERSON("Per Person"),
    PER_VEHICLE("Per Vehicle"),
    PER_GROUP("Per Group"),
    PER_DAY("Per Day"),
    PER_HOUR("Per Hour"),
    PER_SESSION("Per Session"),
    FLAT_RATE("Flat Rate");

    private final String displayName;

    ChargingBasis(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
