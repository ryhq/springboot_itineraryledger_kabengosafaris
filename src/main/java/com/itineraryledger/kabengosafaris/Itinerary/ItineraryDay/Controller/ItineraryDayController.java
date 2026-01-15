package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.DTOs.CreateItineraryDayDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.DTOs.UpdateItineraryDayDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Services.ItineraryDayCreateService;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Services.ItineraryDayDeleteService;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Services.ItineraryDayGetService;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Services.ItineraryDayUpdateService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryDayController - REST controller for managing itinerary days
 */
@RestController
@RequestMapping("/api/itineraries/{itineraryId}/days")
@Tag(name = "Itinerary Day Management", description = "APIs for managing itinerary days")
@Slf4j
public class ItineraryDayController {

    private final ItineraryDayCreateService createService;
    private final ItineraryDayUpdateService updateService;
    private final ItineraryDayDeleteService deleteService;
    private final ItineraryDayGetService getService;

    @Autowired
    public ItineraryDayController(
        ItineraryDayCreateService createService,
        ItineraryDayUpdateService updateService,
        ItineraryDayDeleteService deleteService,
        ItineraryDayGetService getService
    ) {
        this.createService = createService;
        this.updateService = updateService;
        this.deleteService = deleteService;
        this.getService = getService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_ITINERARY_DAY')")
    public ResponseEntity<ApiResponse<?>> createDay(
        @Parameter(description = "Obfuscated itinerary ID") @PathVariable String itineraryId,
        @Valid @RequestBody CreateItineraryDayDTO createDTO
    ) {
        log.info("POST /api/itineraries/{}/days - Creating day {}", itineraryId, createDTO.getDayNumber());
        return createService.createItineraryDay(itineraryId, createDTO);
    }

    @PutMapping("/{dayId}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ITINERARY_DAY')")
    public ResponseEntity<ApiResponse<?>> updateDay(
        @Parameter(description = "Obfuscated itinerary ID") @PathVariable String itineraryId,
        @Parameter(description = "Obfuscated day ID") @PathVariable String dayId,
        @Valid @RequestBody UpdateItineraryDayDTO updateDTO
    ) {
        log.info("PUT /api/itineraries/{}/days/{} - Updating day", itineraryId, dayId);
        return updateService.updateItineraryDay(itineraryId, dayId, updateDTO);
    }

    @DeleteMapping("/{dayId}")
    @PreAuthorize("hasAuthority('PERM_DELETE_ITINERARY_DAY')")
    public ResponseEntity<ApiResponse<?>> deleteDay(
        @Parameter(description = "Obfuscated itinerary ID") @PathVariable String itineraryId,
        @Parameter(description = "Obfuscated day ID") @PathVariable String dayId
    ) {
        log.info("DELETE /api/itineraries/{}/days/{} - Deleting day", itineraryId, dayId);
        return deleteService.deleteItineraryDay(itineraryId, dayId);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_ITINERARY_DAY')")
    public ResponseEntity<ApiResponse<?>> deleteDays(
        @Parameter(description = "Obfuscated itinerary ID") @PathVariable String itineraryId,
        @Parameter(description = "List of day IDs to delete") @RequestBody List<String> dayIds
    ) {
        log.info("DELETE /api/itineraries/{}/days - Deleting {} days", itineraryId, dayIds.size());
        return deleteService.deleteItineraryDays(itineraryId, dayIds);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_ITINERARY_DAY')")
    public ResponseEntity<ApiResponse<?>> getDays(
        @Parameter(description = "Obfuscated itinerary ID") @PathVariable String itineraryId
    ) {
        log.info("GET /api/itineraries/{}/days - Fetching all days", itineraryId);
        return getService.getItineraryDays(itineraryId);
    }

    @GetMapping("/{dayId}")
    @PreAuthorize("hasAuthority('PERM_READ_ITINERARY_DAY')")
    public ResponseEntity<ApiResponse<?>> getDay(
        @Parameter(description = "Obfuscated itinerary ID") @PathVariable String itineraryId,
        @Parameter(description = "Obfuscated day ID") @PathVariable String dayId
    ) {
        log.info("GET /api/itineraries/{}/days/{} - Fetching day", itineraryId, dayId);
        return getService.getItineraryDay(itineraryId, dayId);
    }

    @GetMapping("/number/{dayNumber}")
    @PreAuthorize("hasAuthority('PERM_READ_ITINERARY_DAY')")
    public ResponseEntity<ApiResponse<?>> getDayByNumber(
        @Parameter(description = "Obfuscated itinerary ID") @PathVariable String itineraryId,
        @Parameter(description = "Day number") @PathVariable Integer dayNumber
    ) {
        log.info("GET /api/itineraries/{}/days/number/{} - Fetching day by number", itineraryId, dayNumber);
        return getService.getItineraryDayByNumber(itineraryId, dayNumber);
    }
}
