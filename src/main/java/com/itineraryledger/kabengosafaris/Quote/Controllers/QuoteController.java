package com.itineraryledger.kabengosafaris.Quote.Controllers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
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

import com.itineraryledger.kabengosafaris.Quote.DTOs.CreateQuoteDTO;
import com.itineraryledger.kabengosafaris.Quote.DTOs.UpdateQuoteDTO;
import com.itineraryledger.kabengosafaris.Quote.Enums.QuoteStatus;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices.QuoteCreateService;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices.QuoteDeleteService;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices.QuoteFromItineraryGenerationService;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices.QuoteFullGetService;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices.QuoteGetService;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices.QuoteUpdateService;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices.QuoteVersionService;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteTotalsCalculationService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * QuoteController - REST controller for managing quotes
 *
 * Provides endpoints for CRUD operations on quotes with permission-based access control
 */
@RestController
@RequestMapping("/api/quotes")
@Slf4j
@RequiredArgsConstructor
public class QuoteController {

    private final QuoteCreateService quoteCreateService;
    private final QuoteUpdateService quoteUpdateService;
    private final QuoteDeleteService quoteDeleteService;
    private final QuoteGetService quoteGetService;
    private final QuoteFullGetService quoteFullGetService;
    private final QuoteFromItineraryGenerationService quoteFromItineraryGenerationService;
    private final QuoteTotalsCalculationService totalsCalculationService;
    private final com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices.QuoteCostEstimationService quoteCostEstimationService;
    private final com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices.QuoteResyncAndTemplateService quoteResyncAndTemplateService;
    private final com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices.QuoteStatusService quoteStatusService;
    private final QuoteVersionService quoteVersionService;
    private final IdObfuscator idObfuscator;

    /**
     * Create a new quote
     *
     * @param createQuoteDTO The quote data
     * @return ResponseEntity with ApiResponse containing the created quote
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_QUOTE')")
    public ResponseEntity<ApiResponse<?>> createQuote(
        @Valid @RequestBody CreateQuoteDTO createQuoteDTO
    ) {
        log.info("POST /api/quotes - Creating new quote: {}", createQuoteDTO.getTitle());
        return quoteCreateService.createQuote(createQuoteDTO);
    }

    /**
     * Generate a quote from an itinerary for a specific customer
     *
     * Creates a quote with items automatically populated from itinerary cost estimation.
     * Uses cost estimation service to calculate prices based on itinerary days, activities,
     * accommodation, and park fees.
     *
     * @param itineraryId The obfuscated itinerary ID (required)
     * @param customerId The obfuscated customer ID (required)
     * @param startDate Optional start date for cost estimation (defaults to today)
     * @param currency Preferred currency for cost estimation (default: USD)
     * @param useStoRate Whether to use STO rates (true) or RACK rates (false) - default: false
     * @param validityDays Number of days the quote should be valid (default: 30)
     * @param taxPercentage Optional tax percentage to apply (can be null)
     * @param discountPercentage Optional discount percentage to apply
     * @param discountReason Optional reason for discount
     * @return ResponseEntity with ApiResponse containing the created quote
     */
    @PostMapping("/generate-from-itinerary")
    @PreAuthorize("hasAuthority('PERM_CREATE_QUOTE')")
    public ResponseEntity<ApiResponse<?>> generateQuoteFromItinerary(
        @RequestParam String itineraryId,
        @RequestParam String customerId,
        @RequestParam(required = false) LocalDate startDate,
        @RequestParam(required = false, defaultValue = "USD") String currency,
        @RequestParam(required = false) Boolean useStoRate,
        @RequestParam(required = false, defaultValue = "30") Integer validityDays,
        @RequestParam(required = false) BigDecimal taxPercentage,
        @RequestParam(required = false) BigDecimal discountPercentage,
        @RequestParam(required = false) String discountReason,
        @RequestParam(required = false) BigDecimal agentCommissionPercentage,
        @RequestParam(required = false) String agentCommissionReason,
        @RequestParam(required = false) BigDecimal marginUpliftPercentage,
        @RequestParam(required = false) String marginUpliftReason,
        @RequestParam(required = false, defaultValue = "false") Boolean condense
    ) {
        log.info("POST /api/quotes/generate-from-itinerary - Generating quote from itinerary: {} for customer: {} (condense={})",
            itineraryId, customerId, condense);
        return quoteFromItineraryGenerationService.generateQuoteFromItinerary(
            itineraryId,
            customerId,
            startDate,
            currency,
            useStoRate,
            validityDays,
            taxPercentage,
            discountPercentage,
            discountReason,
            agentCommissionPercentage,
            agentCommissionReason,
            marginUpliftPercentage,
            marginUpliftReason,
            condense
        );
    }

