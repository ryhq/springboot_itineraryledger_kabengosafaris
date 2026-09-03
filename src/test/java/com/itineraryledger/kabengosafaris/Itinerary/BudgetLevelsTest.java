package com.itineraryledger.kabengosafaris.Itinerary;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationCategory;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.BudgetLevelComparisonDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.CostLineItemDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.CurrencyGroupedCostDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.DayCostDetailDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.BudgetLevel;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.CostItemType;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.ExclusionReason;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Aggregators.PerDayCostAggregator;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Levels.ItineraryBudgetLevelService;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.FullItineraryDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The level is decided by money, and the lodge's category only audits it.
 *
 * <p>A category is a label somebody typed. It can be wrong, and a hotel can be dear while calling
 * itself mid-range, so choosing "the luxury option" by label would quote whatever was mislabelled.
 * What the client pays decides; the label is then free to disagree, and a disagreement is worth
 * saying out loud rather than resolving silently.
 */
class BudgetLevelsTest {

    private static final LocalDate START = LocalDate.of(2026, 9, 3);

    // ── the rule ────────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("lowest is the cheapest, high the dearest, medium the lower middle")
    void theThreeLevelsPickByPrice() {
        Trip trip = new Trip();
        trip.night(1, "Masek", "MID_RANGE", 450, true)
            .option("Ndutu Safari Lodge", "LUXURY", 468)
            .option("Nyikani", "MID_RANGE", 400)
            .option("Ndutu Savannah", "BUDGET", 340);

        BudgetLevelComparisonDTO out = trip.compare();

        assertEquals("Ndutu Savannah", pick(out, BudgetLevel.LOWEST, 1).getAccommodationName());
        assertEquals("Ndutu Safari Lodge", pick(out, BudgetLevel.HIGH, 1).getAccommodationName());
        /* 340, 400, 450, 468 — the lower of the two middles, which is "ties to the cheaper" */
        assertEquals("Nyikani", pick(out, BudgetLevel.MEDIUM, 1).getAccommodationName());
    }

    @Test
    @DisplayName("lowest is never dearer than medium, and medium never dearer than high")
    void theTotalsAreMonotonic() {
        Trip trip = new Trip();
        trip.night(1, "Masek", "MID_RANGE", 450, true)
            .option("Ndutu Savannah", "BUDGET", 340)
            .option("Ndutu Safari Lodge", "LUXURY", 468);
        trip.night(2, "Sueños", "LUXURY", 970, true)
            .option("Mbuni Central", "MID_RANGE", 540);

        BudgetLevelComparisonDTO out = trip.compare();

        BigDecimal lowest = total(out, BudgetLevel.LOWEST);
        BigDecimal medium = total(out, BudgetLevel.MEDIUM);
        BigDecimal high = total(out, BudgetLevel.HIGH);

        assertTrue(lowest.compareTo(medium) <= 0,
            "Lowest came out dearer than Medium: " + lowest + " vs " + medium);
        assertTrue(medium.compareTo(high) <= 0,
            "Medium came out dearer than High: " + medium + " vs " + high);
    }

    @Test
    @DisplayName("ranked on the night's total, not on a unit price")
    void perRoomAndPerPersonAreComparedFairly() {
        /*
         * Ndutu Savannah quotes per ROOM and the rest per person sharing. On a two-guest night a
         * per-room 340 and a per-person 225 are 340 and 450: ranking the unit prices puts the
         * dearer bed first and "Lowest" then quotes the expensive one.
         */
        Trip trip = new Trip();
        trip.night(1, "Masek", "MID_RANGE", 450, true).unit(225)
            .option("Ndutu Savannah", "BUDGET", 340).unit(340);

        BudgetLevelComparisonDTO out = trip.compare();

        assertEquals("Ndutu Savannah", pick(out, BudgetLevel.LOWEST, 1).getAccommodationName(),
            "the cheaper NIGHT is the per-room one, even though its unit price is higher");
        assertEquals("Masek", pick(out, BudgetLevel.HIGH, 1).getAccommodationName());
    }

    // ── the category audits, it does not choose ─────────────────────────────────────────────────

    @Test
    @DisplayName("a cheap bed labelled ultra-luxury still counts as the cheapest, and says so")
    void aWrongLabelDoesNotWinAgainstThePrice() {
        Trip trip = new Trip();
        trip.night(1, "Masek", "MID_RANGE", 450, true)
            .option("Mislabelled Camp", "ULTRA_LUXURY", 200);

        BudgetLevelComparisonDTO out = trip.compare();
        BudgetLevelComparisonDTO.NightPickDTO cheapest = pick(out, BudgetLevel.LOWEST, 1);

        assertEquals("Mislabelled Camp", cheapest.getAccommodationName(),
            "the price decides the level; the label does not get a veto");
        assertNotNull(cheapest.getNote(), "a label that contradicts the price has to be surfaced");
        assertTrue(cheapest.getNote().toLowerCase().contains("labelled"),
            "the note should name the disagreement: " + cheapest.getNote());
    }

