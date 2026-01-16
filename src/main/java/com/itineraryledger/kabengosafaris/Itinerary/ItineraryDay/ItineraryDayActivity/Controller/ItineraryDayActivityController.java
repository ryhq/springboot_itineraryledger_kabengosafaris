package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.DTOs.CreateItineraryDayActivityDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.DTOs.ReorderItineraryDayActivitiesDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.DTOs.UpdateItineraryDayActivityDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.Services.ItineraryDayActivityCreateService;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.Services.ItineraryDayActivityDeleteService;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.Services.ItineraryDayActivityGetService;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.Services.ItineraryDayActivityReorderService;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.Services.ItineraryDayActivityUpdateService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryDayActivityController - REST controller for managing itinerary day activities
 */
@RestController
@RequestMapping("/api/itineraries/{itineraryId}/days/{dayId}/activities")
@Slf4j
public class ItineraryDayActivityController {

    private final ItineraryDayActivityCreateService createService;
    private final ItineraryDayActivityUpdateService updateService;
    private final ItineraryDayActivityDeleteService deleteService;
    private final ItineraryDayActivityGetService getService;
    private final ItineraryDayActivityReorderService reorderService;

    @Autowired
    public ItineraryDayActivityController(
        ItineraryDayActivityCreateService createService,
        ItineraryDayActivityUpdateService updateService,
        ItineraryDayActivityDeleteService deleteService,
        ItineraryDayActivityGetService getService,
        ItineraryDayActivityReorderService reorderService
    ) {
        this.createService = createService;
        this.updateService = updateService;
        this.deleteService = deleteService;
        this.getService = getService;
        this.reorderService = reorderService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_ITINERARY_DAY_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> createActivity(
        @PathVariable String itineraryId,
        @PathVariable String dayId,
        @Valid @RequestBody CreateItineraryDayActivityDTO createDTO
    ) {
        log.info("POST /api/itineraries/{}/days/{}/activities - Creating new activity", itineraryId, dayId);
        return createService.createItineraryDayActivity(itineraryId, dayId, createDTO);
    }

    @PutMapping("/{activityId}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ITINERARY_DAY_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> updateActivity(
        @PathVariable String itineraryId,
        @PathVariable String dayId,
        @PathVariable String activityId,
        @Valid @RequestBody UpdateItineraryDayActivityDTO updateDTO
    ) {
        log.info("PUT /api/itineraries/{}/days/{}/activities/{} - Updating activity", itineraryId, dayId, activityId);
        return updateService.updateItineraryDayActivity(itineraryId, dayId, activityId, updateDTO);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_ITINERARY_DAY_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> deleteActivities(
        @PathVariable String itineraryId,
        @PathVariable String dayId,
        @RequestBody List<String> activityIds
    ) {
        log.info("DELETE /api/itineraries/{}/days/{}/activities - Deleting {} activities", itineraryId, dayId, activityIds.size());
        return deleteService.deleteItineraryDayActivities(itineraryId, dayId, activityIds);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_ITINERARY_DAY_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> getActivities(
        @PathVariable String itineraryId,
        @PathVariable String dayId
    ) {
        log.info("GET /api/itineraries/{}/days/{}/activities - Fetching all activities", itineraryId, dayId);
        return getService.getItineraryDayActivities(itineraryId, dayId);
    }

    @GetMapping("/{activityId}")
    @PreAuthorize("hasAuthority('PERM_READ_ITINERARY_DAY_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> getActivity(
        @PathVariable String itineraryId,
        @PathVariable String dayId,
        @PathVariable String activityId
    ) {
        log.info("GET /api/itineraries/{}/days/{}/activities/{} - Fetching activity", itineraryId, dayId, activityId);
        return getService.getItineraryDayActivity(itineraryId, dayId, activityId);
    }

    @PostMapping("/reorder")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ITINERARY_DAY_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> reorderActivities(
        @PathVariable String itineraryId,
        @PathVariable String dayId,
        @Valid @RequestBody ReorderItineraryDayActivitiesDTO reorderDTO
    ) {
        log.info("POST /api/itineraries/{}/days/{}/activities/reorder - Reordering {} activities", itineraryId, dayId, reorderDTO.getActivityOrder().size());
        return reorderService.reorderItineraryDayActivities(itineraryId, dayId, reorderDTO);
    }
}
