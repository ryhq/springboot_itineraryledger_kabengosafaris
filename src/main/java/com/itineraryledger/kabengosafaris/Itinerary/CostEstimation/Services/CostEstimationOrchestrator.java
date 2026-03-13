package com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services;

import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.*;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.CalculationMode;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Aggregators.PerDayCostAggregator;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Aggregators.PerPaxCostAggregator;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Core.RateIssueLoggerService;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.FullItineraryDTO;
import com.itineraryledger.kabengosafaris.Itinerary.Services.ItineraryFullGetService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Orchestrator for cost estimation.
 *
 * Coordinates the cost estimation process:
 * 1. Fetches the full itinerary
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
public class CostEstimationOrchestrator {

    private final ItineraryFullGetService itineraryFullGetService;
    private final PerDayCostAggregator perDayCostAggregator;
    private final PerPaxCostAggregator perPaxCostAggregator;
    private final RateIssueLoggerService rateIssueLoggerService;

    @Lazy
    @Autowired
    private ItineraryCostPersistenceService costPersistenceService;

    /**
     * Estimate costs for an itinerary.
     *
     * @param itineraryIdObfuscated The obfuscated itinerary ID
     * @param startDate Trip start date (defaults to today if null)
     * @param mode Calculation mode (PER_DAY or PER_PAX, defaults to PER_DAY)
     * @return ResponseEntity with cost estimation or error
     */
    public ResponseEntity<ApiResponse<?>> estimate(
            String itineraryIdObfuscated,
            LocalDate startDate,
            CalculationMode mode
    ) {
        try {
            // Apply defaults
            if (startDate == null) {
                startDate = LocalDate.now();
            }
            if (mode == null) {
                mode = CalculationMode.PER_DAY;
            }

            // 1. Fetch full itinerary data
            ResponseEntity<ApiResponse<?>> itineraryResponse = itineraryFullGetService.getFullItinerary(itineraryIdObfuscated);
            if (!itineraryResponse.getStatusCode().is2xxSuccessful()) {
                return itineraryResponse;
            }

            ApiResponse<?> apiResponse = itineraryResponse.getBody();
            if (apiResponse == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Itinerary not found", "ITINERARY_NOT_FOUND")
                );
            }

            FullItineraryDTO itinerary;
            Object responseData = apiResponse.getData();
            if (responseData instanceof FullItineraryDTO) {
                itinerary = (FullItineraryDTO) responseData;
            } else if (responseData instanceof Map) {
                Object itineraryObj = ((Map<?, ?>) responseData).get("itinerary");
                if (itineraryObj instanceof FullItineraryDTO) {
                    itinerary = (FullItineraryDTO) itineraryObj;
                } else {
                    return ResponseEntity.status(404).body(
                        ApiResponse.error(404, "Itinerary not found", "ITINERARY_NOT_FOUND")
                    );
                }
            } else {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Itinerary not found", "ITINERARY_NOT_FOUND")
                );
            }

            // 2. Clear rate issue logger for this estimation
            rateIssueLoggerService.clear();

            // 3. Calculate end date
            LocalDate endDate = startDate.plusDays(itinerary.getTotalDays() - 1);

            // 4. Calculate total pax
            int totalPax = calculateTotalPax(itinerary.getPaxList());
            int carCount = itinerary.getCarCount() != null ? itinerary.getCarCount() : 1;

            // 5. Perform calculation based on mode
            List<DayCostDetailDTO> dayCostDetails = new ArrayList<>();
            List<PaxCategoryCostDTO> paxCostDetails = new ArrayList<>();
            List<CurrencyGroupedCostDTO> grandTotalsByCurrency;

            if (mode == CalculationMode.PER_DAY) {
                dayCostDetails = perDayCostAggregator.aggregateByDay(itinerary, startDate);
                grandTotalsByCurrency = perDayCostAggregator.calculateGrandTotals(dayCostDetails);
            } else {
                paxCostDetails = perPaxCostAggregator.aggregateByPax(itinerary, startDate);
                grandTotalsByCurrency = perPaxCostAggregator.calculateGrandTotals(paxCostDetails);
            }

            // 6. Collect rate issues
            List<RateIssueLogDTO> rateIssues = new ArrayList<>(rateIssueLoggerService.getIssues());

            // 7. Build response
            CostEstimationResponseDTO response = CostEstimationResponseDTO.builder()
                .itineraryId(itinerary.getId())
                .itineraryCode(itinerary.getCode())
                .itineraryName(itinerary.getName())
                .totalDays(itinerary.getTotalDays())
                .totalNights(itinerary.getTotalNights())
                .startDate(startDate)
                .endDate(endDate)
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

            // Async persist cost summary for current date
            final String idForPersistence = itineraryIdObfuscated;
            CompletableFuture.runAsync(() -> {
                try {
                    costPersistenceService.persistByObfuscatedId(idForPersistence);
                } catch (Exception ex) {
                    log.warn("Async cost persistence failed for itinerary {}: {}",
                            idForPersistence, ex.getMessage());
                }
            });

            return ResponseEntity.ok(
                ApiResponse.success(200, "Cost estimation calculated successfully", response)
            );

        } catch (Exception e) {
            log.error("Failed to estimate costs for itinerary: {}", itineraryIdObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to estimate costs: " + e.getMessage(), "COST_ESTIMATION_FAILED")
            );
        }
    }

    /**
     * Calculate total pax count from pax list.
     */
    private int calculateTotalPax(List<FullItineraryDTO.PaxDTO> paxList) {
        if (paxList == null || paxList.isEmpty()) {
            return 0;
        }

        return paxList.stream()
            .mapToInt(p -> p.getCount() != null ? p.getCount() : 0)
            .sum();
    }
}
