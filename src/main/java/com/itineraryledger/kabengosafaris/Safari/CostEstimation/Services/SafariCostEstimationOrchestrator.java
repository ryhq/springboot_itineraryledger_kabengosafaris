package com.itineraryledger.kabengosafaris.Safari.CostEstimation.Services;

import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.*;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.CalculationMode;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Core.RateIssueLoggerService;
import com.itineraryledger.kabengosafaris.Safari.CostEstimation.DTOs.SafariCostEstimationResponseDTO;
import com.itineraryledger.kabengosafaris.Safari.CostEstimation.Services.Aggregators.SafariPerDayCostAggregator;
import com.itineraryledger.kabengosafaris.Safari.CostEstimation.Services.Aggregators.SafariPerPaxCostAggregator;
import com.itineraryledger.kabengosafaris.Safari.DTOs.FullSafariDTO;
import com.itineraryledger.kabengosafaris.Safari.Services.SafariFullGetService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Orchestrator for safari cost estimation.
 *
 * Coordinates the cost estimation process:
 * 1. Fetches the full safari
 * 2. Clears the rate issue logger
 * 3. Delegates to the appropriate aggregator based on mode (PER_DAY or PER_PAX)
 * 4. Calculates grand totals
 * 5. Collects rate issues
 * 6. Builds and returns the response
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SafariCostEstimationOrchestrator {

    private final SafariFullGetService safariFullGetService;
    private final SafariPerDayCostAggregator safariPerDayCostAggregator;
    private final SafariPerPaxCostAggregator safariPerPaxCostAggregator;
    private final RateIssueLoggerService rateIssueLoggerService;

    /**
     * Estimate costs for a safari.
     *
     * @param safariIdObfuscated The obfuscated safari ID
     * @param mode Calculation mode (PER_DAY or PER_PAX, defaults to PER_DAY)
     * @return ResponseEntity with cost estimation or error
     */
    public ResponseEntity<ApiResponse<?>> estimate(
            String safariIdObfuscated,
            CalculationMode mode
    ) {
        try {
            // Apply defaults
            if (mode == null) {
                mode = CalculationMode.PER_DAY;
            }

            // 1. Fetch full safari data
            ResponseEntity<ApiResponse<?>> safariResponse = safariFullGetService.getFullSafari(safariIdObfuscated);
            if (!safariResponse.getStatusCode().is2xxSuccessful()) {
                return safariResponse;
            }

            ApiResponse<?> apiResponse = safariResponse.getBody();
            if (apiResponse == null || apiResponse.getData() == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Safari not found", "SAFARI_NOT_FOUND")
                );
            }

            // Extract safari from wrapped response (getFullSafari now returns {safari: ..., nextId: ..., previousId: ...})
            FullSafariDTO safari;
            Object responseData = apiResponse.getData();
            if (responseData instanceof FullSafariDTO) {
                safari = (FullSafariDTO) responseData;
            } else if (responseData instanceof Map) {
                Object safariObj = ((Map<?, ?>) responseData).get("safari");
                if (safariObj instanceof FullSafariDTO) {
                    safari = (FullSafariDTO) safariObj;
                } else {
                    return ResponseEntity.status(404).body(
                        ApiResponse.error(404, "Safari not found", "SAFARI_NOT_FOUND")
                    );
                }
            } else {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Safari not found", "SAFARI_NOT_FOUND")
                );
            }

            // Validate safari has start and end dates
            if (safari.getStartDate() == null || safari.getEndDate() == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Safari must have start and end dates", "MISSING_DATES")
                );
            }

            // 2. Clear rate issue logger for this estimation
            rateIssueLoggerService.clear();

            // 3. Calculate total pax
            int totalPax = calculateTotalPax(safari.getPaxList());
            int carCount = safari.getCarCount() != null ? safari.getCarCount() : 1;

            // 4. Perform calculation based on mode
            List<DayCostDetailDTO> dayCostDetails = new ArrayList<>();
            List<PaxCategoryCostDTO> paxCostDetails = new ArrayList<>();
            List<CurrencyGroupedCostDTO> grandTotalsByCurrency;

            if (mode == CalculationMode.PER_DAY) {
                dayCostDetails = safariPerDayCostAggregator.aggregateByDay(safari);
                grandTotalsByCurrency = safariPerDayCostAggregator.calculateGrandTotals(dayCostDetails);
            } else {
                paxCostDetails = safariPerPaxCostAggregator.aggregateByPax(safari);
                grandTotalsByCurrency = safariPerPaxCostAggregator.calculateGrandTotals(paxCostDetails);
            }

            // 5. Collect rate issues
            List<RateIssueLogDTO> rateIssues = new ArrayList<>(rateIssueLoggerService.getIssues());

            // 6. Build response
            SafariCostEstimationResponseDTO response = SafariCostEstimationResponseDTO.builder()
                .safariId(safari.getId())
                .safariCode(safari.getCode())
                .safariName(safari.getName())
                .totalDays(safari.getTotalDays())
                .totalNights(safari.getTotalNights())
                .startDate(safari.getStartDate())
                .endDate(safari.getEndDate())
                .calculationMode(mode)
                .totalPax(totalPax)
                .carCount(carCount)
                .dayCostDetails(mode == CalculationMode.PER_DAY ? dayCostDetails : null)
                .paxCostDetails(mode == CalculationMode.PER_PAX ? paxCostDetails : null)
                .grandTotalsByCurrency(grandTotalsByCurrency)
                .rateIssues(rateIssues.isEmpty() ? null : rateIssues)
                .hasIncompleteRates(!rateIssues.isEmpty())
                .estimatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();

            return ResponseEntity.ok(
                ApiResponse.success(200, "Cost estimation calculated successfully", response)
            );

        } catch (Exception e) {
            log.error("Failed to estimate costs for safari: {}", safariIdObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to estimate costs: " + e.getMessage(), "COST_ESTIMATION_FAILED")
            );
        }
    }

    /**
     * Calculate total pax count from pax list.
     */
    private int calculateTotalPax(List<FullSafariDTO.PaxDTO> paxList) {
        if (paxList == null || paxList.isEmpty()) {
            return 0;
        }

        return paxList.stream()
            .mapToInt(p -> p.getCount() != null ? p.getCount() : 0)
            .sum();
    }
}
