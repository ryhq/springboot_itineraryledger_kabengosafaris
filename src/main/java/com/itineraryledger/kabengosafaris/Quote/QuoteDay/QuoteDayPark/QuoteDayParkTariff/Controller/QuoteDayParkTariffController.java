package com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkTariff.Controller;

import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkTariff.DTOs.CreateQuoteDayParkTariffDTO;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkTariff.DTOs.UpdateQuoteDayParkTariffDTO;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkTariff.Services.QuoteDayParkTariffService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class QuoteDayParkTariffController {

    private final QuoteDayParkTariffService service;

    @GetMapping("/api/quote-day-parks/{dayParkId}/tariffs")
    @PreAuthorize("hasAuthority('PERM_READ_QUOTE_DAY_PARK_TARIFF')")
    public ResponseEntity<ApiResponse<?>> list(@PathVariable String dayParkId) {
        return service.list(dayParkId);
    }

    @PostMapping("/api/quote-day-parks/{dayParkId}/tariffs")
    @PreAuthorize("hasAuthority('PERM_CREATE_QUOTE_DAY_PARK_TARIFF')")
    public ResponseEntity<ApiResponse<?>> create(
            @PathVariable String dayParkId,
            @Valid @RequestBody CreateQuoteDayParkTariffDTO dto
    ) {
        return service.create(dayParkId, dto);
    }

    @GetMapping("/api/quote-day-park-tariffs/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_QUOTE_DAY_PARK_TARIFF')")
    public ResponseEntity<ApiResponse<?>> getById(@PathVariable String id) {
        return service.getById(id);
    }

    @PutMapping("/api/quote-day-park-tariffs/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_QUOTE_DAY_PARK_TARIFF')")
    public ResponseEntity<ApiResponse<?>> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateQuoteDayParkTariffDTO dto
    ) {
        return service.update(id, dto);
    }

    @DeleteMapping("/api/quote-day-park-tariffs/{id}")
    @PreAuthorize("hasAuthority('PERM_DELETE_QUOTE_DAY_PARK_TARIFF')")
    public ResponseEntity<ApiResponse<?>> delete(@PathVariable String id) {
        return service.delete(id);
    }
}
