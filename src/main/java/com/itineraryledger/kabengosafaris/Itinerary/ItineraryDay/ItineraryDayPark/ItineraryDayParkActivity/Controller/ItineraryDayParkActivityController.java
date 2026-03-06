package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.DTOs.CreateItineraryDayParkActivityDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.DTOs.ReorderItineraryDayParkActivitiesDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.DTOs.UpdateItineraryDayParkActivityDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.Services.ItineraryDayParkActivityCreateService;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.Services.ItineraryDayParkActivityDeleteService;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.Services.ItineraryDayParkActivityGetService;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.Services.ItineraryDayParkActivityReorderService;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.Services.ItineraryDayParkActivityUpdateService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryDayParkActivityController - REST controller for managing activities within a park visit
 */
@RestController
@RequestMapping("/api/itineraries/{itineraryId}/days/{dayId}/parks/{parkVisitId}/activities")
@Slf4j
public class ItineraryDayParkActivityController {

    private final ItineraryDayParkActivityCreateService createService;
    private final ItineraryDayParkActivityGetService getService;
    private final ItineraryDayParkActivityUpdateService updateService;
    private final ItineraryDayParkActivityDeleteService deleteService;
    private final ItineraryDayParkActivityReorderService reorderService;

    @Autowired
    public ItineraryDayParkActivityController(
        ItineraryDayParkActivityCreateService createService,
        ItineraryDayParkActivityGetService getService,
        ItineraryDayParkActivityUpdateService updateService,
        ItineraryDayParkActivityDeleteService deleteService,
        ItineraryDayParkActivityReorderService reorderService
    ) {
        this.createService = createService;
        this.getService = getService;
        this.updateService = updateService;
        this.deleteService = deleteService;
        this.reorderService = reorderService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_UPDATE_ITINERARY_DAY_PARK_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> addParkActivities(
        @PathVariable String itineraryId,
        @PathVariable String dayId,
        @PathVariable String parkVisitId,
        @Valid @RequestBody List<CreateItineraryDayParkActivityDTO> createDTOs
    ) {
        log.info("POST /api/itineraries/{}/days/{}/parks/{}/activities - Adding {} activities",
            itineraryId, dayId, parkVisitId, createDTOs.size());
        return createService.addParkActivities(parkVisitId, createDTOs);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_ITINERARY_DAY_PARK_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> getParkActivities(
        @PathVariable String itineraryId,
        @PathVariable String dayId,
        @PathVariable String parkVisitId,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection
    ) {
        log.info("GET /api/itineraries/{}/days/{}/parks/{}/activities - Fetching activities",
            itineraryId, dayId, parkVisitId);
        return getService.getParkActivities(parkVisitId, sortBy, sortDirection);
    }

    @PostMapping("/reorder")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ITINERARY_DAY_PARK_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> reorderParkActivities(
        @PathVariable String itineraryId,
        @PathVariable String dayId,
        @PathVariable String parkVisitId,
        @Valid @RequestBody ReorderItineraryDayParkActivitiesDTO reorderDTO
    ) {
        log.info("POST /api/itineraries/{}/days/{}/parks/{}/activities/reorder - Reordering {} activities",
            itineraryId, dayId, parkVisitId, reorderDTO.getActivityOrder().size());
        return reorderService.reorderParkActivities(parkVisitId, reorderDTO);
    }

    @GetMapping("/{activityId}")
    @PreAuthorize("hasAuthority('PERM_READ_ITINERARY_DAY_PARK_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> getParkActivity(
        @PathVariable String itineraryId,
        @PathVariable String dayId,
        @PathVariable String parkVisitId,
        @PathVariable String activityId
    ) {
        log.info("GET /api/itineraries/{}/days/{}/parks/{}/activities/{} - Fetching activity",
            itineraryId, dayId, parkVisitId, activityId);
        return getService.getParkActivity(parkVisitId, activityId);
    }

    @PutMapping("/{activityId}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ITINERARY_DAY_PARK_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> updateParkActivity(
        @PathVariable String itineraryId,
        @PathVariable String dayId,
        @PathVariable String parkVisitId,
        @PathVariable String activityId,
        @Valid @RequestBody UpdateItineraryDayParkActivityDTO updateDTO
    ) {
        log.info("PUT /api/itineraries/{}/days/{}/parks/{}/activities/{} - Updating activity",
            itineraryId, dayId, parkVisitId, activityId);
        return updateService.updateParkActivity(parkVisitId, activityId, updateDTO);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_UPDATE_ITINERARY_DAY_PARK_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> deleteParkActivities(
        @PathVariable String itineraryId,
        @PathVariable String dayId,
        @PathVariable String parkVisitId,
        @RequestBody List<String> activityIds
    ) {
        log.info("DELETE /api/itineraries/{}/days/{}/parks/{}/activities - Deleting {} activities",
            itineraryId, dayId, parkVisitId, activityIds.size());
        return deleteService.deleteParkActivities(parkVisitId, activityIds);
    }
}
