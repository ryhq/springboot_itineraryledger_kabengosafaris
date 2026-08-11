package com.itineraryledger.kabengosafaris.Safari.Controller;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.DTOs.CreateSafariFromItineraryDTO;
import com.itineraryledger.kabengosafaris.Safari.DTOs.UpdateSafariDTO;
import com.itineraryledger.kabengosafaris.Safari.Enums.SafariPhase;
import com.itineraryledger.kabengosafaris.Safari.Enums.SafariState;
import com.itineraryledger.kabengosafaris.Safari.Services.SafariCreateService;
import com.itineraryledger.kabengosafaris.Safari.Services.SafariGetService;
import com.itineraryledger.kabengosafaris.Safari.Services.SafariFullGetService;
import com.itineraryledger.kabengosafaris.Safari.Services.SafariUpdateService;
import com.itineraryledger.kabengosafaris.Safari.Services.SafariDeleteService;
import com.itineraryledger.kabengosafaris.Safari.Services.SafariCostEstimationService;
import com.itineraryledger.kabengosafaris.Safari.Services.SafariCustomerEmailService;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;
import java.time.LocalDate;
import java.util.List;

/**
 * SafariController - REST API endpoints for Safari CRUD operations
 *
 * Note: State transitions are handled by SafariStateTransitionController
 * at /api/safaris/{id}/state/*
 *
 * Base URL: /api/safaris
 */
@RestController
@RequestMapping("/api/safaris")
@Slf4j
public class SafariController {

    private final SafariCreateService safariCreateService;
    private final SafariGetService safariGetService;
    private final SafariFullGetService safariFullGetService;
    private final SafariUpdateService safariUpdateService;
    private final SafariDeleteService safariDeleteService;
    private final SafariCostEstimationService costEstimationService;
    private final SafariCustomerEmailService customerEmailService;
    private final IdObfuscator idObfuscator;

    @Autowired
    public SafariController(
            SafariCreateService safariCreateService,
            SafariGetService safariGetService,
            SafariFullGetService safariFullGetService,
            SafariUpdateService safariUpdateService,
            SafariDeleteService safariDeleteService,
            SafariCostEstimationService costEstimationService,
            SafariCustomerEmailService customerEmailService,
            IdObfuscator idObfuscator
    ) {
        this.safariCreateService = safariCreateService;
        this.safariGetService = safariGetService;
        this.safariFullGetService = safariFullGetService;
        this.safariUpdateService = safariUpdateService;
        this.safariDeleteService = safariDeleteService;
        this.costEstimationService = costEstimationService;
        this.customerEmailService = customerEmailService;
        this.idObfuscator = idObfuscator;
    }

    // ========================
    // CREATE ENDPOINTS
    // ========================

    /**
     * Create a new Safari from an Itinerary template
     *
     * POST /api/safaris
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_SAFARI')")
    public ResponseEntity<ApiResponse<?>> createSafari(
            @Valid @RequestBody CreateSafariFromItineraryDTO dto
    ) {
        return safariCreateService.createSafariFromItinerary(dto);
    }

    // ========================
    // READ ENDPOINTS
    // ========================

    /**
     * Get a single safari by ID
     *
     * GET /api/safaris/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_SAFARI')")
    public ResponseEntity<ApiResponse<?>> getSafariById(
            @PathVariable String id,
            // the list's filters and sort, so prev/next walks that same set
            @ModelAttribute com.itineraryledger.kabengosafaris.Safari.Specifications.SafariFilter filter,
            @RequestParam(required = false) String sortBy
    ) {
        return safariGetService.getSafariById(id, filter, sortBy);
    }

    /**
     * Get a single safari by code
     *
     * GET /api/safaris/code/{code}
     */
    @GetMapping("/code/{code}")
    @PreAuthorize("hasAuthority('PERM_READ_SAFARI')")
    public ResponseEntity<ApiResponse<?>> getSafariByCode(
            @PathVariable String code
    ) {
        return safariGetService.getSafariByCode(code);
    }

    /**
     * Get complete safari with all nested data by ID
     *
     * GET /api/safaris/{id}/full
     */
    @GetMapping("/{id}/full")
    @PreAuthorize("hasAuthority('PERM_READ_SAFARI')")
    public ResponseEntity<ApiResponse<?>> getFullSafari(
            @PathVariable String id
    ) {
        return safariFullGetService.getFullSafari(id);
    }