    @Test
    @DisplayName("high range admits when the dearest bed recorded is not a dear bed")
    void highDoesNotPretendALuxuryOptionExists() {
        Trip trip = new Trip();
        trip.night(1, "Budget Camp", "BUDGET", 120, true)
            .option("Another Budget Camp", "BUDGET", 150);

        BudgetLevelComparisonDTO.NightPickDTO dearest =
            pick(new Trip().copyOf(trip).compare(), BudgetLevel.HIGH, 1);

        assertNotNull(dearest.getNote(), "somebody asking for luxury has to be told there is none");
        assertTrue(dearest.getNote().contains("dearest"),
            "the note should say the dearest recorded bed is only budget: " + dearest.getNote());
    }

    // ── the awkward nights ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("a night with one bed is the same in all three columns, and is counted as such")
    void oneOptionMeansTheColumnsAgree() {
        Trip trip = new Trip();
        trip.night(1, "The only camp", "MID_RANGE", 450, true);

        BudgetLevelComparisonDTO out = trip.compare();

        assertEquals(0, out.getNightsWithAChoice(),
            "three identical columns must read as 'no alternatives recorded', not as 'the levels "
                + "cost the same'");
        for (BudgetLevel level : BudgetLevel.values()) {
            assertEquals("The only camp", pick(out, level, 1).getAccommodationName());
            assertEquals(1, pick(out, level, 1).getOptionsOnThisNight());
        }
    }

    @Test
    @DisplayName("a night priced in two currencies is not ranked, and says why")
    void mixedCurrenciesAreRefusedRatherThanSubtracted() {
        /*
         * Sumawe quotes residents in TZS and non-residents in USD on the same property, so this is
         * reachable with data already in the system. Subtracting shillings from dollars produces a
         * number and it is a lie.
         */
        Trip trip = new Trip();
        trip.night(1, "Sumawe USD", "MID_RANGE", 450, true)
            .optionIn("Sumawe TZS", "MID_RANGE", 900000, "TZS");

        BudgetLevelComparisonDTO out = trip.compare();

        assertTrue(out.getWarnings().stream().anyMatch(w -> w.contains("more than one currency")),
            "the screen has to be told this night was not ranked: " + out.getWarnings());
        for (BudgetLevel level : BudgetLevel.values()) {
            BudgetLevelComparisonDTO.NightPickDTO night = pick(out, level, 1);
            assertEquals("Sumawe USD", night.getAccommodationName(),
                "an unrankable night keeps the bed that is booked now");
            assertNull(night.getDeltaSto(), "no delta across currencies");
        }
    }

    // ── the badge ───────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("every accommodation category maps onto a badge")
    void noCategoryFallsThrough() {
        for (AccommodationCategory category : AccommodationCategory.values()) {
            assertNotNull(BudgetLevel.badgeFor(category),
                category + " has no badge, so adopting a level would leave the header describing "
                    + "the previous beds");
        }
    }

    @Test
    @DisplayName("the badge follows the beds actually adopted")
    void theBadgeIsReadOffThePicks() {
        Trip trip = new Trip();
        trip.night(1, "Mid camp", "MID_RANGE", 450, true).option("Lux camp", "LUXURY", 600);
        trip.night(2, "Mid camp two", "MID_RANGE", 400, true).option("Lux camp two", "LUXURY", 700);

        BudgetLevelComparisonDTO out = trip.compare();

        assertEquals("LUXURY", level(out, BudgetLevel.HIGH).getBadge());
        assertEquals("MID_RANGE", level(out, BudgetLevel.LOWEST).getBadge());
    }

    // ── the current total is left alone ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("working out the levels does not change what the trip costs now")
    void theRecordsOwnTotalIsUntouched() {
        Trip trip = new Trip();
        trip.night(1, "Masek", "MID_RANGE", 450, true).option("Ndutu Savannah", "BUDGET", 340);

        BudgetLevelComparisonDTO out = trip.compare();

        assertEquals(0, new BigDecimal("6160.60").compareTo(
                out.getCurrentTotalsByCurrency().get(0).getGrandTotalSto()),
            "the figure on the record is the cost of what is booked now, whatever the columns say");
    }

    // ── helpers ─────────────────────────────────────────────────────────────────────────────────

