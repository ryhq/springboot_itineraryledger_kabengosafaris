package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.DTOs.CreateSafariDayParkDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.DTOs.UpdateSafariDayParkDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.DTOs.ReorderSafariDayParksDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.Services.SafariDayParkCreateService;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.Services.SafariDayParkDeleteService;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.Services.SafariDayParkGetService;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.Services.SafariDayParkUpdateService;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.Services.SafariDayParkReorderService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * SafariDayParkController - REST controller for managing park visits in safari days
 */
@RestController
@RequestMapping("/api/safaris/{safariId}/days/{dayId}/parks")
@Slf4j
public class SafariDayParkController {

    private final SafariDayParkCreateService createService;
    private final SafariDayParkGetService getService;
    private final SafariDayParkUpdateService updateService;
    private final SafariDayParkDeleteService deleteService;
    private final SafariDayParkReorderService reorderService;

    @Autowired
    public SafariDayParkController(
        SafariDayParkCreateService createService,
        SafariDayParkGetService getService,
        SafariDayParkUpdateService updateService,
        SafariDayParkDeleteService deleteService,
        SafariDayParkReorderService reorderService
    ) {
        this.createService = createService;
        this.getService = getService;
        this.updateService = updateService;
        this.deleteService = deleteService;
        this.reorderService = reorderService;
    }

    /**
     * Create a new park visit for a safari day
     *
     * POST /api/safaris/{safariId}/days/{dayId}/parks
     *
     * @param safariId The obfuscated safari ID
     * @param dayId The obfuscated day ID
     * @param createDTO The park visit data
     * @return ResponseEntity with ApiResponse containing the created park visit
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_SAFARI_DAY_PARK')")
    public ResponseEntity<ApiResponse<?>> createParkVisit(
        @PathVariable String safariId,
        @PathVariable String dayId,
        @Valid @RequestBody CreateSafariDayParkDTO createDTO
    ) {
        log.info("POST /api/safaris/{}/days/{}/parks - Creating park visit", safariId, dayId);
        return createService.createSafariDayPark(safariId, dayId, createDTO);
    }

    /**
     * Get all park visits for a safari day
     *
     * GET /api/safaris/{safariId}/days/{dayId}/parks
     *
     * @param safariId The obfuscated safari ID
     * @param dayId The obfuscated day ID
     * @return ResponseEntity with ApiResponse containing list of park visits
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_SAFARI_DAY_PARK')")
    public ResponseEntity<ApiResponse<?>> getParkVisits(
        @PathVariable String safariId,
        @PathVariable String dayId,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection
    ) {
        log.info("GET /api/safaris/{}/days/{}/parks - Fetching park visits", safariId, dayId);
        return getService.getSafariDayParks(safariId, dayId, sortBy, sortDirection);
    }

    /**
     * Get a specific park visit
     *
     * GET /api/safaris/{safariId}/days/{dayId}/parks/{parkVisitId}
     *
     * @param safariId The obfuscated safari ID
     * @param dayId The obfuscated day ID
     * @param parkVisitId The obfuscated park visit ID
     * @return ResponseEntity with ApiResponse containing the park visit
     */
    @GetMapping("/{parkVisitId}")
    @PreAuthorize("hasAuthority('PERM_READ_SAFARI_DAY_PARK')")
    public ResponseEntity<ApiResponse<?>> getParkVisit(
        @PathVariable String safariId,
        @PathVariable String dayId,
        @PathVariable String parkVisitId
    ,
        /* the sort travels with the record so its arrows keep the list's order */
        @RequestParam(required = false) String sortBy
        , @RequestParam(required = false) String sortDirection
    ) {
        log.info("GET /api/safaris/{}/days/{}/parks/{} - Fetching park visit", safariId, dayId, parkVisitId);
        return getService.getSafariDayPark(safariId, dayId, parkVisitId, sortBy, sortDirection);
    }

    /**
     * Update a park visit
     *
     * PUT /api/safaris/{safariId}/days/{dayId}/parks/{parkVisitId}
     *
     * @param safariId The obfuscated safari ID
     * @param dayId The obfuscated day ID
     * @param parkVisitId The obfuscated park visit ID
     * @param updateDTO The updated park visit data
     * @return ResponseEntity with ApiResponse containing the updated park visit
     */
    @PutMapping("/{parkVisitId}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_SAFARI_DAY_PARK')")
    public ResponseEntity<ApiResponse<?>> updateParkVisit(
        @PathVariable String safariId,
        @PathVariable String dayId,
        @PathVariable String parkVisitId,
        @Valid @RequestBody UpdateSafariDayParkDTO updateDTO
    ) {
        log.info("PUT /api/safaris/{}/days/{}/parks/{} - Updating park visit", safariId, dayId, parkVisitId);
        return updateService.updateSafariDayPark(safariId, dayId, parkVisitId, updateDTO);
    }

    /**
     * Reorder park visits within a safari day
     *
     * POST /api/safaris/{safariId}/days/{dayId}/parks/reorder
     *
     * @param safariId The obfuscated safari ID
     * @param dayId The obfuscated day ID
     * @param reorderDTO The new order for park visits
     * @return ResponseEntity with ApiResponse containing the reordered park visits
     */
    @PostMapping("/reorder")
    @PreAuthorize("hasAuthority('PERM_UPDATE_SAFARI_DAY_PARK')")
    public ResponseEntity<ApiResponse<?>> reorderParkVisits(
        @PathVariable String safariId,
        @PathVariable String dayId,
        @Valid @RequestBody ReorderSafariDayParksDTO reorderDTO
    ) {
        log.info("POST /api/safaris/{}/days/{}/parks/reorder - Reordering park visits", safariId, dayId);
        return reorderService.reorderSafariDayParks(safariId, dayId, reorderDTO);
    }

    /**
     * Delete park visits from a safari day
     *
     * DELETE /api/safaris/{safariId}/days/{dayId}/parks
     *
     * @param safariId The obfuscated safari ID
     * @param dayId The obfuscated day ID
     * @param parkVisitIds List of obfuscated park visit IDs to delete
     * @return ResponseEntity with ApiResponse confirming deletion
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_SAFARI_DAY_PARK')")
    public ResponseEntity<ApiResponse<?>> deleteParkVisits(
        @PathVariable String safariId,
        @PathVariable String dayId,
        @RequestBody List<String> parkVisitIds
    ) {
        log.info("DELETE /api/safaris/{}/days/{}/parks - Deleting {} park visits", safariId, dayId, parkVisitIds.size());
        return deleteService.deleteSafariDayParks(safariId, dayId, parkVisitIds);
    }
}
