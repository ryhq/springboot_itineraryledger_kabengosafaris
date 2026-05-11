package com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayActivity.Controller;

import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayActivity.DTOs.CreateQuoteDayActivityDTO;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayActivity.DTOs.UpdateQuoteDayActivityDTO;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayActivity.Services.QuoteDayActivityService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class QuoteDayActivityController {

    private final QuoteDayActivityService service;

    @GetMapping("/api/quote-days/{dayId}/activities")
    @PreAuthorize("hasAuthority('PERM_READ_QUOTE_DAY_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> list(@PathVariable String dayId) {
        return service.list(dayId);
    }

    @PostMapping("/api/quote-days/{dayId}/activities")
    @PreAuthorize("hasAuthority('PERM_CREATE_QUOTE_DAY_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> create(
            @PathVariable String dayId,
            @Valid @RequestBody CreateQuoteDayActivityDTO dto
    ) {
        return service.create(dayId, dto);
    }

    @GetMapping("/api/quote-day-activities/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_QUOTE_DAY_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> getById(@PathVariable String id) {
        return service.getById(id);
    }

    @PutMapping("/api/quote-day-activities/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_QUOTE_DAY_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateQuoteDayActivityDTO dto
    ) {
        return service.update(id, dto);
    }

    @DeleteMapping("/api/quote-day-activities/{id}")
    @PreAuthorize("hasAuthority('PERM_DELETE_QUOTE_DAY_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> delete(@PathVariable String id) {
        return service.delete(id);
    }

    @PostMapping("/api/quote-days/{dayId}/activities/reorder")
    @PreAuthorize("hasAuthority('PERM_UPDATE_QUOTE_DAY_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> reorder(
            @PathVariable String dayId,
            @RequestBody java.util.List<String> orderedIds
    ) {
        return service.reorder(dayId, orderedIds);
    }
}