    private BudgetLevelComparisonDTO.LevelDTO level(BudgetLevelComparisonDTO out, BudgetLevel level) {
        return out.getLevels().stream().filter(l -> l.getLevel() == level).findFirst().orElseThrow();
    }

    private BudgetLevelComparisonDTO.NightPickDTO pick(
        BudgetLevelComparisonDTO out, BudgetLevel level, int dayNumber
    ) {
        return level(out, level).getNights().stream()
            .filter(n -> n.getDayNumber() == dayNumber).findFirst().orElseThrow();
    }

    private BigDecimal total(BudgetLevelComparisonDTO out, BudgetLevel level) {
        return level(out, level).getTotalsByCurrency().get(0).getGrandTotalSto();
    }

    /** A trip built a night at a time, so a test reads like the itinerary it describes. */
    private static class Trip {
        private final List<DayCostDetailDTO> days = new ArrayList<>();
        private final List<FullItineraryDTO.DayDTO> treeDays = new ArrayList<>();
        private Night last;

        Trip copyOf(Trip other) {
            days.addAll(other.days);
            treeDays.addAll(other.treeDays);
            return this;
        }

        Night night(int dayNumber, String name, String category, double sto, boolean primary) {
            last = new Night(dayNumber, name, category, sto, primary);
            days.add(last.day);
            treeDays.add(last.treeDay);
            return last;
        }

        BudgetLevelComparisonDTO compare() {
            PerDayCostAggregator aggregator = mock(PerDayCostAggregator.class);
            when(aggregator.aggregateByDay(any(), any())).thenReturn(days);
            when(aggregator.calculateGrandTotals(any())).thenReturn(List.of(
                CurrencyGroupedCostDTO.builder()
                    .currency("USD")
                    .accommodationSto(new BigDecimal("2000.00"))
                    .accommodationRack(new BigDecimal("2600.00"))
                    .grandTotalSto(new BigDecimal("6160.60"))
                    .grandTotalRack(new BigDecimal("8008.98"))
                    .build()));

            FullItineraryDTO itinerary = new FullItineraryDTO();
            itinerary.setId("iti");
            itinerary.setName("A trip");
            itinerary.setDays(treeDays);

            return new ItineraryBudgetLevelService(aggregator).compare(itinerary, START);
        }
    }

    /** One night: the bed that is booked, plus every candidate somebody recorded. */
    private static class Night {
        private final DayCostDetailDTO day;
        private final FullItineraryDTO.DayDTO treeDay;
        private int counter;
        private CostLineItemDTO lastLine;

        Night(int dayNumber, String name, String category, double sto, boolean primary) {
            day = DayCostDetailDTO.builder()
                .dayId("day" + dayNumber)
                .dayNumber(dayNumber)
                .date(START.plusDays(dayNumber - 1L))
                .lineItems(new ArrayList<>())
                .excludedLineItems(new ArrayList<>())
                .build();
            treeDay = FullItineraryDTO.DayDTO.builder()
                .id("day" + dayNumber)
                .dayNumber(dayNumber)
                .accommodations(new ArrayList<>())
                .build();
            add(name, category, sto, "USD", primary);
        }

        Night option(String name, String category, double sto) {
            add(name, category, sto, "USD", false);
            return this;
        }

        Night optionIn(String name, String category, double sto, String currency) {
            add(name, category, sto, currency, false);
            return this;
        }

        /** Override the unit price, to prove the ranking uses the night's total instead. */
        Night unit(double unitPrice) {
            lastLine.setStoUnitPrice(BigDecimal.valueOf(unitPrice));
            return this;
        }

        private void add(String name, String category, double sto, String currency, boolean primary) {
            String entryId = "row" + day.getDayNumber() + "-" + (++counter);
            CostLineItemDTO line = CostLineItemDTO.builder()
                .dayNumber(day.getDayNumber())
                .itemType(CostItemType.ACCOMMODATION)
                .itemName(name)
                .entryId(entryId)
                .stoTotalPrice(BigDecimal.valueOf(sto))
                .rackTotalPrice(BigDecimal.valueOf(sto * 1.3))
                .currency(currency)
                .build();
            if (primary) {
                day.getLineItems().add(line);
            } else {
                line.setExclusionReason(ExclusionReason.ALTERNATIVE_ACCOMMODATION);
                day.getExcludedLineItems().add(line);
            }
            lastLine = line;

            day.getExcludedLineItems();
            treeDay.getAccommodations().add(FullItineraryDTO.DayAccommodationDTO.builder()
                .id(entryId)
                .accommodationName(name)
                .accommodationCategory(category)
                .roomTypeName("Double")
                .isAlternative(!primary)
                .build());
        }
    }
}
