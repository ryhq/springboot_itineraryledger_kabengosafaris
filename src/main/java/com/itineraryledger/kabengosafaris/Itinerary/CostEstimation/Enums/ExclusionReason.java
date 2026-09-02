package com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums;

/**
 * Why a priced line is shown but not counted.
 *
 * The estimator used to drop these lines entirely, in three places, one per calculator. That made
 * the alternatives an office had carefully recorded invisible: the only way to learn what a
 * different lodge did to a trip was to make it primary on the Days tab, come back to Cost,
 * recalculate, and read the new number.
 *
 * They are priced now and kept apart from the totals. The reason travels with the line because
 * "not in the total" means three different things to the person reading it: a bed nobody chose
 * yet, an activity offered as an extra, and a fee somebody deliberately took off the price.
 */
public enum ExclusionReason {

    /** A candidate bed for a night that already has one. Priced so it can be compared. */
    ALTERNATIVE_ACCOMMODATION("Alternative", "A candidate for this night, not the one booked"),

    /** Offered to the client as an extra, so it is quoted separately or paid locally. */
    OPTIONAL_ACTIVITY("Optional", "Offered as an extra, not part of the trip price"),

    /** Somebody turned it off. A fee excluded on purpose, or an activity marked not included. */
    NOT_INCLUDED_IN_PRICE("Not included", "Deliberately left out of the trip price"),

    /**
     * A park fee waived on a running safari -- a comp, a resident exemption, a park's own goodwill.
     *
     * Safari-only: an itinerary is a product, and nothing is waived on a product. The estimator
     * used to read the flag nowhere at all, so a waived fee was still charged; it is a line to
     * show with its reason, not one to drop, because somebody has to be able to see what was
     * given away.
     */
    WAIVED_FEE("Waived", "Waived on this safari, so nobody is charged for it");

    private final String label;
    private final String description;

    ExclusionReason(String label, String description) {
        this.label = label;
        this.description = description;
    }

    /** Short enough for a chip beside the line. */
    public String getLabel() {
        return label;
    }

    /** A sentence for a tooltip, so the chip does not have to carry the whole meaning. */
    public String getDescription() {
        return description;
    }
}
