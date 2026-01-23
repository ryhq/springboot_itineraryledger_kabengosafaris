package com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Controller;

import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.CalculationMode;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.CostEstimationOrchestrator;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * REST Controller for itinerary cost estimation.
 *
 * Provides a single endpoint supporting two calculation modes:
 * - PER_DAY: Costs grouped by day with daily subtotals
 * - PER_PAX: Costs grouped by passenger category with per-pax subtotals
 *
 * Both modes return STO and Rack rates separately with no currency mixing.
 */
@RestController
@RequestMapping("/api/itineraries")
@RequiredArgsConstructor
@Slf4j
public class CostEstimationController {

    private final CostEstimationOrchestrator costEstimationOrchestrator;

    /**
     * Estimate costs for an itinerary.
     *
     * GET /api/itineraries/{itineraryId}/cost-estimation
     *
     * Query Parameters:
     * - startDate: Trip start date (optional, defaults to today)
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
     * - Skips alternative accommodations and optional activities
     * - Comprehensive rate issue logging
     *
     * Response:
     * - dayCostDetails: Populated when mode=PER_DAY
     * - paxCostDetails: Populated when mode=PER_PAX
     * - grandTotalsByCurrency: Overall totals by currency
     * - rateIssues: List of missing/inactive rate issues
     *
     * @param itineraryId The obfuscated itinerary ID
     * @param startDate Trip start date (optional, defaults to today)
     * @param mode Calculation mode: PER_DAY or PER_PAX (optional, defaults to PER_DAY)
     * @return Cost estimation response with STO and Rack totals by currency
     */
    @GetMapping("/{itineraryId}/cost-estimation")
    @PreAuthorize("hasAuthority('PERM_READ_ITINERARY')")
    public ResponseEntity<ApiResponse<?>> estimateCosts(
            @PathVariable String itineraryId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false, defaultValue = "PER_DAY") String mode
    ) {
        log.info("Estimating costs for itinerary: {}, startDate: {}, mode: {}", itineraryId, startDate, mode);

        // Parse calculation mode
        CalculationMode calculationMode;
        try {
            calculationMode = CalculationMode.valueOf(mode.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid mode. Must be PER_DAY or PER_PAX", "INVALID_MODE")
            );
        }

        return costEstimationOrchestrator.estimate(itineraryId, startDate, calculationMode);
    }
}
