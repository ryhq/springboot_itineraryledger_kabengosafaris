package com.itineraryledger.kabengosafaris.Safari.Enums;

/**
 * SafariPhase - Represents the TIME-BASED phase of a Safari
 *
 * This is automatically computed based on the safari's start and end dates
 * relative to the current date. These phases are calculated, not manually set.
 *
 * For booking/operational state tracking, see SafariState enum.
 */
public enum SafariPhase {
    // ========================
    // PRE-SAFARI PHASES
    // ========================
    FAR_FUTURE("Far Future", "Safari starts in more than 30 days", 30, Integer.MAX_VALUE),
    UPCOMING("Upcoming", "Safari starts in 8-30 days", 8, 30),
    STARTING_SOON("Starting Soon", "Safari starts in 3-7 days", 3, 7),
    IMMINENT("Imminent", "Safari starts in 1-2 days", 1, 2),
    TODAY("Starting Today", "Safari starts today", 0, 0),

    // ========================
    // ACTIVE PHASES
    // ========================
    DAY_ONE("Day One", "First day of the safari", -1, -1),  // Special: calculated by day number
    EARLY_DAYS("Early Days", "Safari in early days (days 2-3)", -1, -1),
    MID_SAFARI("Mid Safari", "Safari in middle days", -1, -1),
    FINAL_DAYS("Final Days", "Safari in final days", -1, -1),
    LAST_DAY("Last Day", "Final day of the safari", -1, -1),

    // ========================
    // POST-SAFARI PHASES
    // ========================
    JUST_ENDED("Just Ended", "Safari ended within last 7 days", -7, -1),
    RECENTLY_ENDED("Recently Ended", "Safari ended 8-30 days ago", -30, -8),
    PAST("Past", "Safari ended more than 30 days ago", Integer.MIN_VALUE, -31);

    private final String displayName;
    private final String description;
    private final int daysFromStartMin;  // Negative means days after end
    private final int daysFromStartMax;

    SafariPhase(String displayName, String description, int daysFromStartMin, int daysFromStartMax) {
        this.displayName = displayName;
        this.description = description;
        this.daysFromStartMin = daysFromStartMin;
        this.daysFromStartMax = daysFromStartMax;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getDaysFromStartMin() {
        return daysFromStartMin;
    }

    public int getDaysFromStartMax() {
        return daysFromStartMax;
    }

    /**
     * Check if this phase is before the safari starts
     */
    public boolean isPreSafari() {
        return this == FAR_FUTURE || this == UPCOMING || this == STARTING_SOON ||
               this == IMMINENT || this == TODAY;
    }

    /**
     * Check if this phase is during the safari
     */
    public boolean isDuringSafari() {
        return this == DAY_ONE || this == EARLY_DAYS || this == MID_SAFARI ||
               this == FINAL_DAYS || this == LAST_DAY;
    }

    /**
     * Check if this phase is after the safari ends
     */
    public boolean isPostSafari() {
        return this == JUST_ENDED || this == RECENTLY_ENDED || this == PAST;
    }

    /**
     * Check if action is urgently required
     */
    public boolean isUrgent() {
        return this == STARTING_SOON || this == IMMINENT || this == TODAY;
    }

    /**
     * Check if this is within the active safari window
     */
    public boolean isActive() {
        return isDuringSafari();
    }

    /**
     * Get urgency level (1-5, 5 being most urgent)
     */
    public int getUrgencyLevel() {
        return switch (this) {
            case FAR_FUTURE -> 1;
            case UPCOMING -> 2;
            case STARTING_SOON -> 3;
            case IMMINENT -> 4;
            case TODAY, DAY_ONE -> 5;
            case EARLY_DAYS, MID_SAFARI, FINAL_DAYS, LAST_DAY -> 4;
            case JUST_ENDED -> 3;
            case RECENTLY_ENDED -> 2;
            case PAST -> 1;
        };
    }

    /**
     * Get color code for UI display (CSS color names or hex)
     */
    public String getColorCode() {
        return switch (this) {
            case FAR_FUTURE -> "gray";
            case UPCOMING -> "blue";
            case STARTING_SOON -> "orange";
            case IMMINENT -> "red";
            case TODAY -> "green";
            case DAY_ONE, EARLY_DAYS, MID_SAFARI, FINAL_DAYS, LAST_DAY -> "green";
            case JUST_ENDED -> "teal";
            case RECENTLY_ENDED -> "gray";
            case PAST -> "lightgray";
        };
    }
}
