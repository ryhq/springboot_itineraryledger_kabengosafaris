package com.itineraryledger.kabengosafaris.Quote.QuoteDay.Controller;

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

    @PutMapping("/api/quote-days/{dayId}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_QUOTE_DAY')")
    public ResponseEntity<ApiResponse<?>> updateDay(
            @PathVariable String dayId,
            @Valid @RequestBody UpdateQuoteDayDTO dto
    ) {
        log.info("PUT /api/quote-days/{} — updating", dayId);
        return quoteDayService.updateDay(dayId, dto);
    }

    @DeleteMapping("/api/quote-days/{dayId}")
    @PreAuthorize("hasAuthority('PERM_DELETE_QUOTE_DAY')")
    public ResponseEntity<ApiResponse<?>> deleteDay(@PathVariable String dayId) {
        return quoteDayService.deleteDay(dayId);
    }
}
