package com.itineraryledger.kabengosafaris.Flight.Enums;

/**
 * How a flight markup is expressed.
 *
 * <p>Two shapes because the office genuinely uses both: a percentage on a long expensive sector, and
 * a flat handling fee on a short cheap one where a percentage would not cover the work of issuing
 * the ticket. "Air Excel need 300 per person, add 13% or add 50" — both of those, not one of them.
 *
 * <p>Whichever is used, it applies <strong>per person</strong> and <strong>to the fare only</strong>.
 * Taxes and fees are never marked up: they are collected on the airline's behalf and an invoice has
 * to reconcile against theirs.
 */
public enum MarkupType {

    /** A percentage of the net fare. 13 means 13%. */
    PERCENT("Percent", "A percentage added to the net fare, per person"),

    /** A flat amount in the fare's own currency. 50 means 50 per person. */
    AMOUNT("Fixed amount", "A flat amount added to the net fare, per person");

    private final String displayName;
    private final String description;

    MarkupType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
