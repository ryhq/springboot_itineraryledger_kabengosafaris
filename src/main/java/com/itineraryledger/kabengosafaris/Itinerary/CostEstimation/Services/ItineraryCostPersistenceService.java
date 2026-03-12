package com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services;

import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.CostEstimationResponseDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.CurrencyGroupedCostDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Entity.ItineraryCostSummary;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.CalculationMode;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Repository.ItineraryCostSummaryRepository;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary.ItineraryStatus;
import com.itineraryledger.kabengosafaris.Itinerary.Repository.ItineraryRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItineraryCostPersistenceService {

    private final CostEstimationOrchestrator costEstimationOrchestrator;
    private final ItineraryCostSummaryRepository costSummaryRepository;
    private final ItineraryRepository itineraryRepository;
    private final IdObfuscator idObfuscator;

    @Transactional
    public void persistCostSummary(Itinerary itinerary) {
        try {
            String obfuscatedId = idObfuscator.encodeId(itinerary.getId());
            LocalDate today = LocalDate.now();

            ResponseEntity<ApiResponse<?>> response = costEstimationOrchestrator.estimate(
                obfuscatedId, today, CalculationMode.PER_DAY
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("Cost estimation failed for itinerary {} ({})", itinerary.getId(), itinerary.getName());
                return;
            }

            ApiResponse<?> body = response.getBody();
            if (body == null) {
                log.warn("Null response body for itinerary {}", itinerary.getId());
                return;
            }
            Object data = body.getData();
            if (!(data instanceof CostEstimationResponseDTO estimationResponse)) {
                log.warn("Unexpected response type for itinerary {}", itinerary.getId());
                return;
            }

            List<CurrencyGroupedCostDTO> grandTotals = estimationResponse.getGrandTotalsByCurrency();
            if (grandTotals == null || grandTotals.isEmpty()) {
                log.debug("No cost data for itinerary {} ({})", itinerary.getId(), itinerary.getName());
                costSummaryRepository.deleteByItineraryId(itinerary.getId());
                return;
            }

            // Delete old summaries and save new ones
            costSummaryRepository.deleteByItineraryId(itinerary.getId());

            List<ItineraryCostSummary> summaries = new ArrayList<>();
            boolean hasIncompleteRates = Boolean.TRUE.equals(estimationResponse.getHasIncompleteRates());

            for (CurrencyGroupedCostDTO total : grandTotals) {
                ItineraryCostSummary summary = ItineraryCostSummary.builder()
                    .itinerary(itinerary)
                    .currency(total.getCurrency())
                    .accommodationRack(total.getAccommodationRack())
                    .parkFeesRack(total.getParkFeesRack())
                    .activitiesRack(total.getActivitiesRack())
                    .grandTotalRack(total.getGrandTotalRack())
                    .accommodationSto(total.getAccommodationSto())
                    .parkFeesSto(total.getParkFeesSto())
                    .activitiesSto(total.getActivitiesSto())
                    .grandTotalSto(total.getGrandTotalSto())
                    .hasIncompleteRates(hasIncompleteRates)
                    .calculatedAt(LocalDateTime.now())
                    .startDateUsed(today)
                    .build();
                summaries.add(summary);
            }

            costSummaryRepository.saveAll(summaries);
            log.debug("Persisted {} cost summary rows for itinerary {} ({})",
                summaries.size(), itinerary.getId(), itinerary.getName());

        } catch (Exception e) {
            log.error("Failed to persist cost summary for itinerary {} ({})",
                itinerary.getId(), itinerary.getName(), e);
        }
    }

    @Transactional
    public void persistByObfuscatedId(String obfuscatedId) {
        Long id = idObfuscator.decodeId(obfuscatedId);
        itineraryRepository.findById(id).ifPresent(this::persistCostSummary);
    }

    @Transactional
    public void persistAllPublished() {
        List<Itinerary> publishedItineraries = itineraryRepository.findByStatusAndIsActiveTrue(ItineraryStatus.PUBLISHED);

        log.info("Starting cost recalculation for {} published itineraries", publishedItineraries.size());

        int success = 0;
        int failed = 0;

        for (Itinerary itinerary : publishedItineraries) {
            try {
                persistCostSummary(itinerary);
                success++;
            } catch (Exception e) {
                failed++;
                log.error("Failed to recalculate costs for itinerary {}", itinerary.getId(), e);
            }
        }

        log.info("Cost recalculation complete: {} succeeded, {} failed out of {} total",
            success, failed, publishedItineraries.size());
    }
}
