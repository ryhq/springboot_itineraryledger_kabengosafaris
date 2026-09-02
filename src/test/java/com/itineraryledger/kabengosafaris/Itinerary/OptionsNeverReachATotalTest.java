package com.itineraryledger.kabengosafaris.Itinerary;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The one guarantee the price comparison rests on: an option is never in a total.
 *
 * The estimator used to drop alternative beds, optional activities and switched-off fees in three
 * places, one `continue` per calculator, so their prices were invisible. Answering "what would
 * Mbuni do to this trip" meant making it primary on the Days tab, returning to Cost, recalculating
 * and reading the new number.
 *
 * They are priced now, which moves the risk: the day one of these lines leaks into a figure, every
 * quote carrying a recorded alternative is silently too high. On a nine day trip with four options
 * a night that is not a rounding error, it is roughly double.
 *
 * The defence is structural rather than careful. Each calculator exposes the excluded lines through
 * a SEPARATE method, and the aggregator puts them in a SEPARATE field, so leaking one would take a
 * deliberate edit in two places rather than a moment's inattention in one.
 */
class OptionsNeverReachATotalTest {

    private static final Path CALCULATORS = Path.of("src/main/java/com/itineraryledger/"
        + "kabengosafaris/Itinerary/CostEstimation/Services/Calculators");
    private static final Path AGGREGATOR = Path.of("src/main/java/com/itineraryledger/"
        + "kabengosafaris/Itinerary/CostEstimation/Services/Aggregators/PerDayCostAggregator.java");

    private String calculator(String name) throws IOException {
        return Files.readString(CALCULATORS.resolve(name + ".java"));
    }

    @Test
    @DisplayName("the totals are summed from lineItems, and excludedLineItems is never added to them")
    void theTotalsIgnoreTheOptions() throws IOException {
        String source = Files.readString(AGGREGATOR);

        assertFalse(source.contains("allItems.addAll(excludedItems)"),
            "excluded options were added to the list the totals are summed from");
        assertTrue(source.contains("calculateTotalsByCurrency(allItems)"),
            "the day's totals must come from allItems alone");
        assertTrue(source.contains(".excludedLineItems(excludedItems)"),
            "the excluded lines belong in their own field on the day");

        int merged = source.split("allItems\\.addAll\\(excluded", -1).length - 1;
        assertEquals(0, merged, "an excluded list is being merged into the priced list");
    }

    @Test
    @DisplayName("each calculator prices its options through a second method, not the first")
    void thePricedAndTheOfferedAreSeparateEntryPoints() throws IOException {
        /*
         * Two entry points rather than one returning both. It costs a few lines and buys the
         * property that the aggregator cannot hand an option to the totals by accident: the lists
         * arrive from different calls and go to different fields.
         */
        for (String name : new String[] {
            "AccommodationCostCalculator", "ActivityCostCalculator", "ParkTariffCostCalculator"}) {
            String source = calculator(name);
            assertTrue(source.contains("public List<CostLineItemDTO> calculateForDay("),
                name + " lost the method the totals come from");
            assertTrue(source.contains("public List<CostLineItemDTO> calculateExcludedForDay("),
                name + " must expose its excluded lines separately, not inside calculateForDay");
        }
    }

    @Test
    @DisplayName("every excluded line says why, and carries the row a control can change")
    void everyOptionIsActionable() throws IOException {
        /*
         * Without a reason the screen cannot tell an unchosen bed from a deliberately excluded
         * fee. Without the row id a "make primary" button has nothing to address: itemId holds the
         * CATALOGUE id, which is identical for a lodge appearing on four nights.
         */
        for (String name : new String[] {
            "AccommodationCostCalculator", "ActivityCostCalculator", "ParkTariffCostCalculator"}) {
            String source = calculator(name);
            String excludedHalf = source.substring(source.indexOf("calculateExcludedForDay"));
            assertTrue(excludedHalf.contains("setExclusionReason("),
                name + " does not tag its excluded lines with a reason");
            assertTrue(excludedHalf.contains("setEntryId(") || source.contains(".entryId("),
                name + " does not carry the day-row id, so no control can act on the line");
        }
    }

    @Test
    @DisplayName("an alternative's delta is only reported when both sides are the same currency")
    void noCrossCurrencyDeltas() throws IOException {
        /*
         * Subtracting shillings from dollars produces a number, and it is a lie. Sumawe Suites
         * quotes residents in TZS and non-residents in USD on the same property, so this is
         * reachable with data already in the system.
         */
        String half = calculator("AccommodationCostCalculator");
        half = half.substring(half.indexOf("calculateExcludedForDay"));
        int guard = half.indexOf("getCurrency().equals(");
        int delta = half.indexOf("setDeltaVsPrimarySto(");
        assertTrue(guard > 0, "a delta must be guarded by a currency comparison");
        assertTrue(delta > guard, "the currency check has to come before the delta is set");
    }

    @Test
    @DisplayName("an alternative that sleeps outside the day's park is flagged, not silently repriced")
    void feesDoNotFollowTheBed() throws IOException {
        /*
         * Park fees hang off the park VISIT. Swap a camp inside the Serengeti for a lodge in
         * Karatu and the concession fee stays on the day, wrong by a fee per person per night. The
         * estimator cannot fix that on its own, so it must say so.
         */
        String source = calculator("AccommodationCostCalculator");
        assertTrue(source.contains("setSleepsElsewhere("),
            "an alternative must be checked against the day's sleepover park");
        assertTrue(source.contains("ParkEntryType.SLEEP_OVER"),
            "the comparison is against the park the day actually sleeps in");
    }
}
