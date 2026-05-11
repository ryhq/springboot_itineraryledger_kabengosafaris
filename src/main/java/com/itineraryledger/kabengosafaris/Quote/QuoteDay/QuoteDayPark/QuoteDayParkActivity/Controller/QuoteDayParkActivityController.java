package com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkActivity.Controller;

import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkActivity.DTOs.CreateQuoteDayParkActivityDTO;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkActivity.DTOs.UpdateQuoteDayParkActivityDTO;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkActivity.Services.QuoteDayParkActivityService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class QuoteDayParkActivityController {

    private final QuoteDayParkActivityService service;

    @GetMapping("/api/quote-day-parks/{dayParkId}/activities")
    @PreAuthorize("hasAuthority('PERM_READ_QUOTE_DAY_PARK_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> list(@PathVariable String dayParkId) {
        return service.list(dayParkId);
    }

    @PostMapping("/api/quote-day-parks/{dayParkId}/activities")
    @PreAuthorize("hasAuthority('PERM_CREATE_QUOTE_DAY_PARK_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> create(
            @PathVariable String dayParkId,
            @Valid @RequestBody CreateQuoteDayParkActivityDTO dto
    ) {
        return service.create(dayParkId, dto);
    }

    @GetMapping("/api/quote-day-park-activities/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_QUOTE_DAY_PARK_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> getById(@PathVariable String id) {
        return service.getById(id);
    }

    @PutMapping("/api/quote-day-park-activities/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_QUOTE_DAY_PARK_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateQuoteDayParkActivityDTO dto
    ) {
        return service.update(id, dto);
    }

    @DeleteMapping("/api/quote-day-park-activities/{id}")
    @PreAuthorize("hasAuthority('PERM_DELETE_QUOTE_DAY_PARK_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> delete(@PathVariable String id) {
        return service.delete(id);
    }
}
