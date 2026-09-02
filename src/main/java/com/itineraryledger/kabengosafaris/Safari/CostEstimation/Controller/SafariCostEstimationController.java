package com.itineraryledger.kabengosafaris.Safari.CostEstimation.Controller;

import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.CalculationMode;
import com.itineraryledger.kabengosafaris.Safari.CostEstimation.Services.SafariCostEstimationOrchestrator;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for safari cost estimation.
 *
 * Provides a single endpoint supporting two calculation modes:
 * - PER_DAY: Costs grouped by day with daily subtotals
 * - PER_PAX: Costs grouped by passenger category with per-pax subtotals
 *
 * Both modes return STO and Rack rates separately with no currency mixing.
 */
@RestController
@RequestMapping("/api/safaris")
@RequiredArgsConstructor
@Slf4j
public class SafariCostEstimationController {

    private final SafariCostEstimationOrchestrator safariCostEstimationOrchestrator;

    /**
     * Estimate costs for a safari.
     *
     * GET /api/safaris/{safariId}/cost-estimation
     *
     * Query Parameters:
     * - mode: Calculation mode - PER_DAY or PER_PAX (optional, defaults to PER_DAY)
     *
     * Modes:
     * - PER_DAY: Costs grouped by day with daily subtotals
     * - PER_PAX: Costs grouped by passenger category with per-pax subtotals
     *
     * Features:
     * - Returns both STO (Tour Operator) and Rack (Public) rates
     * - Costs grouped by currency (no mixing)
     * - Only uses active rates
     * - Alternative beds, optional activities, fees switched off and fees WAIVED on this safari are
 *   priced into dayCostDetails[].excludedLineItems and counted in no total, so the office can
 *   compare an option without it changing what the safari costs
     * - Comprehensive rate issue logging
     *
     * Response:
     * - dayCostDetails: Populated when mode=PER_DAY
     * - paxCostDetails: Populated when mode=PER_PAX
     * - grandTotalsByCurrency: Overall totals by currency
     * - rateIssues: List of missing/inactive rate issues
     *
     * @param safariId The obfuscated safari ID
     * @param mode Calculation mode: PER_DAY or PER_PAX (optional, defaults to PER_DAY)
     * @return Cost estimation response with STO and Rack totals by currency
     */
    @GetMapping("/{safariId}/cost-estimation")
    @PreAuthorize("hasAuthority('PERM_READ_SAFARI')")
    public ResponseEntity<ApiResponse<?>> estimateCosts(
            @PathVariable String safariId,
            @RequestParam(required = false, defaultValue = "PER_DAY") String mode
    ) {
        log.info("Estimating costs for safari: {}, mode: {}", safariId, mode);

        // Parse calculation mode
        CalculationMode calculationMode;
        try {
            calculationMode = CalculationMode.valueOf(mode.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid mode. Must be PER_DAY or PER_PAX", "INVALID_MODE")
            );
        }

        return safariCostEstimationOrchestrator.estimate(safariId, calculationMode);
    }
}
