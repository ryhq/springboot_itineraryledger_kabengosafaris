package com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayFlight.Controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayFlight.DTOs.CreateQuoteDayFlightDTO;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayFlight.DTOs.UpdateQuoteDayFlightDTO;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayFlight.Services.QuoteDayFlightService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Flights on a day of a quote.
 *
 * <p>Flat paths keyed on the day and on the row, matching the quote's other day children rather
 * than the itinerary's nested shape. The two families genuinely differ here and following the
 * local convention is worth more than looking like the itinerary.
 */
@RestController
@RequiredArgsConstructor
public class QuoteDayFlightController {

    private final QuoteDayFlightService service;

    @GetMapping("/api/quote-days/{dayId}/flights")
    @PreAuthorize("hasAuthority('PERM_READ_QUOTE_DAY_FLIGHT')")
    public ResponseEntity<ApiResponse<?>> list(@PathVariable String dayId) {
        return service.getAll(dayId);
    }

    @PostMapping("/api/quote-days/{dayId}/flights")
    @PreAuthorize("hasAuthority('PERM_CREATE_QUOTE_DAY_FLIGHT')")
    public ResponseEntity<ApiResponse<?>> create(@PathVariable String dayId,
                                                 @Valid @RequestBody CreateQuoteDayFlightDTO dto) {
        return service.create(dayId, dto);
    }

    @GetMapping("/api/quote-day-flights/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_QUOTE_DAY_FLIGHT')")
    public ResponseEntity<ApiResponse<?>> getById(@PathVariable String id) {
        return service.getById(id);
    }

    @PutMapping("/api/quote-day-flights/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_QUOTE_DAY_FLIGHT')")
    public ResponseEntity<ApiResponse<?>> update(@PathVariable String id,
                                                 @Valid @RequestBody UpdateQuoteDayFlightDTO dto) {
        return service.update(id, dto);
    }

    /** Make this the day's primary flight and demote the rest, atomically. */
    @PostMapping("/api/quote-day-flights/{id}/make-primary")
    @PreAuthorize("hasAuthority('PERM_UPDATE_QUOTE_DAY_FLIGHT')")
    public ResponseEntity<ApiResponse<?>> makePrimary(@PathVariable String id) {
        return service.makePrimary(id);
    }

    @DeleteMapping("/api/quote-day-flights")
    @PreAuthorize("hasAuthority('PERM_DELETE_QUOTE_DAY_FLIGHT')")
    public ResponseEntity<ApiResponse<?>> delete(@RequestBody List<String> ids) {
        return service.delete(ids);
    }
}
