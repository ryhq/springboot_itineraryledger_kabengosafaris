package com.itineraryledger.kabengosafaris.Quotation.Controller;

import com.itineraryledger.kabengosafaris.Quotation.DTOs.CreateQuotationDTO;
import com.itineraryledger.kabengosafaris.Quotation.DTOs.UpdateQuotationDTO;
import com.itineraryledger.kabengosafaris.Quotation.Enums.QuotationStatus;
import com.itineraryledger.kabengosafaris.Quotation.Services.*;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * QuotationController - REST controller for managing quotations
 *
 * Endpoints:
 * - POST   /api/quotations                       - Create quotation
 * - GET    /api/quotations                       - List with filters
 * - GET    /api/quotations/{id}                  - Get by ID
 * - GET    /api/quotations/code/{code}           - Get by code
 * - PUT    /api/quotations/{id}                  - Update quotation
 * - DELETE /api/quotations                       - Bulk delete
 * - POST   /api/quotations/{id}/send             - Send to customer
 * - POST   /api/quotations/{id}/mark-viewed      - Mark as viewed
 * - POST   /api/quotations/{id}/revise           - Create revision
 * - POST   /api/quotations/{id}/accept           - Mark as accepted
 * - POST   /api/quotations/{id}/reject           - Mark as rejected
 * - POST   /api/quotations/{id}/cancel           - Cancel quotation
 * - GET    /api/quotations/customer/{customerId} - Customer's quotes
 */
@RestController
@RequestMapping("/api/quotations")
@RequiredArgsConstructor
@Slf4j
public class QuotationController {

    private final QuotationCreateService createService;
    private final QuotationGetService getService;
    private final QuotationUpdateService updateService;
    private final QuotationDeleteService deleteService;
    private final QuotationStatusService statusService;

    // ========================
    // CREATE
    // ========================

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_QUOTATION')")
    public ResponseEntity<ApiResponse<?>> createQuotation(
            @Valid @RequestBody CreateQuotationDTO createDTO
    ) {
        log.info("POST /api/quotations - Creating new quotation");
        return createService.createQuotation(createDTO);
    }

    // ========================
    // READ
    // ========================

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_QUOTATION')")
    public ResponseEntity<ApiResponse<?>> getQuotationById(
            @PathVariable String id
    ) {
        log.info("GET /api/quotations/{} - Fetching quotation", id);
        return getService.getQuotationById(id);
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAuthority('PERM_READ_QUOTATION')")
    public ResponseEntity<ApiResponse<?>> getQuotationByCode(
            @PathVariable String code
    ) {
        log.info("GET /api/quotations/code/{} - Fetching quotation by code", code);
        return getService.getQuotationByCode(code);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_QUOTATION')")
    public ResponseEntity<ApiResponse<?>> getAllQuotations(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String itineraryId,
            @RequestParam(required = false) QuotationStatus status,
            @RequestParam(required = false) String assignedToId,
            @RequestParam(required = false) String createdById,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo,
            @RequestParam(required = false) Boolean expired,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) Boolean originalOnly,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/quotations - Fetching all quotations");

        Sort sort = sortDirection.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        return getService.getAllQuotations(
                customerId, itineraryId, status, assignedToId, createdById,
                name, code, startDateFrom, startDateTo, createdFrom, createdTo,
                expired, currency, originalOnly, keyword, pageable
        );
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAuthority('PERM_READ_QUOTATION')")
    public ResponseEntity<ApiResponse<?>> getCustomerQuotations(
            @PathVariable String customerId,
            @RequestParam(required = false) QuotationStatus status,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/quotations/customer/{} - Fetching customer quotations", customerId);

        Sort sort = sortDirection.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        return getService.getCustomerQuotations(customerId, status, pageable);
    }

    // ========================
    // UPDATE
    // ========================

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_QUOTATION')")
    public ResponseEntity<ApiResponse<?>> updateQuotation(
            @PathVariable String id,
            @Valid @RequestBody UpdateQuotationDTO updateDTO
    ) {
        log.info("PUT /api/quotations/{} - Updating quotation", id);
        return updateService.updateQuotation(id, updateDTO);
    }

    // ========================
    // DELETE
    // ========================

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_QUOTATION')")
    public ResponseEntity<ApiResponse<?>> deleteQuotations(
            @RequestBody List<String> ids
    ) {
        log.info("DELETE /api/quotations - Deleting {} quotations", ids.size());
        return deleteService.deleteQuotations(ids);
    }

    // ========================
    // STATUS TRANSITIONS
    // ========================

    @PostMapping("/{id}/send")
    @PreAuthorize("hasAuthority('PERM_SEND_QUOTATION')")
    public ResponseEntity<ApiResponse<?>> sendQuotation(
            @PathVariable String id
    ) {
        log.info("POST /api/quotations/{}/send - Sending quotation", id);
        return statusService.sendQuotation(id);
    }

    @PostMapping("/{id}/mark-viewed")
    @PreAuthorize("hasAuthority('PERM_UPDATE_QUOTATION')")
    public ResponseEntity<ApiResponse<?>> markQuotationAsViewed(
            @PathVariable String id
    ) {
        log.info("POST /api/quotations/{}/mark-viewed - Marking quotation as viewed", id);
        return statusService.markAsViewed(id);
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAuthority('PERM_UPDATE_QUOTATION')")
    public ResponseEntity<ApiResponse<?>> acceptQuotation(
            @PathVariable String id
    ) {
        log.info("POST /api/quotations/{}/accept - Accepting quotation", id);
        return statusService.acceptQuotation(id);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('PERM_UPDATE_QUOTATION')")
    public ResponseEntity<ApiResponse<?>> rejectQuotation(
            @PathVariable String id,
            @RequestParam(required = false) String reason
    ) {
        log.info("POST /api/quotations/{}/reject - Rejecting quotation", id);
        return statusService.rejectQuotation(id, reason);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('PERM_UPDATE_QUOTATION')")
    public ResponseEntity<ApiResponse<?>> cancelQuotation(
            @PathVariable String id
    ) {
        log.info("POST /api/quotations/{}/cancel - Cancelling quotation", id);
        return statusService.cancelQuotation(id);
    }

    @PostMapping("/{id}/revise")
    @PreAuthorize("hasAuthority('PERM_CREATE_QUOTATION')")
    public ResponseEntity<ApiResponse<?>> reviseQuotation(
            @PathVariable String id
    ) {
        log.info("POST /api/quotations/{}/revise - Creating quotation revision", id);
        return statusService.reviseQuotation(id);
    }
}
