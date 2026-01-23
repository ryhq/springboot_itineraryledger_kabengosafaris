package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.DTOs.CreateItineraryDayParkTariffDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.Services.ItineraryDayParkTariffCreateService;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.Services.ItineraryDayParkTariffDeleteService;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.Services.ItineraryDayParkTariffGetService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryDayParkTariffController - REST controller for managing tariffs within a park visit
 */
@RestController
@RequestMapping("/api/itineraries/{itineraryId}/days/{dayId}/parks/{parkVisitId}/tariffs")
@Slf4j
public class ItineraryDayParkTariffController {

    private final ItineraryDayParkTariffCreateService createService;
    private final ItineraryDayParkTariffGetService getService;
    private final ItineraryDayParkTariffDeleteService deleteService;

    @Autowired
    public ItineraryDayParkTariffController(
        ItineraryDayParkTariffCreateService createService,
        ItineraryDayParkTariffGetService getService,
        ItineraryDayParkTariffDeleteService deleteService
    ) {
        this.createService = createService;
        this.getService = getService;
        this.deleteService = deleteService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_UPDATE_ITINERARY_DAY_PARK_TARIFF')")
    public ResponseEntity<ApiResponse<?>> addParkTariffs(
        @PathVariable String itineraryId,
        @PathVariable String dayId,
        @PathVariable String parkVisitId,
        @Valid @RequestBody List<CreateItineraryDayParkTariffDTO> createDTOs
    ) {
        log.info("POST /api/itineraries/{}/days/{}/parks/{}/tariffs - Adding {} tariffs",
            itineraryId, dayId, parkVisitId, createDTOs.size());
        return createService.addParkTariffs(parkVisitId, createDTOs);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_ITINERARY_DAY_PARK_TARIFF')")
    public ResponseEntity<ApiResponse<?>> getParkTariffs(
        @PathVariable String itineraryId,
        @PathVariable String dayId,
        @PathVariable String parkVisitId
    ) {
        log.info("GET /api/itineraries/{}/days/{}/parks/{}/tariffs - Fetching tariffs",
            itineraryId, dayId, parkVisitId);
        return getService.getParkTariffs(parkVisitId);
    }

    @GetMapping("/{tariffId}")
    @PreAuthorize("hasAuthority('PERM_READ_ITINERARY_DAY_PARK_TARIFF')")
    public ResponseEntity<ApiResponse<?>> getParkTariff(
        @PathVariable String itineraryId,
        @PathVariable String dayId,
        @PathVariable String parkVisitId,
        @PathVariable String tariffId
    ) {
        log.info("GET /api/itineraries/{}/days/{}/parks/{}/tariffs/{} - Fetching tariff",
            itineraryId, dayId, parkVisitId, tariffId);
        return getService.getParkTariff(parkVisitId, tariffId);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_UPDATE_ITINERARY_DAY_PARK_TARIFF')")
    public ResponseEntity<ApiResponse<?>> deleteParkTariffs(
        @PathVariable String itineraryId,
        @PathVariable String dayId,
        @PathVariable String parkVisitId,
        @RequestBody List<String> tariffIds
    ) {
        log.info("DELETE /api/itineraries/{}/days/{}/parks/{}/tariffs - Deleting {} tariffs",
            itineraryId, dayId, parkVisitId, tariffIds.size());
        return deleteService.deleteParkTariffs(parkVisitId, tariffIds);
    }
}
