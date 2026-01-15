package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.DTOs.CreateItineraryDayParkDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Services.ItineraryDayParkCreateService;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Services.ItineraryDayParkDeleteService;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Services.ItineraryDayParkGetService;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.DTOs.CreateItineraryDayParkActivityDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.Services.ItineraryDayParkActivityService;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.DTOs.CreateItineraryDayParkTariffDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.Services.ItineraryDayParkTariffService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryDayParkController - REST controller for managing park visits in itinerary days
 */
@RestController
@RequestMapping("/api/itineraries/{itineraryId}/days/{dayId}/parks")
@Tag(name = "Itinerary Day Park Management", description = "APIs for managing park visits in itinerary days")
@Slf4j
public class ItineraryDayParkController {

    private final ItineraryDayParkCreateService createService;
    private final ItineraryDayParkGetService getService;
    private final ItineraryDayParkDeleteService deleteService;
    private final ItineraryDayParkActivityService activityService;
    private final ItineraryDayParkTariffService tariffService;

    @Autowired
    public ItineraryDayParkController(
        ItineraryDayParkCreateService createService,
        ItineraryDayParkGetService getService,
        ItineraryDayParkDeleteService deleteService,
        ItineraryDayParkActivityService activityService,
        ItineraryDayParkTariffService tariffService
    ) {
        this.createService = createService;
        this.getService = getService;
        this.deleteService = deleteService;
        this.activityService = activityService;
        this.tariffService = tariffService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_ITINERARY_DAY_PARK')")
    public ResponseEntity<ApiResponse<?>> createParkVisit(
        @Parameter(description = "Obfuscated itinerary ID") @PathVariable String itineraryId,
        @Parameter(description = "Obfuscated day ID") @PathVariable String dayId,
        @Valid @RequestBody CreateItineraryDayParkDTO createDTO
    ) {
        log.info("POST /api/itineraries/{}/days/{}/parks - Creating park visit", itineraryId, dayId);
        return createService.createItineraryDayPark(itineraryId, dayId, createDTO);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_ITINERARY_DAY_PARK')")
    public ResponseEntity<ApiResponse<?>> getParkVisits(
        @Parameter(description = "Obfuscated itinerary ID") @PathVariable String itineraryId,
        @Parameter(description = "Obfuscated day ID") @PathVariable String dayId
    ) {
        log.info("GET /api/itineraries/{}/days/{}/parks - Fetching park visits", itineraryId, dayId);
        return getService.getItineraryDayParks(itineraryId, dayId);
    }

    @DeleteMapping("/{parkVisitId}")
    @PreAuthorize("hasAuthority('PERM_DELETE_ITINERARY_DAY_PARK')")
    public ResponseEntity<ApiResponse<?>> deleteParkVisit(
        @Parameter(description = "Obfuscated itinerary ID") @PathVariable String itineraryId,
        @Parameter(description = "Obfuscated day ID") @PathVariable String dayId,
        @Parameter(description = "Obfuscated park visit ID") @PathVariable String parkVisitId
    ) {
        log.info("DELETE /api/itineraries/{}/days/{}/parks/{} - Deleting park visit", itineraryId, dayId, parkVisitId);
        return deleteService.deleteItineraryDayPark(itineraryId, dayId, parkVisitId);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_ITINERARY_DAY_PARK')")
    public ResponseEntity<ApiResponse<?>> deleteParkVisits(
        @Parameter(description = "Obfuscated itinerary ID") @PathVariable String itineraryId,
        @Parameter(description = "Obfuscated day ID") @PathVariable String dayId,
        @Parameter(description = "List of park visit IDs") @RequestBody List<String> parkVisitIds
    ) {
        log.info("DELETE /api/itineraries/{}/days/{}/parks - Deleting {} park visits", itineraryId, dayId, parkVisitIds.size());
        return deleteService.deleteItineraryDayParks(itineraryId, dayId, parkVisitIds);
    }

    // Park Activities endpoints

    @PostMapping("/{parkVisitId}/activities")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ITINERARY_DAY_PARK')")
    public ResponseEntity<ApiResponse<?>> addParkActivities(
        @Parameter(description = "Obfuscated park visit ID") @PathVariable String parkVisitId,
        @Valid @RequestBody List<CreateItineraryDayParkActivityDTO> createDTOs
    ) {
        log.info("POST /api/.../parks/{}/activities - Adding {} activities", parkVisitId, createDTOs.size());
        return activityService.addParkActivities(parkVisitId, createDTOs);
    }

    @GetMapping("/{parkVisitId}/activities")
    @PreAuthorize("hasAuthority('PERM_READ_ITINERARY_DAY_PARK')")
    public ResponseEntity<ApiResponse<?>> getParkActivities(
        @Parameter(description = "Obfuscated park visit ID") @PathVariable String parkVisitId
    ) {
        log.info("GET /api/.../parks/{}/activities - Fetching activities", parkVisitId);
        return activityService.getParkActivities(parkVisitId);
    }

    @DeleteMapping("/{parkVisitId}/activities")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ITINERARY_DAY_PARK')")
    public ResponseEntity<ApiResponse<?>> deleteParkActivities(
        @Parameter(description = "Obfuscated park visit ID") @PathVariable String parkVisitId,
        @Parameter(description = "List of activity entry IDs") @RequestBody List<String> activityIds
    ) {
        log.info("DELETE /api/.../parks/{}/activities - Deleting {} activities", parkVisitId, activityIds.size());
        return activityService.deleteParkActivities(parkVisitId, activityIds);
    }

    // Park Tariffs endpoints

    @PostMapping("/{parkVisitId}/tariffs")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ITINERARY_DAY_PARK')")
    public ResponseEntity<ApiResponse<?>> addParkTariffs(
        @Parameter(description = "Obfuscated park visit ID") @PathVariable String parkVisitId,
        @Valid @RequestBody List<CreateItineraryDayParkTariffDTO> createDTOs
    ) {
        log.info("POST /api/.../parks/{}/tariffs - Adding {} tariffs", parkVisitId, createDTOs.size());
        return tariffService.addParkTariffs(parkVisitId, createDTOs);
    }

    @GetMapping("/{parkVisitId}/tariffs")
    @PreAuthorize("hasAuthority('PERM_READ_ITINERARY_DAY_PARK')")
    public ResponseEntity<ApiResponse<?>> getParkTariffs(
        @Parameter(description = "Obfuscated park visit ID") @PathVariable String parkVisitId
    ) {
        log.info("GET /api/.../parks/{}/tariffs - Fetching tariffs", parkVisitId);
        return tariffService.getParkTariffs(parkVisitId);
    }

    @DeleteMapping("/{parkVisitId}/tariffs")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ITINERARY_DAY_PARK')")
    public ResponseEntity<ApiResponse<?>> deleteParkTariffs(
        @Parameter(description = "Obfuscated park visit ID") @PathVariable String parkVisitId,
        @Parameter(description = "List of tariff entry IDs") @RequestBody List<String> tariffIds
    ) {
        log.info("DELETE /api/.../parks/{}/tariffs - Deleting {} tariffs", parkVisitId, tariffIds.size());
        return tariffService.deleteParkTariffs(parkVisitId, tariffIds);
    }
}
