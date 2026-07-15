package com.itineraryledger.kabengosafaris.Itinerary.Entity;

/**
 * Experiences a client can express interest in via the "Plan Your Safari" planner.
 * Multi-select on a booking inquiry (a client may want several).
 */
public enum TripInterest {
    SAFARI("Safari", "Classic wildlife game-drive safari across Tanzania's national parks"),
    GREAT_MIGRATION("Great Migration", "Witness the wildebeest migration in the Serengeti"),
    ZANZIBAR_BEACH("Beach & Zanzibar", "Indian Ocean beach holiday on Zanzibar"),
    KILIMANJARO("Climb Kilimanjaro", "Trek Mount Kilimanjaro, Africa's highest peak"),
    CULTURE("Culture & People", "Cultural experiences with local communities");

    private final String displayName;
    private final String description;

    TripInterest(String displayName, String description) {
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
