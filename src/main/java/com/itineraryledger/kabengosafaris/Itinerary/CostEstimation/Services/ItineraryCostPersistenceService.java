package com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services;

import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.CostEstimationResponseDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.CurrencyGroupedCostDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.ItineraryCostSummaryDTO;
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
import java.time.format.DateTimeFormatter;
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
    public List<ItineraryCostSummaryDTO> persistCostSummary(Itinerary itinerary) {
        try {
            String obfuscatedId = idObfuscator.encodeId(itinerary.getId());
            LocalDate today = LocalDate.now();

            ResponseEntity<ApiResponse<?>> response = costEstimationOrchestrator.estimate(
                obfuscatedId, today, CalculationMode.PER_DAY
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("Cost estimation failed for itinerary {} ({})", itinerary.getId(), itinerary.getName());
                return List.of();
            }

            ApiResponse<?> body = response.getBody();
            if (body == null) {
                log.warn("Null response body for itinerary {}", itinerary.getId());
                return List.of();
            }
            Object data = body.getData();
            if (!(data instanceof CostEstimationResponseDTO estimationResponse)) {
                log.warn("Unexpected response type for itinerary {}", itinerary.getId());
                return List.of();
            }

            List<CurrencyGroupedCostDTO> grandTotals = estimationResponse.getGrandTotalsByCurrency();
            if (grandTotals == null || grandTotals.isEmpty()) {
                log.debug("No cost data for itinerary {} ({})", itinerary.getId(), itinerary.getName());
                costSummaryRepository.deleteByItineraryId(itinerary.getId());
                itinerary.setFromPriceUsd(null);
                itineraryRepository.save(itinerary);
                return List.of();
            }

            // Delete old summaries, flush to DB, then save new ones
            costSummaryRepository.deleteByItineraryId(itinerary.getId());
            costSummaryRepository.flush();

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

            List<ItineraryCostSummary> saved = costSummaryRepository.saveAll(summaries);
            log.debug("Persisted {} cost summary rows for itinerary {} ({})",
                saved.size(), itinerary.getId(), itinerary.getName());

            // Denormalize a per-person "from" price (USD preferred) so the public
            // listing can sort/filter by price without joining cost summaries.
            ItineraryCostSummary primary = saved.stream()
                .filter(s -> "USD".equalsIgnoreCase(s.getCurrency()))
                .findFirst()
                .orElse(saved.get(0));
            if (primary.getGrandTotalRack() != null) {
                int pax = Math.max(1, itinerary.getTotalPaxCount());
                itinerary.setFromPriceUsd(primary.getGrandTotalRack().doubleValue() / pax);
            } else {
                itinerary.setFromPriceUsd(null);
            }
            itineraryRepository.save(itinerary);

            return saved.stream().map(this::toDTO).toList();

        } catch (Exception e) {
            log.error("Failed to persist cost summary for itinerary {} ({})",
                itinerary.getId(), itinerary.getName(), e);
            return List.of();
        }
    }

    @Transactional
    public List<ItineraryCostSummaryDTO> persistByObfuscatedId(String obfuscatedId) {
        Long id = idObfuscator.decodeId(obfuscatedId);
        return itineraryRepository.findById(id)
            .map(this::persistCostSummary)
            .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public List<ItineraryCostSummaryDTO> getSavedCostSummary(String obfuscatedId) {
        Long id = idObfuscator.decodeId(obfuscatedId);
        return costSummaryRepository.findByItineraryId(id).stream()
            .map(this::toDTO)
            .toList();
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

    private ItineraryCostSummaryDTO toDTO(ItineraryCostSummary entity) {
        return ItineraryCostSummaryDTO.builder()
            .currency(entity.getCurrency())
            .accommodationRack(entity.getAccommodationRack())
            .parkFeesRack(entity.getParkFeesRack())
            .activitiesRack(entity.getActivitiesRack())
            .grandTotalRack(entity.getGrandTotalRack())
            .accommodationSto(entity.getAccommodationSto())
            .parkFeesSto(entity.getParkFeesSto())
            .activitiesSto(entity.getActivitiesSto())
            .grandTotalSto(entity.getGrandTotalSto())
            .hasIncompleteRates(entity.getHasIncompleteRates())
            .calculatedAt(entity.getCalculatedAt() != null
                ? entity.getCalculatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
            .startDateUsed(entity.getStartDateUsed() != null
                ? entity.getStartDateUsed().toString() : null)
            .build();
    }
}
