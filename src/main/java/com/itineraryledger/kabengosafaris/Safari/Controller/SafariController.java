package com.itineraryledger.kabengosafaris.Safari.Controller;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.DTOs.CreateSafariFromItineraryDTO;
import com.itineraryledger.kabengosafaris.Safari.DTOs.UpdateSafariDTO;
import com.itineraryledger.kabengosafaris.Safari.Enums.SafariPhase;
import com.itineraryledger.kabengosafaris.Safari.Enums.SafariState;
import com.itineraryledger.kabengosafaris.Safari.Services.SafariCreateService;
import com.itineraryledger.kabengosafaris.Safari.Services.SafariGetService;
import com.itineraryledger.kabengosafaris.Safari.Services.SafariUpdateService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

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
public class SafariController {

    private final SafariCreateService safariCreateService;
    private final SafariGetService safariGetService;
    private final SafariUpdateService safariUpdateService;

    @Autowired
    public SafariController(
            SafariCreateService safariCreateService,
            SafariGetService safariGetService,
            SafariUpdateService safariUpdateService
    ) {
        this.safariCreateService = safariCreateService;
        this.safariGetService = safariGetService;
        this.safariUpdateService = safariUpdateService;
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
            @PathVariable String id
    ) {
        return safariGetService.getSafariById(id);
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
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) SafariState state,
            @RequestParam(required = false) SafariPhase phase,
            @RequestParam(required = false) String startLocation,
            @RequestParam(required = false) String endLocation,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateTo,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        return safariGetService.getAllSafaris(
                name, code, state, phase, startLocation, endLocation,
                startDateFrom, startDateTo, isActive, keyword,
                page, size, sortDirection
        );
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
}
