package com.itineraryledger.kabengosafaris.Quote.Controllers;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.CalculationMode;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.CostEstimationOrchestrator;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.FullItineraryDTO;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteRepository;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices.QuoteCostEstimationService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * What this quote costs, day by day, at the party that is actually travelling.
 *
 * <p>The itinerary's endpoint answers the same question for the product: two guests, one vehicle,
 * one room a night, because that is what a blueprint is priced at. Neither figure is wrong and
 * they are not each other's answer, which is why this exists rather than the screen reading the
 * itinerary's and hoping.
 *
 * <p>Same response shape as {@code /api/itineraries/{id}/cost-estimation} on purpose: one screen
 * renders all three subjects, so a divergence here would be a divergence in the panel.
 *
 * <p>Nothing is persisted. The itinerary endpoint writes a cost summary afterwards so a product's
 * drift is tracked over time; a quote is a fixed offer with a validity date and has nothing to
 * drift against.
 */
@RestController
@RequestMapping("/api/quotes")
@RequiredArgsConstructor
@Slf4j
public class QuoteCostEstimationController {

    private final CostEstimationOrchestrator orchestrator;
    private final QuoteCostEstimationService quoteCostEstimationService;
    private final QuoteRepository quoteRepository;
    private final IdObfuscator idObfuscator;

    /**
     * GET /api/quotes/{quoteId}/cost-estimation
     *
     * @param startDate prices for this date instead of the quote's own start date; seasons make
     *                  this part of the answer rather than a formatting choice
     * @param mode PER_DAY for the day-by-day map, PER_PAX for the per-band breakdown
     */
    @GetMapping("/{quoteId}/cost-estimation")
    @PreAuthorize("hasAuthority('PERM_READ_QUOTE')")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> estimateCosts(
            @PathVariable String quoteId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false, defaultValue = "PER_DAY") String mode
    ) {
        CalculationMode calculationMode;
        try {
            calculationMode = CalculationMode.valueOf(mode.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid mode. Must be PER_DAY or PER_PAX", "INVALID_MODE")
            );
        }

        Quote quote;
        try {
            quote = quoteRepository.findById(idObfuscator.decodeId(quoteId)).orElse(null);
        } catch (Exception e) {
            quote = null;
        }
        if (quote == null) {
            return ResponseEntity.status(404).body(
                ApiResponse.error(404, "Quote not found: " + quoteId, "QUOTE_NOT_FOUND")
            );
        }
        if (quote.getDays() == null || quote.getDays().isEmpty()) {
            /*
             * Quotes raised before the day snapshot existed carry line items and no day tree.
             * There is nothing to price day by day, and an empty costing would read as a free
             * trip rather than as a missing one.
             */
            return ResponseEntity.status(404).body(
                ApiResponse.error(404,
                    "Quote " + quote.getQuoteCode() + " carries no day snapshot, so it cannot be "
                        + "costed day by day.",
                    "COSTING_NOT_AVAILABLE")
            );
        }

        LocalDate pricingDate = startDate != null
            ? startDate
            : (quote.getSafariStartDate() != null ? quote.getSafariStartDate() : LocalDate.now());

        log.info("Costing quote {} for {} in mode {}", quote.getQuoteCode(), pricingDate, calculationMode);

        FullItineraryDTO projection = quoteCostEstimationService.buildSyntheticDTO(quote);
        return orchestrator.estimateFrom(projection, pricingDate, calculationMode);
    }
}
