package com.itineraryledger.kabengosafaris.Invoice.Controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.itineraryledger.kabengosafaris.Invoice.DTOs.CreateInvoiceShareDTO;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.UpdateInvoiceShareDTO;
import com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceServices.InvoiceShareService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The payers behind one invoice.
 *
 * <p>Two URL spaces, following the house pattern for child collections: the collection hangs off
 * its parent, and a single row is addressable on its own so a click-to-edit save does not need to
 * know which invoice it belongs to.
 *
 * <p>Authority is {@code PERM_UPDATE_INVOICE} throughout rather than a new
 * {@code PERM_*_INVOICE_SHARE} family. Permissions are matched by NAME against
 * {@code permissions/entities.json} plus {@code custom-permissions.json}, and a name neither file
 * generates is denied to everybody, SUPERADMIN included. Deciding who pays which part of a bill
 * is editing the bill, so the invoice's own authority is the honest one anyway.
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class InvoiceShareController {

    private final InvoiceShareService shareService;

    @GetMapping("/api/invoices/{invoiceId}/shares")
    @PreAuthorize("hasAuthority('PERM_READ_INVOICE')")
    public ResponseEntity<ApiResponse<?>> list(@PathVariable String invoiceId) {
        return shareService.list(invoiceId);
    }

    @PostMapping("/api/invoices/{invoiceId}/shares")
    @PreAuthorize("hasAuthority('PERM_UPDATE_INVOICE')")
    public ResponseEntity<ApiResponse<?>> create(
        @PathVariable String invoiceId,
        @Valid @RequestBody CreateInvoiceShareDTO dto
    ) {
        log.info("POST /api/invoices/{}/shares", invoiceId);
        return shareService.create(invoiceId, dto);
    }

    /** Every share across every invoice, for a picker that does not know the parent yet. */
    @GetMapping("/api/invoice-shares")
    @PreAuthorize("hasAuthority('PERM_READ_INVOICE')")
    public ResponseEntity<ApiResponse<?>> listAll(
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size,
        @RequestParam(defaultValue = "updatedAt") String sortBy,
        @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        return shareService.listAll(keyword, page, size, sortBy, sortDirection);
    }

    @GetMapping("/api/invoice-shares/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_INVOICE')")
    public ResponseEntity<ApiResponse<?>> getOne(@PathVariable String id) {
        return shareService.getOne(id);
    }

    @PutMapping("/api/invoice-shares/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_INVOICE')")
    public ResponseEntity<ApiResponse<?>> update(
        @PathVariable String id,
        @Valid @RequestBody UpdateInvoiceShareDTO dto
    ) {
        return shareService.update(id, dto);
    }

    /** Bulk delete, bare array body, per-id outcomes — the house delete contract. */
    @DeleteMapping("/api/invoice-shares")
    @PreAuthorize("hasAuthority('PERM_UPDATE_INVOICE')")
    public ResponseEntity<ApiResponse<?>> delete(@RequestBody List<String> ids) {
        return shareService.delete(ids);
    }
}
