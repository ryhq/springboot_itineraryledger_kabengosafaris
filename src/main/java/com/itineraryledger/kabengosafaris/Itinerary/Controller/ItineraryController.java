package com.itineraryledger.kabengosafaris.Itinerary.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Itinerary.Entity.BudgetCategory;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.TripType;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary.ItineraryStatus;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.CreateItineraryDTO;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.UpdateItineraryDTO;
import com.itineraryledger.kabengosafaris.Itinerary.Services.ItineraryCreateService;
import com.itineraryledger.kabengosafaris.Itinerary.Services.ItineraryDeleteService;
import com.itineraryledger.kabengosafaris.Itinerary.Services.ItineraryGetService;
import com.itineraryledger.kabengosafaris.Itinerary.Services.ItineraryUpdateService;
import com.itineraryledger.kabengosafaris.Itinerary.Services.ItineraryStatusService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryController - REST controller for managing safari itineraries
 */
@RestController
@RequestMapping("/api/itineraries")
@Slf4j
public class ItineraryController {

    private final ItineraryCreateService createService;
    private final ItineraryUpdateService updateService;
    private final ItineraryDeleteService deleteService;
    private final ItineraryGetService getService;
    private final ItineraryStatusService statusService;

    @Autowired
    public ItineraryController(
        ItineraryCreateService createService,
        ItineraryUpdateService updateService,
        ItineraryDeleteService deleteService,
        ItineraryGetService getService,
        ItineraryStatusService statusService
    ) {
        this.createService = createService;
        this.updateService = updateService;
        this.deleteService = deleteService;
        this.getService = getService;
        this.statusService = statusService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_ITINERARY')")
    public ResponseEntity<ApiResponse<?>> createItinerary(
        @Valid @RequestBody CreateItineraryDTO createDTO
    ) {
        log.info("POST /api/itineraries - Creating new itinerary: {}", createDTO.getName());
        return createService.createItinerary(createDTO);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ITINERARY')")
    public ResponseEntity<ApiResponse<?>> updateItinerary(
        @PathVariable String id,
        @Valid @RequestBody UpdateItineraryDTO updateDTO
    ) {
        log.info("PUT /api/itineraries/{} - Updating itinerary", id);
        return updateService.updateItinerary(id, updateDTO);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_ITINERARY')")
    public ResponseEntity<ApiResponse<?>> deleteItineraries(
        @RequestBody List<String> ids
    ) {
        log.info("DELETE /api/itineraries - Deleting {} itineraries", ids.size());
        return deleteService.deleteItineraries(ids);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_ITINERARY')")
    public ResponseEntity<ApiResponse<?>> getItineraryById(
        @PathVariable String id
    ) {
        log.info("GET /api/itineraries/{} - Fetching itinerary", id);
        return getService.getItineraryById(id);
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAuthority('PERM_READ_ITINERARY')")
    public ResponseEntity<ApiResponse<?>> getItineraryByCode(
        @PathVariable String code
    ) {
        log.info("GET /api/itineraries/code/{} - Fetching itinerary by code", code);
        return getService.getItineraryByCode(code);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_ITINERARY')")
    public ResponseEntity<ApiResponse<?>> getAllItineraries(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String code,
        @RequestParam(required = false) ItineraryStatus status,
        @RequestParam(required = false) TripType tripType,
        @RequestParam(required = false) BudgetCategory budgetCategory,
        @RequestParam(required = false) String startLocation,
        @RequestParam(required = false) String endLocation,
        @RequestParam(required = false) Integer totalDays,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) Boolean isDayTrip,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/itineraries - Fetching all itineraries");
        return getService.getAllItineraries(name, code, status, tripType, budgetCategory, startLocation, endLocation, totalDays, isActive, isDayTrip, keyword, page, size, sortDirection);
    }

    // Status management endpoints

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('PERM_PUBLISH_ITINERARY')")
    public ResponseEntity<ApiResponse<?>> publishItinerary(
        @PathVariable String id
    ) {
        log.info("POST /api/itineraries/{}/publish - Publishing itinerary", id);
        return statusService.publishItinerary(id);
    }

    @PostMapping("/{id}/unpublish")
    @PreAuthorize("hasAuthority('PERM_UNPUBLISH_ITINERARY')")
    public ResponseEntity<ApiResponse<?>> unpublishItinerary(
        @PathVariable String id
    ) {
        log.info("POST /api/itineraries/{}/unpublish - Unpublishing itinerary", id);
        return statusService.unpublishItinerary(id);
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAuthority('PERM_ARCHIVE_ITINERARY')")
    public ResponseEntity<ApiResponse<?>> archiveItinerary(
        @PathVariable String id
    ) {
        log.info("POST /api/itineraries/{}/archive - Archiving itinerary", id);
        return statusService.archiveItinerary(id);
    }

    @PostMapping("/{id}/unarchive")
    @PreAuthorize("hasAuthority('PERM_UNARCHIVE_ITINERARY')")
    public ResponseEntity<ApiResponse<?>> unarchiveItinerary(
        @PathVariable String id
    ) {
        log.info("POST /api/itineraries/{}/unarchive - Unarchiving itinerary", id);
        return statusService.unarchiveItinerary(id);
    }
}
