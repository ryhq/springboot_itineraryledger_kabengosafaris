package com.itineraryledger.kabengosafaris.Customer.Enums;

/**
 * CustomerSource - Defines how the customer found the business
 *
 * Used for tracking marketing effectiveness and customer acquisition channels.
 */
public enum CustomerSource {
    WEBSITE("Website", "Found through company website"),
    REFERRAL("Referral", "Referred by existing customer or partner"),
    AGENT("Agent", "Through travel agent or partner"),
    SOCIAL_MEDIA("Social Media", "Through social media platforms"),
    TRADE_SHOW("Trade Show", "Met at trade show or exhibition"),
    REPEAT("Repeat Customer", "Returning customer from previous booking"),
    SEARCH_ENGINE("Search Engine", "Found via Google, Bing, etc."),
    ADVERTISEMENT("Advertisement", "Through paid advertising"),
    OTHER("Other", "Other acquisition channel");

    private final String displayName;
    private final String description;

    CustomerSource(String displayName, String description) {
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
