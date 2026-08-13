package com.itineraryledger.kabengosafaris.Invoice.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Invoice.Services.PaymentServices.PaymentListService;
import com.itineraryledger.kabengosafaris.Invoice.Specifications.PaymentFilter;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Money in, across every invoice.
 *
 * A separate controller from the per-invoice one because it answers a different
 * question: that one is mounted under an invoice you already know, and this is for
 * when you do not — a line on a bank statement, a customer asking whether their
 * transfer arrived.
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentListController {

    private final PaymentListService listService;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_INVOICE')")
    public ResponseEntity<ApiResponse<?>> list(
        @ModelAttribute PaymentFilter filter,
        @RequestParam(required = false) Boolean includeStats,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "20") Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/payments");
        return listService.list(filter, includeStats, page, size, sortBy, sortDirection);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_INVOICE')")
    public ResponseEntity<ApiResponse<?>> getOne(
        @PathVariable String id,
        // the list's filters and sort, so prev/next walks that same set
        @ModelAttribute PaymentFilter filter,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection
    ) {
        return listService.getOne(id, filter, sortBy, sortDirection);
    }
}
