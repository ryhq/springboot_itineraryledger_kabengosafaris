package com.itineraryledger.kabengosafaris.Quote.QuotePax.Controller;

import com.itineraryledger.kabengosafaris.Quote.QuotePax.DTOs.UpsertQuotePaxDTO;
import com.itineraryledger.kabengosafaris.Quote.QuotePax.Services.QuotePaxService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quotes/{quoteId}/pax")
@RequiredArgsConstructor
@Slf4j
public class QuotePaxController {

    private final QuotePaxService quotePaxService;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_QUOTE_PAX')")
    public ResponseEntity<ApiResponse<?>> getPax(@PathVariable String quoteId) {
        return quotePaxService.getQuotePax(quoteId);
    }

    @GetMapping("/{paxId}")
    @PreAuthorize("hasAuthority('PERM_READ_QUOTE_PAX')")
    public ResponseEntity<ApiResponse<?>> getPaxById(
            @PathVariable String quoteId,
            @PathVariable String paxId
    ) {
        return quotePaxService.getQuotePaxById(quoteId, paxId);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_UPDATE_QUOTE_PAX')")
    public ResponseEntity<ApiResponse<?>> upsertPax(
            @PathVariable String quoteId,
            @Valid @RequestBody List<UpsertQuotePaxDTO> dtos
    ) {
        log.info("POST /api/quotes/{}/pax — upserting {} pax entries", quoteId, dtos.size());
        return quotePaxService.upsertQuotePax(quoteId, dtos);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_QUOTE_PAX')")
    public ResponseEntity<ApiResponse<?>> deletePax(
            @PathVariable String quoteId,
            @RequestBody List<String> paxIds
    ) {
        log.info("DELETE /api/quotes/{}/pax — deleting {} pax entries", quoteId, paxIds.size());
        return quotePaxService.deleteQuotePax(quoteId, paxIds);
    }
}
