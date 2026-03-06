package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.DTOs.CreateSafariDayActivityDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.DTOs.ReorderSafariDayActivitiesDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.DTOs.UpdateSafariDayActivityDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.Services.SafariDayActivityCreateService;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.Services.SafariDayActivityDeleteService;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.Services.SafariDayActivityGetService;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.Services.SafariDayActivityReorderService;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.Services.SafariDayActivityUpdateService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * SafariDayActivityController - REST controller for managing safari day activities
 */
@RestController
@RequestMapping("/api/safaris/{safariId}/days/{dayId}/activities")
@Slf4j
public class SafariDayActivityController {

    private final SafariDayActivityCreateService createService;
    private final SafariDayActivityUpdateService updateService;
    private final SafariDayActivityDeleteService deleteService;
    private final SafariDayActivityGetService getService;
    private final SafariDayActivityReorderService reorderService;

    @Autowired
    public SafariDayActivityController(
        SafariDayActivityCreateService createService,
        SafariDayActivityUpdateService updateService,
        SafariDayActivityDeleteService deleteService,
        SafariDayActivityGetService getService,
        SafariDayActivityReorderService reorderService
    ) {
        this.createService = createService;
        this.updateService = updateService;
        this.deleteService = deleteService;
        this.getService = getService;
        this.reorderService = reorderService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_SAFARI_DAY_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> createActivity(
        @PathVariable String safariId,
        @PathVariable String dayId,
        @Valid @RequestBody CreateSafariDayActivityDTO createDTO
    ) {
        log.info("POST /api/safaris/{}/days/{}/activities - Creating new activity", safariId, dayId);
        return createService.createSafariDayActivity(safariId, dayId, createDTO);
    }

    @PutMapping("/{activityId}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_SAFARI_DAY_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> updateActivity(
        @PathVariable String safariId,
        @PathVariable String dayId,
        @PathVariable String activityId,
        @Valid @RequestBody UpdateSafariDayActivityDTO updateDTO
    ) {
        log.info("PUT /api/safaris/{}/days/{}/activities/{} - Updating activity", safariId, dayId, activityId);
        return updateService.updateSafariDayActivity(safariId, dayId, activityId, updateDTO);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_SAFARI_DAY_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> deleteActivities(
        @PathVariable String safariId,
        @PathVariable String dayId,
        @RequestBody List<String> activityIds
    ) {
        log.info("DELETE /api/safaris/{}/days/{}/activities - Deleting {} activities", safariId, dayId, activityIds.size());
        return deleteService.deleteSafariDayActivities(safariId, dayId, activityIds);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_SAFARI_DAY_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> getActivities(
        @PathVariable String safariId,
        @PathVariable String dayId,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection
    ) {
        log.info("GET /api/safaris/{}/days/{}/activities - Fetching all activities", safariId, dayId);
        return getService.getSafariDayActivities(safariId, dayId, sortBy, sortDirection);
    }

    @GetMapping("/{activityId}")
    @PreAuthorize("hasAuthority('PERM_READ_SAFARI_DAY_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> getActivity(
        @PathVariable String safariId,
        @PathVariable String dayId,
        @PathVariable String activityId
    ) {
        log.info("GET /api/safaris/{}/days/{}/activities/{} - Fetching activity", safariId, dayId, activityId);
        return getService.getSafariDayActivity(safariId, dayId, activityId);
    }

    @PostMapping("/reorder")
    @PreAuthorize("hasAuthority('PERM_UPDATE_SAFARI_DAY_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> reorderActivities(
        @PathVariable String safariId,
        @PathVariable String dayId,
        @Valid @RequestBody ReorderSafariDayActivitiesDTO reorderDTO
    ) {
        log.info("POST /api/safaris/{}/days/{}/activities/reorder - Reordering {} activities", safariId, dayId, reorderDTO.getActivityOrder().size());
        return reorderService.reorderSafariDayActivities(safariId, dayId, reorderDTO);
    }
}
