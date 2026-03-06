package com.itineraryledger.kabengosafaris.Invoice.Controller;

import java.time.LocalDate;
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

import com.itineraryledger.kabengosafaris.Invoice.DTOs.CreateInvoiceDTO;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.CreateInvoiceFromSafariDTO;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.UpdateInvoiceDTO;
import com.itineraryledger.kabengosafaris.Invoice.Enums.InvoiceStatus;
import com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceServices.InvoiceCreateService;
import com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceServices.InvoiceDeleteService;
import com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceServices.InvoiceFromSafariGenerationService;
import com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceServices.InvoiceFullGetService;
import com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceServices.InvoiceGetService;
import com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceServices.InvoiceTotalsCalculationService;
import com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceServices.InvoiceUpdateService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/invoices")
@Slf4j
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceCreateService invoiceCreateService;
    private final InvoiceUpdateService invoiceUpdateService;
    private final InvoiceDeleteService invoiceDeleteService;
    private final InvoiceGetService invoiceGetService;
    private final InvoiceFullGetService invoiceFullGetService;
    private final InvoiceFromSafariGenerationService invoiceFromSafariGenerationService;
    private final InvoiceTotalsCalculationService totalsCalculationService;
    private final IdObfuscator idObfuscator;

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_INVOICE')")
    public ResponseEntity<ApiResponse<?>> createInvoice(@Valid @RequestBody CreateInvoiceDTO createInvoiceDTO) {
        log.info("POST /api/invoices - Creating new invoice: {}", createInvoiceDTO.getTitle());
        return invoiceCreateService.createInvoice(createInvoiceDTO);
    }

    @PostMapping("/from-safari")
    @PreAuthorize("hasAuthority('PERM_CREATE_INVOICE')")
    public ResponseEntity<ApiResponse<?>> generateInvoiceFromSafari(@Valid @RequestBody CreateInvoiceFromSafariDTO dto) {
        log.info("POST /api/invoices/from-safari - Generating invoice from safari: {}", dto.getSafariId());
        return invoiceFromSafariGenerationService.generateInvoiceFromSafari(dto);
    }

    @PutMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_INVOICE')")
    public ResponseEntity<ApiResponse<?>> updateInvoice(
        @PathVariable String idObfuscated,
        @Valid @RequestBody UpdateInvoiceDTO updateInvoiceDTO
    ) {
        log.info("PUT /api/invoices/{} - Updating invoice", idObfuscated);
        return invoiceUpdateService.updateInvoice(idObfuscated, updateInvoiceDTO);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_INVOICE')")
    public ResponseEntity<ApiResponse<?>> deleteInvoices(@RequestBody List<String> idObfuscatedList) {
        log.info("DELETE /api/invoices - Deleting {} invoices", idObfuscatedList.size());
        return invoiceDeleteService.deleteInvoices(idObfuscatedList);
    }

    @GetMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_READ_INVOICE')")
    public ResponseEntity<ApiResponse<?>> getInvoiceById(@PathVariable String idObfuscated) {
        log.info("GET /api/invoices/{} - Fetching invoice by ID", idObfuscated);
        return invoiceGetService.getInvoiceById(idObfuscated);
    }

    @GetMapping("/code/{invoiceCode}")
    @PreAuthorize("hasAuthority('PERM_READ_INVOICE')")
    public ResponseEntity<ApiResponse<?>> getInvoiceByCode(@PathVariable String invoiceCode) {
        log.info("GET /api/invoices/code/{} - Fetching invoice by code", invoiceCode);
        return invoiceGetService.getInvoiceByCode(invoiceCode);
    }

    @GetMapping("/{idObfuscated}/full")
    @PreAuthorize("hasAuthority('PERM_READ_INVOICE')")
    public ResponseEntity<ApiResponse<?>> getFullInvoice(@PathVariable String idObfuscated) {
        log.info("GET /api/invoices/{}/full - Fetching full invoice with all nested data", idObfuscated);
        return invoiceFullGetService.getFullInvoice(idObfuscated);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_INVOICE')")
    public ResponseEntity<ApiResponse<?>> getAllInvoices(
        @RequestParam(required = false) String invoiceCode,
        @RequestParam(required = false) String title,
        @RequestParam(required = false) InvoiceStatus status,
        @RequestParam(required = false) String customerId,
        @RequestParam(required = false) String safariId,
        @RequestParam(required = false) String createdById,
        @RequestParam(required = false) String updatedById,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) LocalDate issueDateAfter,
        @RequestParam(required = false) LocalDate issueDateBefore,
        @RequestParam(required = false) LocalDate dueDateAfter,
        @RequestParam(required = false) LocalDate dueDateBefore,
        @RequestParam(required = false) LocalDate sentAfter,
        @RequestParam(required = false) LocalDate sentBefore,
        @RequestParam(required = false) Boolean isOverdue,
        @RequestParam(required = false) String statusGroup,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/invoices - Fetching all invoices with filters");
        return invoiceGetService.getAllInvoices(
            invoiceCode, title, status, customerId, safariId,
            createdById, updatedById, isActive, issueDateAfter, issueDateBefore,
            dueDateAfter, dueDateBefore, sentAfter, sentBefore, isOverdue,
            statusGroup, page, size, sortBy, sortDirection
        );
    }

    @PostMapping("/{idObfuscated}/recalculate-totals")
    @PreAuthorize("hasAuthority('PERM_UPDATE_INVOICE')")
    public ResponseEntity<ApiResponse<?>> recalculateTotals(@PathVariable String idObfuscated) {
        log.info("POST /api/invoices/{}/recalculate-totals - Recalculating invoice totals", idObfuscated);
        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            totalsCalculationService.recalculateTotals(id);
            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Invoice totals recalculation triggered successfully", null)
            );
        } catch (Exception e) {
            log.error("Error recalculating invoice totals", e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Failed to recalculate invoice totals", "RECALCULATION_FAILED")
            );
        }
    }
}
