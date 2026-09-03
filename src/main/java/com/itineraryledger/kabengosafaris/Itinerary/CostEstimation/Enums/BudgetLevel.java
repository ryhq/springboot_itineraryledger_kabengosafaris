package com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationCategory;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.BudgetCategory;

/**
 * The three levels a trip can be quoted at.
 *
 * <p>A level is decided by MONEY, not by a lodge's category. The category is a label somebody
 * typed: it can be wrong, and a hotel can carry a high price while calling itself mid-range. What
 * the client pays is the thing that decides whether they say yes, so each night's recorded options
 * are ranked by what that night actually costs and the cheapest, the middle and the dearest become
 * Lowest, Medium and High.
 *
 * <p>Ranked on the night's TOTAL for the party, never on a unit price: some rates are quoted per
 * room and some per person sharing, and comparing those two unit prices ranks them backwards.
 *
 * <p>The category still earns its keep twice. It audits the data, because a night whose cheapest
 * option is labelled ULTRA_LUXURY is either a mislabelled lodge or a genuine bargain and both are
 * worth knowing. And it names what a column actually got, so "High" can admit that the dearest
 * option recorded for a night is still a mid-range camp, which is a prompt to go and record a
 * better one rather than a silent shrug.
 */
public enum BudgetLevel {

    /** The dearest option recorded for each night. */
    HIGH("High range", "The dearest option recorded for each night"),

    /** The option nearest the middle of each night's prices, ties to the cheaper. */
    MEDIUM("Medium", "The middle option for each night"),

    /** The cheapest option recorded for each night. */
    LOWEST("Lowest", "The cheapest option recorded for each night");

    private final String label;
    private final String description;

    BudgetLevel(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    /**
     * The badge an itinerary should carry, given the category of one bed that was adopted.
     *
     * <p>Exhaustive on purpose, with no default branch: {@link AccommodationCategory} has six
     * values and {@link BudgetCategory} five, and a category that fell through a default would
     * quietly leave the header describing the previous beds. PREMIUM has no counterpart of its own
     * and reads as LUXURY, which is the nearer of the two neighbours it sits between.
     */
    public static BudgetCategory badgeFor(AccommodationCategory category) {
        return switch (category) {
            case ULTRA_LUXURY -> BudgetCategory.ULTRA_LUXURY;
            case LUXURY, PREMIUM -> BudgetCategory.LUXURY;
            case MID_RANGE -> BudgetCategory.MID_RANGE;
            case BUDGET -> BudgetCategory.BUDGET;
            case BACKPACKER -> BudgetCategory.BACKPACKER;
        };
    }

    /** How dear a category claims to be, so a tie between two of them can pick the dearer. */
    public static int rankOf(AccommodationCategory category) {
        return switch (category) {
            case ULTRA_LUXURY -> 5;
            case LUXURY -> 4;
            case PREMIUM -> 3;
            case MID_RANGE -> 2;
            case BUDGET -> 1;
            case BACKPACKER -> 0;
        };
    }
}
