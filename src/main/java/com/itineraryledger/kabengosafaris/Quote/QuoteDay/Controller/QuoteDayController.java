package com.itineraryledger.kabengosafaris.Quote.QuoteDay.Controller;

import com.itineraryledger.kabengosafaris.Quote.QuoteDay.DTOs.CreateQuoteDayDTO;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.DTOs.UpdateQuoteDayDTO;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.Services.QuoteDayService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class QuoteDayController {

    private final QuoteDayService quoteDayService;
    private final com.itineraryledger.kabengosafaris.Quote.QuoteDay.Services.QuoteDayTreeService quoteDayTreeService;

    /**
     * Every day with its parks, fees, activities and stays - one request.
     *
     * The day-configuration screen needs all of it at once to say what each day
     * is missing; asking day by day is one request per day plus one per park.
     */
    @GetMapping("/api/quotes/{quoteId}/days/full")
    @PreAuthorize("hasAuthority('PERM_READ_QUOTE_DAY')")
    public ResponseEntity<ApiResponse<?>> listDayTree(@PathVariable String quoteId) {
        return quoteDayTreeService.getDayTree(quoteId);
    }

    @GetMapping("/api/quotes/{quoteId}/days")
    @PreAuthorize("hasAuthority('PERM_READ_QUOTE_DAY')")
    public ResponseEntity<ApiResponse<?>> listDaysForQuote(@PathVariable String quoteId) {
        return quoteDayService.getDays(quoteId);
    }

    @GetMapping("/api/quote-days/{dayId}")
    @PreAuthorize("hasAuthority('PERM_READ_QUOTE_DAY')")
    public ResponseEntity<ApiResponse<?>> getDay(@PathVariable String dayId) {
        return quoteDayService.getDayById(dayId);
    }

    /**
     * Add a day the quotation's itinerary does not have.
     *
     * <p>The catalogue trip is a product; this quotation is one client's version of it, and the
     * client's version is the one that moves. An arrival night before the trip starts, or a few
     * nights in town after it ends, is a change to this quote - not a reason to build a second
     * itinerary that will never be sold to anybody else.
     */
    @PostMapping("/api/quotes/{quoteId}/days")
    @PreAuthorize("hasAuthority('PERM_CREATE_QUOTE_DAY')")
    public ResponseEntity<ApiResponse<?>> createDay(
            @PathVariable String quoteId,
            @Valid @RequestBody CreateQuoteDayDTO dto
    ) {
        log.info("POST /api/quotes/{}/days - adding day at position {}", quoteId, dto.getPosition());
        return quoteDayService.createDay(quoteId, dto);
    }

    /** Another night here: the copy lands directly after the day it came from. */
    @PostMapping("/api/quote-days/{dayId}/duplicate")
    @PreAuthorize("hasAuthority('PERM_CREATE_QUOTE_DAY')")
    public ResponseEntity<ApiResponse<?>> duplicateDay(@PathVariable String dayId) {
        log.info("POST /api/quote-days/{}/duplicate", dayId);
        return quoteDayService.duplicateDay(dayId);
    }

    @PutMapping("/api/quote-days/{dayId}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_QUOTE_DAY')")
    public ResponseEntity<ApiResponse<?>> updateDay(
            @PathVariable String dayId,
            @Valid @RequestBody UpdateQuoteDayDTO dto
    ) {
        log.info("PUT /api/quote-days/{} - updating", dayId);
        return quoteDayService.updateDay(dayId, dto);
    }

    @DeleteMapping("/api/quote-days/{dayId}")
    @PreAuthorize("hasAuthority('PERM_DELETE_QUOTE_DAY')")
    public ResponseEntity<ApiResponse<?>> deleteDay(@PathVariable String dayId) {
        return quoteDayService.deleteDay(dayId);
    }

    /**
     * Bulk-reorder a Quote's days. Client sends an ordered list of day IDs;
     * server reassigns dayNumber = position-in-list (starting at 1) and
     * regenerates the dayTag. Triggers a recalc once at the end.
     */
    @PostMapping("/api/quotes/{quoteId}/days/reorder")
    @PreAuthorize("hasAuthority('PERM_UPDATE_QUOTE_DAY')")
    public ResponseEntity<ApiResponse<?>> reorderDays(
            @PathVariable String quoteId,
            @RequestBody java.util.List<String> orderedDayIds
    ) {
        log.info("POST /api/quotes/{}/days/reorder - {} day ids", quoteId,
                orderedDayIds != null ? orderedDayIds.size() : 0);
        return quoteDayService.reorderDays(quoteId, orderedDayIds);
    }
}
