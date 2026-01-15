package com.itineraryledger.kabengosafaris.Itinerary.Entity;

/**
 * Enumeration of budget categories for safari itineraries
 * Similar to AccommodationCategory - helps classify itineraries by price level
 */
public enum BudgetCategory {
    ULTRA_LUXURY("Ultra Luxury", "Exclusive ultra-luxury experience with premium lodges and private services", 5),
    LUXURY("Luxury", "High-end safari with luxury lodges and top-tier services", 4),
    MID_RANGE("Mid-Range", "Comfortable mid-range safari with quality accommodations", 3),
    BUDGET("Budget", "Affordable safari experience with basic but clean accommodations", 2),
    BACKPACKER("Backpacker", "Budget-friendly option for backpackers and budget travelers", 1);

    private final String displayName;
    private final String description;
    private final int tier; // 1-5 tier level for sorting/filtering

    BudgetCategory(String displayName, String description, int tier) {
        this.displayName = displayName;
        this.description = description;
        this.tier = tier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getTier() {
        return tier;
    }
}