    /**
     * Update an existing quote (metadata only, not relationships)
     *
     * @param idObfuscated The obfuscated quote ID
     * @param updateQuoteDTO The updated quote data
     * @return ResponseEntity with ApiResponse containing the updated quote
     */
    @PutMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_QUOTE')")
    public ResponseEntity<ApiResponse<?>> updateQuote(
        @PathVariable String idObfuscated,
        @Valid @RequestBody UpdateQuoteDTO updateQuoteDTO
    ) {
        log.info("PUT /api/quotes/{} - Updating quote", idObfuscated);
        return quoteUpdateService.updateQuote(idObfuscated, updateQuoteDTO);
    }

    /**
     * Delete quotes by list of IDs
     *
     * @param idObfuscatedList List of obfuscated quote IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_QUOTE')")
    public ResponseEntity<ApiResponse<?>> deleteQuotes(
        @RequestBody List<String> idObfuscatedList
    ) {
        log.info("DELETE /api/quotes - Deleting {} quotes", idObfuscatedList.size());
        return quoteDeleteService.deleteQuotes(idObfuscatedList);
    }

    /**
     * Get a single quote by ID
     *
     * @param idObfuscated The obfuscated quote ID
     * @return ResponseEntity with ApiResponse containing the quote
     */
    @GetMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_READ_QUOTE')")
    public ResponseEntity<ApiResponse<?>> getQuoteById(
        @PathVariable String idObfuscated
    ) {
        log.info("GET /api/quotes/{} - Fetching quote by ID", idObfuscated);
        return quoteGetService.getQuoteById(idObfuscated);
    }

    /**
     * Get a single quote by quote code
     *
     * @param quoteCode The quote code
     * @return ResponseEntity with ApiResponse containing the quote
     */
    @GetMapping("/code/{quoteCode}")
    @PreAuthorize("hasAuthority('PERM_READ_QUOTE')")
    public ResponseEntity<ApiResponse<?>> getQuoteByCode(
        @PathVariable String quoteCode
    ) {
        log.info("GET /api/quotes/code/{} - Fetching quote by code", quoteCode);
        return quoteGetService.getQuoteByCode(quoteCode);
    }

    /**
     * Get complete quote with all nested data
     *
     * @param idObfuscated The obfuscated quote ID
     * @return ResponseEntity with ApiResponse containing the full quote
     */
    @GetMapping("/{idObfuscated}/full")
    @PreAuthorize("hasAuthority('PERM_READ_QUOTE')")
    public ResponseEntity<ApiResponse<?>> getFullQuote(
        @PathVariable String idObfuscated
    ) {
        log.info("GET /api/quotes/{}/full - Fetching full quote with all nested data", idObfuscated);
        return quoteFullGetService.getFullQuote(idObfuscated);
    }

