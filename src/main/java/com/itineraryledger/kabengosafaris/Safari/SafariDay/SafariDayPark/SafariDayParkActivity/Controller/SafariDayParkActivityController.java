package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.DTOs.CreateSafariDayParkActivityDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.DTOs.ReorderSafariDayParkActivitiesDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.DTOs.UpdateSafariDayParkActivityDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.Services.SafariDayParkActivityCreateService;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.Services.SafariDayParkActivityDeleteService;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.Services.SafariDayParkActivityGetService;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.Services.SafariDayParkActivityReorderService;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.Services.SafariDayParkActivityUpdateService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * SafariDayParkActivityController - REST controller for managing activities within a safari park visit
 */
@RestController
@RequestMapping("/api/safaris/{safariId}/days/{dayId}/parks/{parkVisitId}/activities")
@Slf4j
public class SafariDayParkActivityController {

    private final SafariDayParkActivityCreateService createService;
    private final SafariDayParkActivityGetService getService;
    private final SafariDayParkActivityUpdateService updateService;
    private final SafariDayParkActivityDeleteService deleteService;
    private final SafariDayParkActivityReorderService reorderService;

    @Autowired
    public SafariDayParkActivityController(
        SafariDayParkActivityCreateService createService,
        SafariDayParkActivityGetService getService,
        SafariDayParkActivityUpdateService updateService,
        SafariDayParkActivityDeleteService deleteService,
        SafariDayParkActivityReorderService reorderService
    ) {
        this.createService = createService;
        this.getService = getService;
        this.updateService = updateService;
        this.deleteService = deleteService;
        this.reorderService = reorderService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_UPDATE_SAFARI_DAY_PARK_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> addParkActivities(
        @PathVariable String safariId,
        @PathVariable String dayId,
        @PathVariable String parkVisitId,
        @Valid @RequestBody List<CreateSafariDayParkActivityDTO> createDTOs
    ) {
        log.info("POST /api/safaris/{}/days/{}/parks/{}/activities - Adding {} activities",
            safariId, dayId, parkVisitId, createDTOs.size());
        return createService.addParkActivities(parkVisitId, createDTOs);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_SAFARI_DAY_PARK_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> getParkActivities(
        @PathVariable String safariId,
        @PathVariable String dayId,
        @PathVariable String parkVisitId,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection
    ) {
        log.info("GET /api/safaris/{}/days/{}/parks/{}/activities - Fetching activities",
            safariId, dayId, parkVisitId);
        return getService.getParkActivities(parkVisitId, sortBy, sortDirection);
    }

    @PostMapping("/reorder")
    @PreAuthorize("hasAuthority('PERM_UPDATE_SAFARI_DAY_PARK_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> reorderParkActivities(
        @PathVariable String safariId,
        @PathVariable String dayId,
        @PathVariable String parkVisitId,
        @Valid @RequestBody ReorderSafariDayParkActivitiesDTO reorderDTO
    ) {
        log.info("POST /api/safaris/{}/days/{}/parks/{}/activities/reorder - Reordering {} activities",
            safariId, dayId, parkVisitId, reorderDTO.getActivityOrder().size());
        return reorderService.reorderParkActivities(parkVisitId, reorderDTO);
    }

    @GetMapping("/{activityId}")
    @PreAuthorize("hasAuthority('PERM_READ_SAFARI_DAY_PARK_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> getParkActivity(
        @PathVariable String safariId,
        @PathVariable String dayId,
        @PathVariable String parkVisitId,
        @PathVariable String activityId
    ,
        /* the sort travels with the record so its arrows keep the list's order */
        @RequestParam(required = false) String sortBy
        , @RequestParam(required = false) String sortDirection
    ) {
        log.info("GET /api/safaris/{}/days/{}/parks/{}/activities/{} - Fetching activity",
            safariId, dayId, parkVisitId, activityId);
        return getService.getParkActivity(parkVisitId, activityId, sortBy, sortDirection);
    }

    @PutMapping("/{activityId}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_SAFARI_DAY_PARK_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> updateParkActivity(
        @PathVariable String safariId,
        @PathVariable String dayId,
        @PathVariable String parkVisitId,
        @PathVariable String activityId,
        @Valid @RequestBody UpdateSafariDayParkActivityDTO updateDTO
    ) {
        log.info("PUT /api/safaris/{}/days/{}/parks/{}/activities/{} - Updating activity",
            safariId, dayId, parkVisitId, activityId);
        return updateService.updateParkActivity(parkVisitId, activityId, updateDTO);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_UPDATE_SAFARI_DAY_PARK_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> deleteParkActivities(
        @PathVariable String safariId,
        @PathVariable String dayId,
        @PathVariable String parkVisitId,
        @RequestBody List<String> activityIds
    ) {
        log.info("DELETE /api/safaris/{}/days/{}/parks/{}/activities - Deleting {} activities",
            safariId, dayId, parkVisitId, activityIds.size());
        return deleteService.deleteParkActivities(parkVisitId, activityIds);
    }
}