    /**
     * Get all safaris with filtering, pagination, and sorting
     *
     * GET /api/safaris
     *
     * Query parameters:
     * - name: Filter by name (partial match)
     * - code: Filter by code (partial match)
     * - state: Filter by booking/operational state (DRAFT, CONFIRMED, CANCELLED, etc.)
     * - phase: Filter by time-based phase (FAR_FUTURE, UPCOMING, STARTING_SOON, IN_PROGRESS, etc.)
     * - startLocation: Filter by start location
     * - endLocation: Filter by end location
     * - startDateFrom: Filter safaris starting from this date
     * - startDateTo: Filter safaris starting before this date
     * - isActive: Filter by active status
     * - keyword: Search across multiple fields
     * - page: Page number (0-indexed)
     * - size: Page size
     * - sortDirection: Sort direction (asc/desc)
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_SAFARI')")
    public ResponseEntity<ApiResponse<?>> getAllSafaris(
            @ModelAttribute com.itineraryledger.kabengosafaris.Safari.Specifications.SafariFilter filter,
            @RequestParam(required = false) Boolean includeStats,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        return safariGetService.getAllSafaris(filter, includeStats, page, size, sortBy, sortDirection);
    }

    // ========================
    // UPDATE ENDPOINTS
    // ========================

    /**
     * Update a safari's basic fields (not state)
     *
     * PUT /api/safaris/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_SAFARI')")
    public ResponseEntity<ApiResponse<?>> updateSafari(
            @PathVariable String id,
            @Valid @RequestBody UpdateSafariDTO dto
    ) {
        return safariUpdateService.updateSafari(id, dto);
    }

    // ========================
    // DELETE ENDPOINTS
    // ========================

    /**
     * Delete safaris by list of IDs
     *
     * Only DRAFT safaris can be deleted. Safaris in other states will be skipped.
     *
     * DELETE /api/safaris
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_SAFARI')")
    public ResponseEntity<ApiResponse<?>> deleteSafaris(
            @RequestBody List<String> ids
    ) {
        log.info("DELETE /api/safaris - Deleting {} safaris (only DRAFT allowed)", ids.size());
        return safariDeleteService.deleteSafaris(ids);
    }

    // ========================
    // COST ESTIMATION
    // ========================

    /**
     * Estimate costs for a safari
     *
     * Returns a detailed budget calculation based on:
     * - Park fees (tariffs) for each park visit
     * - Accommodation rates per night
     * - Activity rates (standalone and park activities)
     *
     * Uses the safari's actual start and end dates to determine applicable seasons.
     * Calculates costs based on the configured pax categories and car count.
     *
     * @param id Safari ID (obfuscated)
     * @param useStoRate Whether to use STO (Special Tour Operator) rates vs RACK rates (default: true)
     * @param currency Preferred output currency (default: USD)
     * @return Cost estimation response with detailed breakdown
     */
    @GetMapping("/{id}/estimate-cost")
    @PreAuthorize("hasAuthority('PERM_READ_SAFARI')")
    public ResponseEntity<ApiResponse<?>> estimateCost(
        @PathVariable String id,
        @RequestParam(required = false, defaultValue = "true") Boolean useStoRate,
        @RequestParam(required = false, defaultValue = "USD") String currency
    ) {
        log.info("GET /api/safaris/{}/estimate-cost - Estimating costs (useStoRate: {}, currency: {})",
            id, useStoRate, currency);
        return costEstimationService.estimateCosts(id, useStoRate, currency);
    }

    // ========================
    // CUSTOMER EMAIL ENDPOINTS
    // ========================

    /**
     * Send safari details email to the customer.
     * Auto-populates from safari data. Optional PDF attachment and translation.
     *
     * POST /api/safaris/{id}/send-details
     */
    @PostMapping("/{id}/send-details")
    @PreAuthorize("hasAuthority('PERM_READ_SAFARI')")
    public ResponseEntity<ApiResponse<?>> sendSafariDetails(
        @PathVariable String id,
        @RequestParam(required = false) String language,
        @RequestParam(required = false) String emailTemplateId,
        @RequestParam(required = false) String pdfTemplateId,
        @RequestParam(defaultValue = "true") boolean attachPdf
    ) {
        log.info("POST /api/safaris/{}/send-details (language: {}, attachPdf: {})", id, language, attachPdf);

        Long decodedEmailTemplateId = null;
        if (emailTemplateId != null && !emailTemplateId.isBlank()) {
            decodedEmailTemplateId = idObfuscator.decodeId(emailTemplateId);
        }

        return customerEmailService.sendSafariDetails(id, language, decodedEmailTemplateId, pdfTemplateId, attachPdf);
    }

    /**
     * Send a freeform message to the customer about their safari.
     * Operator provides subject and message body.
     *
     * POST /api/safaris/{id}/send-message
     */
    @PostMapping("/{id}/send-message")
    @PreAuthorize("hasAuthority('PERM_READ_SAFARI')")
    public ResponseEntity<ApiResponse<?>> sendCustomerMessage(
        @PathVariable String id,
        @RequestParam String messageSubject,
        @RequestParam String messageBody,
        @RequestParam(required = false) String language,
        @RequestParam(required = false) String emailTemplateId
    ) {
        log.info("POST /api/safaris/{}/send-message (subject: {})", id, messageSubject);

        Long decodedEmailTemplateId = null;
        if (emailTemplateId != null && !emailTemplateId.isBlank()) {
            decodedEmailTemplateId = idObfuscator.decodeId(emailTemplateId);
        }

        return customerEmailService.sendCustomerMessage(id, messageSubject, messageBody, language, decodedEmailTemplateId);
    }
}
