package com.itineraryledger.kabengosafaris.Inclusion.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.itineraryledger.kabengosafaris.Inclusion.DTOs.SetDocumentInclusionsDTO;
import com.itineraryledger.kabengosafaris.Inclusion.Services.DocumentInclusionService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * What a quote, safari or invoice says its price covers — read, edited and reset.
 *
 * <p>One controller for the three, because they are one idea: each document inherits from the one
 * above it, may be edited, and may be put back. Splitting it across three files would put the same
 * three-line method in three places and let them drift.
 *
 * <p>Guarded by each document's own permissions rather than an entity of its own. This is a
 * property of the document, not a browsable resource, and the permission catalogue is already near
 * five hundred rows.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DocumentInclusionController {

    private final DocumentInclusionService service;

    /* ---------------------------------------------------------------- quote */

    @GetMapping("/quotes/{id}/inclusions")
    @PreAuthorize("hasAuthority('PERM_READ_QUOTE')")
    public ResponseEntity<ApiResponse<?>> getQuote(@PathVariable String id) {
        return service.getQuote(id);
    }

    @PutMapping("/quotes/{id}/inclusions")
    @PreAuthorize("hasAuthority('PERM_UPDATE_QUOTE')")
    public ResponseEntity<ApiResponse<?>> setQuote(
        @PathVariable String id,
        @Valid @RequestBody SetDocumentInclusionsDTO dto
    ) {
        return service.setQuote(id, dto);
    }

    /** Back to what the itinerary says, discarding anything changed on the quote. */
    @PostMapping("/quotes/{id}/inclusions/reset")
    @PreAuthorize("hasAuthority('PERM_UPDATE_QUOTE')")
    public ResponseEntity<ApiResponse<?>> resetQuote(@PathVariable String id) {
        return service.resetQuote(id);
    }

    /* --------------------------------------------------------------- safari */

    @GetMapping("/safaris/{id}/inclusions")
    @PreAuthorize("hasAuthority('PERM_READ_SAFARI')")
    public ResponseEntity<ApiResponse<?>> getSafari(@PathVariable String id) {
        return service.getSafari(id);
    }

    @PutMapping("/safaris/{id}/inclusions")
    @PreAuthorize("hasAuthority('PERM_UPDATE_SAFARI')")
    public ResponseEntity<ApiResponse<?>> setSafari(
        @PathVariable String id,
        @Valid @RequestBody SetDocumentInclusionsDTO dto
    ) {
        return service.setSafari(id, dto);
    }

    /** Back to the quote the customer accepted, or the itinerary when there was no quote. */
    @PostMapping("/safaris/{id}/inclusions/reset")
    @PreAuthorize("hasAuthority('PERM_UPDATE_SAFARI')")
    public ResponseEntity<ApiResponse<?>> resetSafari(@PathVariable String id) {
        return service.resetSafari(id);
    }

    /* -------------------------------------------------------------- invoice */

    @GetMapping("/invoices/{id}/inclusions")
    @PreAuthorize("hasAuthority('PERM_READ_INVOICE')")
    public ResponseEntity<ApiResponse<?>> getInvoice(@PathVariable String id) {
        return service.getInvoice(id);
    }

    @PutMapping("/invoices/{id}/inclusions")
    @PreAuthorize("hasAuthority('PERM_UPDATE_INVOICE')")
    public ResponseEntity<ApiResponse<?>> setInvoice(
        @PathVariable String id,
        @Valid @RequestBody SetDocumentInclusionsDTO dto
    ) {
        return service.setInvoice(id, dto);
    }

    /** Back to the safari being billed. */
    @PostMapping("/invoices/{id}/inclusions/reset")
    @PreAuthorize("hasAuthority('PERM_UPDATE_INVOICE')")
    public ResponseEntity<ApiResponse<?>> resetInvoice(@PathVariable String id) {
        return service.resetInvoice(id);
    }
}
