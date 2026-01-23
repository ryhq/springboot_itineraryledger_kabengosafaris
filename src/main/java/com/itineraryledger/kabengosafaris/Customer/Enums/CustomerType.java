package com.itineraryledger.kabengosafaris.Customer.Enums;

/**
 * CustomerType - Defines the classification of customers
 *
 * Types:
 * - INDIVIDUAL: Private person booking directly
 * - CORPORATE: Company or organization
 * - TRAVEL_AGENT: Travel agency or tour operator
 */
public enum CustomerType {
    INDIVIDUAL("Individual", "Private person booking directly"),
    CORPORATE("Corporate", "Company or organization"),
    TRAVEL_AGENT("Travel Agent", "Travel agency or tour operator");

    private final String displayName;
    private final String description;

    CustomerType(String displayName, String description) {
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
