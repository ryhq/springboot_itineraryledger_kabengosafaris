package com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Controller;

import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.ItineraryCostSummaryDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.CalculationMode;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.CostEstimationOrchestrator;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.ItineraryCostPersistenceService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
    private final ItineraryCostPersistenceService costPersistenceService;

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

        ResponseEntity<ApiResponse<?>> result = costEstimationOrchestrator.estimate(itineraryId, startDate, calculationMode);

        // Async persist cost summary (fire-and-forget, no circular call)
        if (result.getStatusCode().is2xxSuccessful()) {
            final String idForPersistence = itineraryId;
            CompletableFuture.runAsync(() -> {
                try {
                    costPersistenceService.persistByObfuscatedId(idForPersistence);
                } catch (Exception ex) {
                    log.warn("Async cost persistence failed for itinerary {}: {}",
                            idForPersistence, ex.getMessage());
                }
            });
        }

        return result;
    }

    /**
     * Get saved cost summary for an itinerary.
     */
    @GetMapping("/{itineraryId}/cost-summary")
    @PreAuthorize("hasAuthority('PERM_READ_ITINERARY')")
    public ResponseEntity<ApiResponse<?>> getSavedCostSummary(@PathVariable String itineraryId) {
        List<ItineraryCostSummaryDTO> summaries = costPersistenceService.getSavedCostSummary(itineraryId);
        if (summaries.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(200, "No saved cost summary found", null));
        }
        return ResponseEntity.ok(ApiResponse.success(200, "Cost summary retrieved successfully", summaries));
    }

    /**
     * Recalculate and persist cost summary for a single itinerary.
     * Returns the newly saved cost summaries.
     */
    @PostMapping("/{itineraryId}/cost-summary/recalculate")
    @PreAuthorize("hasAuthority('PERM_READ_ITINERARY')")
    public ResponseEntity<ApiResponse<?>> recalculateCostSummary(@PathVariable String itineraryId) {
        log.info("Recalculating cost summary for itinerary: {}", itineraryId);
        List<ItineraryCostSummaryDTO> summaries = costPersistenceService.persistByObfuscatedId(itineraryId);
        if (summaries.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(200, "No cost data to persist", null));
        }
        return ResponseEntity.ok(ApiResponse.success(200, "Cost summary recalculated and saved", summaries));
    }

    /**
     * Recalculate and persist cost summaries for all published itineraries.
     */
    @PostMapping("/cost-summary/recalculate-all")
    @PreAuthorize("hasAuthority('PERM_READ_ITINERARY')")
    public ResponseEntity<ApiResponse<?>> recalculateAllCostSummaries() {
        log.info("Triggering cost recalculation for all published itineraries");
        CompletableFuture.runAsync(() -> {
            try {
                costPersistenceService.persistAllPublished();
            } catch (Exception ex) {
                log.error("Bulk cost recalculation failed: {}", ex.getMessage());
            }
        });
        return ResponseEntity.ok(ApiResponse.success(200,
            "Cost recalculation started for all published itineraries", null));
    }
}
