package com.itineraryledger.kabengosafaris.Invoice.Enums;

import lombok.Getter;

/**
 * Type enum for InvoiceLineItem to categorize different kinds of items in an invoice
 */
@Getter
public enum InvoiceItemType {
    ACCOMMODATION("Accommodation", "Hotel, lodge, camp, or other accommodation"),
    PARK_FEE("Park Fee", "National park entrance fees and conservation fees"),
    ACTIVITY("Activity", "Safari activities like game drives, balloon rides, etc."),
    TRANSPORT("Transport", "Vehicle rental, transfers, flights"),
    GUIDE("Guide", "Tour guide services"),
    MEALS("Meals", "Breakfast, lunch, dinner"),
    EQUIPMENT("Equipment", "Camping gear, binoculars, etc."),
    INSURANCE("Insurance", "Travel insurance, medical insurance"),
    VISA("Visa", "Visa processing fees"),
    OTHER("Other", "Miscellaneous items");

    private final String displayName;
    private final String description;

    InvoiceItemType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
