package com.itineraryledger.kabengosafaris.Itinerary.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Itinerary.Entity.BudgetCategory;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.TripType;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary.ItineraryStatus;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.CreateItineraryDTO;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.DuplicateItineraryDTO;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.UpdateItineraryDTO;
import com.itineraryledger.kabengosafaris.Itinerary.Services.ItineraryCostEstimationService;
import com.itineraryledger.kabengosafaris.Itinerary.Services.ItineraryCreateService;
import com.itineraryledger.kabengosafaris.Itinerary.Services.ItineraryDeleteService;
import com.itineraryledger.kabengosafaris.Itinerary.Services.ItineraryDuplicateService;
import com.itineraryledger.kabengosafaris.Itinerary.Services.ItineraryFullGetService;
import com.itineraryledger.kabengosafaris.Itinerary.Services.ItineraryGetService;
import com.itineraryledger.kabengosafaris.Itinerary.Services.ItineraryUpdateService;
import com.itineraryledger.kabengosafaris.Itinerary.Services.ItineraryStatusService;

import java.time.LocalDate;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryController - REST controller for managing safari itineraries
 */
@RestController
@RequestMapping("/api/itineraries")
@Slf4j
public class ItineraryController {

    private final ItineraryCreateService createService;
    private final ItineraryUpdateService updateService;
    private final ItineraryDeleteService deleteService;
    private final ItineraryGetService getService;
    private final ItineraryFullGetService fullGetService;
    private final ItineraryStatusService statusService;
    private final ItineraryCostEstimationService costEstimationService;
    private final ItineraryDuplicateService duplicateService;

