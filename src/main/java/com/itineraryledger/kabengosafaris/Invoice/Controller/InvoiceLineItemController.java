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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.itineraryledger.kabengosafaris.Invoice.DTOs.CreateInvoiceLineItemDTO;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.ReorderInvoiceItemsDTO;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.UpdateInvoiceLineItemDTO;
import com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceLineItemServices.InvoiceLineItemCreateService;
import com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceLineItemServices.InvoiceLineItemDeleteService;
import com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceLineItemServices.InvoiceLineItemGetService;
import com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceLineItemServices.InvoiceLineItemReorderService;
import com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceLineItemServices.InvoiceLineItemUpdateService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/invoices/{invoiceId}/line-items")
@Slf4j
@RequiredArgsConstructor
public class InvoiceLineItemController {

    private final InvoiceLineItemCreateService invoiceLineItemCreateService;
    private final InvoiceLineItemUpdateService invoiceLineItemUpdateService;
    private final InvoiceLineItemDeleteService invoiceLineItemDeleteService;
    private final InvoiceLineItemGetService invoiceLineItemGetService;
    private final InvoiceLineItemReorderService invoiceLineItemReorderService;

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_INVOICE_LINE_ITEM')")
    public ResponseEntity<ApiResponse<?>> createInvoiceLineItem(
        @PathVariable String invoiceId,
        @Valid @RequestBody CreateInvoiceLineItemDTO createDTO
    ) {
        log.info("POST /api/invoices/{}/line-items - Creating new invoice line item", invoiceId);
        return invoiceLineItemCreateService.createInvoiceLineItem(invoiceId, createDTO);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_INVOICE_LINE_ITEM')")
    public ResponseEntity<ApiResponse<?>> getAllInvoiceLineItems(
        @PathVariable String invoiceId,
        @RequestParam(required = false) String itemName,
        @RequestParam(required = false) String description,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size,
        @RequestParam(required = false, defaultValue = "asc") String sortDirection
    ) {
        log.info("GET /api/invoices/{}/line-items - Fetching all invoice line items", invoiceId);
        return invoiceLineItemGetService.getAllInvoiceLineItems(
            invoiceId, itemName, description, isActive, page, size, sortDirection
        );
    }

    @GetMapping("/{itemId}")
    @PreAuthorize("hasAuthority('PERM_READ_INVOICE_LINE_ITEM')")
    public ResponseEntity<ApiResponse<?>> getInvoiceLineItemById(
        @PathVariable String invoiceId,
        @PathVariable String itemId
    ) {
        log.info("GET /api/invoices/{}/line-items/{} - Fetching invoice line item by ID", invoiceId, itemId);
        return invoiceLineItemGetService.getInvoiceLineItemById(invoiceId, itemId);
    }

    @PutMapping("/{itemId}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_INVOICE_LINE_ITEM')")
    public ResponseEntity<ApiResponse<?>> updateInvoiceLineItem(
        @PathVariable String invoiceId,
        @PathVariable String itemId,
        @Valid @RequestBody UpdateInvoiceLineItemDTO updateDTO
    ) {
        log.info("PUT /api/invoices/{}/line-items/{} - Updating invoice line item", invoiceId, itemId);
        return invoiceLineItemUpdateService.updateInvoiceLineItem(invoiceId, itemId, updateDTO);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_INVOICE_LINE_ITEM')")
    public ResponseEntity<ApiResponse<?>> deleteInvoiceLineItems(
        @PathVariable String invoiceId,
        @RequestBody List<String> itemIds
    ) {
        log.info("DELETE /api/invoices/{}/line-items - Deleting {} invoice line items", invoiceId, itemIds.size());
        return invoiceLineItemDeleteService.deleteInvoiceLineItems(invoiceId, itemIds);
    }

    @PostMapping("/reorder")
    @PreAuthorize("hasAuthority('PERM_UPDATE_INVOICE_LINE_ITEM')")
    public ResponseEntity<ApiResponse<?>> reorderInvoiceLineItems(
        @PathVariable String invoiceId,
        @Valid @RequestBody ReorderInvoiceItemsDTO reorderDTO
    ) {
        log.info("POST /api/invoices/{}/line-items/reorder - Reordering invoice line items", invoiceId);
        return invoiceLineItemReorderService.reorderInvoiceLineItems(invoiceId, reorderDTO);
    }
}
