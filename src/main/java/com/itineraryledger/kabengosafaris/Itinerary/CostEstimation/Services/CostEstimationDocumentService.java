package com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services;

import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.CostEstimationResponseDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.CurrencyGroupedCostDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.FullCostEstimationDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.CalculationMode;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.Repository.ItineraryRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Assembles a costing into the shape a document needs.
 *
 * The estimator answers one question per call — by day OR by pax band — because
 * a screen only ever shows one at a time. A document is read once and cannot ask
 * again, so this runs both and hands over the pair, letting a template print
 * either without the caller having chosen in advance.
 *
 * <p>It also does the arithmetic a template should never do. Gross profit,
 * margin and per-head figures are computed here: a template that has to subtract
 * is a template that can subtract wrongly, and the number it would get wrong is
 * the profit.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CostEstimationDocumentService {

    private final CostEstimationOrchestrator orchestrator;
    private final ItineraryCostPersistenceService persistenceService;
    private final ItineraryRepository itineraryRepository;
    private final IdObfuscator idObfuscator;

    /**
     * Everything a costing document needs, or null when the itinerary cannot be
     * priced at all (missing, or the estimator refused).
     *
     * @param startDate the date to price for; today when null, because seasons
     *                  make this the difference between two valid answers
     */
    @Transactional(readOnly = true)
    public FullCostEstimationDTO build(String itineraryIdObfuscated, LocalDate startDate) {
        LocalDate pricingDate = startDate != null ? startDate : LocalDate.now();

        CostEstimationResponseDTO byDay = estimate(itineraryIdObfuscated, pricingDate, CalculationMode.PER_DAY);
        if (byDay == null) return null;

        // the second pass costs seconds and saves the template from a mode it
        // cannot ask for
        CostEstimationResponseDTO byPax = estimate(itineraryIdObfuscated, pricingDate, CalculationMode.PER_PAX);

        Itinerary itinerary = loadItinerary(itineraryIdObfuscated);

        int totalPax = byDay.getTotalPax() != null ? byDay.getTotalPax() : 0;

        return FullCostEstimationDTO.builder()
            .subjectId(itineraryIdObfuscated)
            // the one place this says what it is costing; a safari fills the same shape
            .subjectKind("ITINERARY")
            .subjectCode(byDay.getItineraryCode())
            .subjectName(byDay.getItineraryName())
            .tripType(itinerary != null && itinerary.getTripType() != null
                ? itinerary.getTripType().getDisplayName() : null)
            .budgetCategory(itinerary != null && itinerary.getBudgetCategory() != null
                ? itinerary.getBudgetCategory().getDisplayName() : null)
            .totalDays(byDay.getTotalDays())
            .totalNights(byDay.getTotalNights())
            .carCount(byDay.getCarCount())
            .startDate(byDay.getStartDate() != null ? byDay.getStartDate() : pricingDate)
            .endDate(byDay.getEndDate())
            .estimatedAt(byDay.getEstimatedAt())
            .totalPax(totalPax)
            .dayCostDetails(byDay.getDayCostDetails() != null ? byDay.getDayCostDetails() : new ArrayList<>())
            .paxCostDetails(byPax != null && byPax.getPaxCostDetails() != null
                ? byPax.getPaxCostDetails() : new ArrayList<>())
            .totals(withMargins(byDay.getGrandTotalsByCurrency(), totalPax))
            .hasIncompleteRates(Boolean.TRUE.equals(byDay.getHasIncompleteRates()))
            .rateIssues(byDay.getRateIssues() != null ? byDay.getRateIssues() : new ArrayList<>())
            .rateIssueCount(byDay.getRateIssues() != null ? byDay.getRateIssues().size() : 0)
            .savedSummary(persistenceService.getSavedCostSummary(itineraryIdObfuscated))
            .build();
    }

    /** One pass of the estimator, unwrapped, or null if it refused. */
    private CostEstimationResponseDTO estimate(String id, LocalDate startDate, CalculationMode mode) {
        try {
            ResponseEntity<ApiResponse<?>> response = orchestrator.estimate(id, startDate, mode);
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("Cost estimation refused for {} in mode {}: {}", id, mode, response.getStatusCode());
                return null;
            }
            ApiResponse<?> body = response.getBody();
            Object data = body != null ? body.getData() : null;
            return data instanceof CostEstimationResponseDTO dto ? dto : null;
        } catch (Exception e) {
            log.error("Cost estimation failed for {} in mode {}", id, mode, e);
            return null;
        }
    }

    private Itinerary loadItinerary(String idObfuscated) {
        try {
            return itineraryRepository.findById(idObfuscator.decodeId(idObfuscated)).orElse(null);
        } catch (Exception e) {
            // the header loses its trip type; the figures are unaffected
            log.warn("Could not load itinerary {} for the document header", idObfuscated);
            return null;
        }
    }

    /**
     * Copies each currency block and adds the numbers a document is judged on.
     *
     * Margin is a percentage of RACK, not of cost: it answers "how much of the
     * selling price is ours", which is the question a quote is argued over.
     */
    private List<FullCostEstimationDTO.CostTotalsDTO> withMargins(
        List<CurrencyGroupedCostDTO> source,
        int totalPax
    ) {
        List<FullCostEstimationDTO.CostTotalsDTO> out = new ArrayList<>();
        if (source == null) return out;

        for (CurrencyGroupedCostDTO totals : source) {
            BigDecimal sto = nz(totals.getGrandTotalSto());
            BigDecimal rack = nz(totals.getGrandTotalRack());
            BigDecimal profit = rack.subtract(sto);

            BigDecimal marginPercent = rack.compareTo(BigDecimal.ZERO) > 0
                ? profit.multiply(BigDecimal.valueOf(100)).divide(rack, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

            BigDecimal perPaxSto = totalPax > 0
                ? sto.divide(BigDecimal.valueOf(totalPax), 2, RoundingMode.HALF_UP)
                : null;
            BigDecimal perPaxRack = totalPax > 0
                ? rack.divide(BigDecimal.valueOf(totalPax), 2, RoundingMode.HALF_UP)
                : null;

            out.add(FullCostEstimationDTO.CostTotalsDTO.builder()
                .currency(totals.getCurrency())
                .accommodationSto(totals.getAccommodationSto())
                .accommodationRack(totals.getAccommodationRack())
                .parkFeesSto(totals.getParkFeesSto())
                .parkFeesRack(totals.getParkFeesRack())
                .activitiesSto(totals.getActivitiesSto())
                .activitiesRack(totals.getActivitiesRack())
                .grandTotalSto(sto)
                .grandTotalRack(rack)
                .grossProfit(profit)
                .marginPercent(marginPercent)
                .stoPerPax(perPaxSto)
                .rackPerPax(perPaxRack)
                .build());
        }
        return out;
    }

    private BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