    @Autowired
    public ItineraryController(
        ItineraryCreateService createService,
        ItineraryUpdateService updateService,
        ItineraryDeleteService deleteService,
        ItineraryGetService getService,
        ItineraryFullGetService fullGetService,
        ItineraryStatusService statusService,
        ItineraryCostEstimationService costEstimationService,
        ItineraryDuplicateService duplicateService
    ) {
        this.createService = createService;
        this.updateService = updateService;
        this.deleteService = deleteService;
        this.getService = getService;
        this.fullGetService = fullGetService;
        this.statusService = statusService;
        this.costEstimationService = costEstimationService;
        this.duplicateService = duplicateService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_ITINERARY')")
    public ResponseEntity<ApiResponse<?>> createItinerary(
        @Valid @RequestBody CreateItineraryDTO createDTO
    ) {
        log.info("POST /api/itineraries - Creating new itinerary: {}", createDTO.getName());
        return createService.createItinerary(createDTO);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ITINERARY')")
    public ResponseEntity<ApiResponse<?>> updateItinerary(
        @PathVariable String id,
        @Valid @RequestBody UpdateItineraryDTO updateDTO
    ) {
        log.info("PUT /api/itineraries/{} - Updating itinerary", id);
        return updateService.updateItinerary(id, updateDTO);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_ITINERARY')")
    public ResponseEntity<ApiResponse<?>> deleteItineraries(
        @RequestBody List<String> ids
    ) {
        log.info("DELETE /api/itineraries - Deleting {} itineraries", ids.size());
        return deleteService.deleteItineraries(ids);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_ITINERARY')")
    public ResponseEntity<ApiResponse<?>> getItineraryById(
        @PathVariable String id,
        // the list's filters and sort, so prev/next stays inside the set on screen
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String code,
        @RequestParam(required = false) ItineraryStatus status,
        @RequestParam(required = false) TripType tripType,
        @RequestParam(required = false) BudgetCategory budgetCategory,
        @RequestParam(required = false) String startLocation,
        @RequestParam(required = false) String endLocation,
        @RequestParam(required = false) Integer totalDays,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) Boolean isDayTrip,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String sortBy
    ) {
        log.info("GET /api/itineraries/{} - Fetching itinerary", id);
        return getService.getItineraryById(
            id, name, code, status, tripType, budgetCategory, startLocation, endLocation,
            totalDays, isActive, isDayTrip, keyword, sortBy
        );
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAuthority('PERM_READ_ITINERARY')")
    public ResponseEntity<ApiResponse<?>> getItineraryByCode(
        @PathVariable String code
    ) {
        log.info("GET /api/itineraries/code/{} - Fetching itinerary by code", code);
        return getService.getItineraryByCode(code);
    }

    @GetMapping("/{id}/full")
    @PreAuthorize("hasAuthority('PERM_READ_ITINERARY')")
    public ResponseEntity<ApiResponse<?>> getFullItinerary(
        @PathVariable String id
    ) {
        log.info("GET /api/itineraries/{}/full - Fetching full itinerary with all nested data", id);
        return fullGetService.getFullItinerary(id);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_ITINERARY')")
    public ResponseEntity<ApiResponse<?>> getAllItineraries(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String code,
        @RequestParam(required = false) ItineraryStatus status,
        @RequestParam(required = false) TripType tripType,
        @RequestParam(required = false) BudgetCategory budgetCategory,
        @RequestParam(required = false) String startLocation,
        @RequestParam(required = false) String endLocation,
        @RequestParam(required = false) Integer totalDays,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) Boolean isDayTrip,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Boolean includeStats,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/itineraries - Fetching all itineraries");
        return getService.getAllItineraries(name, code, status, tripType, budgetCategory, startLocation, endLocation, totalDays, isActive, isDayTrip, keyword, includeStats, page, size, sortBy, sortDirection);
    }

    // shared bulk-flag endpoint (see Response/BulkFlags)
    @Autowired
    private com.itineraryledger.kabengosafaris.Response.BulkFlags bulkFlags;

    @Autowired
    private com.itineraryledger.kabengosafaris.Itinerary.Repository.ItineraryRepository bulkFlagsRepository;

    /**
     * PATCH /bulk — one request for a whole selection.
     *
     * Retiring last season's catalogue is one action, not fifty. Only the flags
     * present in the body apply, and the response reports per-id outcomes rather
     * than a bare 200 that hides what did not change.
     *
     * isActive is NOT the publishing status: a deactivated itinerary is out of
     * use everywhere, while archiving is the status move. Both exist because
     * they answer different questions.
     */
    @PatchMapping("/bulk")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ITINERARY')")
    public ResponseEntity<?> bulkFlags(
        @RequestBody com.itineraryledger.kabengosafaris.Response.BulkFlags.Request request
    ) {
        return bulkFlags.apply("itinerary", bulkFlagsRepository, request, entity -> {
            if (request.getIsActive() != null) entity.setIsActive(request.getIsActive());
        });
    }

    /**
     * Duplicate ONE finished itinerary into a fresh draft.
     *
     * The source must be COMPLETE or PUBLISHED — a draft has nothing settled
     * worth copying and an archived one was deliberately retired. Exactly one
     * per call: a copy is a decision about a specific template, and the response
     * has to be able to say what the new record contains.
     *
     * The body is optional; without it everything except images and documents is
     * copied. See {@link DuplicateItineraryDTO} for the per-part flags.
     */
    @PostMapping("/{id}/duplicate")
    @PreAuthorize("hasAuthority('PERM_DUPLICATE_ITINERARY')")
    public ResponseEntity<ApiResponse<?>> duplicateItinerary(
        @PathVariable String id,
        @RequestBody(required = false) DuplicateItineraryDTO duplicateDTO
    ) {
        log.info("POST /api/itineraries/{}/duplicate - Duplicating itinerary", id);
        return duplicateService.duplicateItinerary(id, duplicateDTO);
    }

    // ========================
    // STATUS MANAGEMENT ENDPOINTS
    // ========================

    /**
     * Evaluate itinerary status automatically based on completeness
     * This endpoint can be called to check if itinerary meets requirements
     * and automatically transitions between DRAFT and COMPLETE
     */
    @PostMapping("/{id}/evaluate-status")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ITINERARY')")
    public ResponseEntity<ApiResponse<?>> evaluateStatus(
        @PathVariable String id
    ) {
        log.info("POST /api/itineraries/{}/evaluate-status - Evaluating itinerary status", id);
        return statusService.evaluateStatus(id);
    }

    /**
     * Explicitly mark itinerary as COMPLETE
     * Only allowed if in DRAFT status and meets all requirements
     */
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('PERM_COMPLETE_ITINERARY')")
    public ResponseEntity<ApiResponse<?>> markAsComplete(
        @PathVariable String id
    ) {
        log.info("POST /api/itineraries/{}/complete - Marking itinerary as complete", id);
        return statusService.markAsComplete(id);
    }

    /**
     * Revert itinerary to DRAFT status
     * Allowed from COMPLETE or PUBLISHED status
     */
    @PostMapping("/{id}/revert-to-draft")
    @PreAuthorize("hasAuthority('PERM_REVERT_ITINERARY_TO_DRAFT')")
    public ResponseEntity<ApiResponse<?>> revertToDraft(
        @PathVariable String id
    ) {
        log.info("POST /api/itineraries/{}/revert-to-draft - Reverting itinerary to draft", id);
        return statusService.revertToDraft(id);
    }

    /**
     * Publish itinerary (make available for booking/creating safaris)
     * Only allowed if status is COMPLETE
     */
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('PERM_PUBLISH_ITINERARY')")
    public ResponseEntity<ApiResponse<?>> publishItinerary(
        @PathVariable String id
    ) {
        log.info("POST /api/itineraries/{}/publish - Publishing itinerary", id);
        return statusService.publishItinerary(id);
    }

    @PostMapping("/{id}/unpublish")
    @PreAuthorize("hasAuthority('PERM_UNPUBLISH_ITINERARY')")
    public ResponseEntity<ApiResponse<?>> unpublishItinerary(
        @PathVariable String id
    ) {
        log.info("POST /api/itineraries/{}/unpublish - Unpublishing itinerary", id);
        return statusService.unpublishItinerary(id);
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAuthority('PERM_ARCHIVE_ITINERARY')")
    public ResponseEntity<ApiResponse<?>> archiveItinerary(
        @PathVariable String id
    ) {
        log.info("POST /api/itineraries/{}/archive - Archiving itinerary", id);
        return statusService.archiveItinerary(id);
    }

    @PostMapping("/{id}/unarchive")
    @PreAuthorize("hasAuthority('PERM_UNARCHIVE_ITINERARY')")
    public ResponseEntity<ApiResponse<?>> unarchiveItinerary(
        @PathVariable String id
    ) {
        log.info("POST /api/itineraries/{}/unarchive - Unarchiving itinerary", id);
        return statusService.unarchiveItinerary(id);
    }

    // ========================
    // COST ESTIMATION
    // ========================

    /**
     * Estimate costs for an itinerary
     *
     * Returns a quick budget calculation based on:
     * - Park fees (tariffs)
     * - Accommodation rates
     * - Activity rates
     *
     * Uses current rates based on season determined from start date.
     *
     * @param id Itinerary ID (obfuscated)
     * @param startDate Optional start date for season determination (defaults to today)
     * @param useStoRate Whether to use STO rates (default: true)
     * @param currency Preferred output currency (default: USD)
     */
    @GetMapping("/{id}/estimate-cost")
    @PreAuthorize("hasAuthority('PERM_READ_ITINERARY')")
    public ResponseEntity<ApiResponse<?>> estimateCost(
        @PathVariable String id,
        @RequestParam(required = false) LocalDate startDate,
        @RequestParam(required = false, defaultValue = "true") Boolean useStoRate,
        @RequestParam(required = false, defaultValue = "USD") String currency
    ) {
        log.info("GET /api/itineraries/{}/estimate-cost - Estimating costs (startDate: {}, useStoRate: {}, currency: {})",
            id, startDate, useStoRate, currency);
        return costEstimationService.estimateCosts(id, startDate, useStoRate, currency);
    }
}
