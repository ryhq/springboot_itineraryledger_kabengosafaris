package com.itineraryledger.kabengosafaris.Expense.Enums;

import lombok.Getter;

/**
 * ExpenseCategory — buckets a single expense line by what we paid for.
 * Mirrors InvoiceItemType but oriented around outgoing operational costs.
 */
@Getter
public enum ExpenseCategory {
    ACCOMMODATION("Accommodation", "Lodge, hotel or camp payment"),
    FUEL("Fuel", "Petrol or diesel"),
    VEHICLE_SERVICE("Vehicle service", "Repairs, maintenance, tyres"),
    PARK_FEE("Park fee", "National park entry / conservation fees"),
    TRANSPORT("Transport", "Transfers, flights, external transporters"),
    GUIDE_FEE("Guide fee", "Freelance / contracted guide payments"),
    RENT("Rent", "Office, warehouse or accommodation rent"),
    MEALS("Meals", "Food provisions, restaurant bills"),
    EQUIPMENT("Equipment", "Camping gear, tools, supplies"),
    INSURANCE("Insurance", "Vehicle, liability, travel insurance"),
    VISA("Visa", "Visa or permit processing"),
    UTILITY("Utility", "Water, power, internet, phone"),
    SALARY("Salary", "Wage / salary payments to staff or freelancers"),
    TAX("Tax", "Tax, license or government fee"),
    OTHER("Other", "Anything else");

    private final String displayName;
    private final String description;

    ExpenseCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
