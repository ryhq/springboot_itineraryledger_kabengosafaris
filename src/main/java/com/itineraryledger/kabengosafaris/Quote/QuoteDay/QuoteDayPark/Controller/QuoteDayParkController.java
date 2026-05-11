package com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.Controller;

import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.DTOs.CreateQuoteDayParkDTO;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.DTOs.UpdateQuoteDayParkDTO;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.Services.QuoteDayParkService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class QuoteDayParkController {

    private final QuoteDayParkService service;

    @GetMapping("/api/quote-days/{dayId}/parks")
    @PreAuthorize("hasAuthority('PERM_READ_QUOTE_DAY_PARK')")
    public ResponseEntity<ApiResponse<?>> list(@PathVariable String dayId) {
        return service.list(dayId);
    }

    @PostMapping("/api/quote-days/{dayId}/parks")
    @PreAuthorize("hasAuthority('PERM_CREATE_QUOTE_DAY_PARK')")
    public ResponseEntity<ApiResponse<?>> create(
            @PathVariable String dayId,
            @Valid @RequestBody CreateQuoteDayParkDTO dto
    ) {
        return service.create(dayId, dto);
    }

    @GetMapping("/api/quote-day-parks/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_QUOTE_DAY_PARK')")
    public ResponseEntity<ApiResponse<?>> getById(@PathVariable String id) {
        return service.getById(id);
    }

    @PutMapping("/api/quote-day-parks/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_QUOTE_DAY_PARK')")
    public ResponseEntity<ApiResponse<?>> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateQuoteDayParkDTO dto
    ) {
        return service.update(id, dto);
    }

    @DeleteMapping("/api/quote-day-parks/{id}")
    @PreAuthorize("hasAuthority('PERM_DELETE_QUOTE_DAY_PARK')")
    public ResponseEntity<ApiResponse<?>> delete(@PathVariable String id) {
        return service.delete(id);
    }
}