    /**
     * Manually recalculate quote totals (subtotals, taxes, discounts, grand totals)
     *
     * This endpoint triggers an asynchronous recalculation of all financial totals
     * for the specified quote based on its current items.
     *
     * @param idObfuscated The obfuscated quote ID
     * @return ResponseEntity with ApiResponse confirming recalculation was triggered
     */
    @PostMapping("/{idObfuscated}/recalculate-totals")
    @PreAuthorize("hasAuthority('PERM_UPDATE_QUOTE')")
    public ResponseEntity<ApiResponse<?>> recalculateTotals(
        @PathVariable String idObfuscated
    ) {
        log.info("POST /api/quotes/{}/recalculate-totals - Triggering totals recalculation", idObfuscated);
        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            if (id == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid quote ID", "INVALID_QUOTE_ID")
                );
            }
            totalsCalculationService.recalculateTotals(id);
            return ResponseEntity.ok(
                ApiResponse.success(200, "Totals recalculation triggered successfully", null)
            );
        } catch (Exception e) {
            log.error("Error triggering totals recalculation for quote: {}", idObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to trigger recalculation", "RECALCULATION_FAILED")
            );
        }
    }

    /**
     * Re-derive the Quote's items from its current day-tree × pax mix.
     * Drops the existing items and regenerates them via the shared cost
     * estimation engine, then refreshes totals. No-op for legacy quotes
     * that have no day snapshot.
     */
    @PostMapping("/{idObfuscated}/recalculate-items")
    @PreAuthorize("hasAuthority('PERM_UPDATE_QUOTE')")
    public ResponseEntity<ApiResponse<?>> recalculateItems(@PathVariable String idObfuscated) {
        log.info("POST /api/quotes/{}/recalculate-items - Recalculating items from Quote tree", idObfuscated);
        return quoteCostEstimationService.recalculateItemsForQuote(idObfuscated);
    }

    /**
     * Promote this Quote's customised day-tree back to the Itinerary
     * catalog as a new template (DRAFT status). Useful when sales builds
     * a great variant on a quote and wants to reuse it for future customers.
     */
    @PostMapping("/{idObfuscated}/save-as-itinerary")
    @PreAuthorize("hasAuthority('PERM_CREATE_ITINERARY')")
    public ResponseEntity<ApiResponse<?>> saveAsItinerary(
            @PathVariable String idObfuscated,
            @RequestBody(required = false) java.util.Map<String, String> body
    ) {
        String name = body != null ? body.get("name") : null;
        String description = body != null ? body.get("description") : null;
        log.info("POST /api/quotes/{}/save-as-itinerary — promoting to template", idObfuscated);
        return quoteResyncAndTemplateService.saveAsItinerary(idObfuscated, name, description);
    }

    /**
     * Destructive: wipes the Quote's snapshot and re-snapshots from the
     * source Itinerary. All per-customer customisations are discarded.
     * Items are re-derived from the fresh tree.
     */
    @PostMapping("/{idObfuscated}/resync-from-itinerary")
    @PreAuthorize("hasAuthority('PERM_UPDATE_QUOTE')")
    public ResponseEntity<ApiResponse<?>> resyncFromItinerary(@PathVariable String idObfuscated) {
        log.info("POST /api/quotes/{}/resync-from-itinerary — wiping and re-snapshotting", idObfuscated);
        return quoteResyncAndTemplateService.resyncFromItinerary(idObfuscated);
    }

    /**
     * Get all quotes with pagination, sorting, and filtering
     *
     * @param quoteCode Filter by quote code (partial match)
     * @param title Filter by title (partial match)
     * @param status Filter by status
     * @param itineraryId Filter by itinerary ID (obfuscated)
     * @param customerId Filter by customer ID (obfuscated)
     * @param approverId Filter by approver ID (obfuscated)
     * @param approvedById Filter by approved by ID (obfuscated)
     * @param createdById Filter by created by ID (obfuscated)
     * @param updatedById Filter by updated by ID (obfuscated)
     * @param isStoRate Filter by STO rate flag
     * @param isActive Filter by active status
     * @param validOn Filter by valid on date
     * @param sentAfter Filter by sent after date
     * @param sentBefore Filter by sent before date
     * @param version Filter by version number
     * @param statusGroup Filter by status group (draft, pending, active, closed)
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortDirection Sort direction (asc/desc, default: desc)
     * @return ResponseEntity with ApiResponse containing paginated quotes
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_QUOTE')")
    public ResponseEntity<ApiResponse<?>> getAllQuotes(
        @RequestParam(required = false) String quoteCode,
        @RequestParam(required = false) String title,
        @RequestParam(required = false) QuoteStatus status,
        @RequestParam(required = false) String itineraryId,
        @RequestParam(required = false) String customerId,
        @RequestParam(required = false) String approverId,
        @RequestParam(required = false) String approvedById,
        @RequestParam(required = false) String createdById,
        @RequestParam(required = false) String updatedById,
        @RequestParam(required = false) Boolean isStoRate,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) LocalDate validOn,
        @RequestParam(required = false) LocalDate sentAfter,
        @RequestParam(required = false) LocalDate sentBefore,
        @RequestParam(required = false) Integer version,
        @RequestParam(required = false) String statusGroup,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/quotes - Fetching all quotes with filters");
        return quoteGetService.getAllQuotes(
            quoteCode,
            title,
            status,
            itineraryId,
            customerId,
            approverId,
            approvedById,
            createdById,
            updatedById,
            isStoRate,
            isActive,
            validOn,
            sentAfter,
            sentBefore,
            version,
            statusGroup,
            page,
            size,
            sortBy,
            sortDirection
        );
    }

    // ========================
    // STATUS MANAGEMENT ENDPOINTS
    // ========================

    /**
     * Mark quote as READY (ready to send to customer)
     * Only allowed from DRAFT status
     */
    @PostMapping("/{id}/mark-ready")
    @PreAuthorize("hasAuthority('PERM_READY_QUOTE')")
    public ResponseEntity<ApiResponse<?>> markAsReady(
        @PathVariable String id
    ) {
        log.info("POST /api/quotes/{}/mark-ready - Marking quote as ready", id);
        return quoteStatusService.markAsReady(id);
    }

    /**
     * Send quote to customer
     * Allowed from DRAFT (if ready), READY, SENT (resend), ACCEPTED (resend), CONVERTED (resend)
     *
     * @param id The obfuscated quote ID
     * @param language Optional language code for translating the email and PDF (e.g., "fr", "de", "sw").
     *                 Falls back to the customer's preferredLanguage if not provided.
     * @param emailTemplateId Optional obfuscated ID of a specific SEND_QUOTE email template to use.
     *                        Falls back to the default template if not provided.
     * @param pdfTemplateId Optional obfuscated ID of a specific PDF template to use for quote PDF generation.
     *                      Falls back to the default PDF template if not provided.
     * @param saveAsQuoteDocument Whether to save the generated PDF as a quote document (default: true)
     * @param saveAsCustomerDocument Whether to save the generated PDF as a customer document (default: true)
     */
    @PostMapping("/{id}/send")
    @PreAuthorize("hasAuthority('PERM_SEND_QUOTE')")
    public ResponseEntity<ApiResponse<?>> sendQuote(
        @PathVariable String id,
        @RequestParam(required = false) String language,
        @RequestParam(required = false) String emailTemplateId,
        @RequestParam(required = false) String pdfTemplateId,
        @RequestParam(defaultValue = "true") boolean saveAsQuoteDocument,
        @RequestParam(defaultValue = "true") boolean saveAsCustomerDocument
    ) {
        log.info("POST /api/quotes/{}/send - Sending quote (language: {}, emailTemplateId: {}, pdfTemplateId: {}, saveQuoteDoc: {}, saveCustomerDoc: {})",
            id, language, emailTemplateId, pdfTemplateId, saveAsQuoteDocument, saveAsCustomerDocument);
        return quoteStatusService.sendQuote(id, language, emailTemplateId, pdfTemplateId, saveAsQuoteDocument, saveAsCustomerDocument);
    }

    /**
     * Mark quote as ACCEPTED by customer
     * Only allowed from SENT status
     */
    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAuthority('PERM_ACCEPT_QUOTE')")
    public ResponseEntity<ApiResponse<?>> acceptQuote(
        @PathVariable String id
    ) {
        log.info("POST /api/quotes/{}/accept - Marking quote as accepted", id);
        return quoteStatusService.acceptQuote(id);
    }

    /**
     * Mark quote as REJECTED by customer
     * Only allowed from SENT status
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('PERM_REJECT_QUOTE')")
    public ResponseEntity<ApiResponse<?>> rejectQuote(
        @PathVariable String id
    ) {
        log.info("POST /api/quotes/{}/reject - Marking quote as rejected", id);
        return quoteStatusService.rejectQuote(id);
    }

    /**
     * Mark quote as EXPIRED
     * Allowed from SENT or READY status
     */
    @PostMapping("/{id}/expire")
    @PreAuthorize("hasAuthority('PERM_EXPIRE_QUOTE')")
    public ResponseEntity<ApiResponse<?>> expireQuote(
        @PathVariable String id
    ) {
        log.info("POST /api/quotes/{}/expire - Marking quote as expired", id);
        return quoteStatusService.expireQuote(id);
    }

    /**
     * Cancel a quote
     * Allowed from any status except CONVERTED
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('PERM_CANCEL_QUOTE')")
    public ResponseEntity<ApiResponse<?>> cancelQuote(
        @PathVariable String id
    ) {
        log.info("POST /api/quotes/{}/cancel - Cancelling quote", id);
        return quoteStatusService.cancelQuote(id);
    }

    /**
     * Revert quote to DRAFT status
     * Allowed from READY or SENT status
     */
    @PostMapping("/{id}/revert-to-draft")
    @PreAuthorize("hasAuthority('PERM_REVERT_QUOTE_TO_DRAFT')")
    public ResponseEntity<ApiResponse<?>> revertToDraft(
        @PathVariable String id
    ) {
        log.info("POST /api/quotes/{}/revert-to-draft - Reverting quote to draft", id);
        return quoteStatusService.revertToDraft(id);
    }

    /**
     * Convert quote to booking/safari
     * Only allowed from ACCEPTED status
     *
     * Creates a Safari (DRAFT) from the quote's itinerary and customer.
     * Uses the quote's safariStartDate by default (the date used for cost estimation).
     * Invoice is NOT created here — it is created later when the safari reaches
     * CONFIRMED state via POST /api/safaris/{id}/state/request-payment.
     *
     * @param id The obfuscated quote ID
     * @param startDate Optional override for the safari start date. If not provided, uses the
     *                  quote's safariStartDate (the date used for cost estimation, ensuring season/rate consistency).
     */
    @PostMapping("/{id}/convert")
    @PreAuthorize("hasAuthority('PERM_CONVERT_QUOTE')")
    public ResponseEntity<ApiResponse<?>> convertQuote(
        @PathVariable String id,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate
    ) {
        log.info("POST /api/quotes/{}/convert - Converting quote to booking (startDate: {})", id, startDate);
        return quoteStatusService.convertQuote(id, startDate);
    }

    /**
     * Create a new version of a quote
     * Allowed from SENT, ACCEPTED, REJECTED, EXPIRED, CANCELLED statuses.
     * Deep copies the quote into a new DRAFT with incremented version.
     * If startDate differs from original, items are re-generated from itinerary cost estimation.
     * Unspecified params inherit from the original quote.
     *
     * @param id The obfuscated quote ID
     * @param versionNotes Optional notes explaining what changed
     * @param startDate Optional new safari start date (inherits from original if not provided)
     * @param currency Optional currency override (inherits from original if not provided)
     * @param useStoRate Optional STO rate flag override (inherits from original if not provided)
     * @param validityDays Validity period in days from today (default: 30)
     * @param taxPercentage Optional tax percentage override (inherits from original if not provided)
     * @param discountPercentage Optional discount percentage override (inherits from original if not provided)
     * @param discountReason Optional discount reason override (inherits from original if not provided)
     */
    @PostMapping("/{id}/create-new-version")
    @PreAuthorize("hasAuthority('PERM_CREATE_QUOTE')")
    public ResponseEntity<ApiResponse<?>> createNewVersion(
        @PathVariable String id,
        @RequestParam(required = false) String versionNotes,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) String currency,
        @RequestParam(required = false) Boolean useStoRate,
        @RequestParam(required = false, defaultValue = "30") Integer validityDays,
        @RequestParam(required = false) BigDecimal taxPercentage,
        @RequestParam(required = false) BigDecimal discountPercentage,
        @RequestParam(required = false) String discountReason
    ) {
        log.info("POST /api/quotes/{}/create-new-version - Creating new version (startDate: {}, currency: {}, useStoRate: {})",
            id, startDate, currency, useStoRate);
        return quoteVersionService.createNewVersion(id, versionNotes, startDate, currency, useStoRate,
            validityDays, taxPercentage, discountPercentage, discountReason);
    }
}
