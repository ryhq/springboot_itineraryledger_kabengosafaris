package com.itineraryledger.kabengosafaris.Park;

/**
 * Enum for different types of parks in Tanzania
 */
public enum ParkType {
    NATIONAL_PARK("National Park"),
    WILDLIFE_RESERVE("Wildlife Reserve"),
    GAME_RESERVE("Game Reserve"),
    CONSERVATION_AREA("Conservation Area"),
    MARINE_PARK("Marine Park"),
    FOREST_RESERVE("Forest Reserve"),
    NATURE_RESERVE("Nature Reserve");

    private final String displayName;

    ParkType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
