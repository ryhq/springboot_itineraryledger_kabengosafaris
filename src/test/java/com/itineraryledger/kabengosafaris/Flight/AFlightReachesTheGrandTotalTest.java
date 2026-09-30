package com.itineraryledger.kabengosafaris.Flight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.CurrencyGroupedCostDTO;

/**
 * A flight has to reach the total, not just the day it sits on.
 *
 * <p>Found while quoting a twelve-day trip with one Kogatende to Zanzibar sector. The day showed
 * the flight at its full 1,548.38 and the grand total came back 1,548.38 lower than the sum of its
 * own line items. Every other cost type was rolled up from the day into the grand total;
 * {@code addFlightCost} was called on the day and never on the total, in all four aggregators.
 *
 * <p>This is the worst shape a pricing bug can take: the number is visible, it looks deliberate,
 * and the figure printed underneath it quietly disagrees. Nobody re-adds a column they can see.
 */
class AFlightReachesTheGrandTotalTest {

    private static final List<Path> AGGREGATORS = List.of(
        Path.of("src/main/java/com/itineraryledger/kabengosafaris/Itinerary/CostEstimation/Services/Aggregators/PerDayCostAggregator.java"),
        Path.of("src/main/java/com/itineraryledger/kabengosafaris/Itinerary/CostEstimation/Services/Aggregators/PerPaxCostAggregator.java"),
        Path.of("src/main/java/com/itineraryledger/kabengosafaris/Safari/CostEstimation/Services/Aggregators/SafariPerDayCostAggregator.java"),
        Path.of("src/main/java/com/itineraryledger/kabengosafaris/Safari/CostEstimation/Services/Aggregators/SafariPerPaxCostAggregator.java"));

    @Test
    @DisplayName("the grand total adds the flight as well as everything else")
    void theTotalIncludesTheFlight() {
        CurrencyGroupedCostDTO totals = CurrencyGroupedCostDTO.builder().currency("USD").build();
        totals.addAccommodationCost(new BigDecimal("100"), new BigDecimal("130"));
        totals.addParkFeeCost(new BigDecimal("50"), new BigDecimal("65"));
        totals.addActivityCost(new BigDecimal("200"), new BigDecimal("260"));
        totals.addFlightCost(new BigDecimal("400"), new BigDecimal("520"));
        totals.calculateGrandTotals();

        assertEquals(0, new BigDecimal("750").compareTo(totals.getGrandTotalSto()),
            "STO total must carry the flight: " + totals.getGrandTotalSto());
        assertEquals(0, new BigDecimal("975").compareTo(totals.getGrandTotalRack()),
            "rack total must carry the flight: " + totals.getGrandTotalRack());
    }

    @Test
    @DisplayName("every aggregator rolls the flight up, not only three of the four cost types")
    void everyAggregatorRollsTheFlightUp() throws IOException {
        for (Path p : AGGREGATORS) {
            String src = Files.readString(p);
            assertTrue(src.contains("grandTotal.addFlightCost("),
                p.getFileName() + " rolls accommodation, park fees and activities into the grand "
                    + "total but not the flight, so a sector shows on the day and vanishes from the total");
        }
    }
}
