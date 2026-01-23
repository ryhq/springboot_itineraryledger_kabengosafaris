package com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.CalculationMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for cost estimation.
 * Contains all cost data based on the calculation mode (PER_DAY or PER_PAX).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CostEstimationResponseDTO {

    // ========================
    // ITINERARY REFERENCE
    // ========================

    /**
     * Obfuscated itinerary ID
     */
    private String itineraryId;

    /**
     * Itinerary code
     */
    private String itineraryCode;

    /**
     * Itinerary name
     */
    private String itineraryName;

    /**
     * Total number of days
     */
    private Integer totalDays;

    /**
     * Total number of nights
     */
    private Integer totalNights;

    // ========================
    // DATE CONTEXT
    // ========================

    /**
     * Trip start date
     */
    private LocalDate startDate;

    /**
     * Trip end date
     */
    private LocalDate endDate;

    // ========================
    // CALCULATION CONTEXT
    // ========================

    /**
     * Calculation mode used: PER_DAY or PER_PAX
     */
    private CalculationMode calculationMode;

    /**
     * Total number of passengers
     */
    private Integer totalPax;

    /**
     * Number of vehicles
     */
    private Integer carCount;

    // ========================
    // PER-DAY MODE DATA
    // ========================

    /**
     * Cost details grouped by day.
     * Only populated when calculationMode = PER_DAY
     */
    @Builder.Default
    private List<DayCostDetailDTO> dayCostDetails = new ArrayList<>();

    // ========================
    // PER-PAX MODE DATA
    // ========================

    /**
     * Cost details grouped by pax category.
     * Only populated when calculationMode = PER_PAX
     */
    @Builder.Default
    private List<PaxCategoryCostDTO> paxCostDetails = new ArrayList<>();

    // ========================
    // SUMMARY
    // ========================

    /**
     * Grand totals grouped by currency.
     * Includes STO and Rack totals for all cost categories.
     */
    @Builder.Default
    private List<CurrencyGroupedCostDTO> grandTotalsByCurrency = new ArrayList<>();

    // ========================
    // METADATA
    // ========================

    /**
     * All rate issues found during calculation
     */
    @Builder.Default
    private List<RateIssueLogDTO> rateIssues = new ArrayList<>();

    /**
     * True if any rate issues were found
     */
    private Boolean hasIncompleteRates;

    /**
     * Timestamp when estimation was calculated
     */
    private String estimatedAt;
}
