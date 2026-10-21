package com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.CostEstimationResponseDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.FullCostEstimationDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.CalculationMode;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.CostEstimationDocumentService;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.CostEstimationOrchestrator;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.FullItineraryDTO;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The costing document, for a quote rather than for the product it came from.
 *
 * <p>An itinerary is a blueprint and prices at its reference party: two guests, one vehicle, one
 * room a night. That is the right answer for the product sheet and the wrong one for a client,
 * because the party that is actually travelling is recorded on the quote. Six guests in three
 * double rooms across two vehicles is not the blueprint multiplied by three -- the per-vehicle and
 * per-group lines do not scale with heads -- so a costing taken from the itinerary cannot be
 * handed to the person paying.
 *
 * <p>Nothing here prices anything. The quote's own day snapshot is projected by
 * {@link QuoteCostEstimationService#buildSyntheticDTO(Quote)}, the same projection that writes the
 * quote's line items, and handed to the same engine through
 * {@link CostEstimationOrchestrator#estimateFrom}. One projection and one engine is the whole
 * point: a second of either is how a costing sheet and the quotation beside it come to disagree.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuoteCostingDocumentService {

    private final QuoteRepository quoteRepository;
    private final QuoteCostEstimationService quoteCostEstimationService;
    private final CostEstimationOrchestrator orchestrator;
    private final CostEstimationDocumentService costingShape;
    private final IdObfuscator idObfuscator;

    /**
     * Everything a costing document needs for a quote, or null when it cannot be priced.
     *
     * @param quoteIdObfuscated the quote to cost
     * @param startDateOverride prices for this date instead of the quote's own start date; the
     *                          quote's date is the right default, since that is when the trip
     *                          runs and seasons decide the rate
     */
    @Transactional(readOnly = true)
    public FullCostEstimationDTO build(String quoteIdObfuscated, LocalDate startDateOverride) {
        Quote quote = load(quoteIdObfuscated);
        if (quote == null) {
            log.warn("Costing requested for quote {}, which does not exist", quoteIdObfuscated);
            return null;
        }
        if (quote.getDays() == null || quote.getDays().isEmpty()) {
            /*
             * A quote raised before the day snapshot existed has nothing to cost day by day. Its
             * line items are still whatever was written at the time; what cannot be produced is
             * this document, and saying so beats printing an empty one.
             */
            log.info("Quote {} has no day snapshot, so it cannot be costed day by day",
                quote.getQuoteCode());
            return null;
        }

        LocalDate pricingDate = startDateOverride != null
            ? startDateOverride
            : (quote.getSafariStartDate() != null ? quote.getSafariStartDate() : LocalDate.now());

        FullItineraryDTO projection = quoteCostEstimationService.buildSyntheticDTO(quote);

        CostEstimationResponseDTO byDay = estimate(projection, pricingDate, CalculationMode.PER_DAY, quote);
        if (byDay == null) return null;

        /*
         * The second pass is what fills the per-band table. It is a few hundred milliseconds and
         * it saves the template from asking for a mode it was not rendered in.
         */
        CostEstimationResponseDTO byPax = estimate(projection, pricingDate, CalculationMode.PER_PAX, quote);

        int totalPax = byDay.getTotalPax() != null ? byDay.getTotalPax() : 0;

        return FullCostEstimationDTO.builder()
            .subjectId(quoteIdObfuscated)
            .subjectKind("QUOTE")
            .subjectCode(quote.getQuoteCode())
            .subjectName(quote.getTitle() != null ? quote.getTitle() : byDay.getItineraryName())
            .tripType(quote.getItinerary() != null && quote.getItinerary().getTripType() != null
                ? quote.getItinerary().getTripType().getDisplayName() : null)
            .budgetCategory(quote.getItinerary() != null && quote.getItinerary().getBudgetCategory() != null
                ? quote.getItinerary().getBudgetCategory().getDisplayName() : null)
            .totalDays(byDay.getTotalDays())
            .totalNights(byDay.getTotalNights())
            .carCount(byDay.getCarCount())
            .startDate(byDay.getStartDate() != null ? byDay.getStartDate() : pricingDate)
            .endDate(byDay.getEndDate())
            .estimatedAt(costingShape.readableTimestamp(byDay.getEstimatedAt()))
            .totalPax(totalPax)
            .dayCostDetails(byDay.getDayCostDetails() != null ? byDay.getDayCostDetails() : new ArrayList<>())
            .paxCostDetails(byPax != null && byPax.getPaxCostDetails() != null
                ? byPax.getPaxCostDetails() : new ArrayList<>())
            .totals(costingShape.totalsWithMargins(byDay.getGrandTotalsByCurrency(), totalPax))
            .quoteTotals(quotedFigures(quote, totalPax))
            .hasIncompleteRates(Boolean.TRUE.equals(byDay.getHasIncompleteRates()))
            .rateIssues(byDay.getRateIssues() != null ? byDay.getRateIssues() : new ArrayList<>())
            .rateIssueCount(byDay.getRateIssues() != null ? byDay.getRateIssues().size() : 0)
            /*
             * No saved summary. That is the itinerary's stored price, recalculated nightly so a
             * product's drift is visible. A quote is a fixed offer with a validity date; showing
             * it against today's rates would invite somebody to "correct" a figure a customer has
             * already been given.
             */
            .savedSummary(new ArrayList<>())
            .build();
    }

    /**
     * What the customer was quoted, per currency, taken from the quote's own stored totals.
     *
     * <p>Not recalculated. The quotation PDF prints these same rows, so reading them rather than
     * deriving them again is what guarantees the two documents agree. The cost tables above are a
     * different question -- what the trip costs to run -- and the gap between them is the tax,
     * the discount and any uplift the quote carries.
     */
    private List<FullCostEstimationDTO.QuoteTotalsDTO> quotedFigures(Quote quote, int totalPax) {
        Map<String, FullCostEstimationDTO.QuoteTotalsDTO.QuoteTotalsDTOBuilder> byCurrency =
            new LinkedHashMap<>();

        index(byCurrency, quote.getSubtotals(), (b, amount) -> b.subtotal(amount));
        index(byCurrency, quote.getTaxes(), (b, amount) -> b.taxAmount(amount));
        index(byCurrency, quote.getDiscounts(), (b, amount) -> b.discountAmount(amount));
        index(byCurrency, quote.getGrandTotals(), (b, amount) -> b.grandTotal(amount));

        List<FullCostEstimationDTO.QuoteTotalsDTO> out = new ArrayList<>();
        for (Map.Entry<String, FullCostEstimationDTO.QuoteTotalsDTO.QuoteTotalsDTOBuilder> entry
                : byCurrency.entrySet()) {
            FullCostEstimationDTO.QuoteTotalsDTO row = entry.getValue()
                .currency(entry.getKey())
                .taxPercentage(quote.getTaxPercentage())
                .build();
            if (row.getGrandTotal() != null && totalPax > 0) {
                row.setPerGuest(row.getGrandTotal()
                    .divide(BigDecimal.valueOf(totalPax), 2, RoundingMode.HALF_UP));
            }
            out.add(row);
        }
        return out;
    }

    /** Folds one of the quote's price lists into the per-currency rows being assembled. */
    private void index(
        Map<String, FullCostEstimationDTO.QuoteTotalsDTO.QuoteTotalsDTOBuilder> rows,
        List<Price> prices,
        java.util.function.BiConsumer<FullCostEstimationDTO.QuoteTotalsDTO.QuoteTotalsDTOBuilder, BigDecimal> set
    ) {
        if (prices == null) return;
        for (Price price : prices) {
            if (price == null || price.getCurrency() == null) continue;
            set.accept(
                rows.computeIfAbsent(price.getCurrency(),
                    c -> FullCostEstimationDTO.QuoteTotalsDTO.builder()),
                price.getTotalPrice());
        }
    }

    /** One pass of the engine over the quote's projection, unwrapped, or null if it refused. */
    private CostEstimationResponseDTO estimate(
        FullItineraryDTO projection,
        LocalDate startDate,
        CalculationMode mode,
        Quote quote
    ) {
        try {
            ResponseEntity<ApiResponse<?>> response = orchestrator.estimateFrom(projection, startDate, mode);
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("Costing refused for quote {} in mode {}: {}",
                    quote.getQuoteCode(), mode, response.getStatusCode());
                return null;
            }
            ApiResponse<?> body = response.getBody();
            Object data = body != null ? body.getData() : null;
            return data instanceof CostEstimationResponseDTO dto ? dto : null;
        } catch (Exception e) {
            log.error("Costing failed for quote {} in mode {}", quote.getQuoteCode(), mode, e);
            return null;
        }
    }

    private Quote load(String idObfuscated) {
        try {
            return quoteRepository.findById(idObfuscator.decodeId(idObfuscated)).orElse(null);
        } catch (Exception e) {
            log.warn("Could not read quote {} for its costing", idObfuscated);
            return null;
        }
    }
}
