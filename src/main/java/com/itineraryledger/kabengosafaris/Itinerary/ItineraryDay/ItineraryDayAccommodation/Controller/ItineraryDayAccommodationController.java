package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.DTOs.CreateItineraryDayAccommodationDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.DTOs.UpdateItineraryDayAccommodationDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.Services.ItineraryDayAccommodationCreateService;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.Services.ItineraryDayAccommodationDeleteService;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.Services.ItineraryDayAccommodationGetService;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.Services.ItineraryDayAccommodationUpdateService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryDayAccommodationController - REST controller for managing accommodations in itinerary days
 */
@RestController
@RequestMapping("/api/itineraries/{itineraryId}/days/{dayId}/accommodations")
@Slf4j
public class ItineraryDayAccommodationController {

    private final ItineraryDayAccommodationCreateService createService;
    private final ItineraryDayAccommodationGetService getService;
    private final ItineraryDayAccommodationUpdateService updateService;
    private final ItineraryDayAccommodationDeleteService deleteService;

    @Autowired
    public ItineraryDayAccommodationController(
        ItineraryDayAccommodationCreateService createService,
        ItineraryDayAccommodationGetService getService,
        ItineraryDayAccommodationUpdateService updateService,
        ItineraryDayAccommodationDeleteService deleteService
    ) {
        this.createService = createService;
        this.getService = getService;
        this.updateService = updateService;
        this.deleteService = deleteService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_ITINERARY_DAY_ACCOMMODATION')")
    public ResponseEntity<ApiResponse<?>> createAccommodation(
        @PathVariable String itineraryId,
        @PathVariable String dayId,
        @Valid @RequestBody CreateItineraryDayAccommodationDTO createDTO
    ) {
        log.info("POST /api/itineraries/{}/days/{}/accommodations - Creating accommodation", itineraryId, dayId);
        return createService.createItineraryDayAccommodation(itineraryId, dayId, createDTO);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_ITINERARY_DAY_ACCOMMODATION')")
    public ResponseEntity<ApiResponse<?>> getAccommodations(
        @PathVariable String itineraryId,
        @PathVariable String dayId
    ) {
        log.info("GET /api/itineraries/{}/days/{}/accommodations - Fetching accommodations", itineraryId, dayId);
        return getService.getItineraryDayAccommodations(itineraryId, dayId);
    }

    @PutMapping("/{accommodationId}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ITINERARY_DAY_ACCOMMODATION')")
    public ResponseEntity<ApiResponse<?>> updateAccommodation(
        @PathVariable String itineraryId,
        @PathVariable String dayId,
        @PathVariable String accommodationId,
        @Valid @RequestBody UpdateItineraryDayAccommodationDTO updateDTO
    ) {
        log.info("PUT /api/itineraries/{}/days/{}/accommodations/{} - Updating accommodation", itineraryId, dayId, accommodationId);
        return updateService.updateItineraryDayAccommodation(itineraryId, dayId, accommodationId, updateDTO);
    }

    @DeleteMapping("/{accommodationId}")
    @PreAuthorize("hasAuthority('PERM_DELETE_ITINERARY_DAY_ACCOMMODATION')")
    public ResponseEntity<ApiResponse<?>> deleteAccommodation(
        @PathVariable String itineraryId,
        @PathVariable String dayId,
        @PathVariable String accommodationId
    ) {
        log.info("DELETE /api/itineraries/{}/days/{}/accommodations/{} - Deleting accommodation", itineraryId, dayId, accommodationId);
        return deleteService.deleteItineraryDayAccommodation(itineraryId, dayId, accommodationId);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_ITINERARY_DAY_ACCOMMODATION')")
    public ResponseEntity<ApiResponse<?>> deleteAccommodations(
        @PathVariable String itineraryId,
        @PathVariable String dayId,
        @RequestBody List<String> accommodationIds
    ) {
        log.info("DELETE /api/itineraries/{}/days/{}/accommodations - Deleting {} accommodations", itineraryId, dayId, accommodationIds.size());
        return deleteService.deleteItineraryDayAccommodations(itineraryId, dayId, accommodationIds);
    }
}
