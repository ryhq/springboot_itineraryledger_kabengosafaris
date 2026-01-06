package com.itineraryledger.kabengosafaris.Accommodation.Entities;

/**
 * Enumeration of accommodation types in Tanzania tourism industry
 */
public enum AccommodationType {
    HOTEL("Hotel", "Traditional hotel accommodation"),
    LODGE("Lodge", "Safari lodge, typically in or near national parks"),
    TENTED_CAMP("Tented Camp", "Luxury or semi-permanent tented accommodation"),
    MOBILE_CAMP("Mobile Camp", "Seasonal mobile tented camp that moves locations"),
    GUESTHOUSE("Guesthouse", "Small guesthouse or bed & breakfast"),
    HOSTEL("Hostel", "Budget hostel accommodation"),
    RESORT("Resort", "Beach or mountain resort"),
    VILLA("Villa", "Private villa or vacation rental"),
    COTTAGE("Cottage", "Cottage or bungalow"),
    APARTMENT("Apartment", "Serviced apartment or holiday let"),
    CAMPSITE("Campsite", "Public or private camping ground"),
    BANDA("Banda", "Traditional East African thatched-roof accommodation"),
    TREE_HOUSE("Tree House", "Elevated tree house accommodation"),
    ECO_LODGE("Eco-Lodge", "Environmentally sustainable lodge"),
    BOUTIQUE_HOTEL("Boutique Hotel", "Small luxury boutique hotel"),
    OTHER("Other", "Other types of accommodation");

    private final String displayName;
    private final String description;

    AccommodationType(String displayName, String description) {
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
