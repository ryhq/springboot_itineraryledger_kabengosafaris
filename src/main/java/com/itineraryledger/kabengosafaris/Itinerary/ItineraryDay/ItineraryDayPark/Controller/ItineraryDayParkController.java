package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.DTOs.CreateItineraryDayParkDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.DTOs.UpdateItineraryDayParkDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.DTOs.ReorderItineraryDayParksDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Services.ItineraryDayParkCreateService;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Services.ItineraryDayParkDeleteService;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Services.ItineraryDayParkGetService;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Services.ItineraryDayParkUpdateService;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Services.ItineraryDayParkReorderService;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.DTOs.CreateItineraryDayParkActivityDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.Services.ItineraryDayParkActivityService;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.DTOs.CreateItineraryDayParkTariffDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.Services.ItineraryDayParkTariffService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryDayParkController - REST controller for managing park visits in itinerary days
 */
@RestController
@RequestMapping("/api/itineraries/{itineraryId}/days/{dayId}/parks")
@Slf4j
public class ItineraryDayParkController {

    private final ItineraryDayParkCreateService createService;
    private final ItineraryDayParkGetService getService;
    private final ItineraryDayParkUpdateService updateService;
    private final ItineraryDayParkDeleteService deleteService;
    private final ItineraryDayParkReorderService reorderService;
    private final ItineraryDayParkActivityService activityService;
    private final ItineraryDayParkTariffService tariffService;

    @Autowired
    public ItineraryDayParkController(
        ItineraryDayParkCreateService createService,
        ItineraryDayParkGetService getService,
        ItineraryDayParkUpdateService updateService,
        ItineraryDayParkDeleteService deleteService,
        ItineraryDayParkReorderService reorderService,
        ItineraryDayParkActivityService activityService,
        ItineraryDayParkTariffService tariffService
    ) {
        this.createService = createService;
        this.getService = getService;
        this.updateService = updateService;
        this.deleteService = deleteService;
        this.reorderService = reorderService;
        this.activityService = activityService;
        this.tariffService = tariffService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_ITINERARY_DAY_PARK')")
    public ResponseEntity<ApiResponse<?>> createParkVisit(
        @PathVariable String itineraryId,
        @PathVariable String dayId,
        @Valid @RequestBody CreateItineraryDayParkDTO createDTO
    ) {
        log.info("POST /api/itineraries/{}/days/{}/parks - Creating park visit", itineraryId, dayId);
        return createService.createItineraryDayPark(itineraryId, dayId, createDTO);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_ITINERARY_DAY_PARK')")
    public ResponseEntity<ApiResponse<?>> getParkVisits(
        @PathVariable String itineraryId,
        @PathVariable String dayId
    ) {
        log.info("GET /api/itineraries/{}/days/{}/parks - Fetching park visits", itineraryId, dayId);
        return getService.getItineraryDayParks(itineraryId, dayId);
    }

    @PutMapping("/{parkVisitId}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ITINERARY_DAY_PARK')")
    public ResponseEntity<ApiResponse<?>> updateParkVisit(
        @PathVariable String itineraryId,
        @PathVariable String dayId,
        @PathVariable String parkVisitId,
        @Valid @RequestBody UpdateItineraryDayParkDTO updateDTO
    ) {
        log.info("PUT /api/itineraries/{}/days/{}/parks/{} - Updating park visit", itineraryId, dayId, parkVisitId);
        return updateService.updateItineraryDayPark(itineraryId, dayId, parkVisitId, updateDTO);
    }

    @PostMapping("/reorder")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ITINERARY_DAY_PARK')")
    public ResponseEntity<ApiResponse<?>> reorderParkVisits(
        @PathVariable String itineraryId,
        @PathVariable String dayId,
        @Valid @RequestBody ReorderItineraryDayParksDTO reorderDTO
    ) {
        log.info("POST /api/itineraries/{}/days/{}/parks/reorder - Reordering park visits", itineraryId, dayId);
        return reorderService.reorderItineraryDayParks(itineraryId, dayId, reorderDTO);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_ITINERARY_DAY_PARK')")
    public ResponseEntity<ApiResponse<?>> deleteParkVisits(
        @PathVariable String itineraryId,
        @PathVariable String dayId,
        @RequestBody List<String> parkVisitIds
    ) {
        log.info("DELETE /api/itineraries/{}/days/{}/parks - Deleting {} park visits", itineraryId, dayId, parkVisitIds.size());
        return deleteService.deleteItineraryDayParks(itineraryId, dayId, parkVisitIds);
    }

    // Park Activities endpoints

    @PostMapping("/{parkVisitId}/activities")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ITINERARY_DAY_PARK')")
    public ResponseEntity<ApiResponse<?>> addParkActivities(
        @PathVariable String parkVisitId,
        @Valid @RequestBody List<CreateItineraryDayParkActivityDTO> createDTOs
    ) {
        log.info("POST /api/.../parks/{}/activities - Adding {} activities", parkVisitId, createDTOs.size());
        return activityService.addParkActivities(parkVisitId, createDTOs);
    }

    @GetMapping("/{parkVisitId}/activities")
    @PreAuthorize("hasAuthority('PERM_READ_ITINERARY_DAY_PARK')")
    public ResponseEntity<ApiResponse<?>> getParkActivities(
        @PathVariable String parkVisitId
    ) {
        log.info("GET /api/.../parks/{}/activities - Fetching activities", parkVisitId);
        return activityService.getParkActivities(parkVisitId);
    }

    @DeleteMapping("/{parkVisitId}/activities")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ITINERARY_DAY_PARK')")
    public ResponseEntity<ApiResponse<?>> deleteParkActivities(
        @PathVariable String parkVisitId,
        @RequestBody List<String> activityIds
    ) {
        log.info("DELETE /api/.../parks/{}/activities - Deleting {} activities", parkVisitId, activityIds.size());
        return activityService.deleteParkActivities(parkVisitId, activityIds);
    }

    // Park Tariffs endpoints

    @PostMapping("/{parkVisitId}/tariffs")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ITINERARY_DAY_PARK')")
    public ResponseEntity<ApiResponse<?>> addParkTariffs(
        @PathVariable String parkVisitId,
        @Valid @RequestBody List<CreateItineraryDayParkTariffDTO> createDTOs
    ) {
        log.info("POST /api/.../parks/{}/tariffs - Adding {} tariffs", parkVisitId, createDTOs.size());
        return tariffService.addParkTariffs(parkVisitId, createDTOs);
    }

    @GetMapping("/{parkVisitId}/tariffs")
    @PreAuthorize("hasAuthority('PERM_READ_ITINERARY_DAY_PARK')")
    public ResponseEntity<ApiResponse<?>> getParkTariffs(
        @PathVariable String parkVisitId
    ) {
        log.info("GET /api/.../parks/{}/tariffs - Fetching tariffs", parkVisitId);
        return tariffService.getParkTariffs(parkVisitId);
    }

    @DeleteMapping("/{parkVisitId}/tariffs")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ITINERARY_DAY_PARK')")
    public ResponseEntity<ApiResponse<?>> deleteParkTariffs(
        @PathVariable String parkVisitId,
        @RequestBody List<String> tariffIds
    ) {
        log.info("DELETE /api/.../parks/{}/tariffs - Deleting {} tariffs", parkVisitId, tariffIds.size());
        return tariffService.deleteParkTariffs(parkVisitId, tariffIds);
    }
}
