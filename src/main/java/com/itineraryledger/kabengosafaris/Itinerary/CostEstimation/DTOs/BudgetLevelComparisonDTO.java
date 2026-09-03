package com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs;

import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.BudgetLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What this trip would cost at each of the three levels, and what adopting one would change.
 *
 * <p>Answers the question the office is actually asked -- "what is this in luxury, and what is the
 * cheapest you can do it for" -- without touching the record. Before this, answering it meant
 * editing the days, returning to Cost, recalculating, reading the number, and doing it again for
 * the next level: three mutations of a shared product to answer one question about one client.
 *
 * <p>These totals are hypothetical and are reported apart from the itinerary's own. The number on
 * the record stays the cost of what is primary NOW; a level total says what it would become.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetLevelComparisonDTO {

    private String itineraryId;
    private String itineraryName;
    private LocalDate startDate;

    /** The itinerary's current badge, so a column can say it disagrees with the beds. */
    private String currentBudgetCategory;

    /** What it costs as it stands, per currency, so a level's delta has something to be against. */
    private List<CurrencyGroupedCostDTO> currentTotalsByCurrency = new ArrayList<>();

    private Integer overnightNights;

    /**
     * Nights with more than one option recorded.
     *
     * <p>The honest headline of the whole screen. Where this is 0 the three columns are identical,
     * and that means "nobody has recorded any alternatives", not "the levels cost the same".
     */
    private Integer nightsWithAChoice;

    private List<LevelDTO> levels = new ArrayList<>();

    /** Nights that could not be ranked at all, with the reason, rather than ranked wrongly. */
    private List<String> warnings = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LevelDTO {

        private BudgetLevel level;
        private String label;
        private String description;

        /** The whole trip at this level: what is priced now, with each night's bed swapped. */
        private List<CurrencyGroupedCostDTO> totalsByCurrency = new ArrayList<>();

        /** Signed, per currency, against the current total. Positive means dearer. */
        private Map<String, BigDecimal> deltaSto = new LinkedHashMap<>();
        private Map<String, BigDecimal> deltaRack = new LinkedHashMap<>();

        /** The badge the itinerary would carry, read off the beds this level actually adopts. */
        private String badge;

        /** How many of each lodge category this level picked, so "High" can be checked. */
        private Map<String, Integer> categoryMix = new LinkedHashMap<>();

        private Integer nightsChanged;
        private Integer nightsAlreadyThere;
        private Integer nightsWithoutAChoice;

        private List<NightPickDTO> nights = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NightPickDTO {

        private String dayId;
        private Integer dayNumber;
        private LocalDate date;
        private String dayTitle;

        /** The day-row to promote. Not the accommodation id: one lodge can appear on four nights. */
        private String entryId;

        private String accommodationName;
        private String roomTypeName;

        /** The lodge's own label, for the audit, never for the choice. */
        private String category;

        private BigDecimal sto;
        private BigDecimal rack;
        private String currency;

        /** Against this night's current bed. Withheld across currencies. */
        private BigDecimal deltaSto;
        private BigDecimal deltaRack;

        private Boolean isCurrentPrimary;

        /** How many options this night had to choose from. One means all three levels agree. */
        private Integer optionsOnThisNight;

        /**
         * What is odd about this pick, if anything.
         *
         * <p>Where the price and the label disagree -- the cheapest bed on the night calling itself
         * Ultra-Luxury -- this says so. Where the dearest option recorded is still mid-range, it
         * says that too, because the answer to "what is this in luxury" is then "you have not
         * recorded a luxury option for this night".
         */
        private String note;
    }
}
