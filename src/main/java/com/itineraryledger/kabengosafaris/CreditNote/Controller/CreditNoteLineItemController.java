package com.itineraryledger.kabengosafaris.CreditNote.Controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.itineraryledger.kabengosafaris.CreditNote.DTOs.CloneInvoiceLineItemsDTO;
import com.itineraryledger.kabengosafaris.CreditNote.DTOs.CreateCreditNoteLineItemDTO;
import com.itineraryledger.kabengosafaris.CreditNote.Services.CreditNoteLineItemServices.CreditNoteLineItemCreateService;
import com.itineraryledger.kabengosafaris.CreditNote.Services.CreditNoteLineItemServices.CreditNoteLineItemDeleteService;
import com.itineraryledger.kabengosafaris.CreditNote.Services.CreditNoteLineItemServices.CreditNoteLineItemGetService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/credit-notes/{creditNoteId}/line-items")
@Slf4j
@RequiredArgsConstructor
public class CreditNoteLineItemController {

    private final CreditNoteLineItemCreateService creditNoteLineItemCreateService;
    private final CreditNoteLineItemDeleteService creditNoteLineItemDeleteService;
    private final CreditNoteLineItemGetService creditNoteLineItemGetService;

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_CREDIT_NOTE')")
    public ResponseEntity<ApiResponse<?>> createLineItem(
        @PathVariable String creditNoteId,
        @Valid @RequestBody CreateCreditNoteLineItemDTO createDTO
    ) {
        log.info("POST /api/credit-notes/{}/line-items - Creating new credit note line item", creditNoteId);
        return creditNoteLineItemCreateService.createCreditNoteLineItem(creditNoteId, createDTO);
    }

    /**
     * Clone selected InvoiceLineItems into this credit note as CreditNoteLineItems.
     * POST /api/credit-notes/{creditNoteId}/line-items/clone-invoice-items
     *
     * Body: { "invoiceLineItemIds": ["abc...", "def..."], "force": false }
     * Each referenced invoice line item must belong to the credit note's invoice.
     */
    @PostMapping("/clone-invoice-items")
    @PreAuthorize("hasAuthority('PERM_CREATE_CREDIT_NOTE')")
    public ResponseEntity<ApiResponse<?>> cloneInvoiceLineItems(
        @PathVariable String creditNoteId,
        @Valid @RequestBody CloneInvoiceLineItemsDTO cloneDTO
    ) {
        log.info("POST /api/credit-notes/{}/line-items/clone-invoice-items - Cloning {} invoice line items",
            creditNoteId, cloneDTO.getInvoiceLineItemIds() != null ? cloneDTO.getInvoiceLineItemIds().size() : 0);
        return creditNoteLineItemCreateService.cloneInvoiceLineItems(creditNoteId, cloneDTO);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_CREDIT_NOTE')")
    public ResponseEntity<ApiResponse<?>> getLineItems(@PathVariable String creditNoteId) {
        log.info("GET /api/credit-notes/{}/line-items - Fetching credit note line items", creditNoteId);
        return creditNoteLineItemGetService.getLineItemsByCreditNote(creditNoteId);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_CREDIT_NOTE')")
    public ResponseEntity<ApiResponse<?>> deleteLineItems(
        @PathVariable String creditNoteId,
        @RequestBody List<String> itemIds
    ) {
        log.info("DELETE /api/credit-notes/{}/line-items - Deleting {} credit note line items", creditNoteId, itemIds.size());
        return creditNoteLineItemDeleteService.deleteCreditNoteLineItems(creditNoteId, itemIds);
    }
}
