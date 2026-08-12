package com.itineraryledger.kabengosafaris.Expense.Enums;

import lombok.Getter;

/**
 * The kinds of thing on a safari day that a bill can be paying for.
 *
 * Four, because four different records charge money: a night somewhere, entry to
 * a park, an activity inside a park, and an activity that belongs to the day
 * itself rather than to any park (the fuel for a game drive, a guide's
 * allowance).
 */
@Getter
public enum ExpenseSubjectType {

    ACCOMMODATION("Stay", "A night at a property, on one day of the trip"),
    PARK("Park fee", "Entry, conservation or concession charges for one park visit"),
    PARK_ACTIVITY("Activity in a park", "Something done inside a park on one day"),
    DAY_ACTIVITY("Day activity", "An activity tied to the day rather than to a park");

    private final String displayName;
    private final String description;

    ExpenseSubjectType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
