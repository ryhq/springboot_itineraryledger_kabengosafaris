package com.itineraryledger.kabengosafaris.Flight;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.CurrencyGroupedCostDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.CostItemType;

/**
 * A cost type nothing totals is worse than a cost type nobody added.
 *
 * <p>The aggregators sum a day by an if/else chain over {@link CostItemType}, and the chain has no
 * else. A value with no branch therefore produces a line item that renders on the day and is absent
 * from the day's total: a figure on screen the sum does not agree with, and nothing anywhere saying
 * why. Adding FLIGHT to the enum was half a change; this holds the other half in place.
 *
 * <p>Checked against every aggregator, not only the itinerary's — the Safari ones import the same
 * enum and the same DTO, so one forgotten branch there fails the same way on a safari.
 */
class EveryCostTypeReachesTheTotalTest {

    private static final List<Path> AGGREGATORS = List.of(
        Path.of("src/main/java/com/itineraryledger/kabengosafaris/Itinerary/CostEstimation/Services/Aggregators/PerDayCostAggregator.java"),
        Path.of("src/main/java/com/itineraryledger/kabengosafaris/Itinerary/CostEstimation/Services/Aggregators/PerPaxCostAggregator.java"),
        Path.of("src/main/java/com/itineraryledger/kabengosafaris/Safari/CostEstimation/Services/Aggregators/SafariPerDayCostAggregator.java"),
        Path.of("src/main/java/com/itineraryledger/kabengosafaris/Safari/CostEstimation/Services/Aggregators/SafariPerPaxCostAggregator.java"));

    @Test
    @DisplayName("every cost type has a branch in every aggregator")
    void noTypeIsSilentlyDropped() throws IOException {
        List<String> offences = new ArrayList<>();

        for (Path aggregator : AGGREGATORS) {
            if (!Files.exists(aggregator)) {
                offences.add(aggregator.getFileName() + " has moved — this test can no longer see it");
                continue;
            }
            String source = Files.readString(aggregator);
            for (CostItemType type : CostItemType.values()) {
                if (!source.contains("CostItemType." + type.name())) {
                    offences.add(aggregator.getFileName() + " never mentions CostItemType." + type.name()
                        + ", so a " + type.getDisplayName() + " line shows on the day and is missing "
                        + "from the total");
                }
            }
        }

        assertTrue(offences.isEmpty(), () -> String.join("\n", offences));
    }

    @Test
    @DisplayName("the totals DTO can actually hold every type")
    void theDtoHasSomewhereToPutEachType() {
        CurrencyGroupedCostDTO totals = CurrencyGroupedCostDTO.builder().currency("USD").build();

        totals.addAccommodationCost(new BigDecimal("100"), new BigDecimal("130"));
        totals.addParkFeeCost(new BigDecimal("50"), new BigDecimal("65"));
        totals.addActivityCost(new BigDecimal("25"), new BigDecimal("32.50"));
        totals.addFlightCost(new BigDecimal("364"), new BigDecimal("364"));

        assertEquals(new BigDecimal("364"), totals.getFlightsSto());
        /*
         * Equal on purpose. A flight carries no rack multiplier — the house 1.3 is an accommodation
         * rule, and a 316 fare marked up 30% is 411 for a seat the airline publishes at 316 — so the
         * markup is already inside both figures and they agree.
         */
        assertEquals(totals.getFlightsSto(), totals.getFlightsRack(),
            "a flight has no rack multiplier, so its two totals are the same number");
    }

    @Test
    @DisplayName("every category actually moves the grand total")
    void everyCategoryFeedsTheGrandTotal() {
        /*
         * Found in production, with the first flight ever attached to a real trip. The day showed a
         * 764.16 flight line, flightsSto agreed with it, and grandTotalSto was the figure from
         * before the flight existed — because calculateGrandTotals() named three categories and
         * flights were the fourth.
         *
         * Adding a category is three edits — the field, the adder, and the sum — and nothing but
         * this test holds them together. Each category is checked on its own, so the failure
         * message names the one that was forgotten.
         */
        record Category(String name, java.util.function.BiConsumer<CurrencyGroupedCostDTO, BigDecimal> add) {}

        List<Category> categories = List.of(
            new Category("accommodation", (t, v) -> t.addAccommodationCost(v, v)),
            new Category("park fees", (t, v) -> t.addParkFeeCost(v, v)),
            new Category("activities", (t, v) -> t.addActivityCost(v, v)),
            new Category("flights", (t, v) -> t.addFlightCost(v, v)));

        for (Category category : categories) {
            CurrencyGroupedCostDTO totals = CurrencyGroupedCostDTO.builder().currency("USD").build();
            category.add().accept(totals, new BigDecimal("100"));
            totals.calculateGrandTotals();

            assertEquals(0, totals.getGrandTotalSto().compareTo(new BigDecimal("100")),
                () -> category.name() + " does not reach grandTotalSto — the line shows on the day "
                    + "and the total disagrees with it");
            assertEquals(0, totals.getGrandTotalRack().compareTo(new BigDecimal("100")),
                () -> category.name() + " does not reach grandTotalRack");
        }
    }

    @Test
    @DisplayName("FLIGHT is its own category, not folded into activities")
    void flightIsNotAnActivity() {
        CurrencyGroupedCostDTO totals = CurrencyGroupedCostDTO.builder().currency("USD").build();
        totals.addFlightCost(new BigDecimal("364"), new BigDecimal("364"));

        assertEquals(0, totals.getActivitiesSto().signum(),
            "a flight is the one line a client can price-check against a public timetable, and an "
            + "operator reconciling an airline invoice needs it on its own");
        assertNotNull(CostItemType.valueOf("FLIGHT"));
    }
}
