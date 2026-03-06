package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.DTOs.CreateItineraryDayDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.DTOs.ReorderItineraryDaysDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.DTOs.UpdateItineraryDayDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Services.ItineraryDayCreateService;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Services.ItineraryDayDeleteService;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Services.ItineraryDayGetService;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Services.ItineraryDayReorderService;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Services.ItineraryDayUpdateService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryDayController - REST controller for managing itinerary days
 */
@RestController
@RequestMapping("/api/itineraries/{itineraryId}/days")
@Slf4j
public class ItineraryDayController {

    private final ItineraryDayCreateService createService;
    private final ItineraryDayUpdateService updateService;
    private final ItineraryDayDeleteService deleteService;
    private final ItineraryDayGetService getService;
    private final ItineraryDayReorderService reorderService;

    @Autowired
    public ItineraryDayController(
        ItineraryDayCreateService createService,
        ItineraryDayUpdateService updateService,
        ItineraryDayDeleteService deleteService,
        ItineraryDayGetService getService,
        ItineraryDayReorderService reorderService
    ) {
        this.createService = createService;
        this.updateService = updateService;
        this.deleteService = deleteService;
        this.getService = getService;
        this.reorderService = reorderService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_ITINERARY_DAY')")
    public ResponseEntity<ApiResponse<?>> createDay(
        @PathVariable String itineraryId,
        @Valid @RequestBody CreateItineraryDayDTO createDTO
    ) {
        log.info("POST /api/itineraries/{}/days - Creating new day: {}", itineraryId, createDTO.getTitle());
        return createService.createItineraryDay(itineraryId, createDTO);
    }

    @PutMapping("/{dayId}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ITINERARY_DAY')")
    public ResponseEntity<ApiResponse<?>> updateDay(
        @PathVariable String itineraryId,
        @PathVariable String dayId,
        @Valid @RequestBody UpdateItineraryDayDTO updateDTO
    ) {
        log.info("PUT /api/itineraries/{}/days/{} - Updating day", itineraryId, dayId);
        return updateService.updateItineraryDay(itineraryId, dayId, updateDTO);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_ITINERARY_DAY')")
    public ResponseEntity<ApiResponse<?>> deleteDays(
        @PathVariable String itineraryId,
        @RequestBody List<String> dayIds
    ) {
        log.info("DELETE /api/itineraries/{}/days - Deleting {} days", itineraryId, dayIds.size());
        return deleteService.deleteItineraryDays(itineraryId, dayIds);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_ITINERARY_DAY')")
    public ResponseEntity<ApiResponse<?>> getDays(
        @PathVariable String itineraryId,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection
    ) {
        log.info("GET /api/itineraries/{}/days - Fetching all days", itineraryId);
        return getService.getItineraryDays(itineraryId, sortBy, sortDirection);
    }

    @GetMapping("/{dayId}")
    @PreAuthorize("hasAuthority('PERM_READ_ITINERARY_DAY')")
    public ResponseEntity<ApiResponse<?>> getDay(
        @PathVariable String itineraryId,
        @PathVariable String dayId
    ) {
        log.info("GET /api/itineraries/{}/days/{} - Fetching day", itineraryId, dayId);
        return getService.getItineraryDay(itineraryId, dayId);
    }

    @PostMapping("/reorder")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ITINERARY_DAY')")
    public ResponseEntity<ApiResponse<?>> reorderDays(
        @PathVariable String itineraryId,
        @Valid @RequestBody ReorderItineraryDaysDTO reorderDTO
    ) {
        log.info("POST /api/itineraries/{}/days/reorder - Reordering {} days", itineraryId, reorderDTO.getDayOrder().size());
        return reorderService.reorderItineraryDays(itineraryId, reorderDTO);
    }
}
