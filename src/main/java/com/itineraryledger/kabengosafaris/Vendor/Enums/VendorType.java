package com.itineraryledger.kabengosafaris.Vendor.Enums;

/**
 * VendorType — categorises external parties we PAY (not customers we bill).
 * Used by the Expense module to bucket spend by vendor kind.
 */
public enum VendorType {
    LODGE("Lodge", "Hotel, lodge, camp or other accommodation supplier"),
    FUEL_STATION("Fuel station", "Petrol or diesel supplier"),
    MECHANIC("Mechanic", "Vehicle repair, maintenance or servicing"),
    TRANSPORTER("Transporter", "External transport / shuttle provider"),
    PARK_AUTHORITY("Park authority", "National park or conservation authority"),
    RENTAL_COMPANY("Rental company", "Vehicle rental or hire firm"),
    GUIDE("Guide", "External freelance or contracted guide"),
    LANDLORD("Landlord", "Property owner for rented premises"),
    INSURER("Insurer", "Insurance provider"),
    UTILITY("Utility", "Water, electricity, internet, telephony"),
    GOVERNMENT("Government", "Tax authority, immigration, licensing fees"),
    OTHER("Other", "Any other vendor that does not fit a specific category");

    private final String displayName;
    private final String description;

    VendorType(String displayName, String description) {
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
