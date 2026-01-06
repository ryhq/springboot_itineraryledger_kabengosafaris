package com.itineraryledger.kabengosafaris.Accommodation.Entities;

/**
 * Enumeration of accommodation categories based on price and service level
 */
public enum AccommodationCategory {
    LUXURY("Luxury", "High-end luxury accommodation with premium services", 5),
    PREMIUM("Premium", "Upscale accommodation with excellent facilities", 4),
    MID_RANGE("Mid-Range", "Comfortable mid-range accommodation", 3),
    BUDGET("Budget", "Affordable budget-friendly accommodation", 2),
    BACKPACKER("Backpacker", "Basic backpacker/hostel accommodation", 1),
    ULTRA_LUXURY("Ultra Luxury", "Exclusive ultra-luxury accommodation", 5);

    private final String displayName;
    private final String description;
    private final int approximateStars;

    AccommodationCategory(String displayName, String description, int approximateStars) {
        this.displayName = displayName;
        this.description = description;
        this.approximateStars = approximateStars;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getApproximateStars() {
        return approximateStars;
    }
}
