package com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayAccommodation.Controller;

import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayAccommodation.DTOs.CreateQuoteDayAccommodationDTO;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayAccommodation.DTOs.UpdateQuoteDayAccommodationDTO;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayAccommodation.Services.QuoteDayAccommodationService;
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
public class QuoteDayAccommodationController {

    private final QuoteDayAccommodationService service;

    @GetMapping("/api/quote-days/{dayId}/accommodations")
    @PreAuthorize("hasAuthority('PERM_READ_QUOTE_DAY_ACCOMMODATION')")
    public ResponseEntity<ApiResponse<?>> list(@PathVariable String dayId) {
        return service.list(dayId);
    }

    @PostMapping("/api/quote-days/{dayId}/accommodations")
    @PreAuthorize("hasAuthority('PERM_CREATE_QUOTE_DAY_ACCOMMODATION')")
    public ResponseEntity<ApiResponse<?>> create(
            @PathVariable String dayId,
            @Valid @RequestBody CreateQuoteDayAccommodationDTO dto
    ) {
        return service.create(dayId, dto);
    }

    @GetMapping("/api/quote-day-accommodations/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_QUOTE_DAY_ACCOMMODATION')")
    public ResponseEntity<ApiResponse<?>> getById(@PathVariable String id) {
        return service.getById(id);
    }

    @PutMapping("/api/quote-day-accommodations/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_QUOTE_DAY_ACCOMMODATION')")
    public ResponseEntity<ApiResponse<?>> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateQuoteDayAccommodationDTO dto
    ) {
        return service.update(id, dto);
    }

    @DeleteMapping("/api/quote-day-accommodations/{id}")
    @PreAuthorize("hasAuthority('PERM_DELETE_QUOTE_DAY_ACCOMMODATION')")
    public ResponseEntity<ApiResponse<?>> delete(@PathVariable String id) {
        return service.delete(id);
    }
}
