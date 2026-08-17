package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.DTOs.CreateItineraryDayDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.DTOs.DuplicateItineraryDayDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.DTOs.ReorderItineraryDaysDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.DTOs.UpdateItineraryDayDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Services.ItineraryDayCreateService;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Services.ItineraryDayDeleteService;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Services.ItineraryDayDuplicateService;
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
    private final ItineraryDayDuplicateService duplicateService;

    @Autowired
    public ItineraryDayController(
        ItineraryDayCreateService createService,
        ItineraryDayUpdateService updateService,
        ItineraryDayDeleteService deleteService,
        ItineraryDayGetService getService,
        ItineraryDayReorderService reorderService,
        ItineraryDayDuplicateService duplicateService
    ) {
        this.createService = createService;
        this.updateService = updateService;
        this.deleteService = deleteService;
        this.getService = getService;
        this.reorderService = reorderService;
        this.duplicateService = duplicateService;
    }

    /**
     * Copy a day, with everything on it, one or more times.
     *
     * Three nights in the same park are three days that differ by a sentence.
     * The copies land directly after the source and the rest are renumbered,
     * because "another night here" means next, not last. The itinerary's day
     * count is the ceiling — the refusal names it rather than making an
     * itinerary that can never be completed.
     */
    @PostMapping("/{dayId}/duplicate")
    @PreAuthorize("hasAuthority('PERM_CREATE_ITINERARY_DAY')")
    public ResponseEntity<ApiResponse<?>> duplicateDay(
        @PathVariable String itineraryId,
        @PathVariable String dayId,
        @RequestBody(required = false) DuplicateItineraryDayDTO duplicateDTO
    ) {
        log.info("POST /api/itineraries/{}/days/{}/duplicate - Duplicating day", itineraryId, dayId);
        return duplicateService.duplicateDay(itineraryId, dayId, duplicateDTO);
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
    ,
        /* the sort travels with the record so its arrows keep the list's order */
        @RequestParam(required = false) String sortBy
        , @RequestParam(required = false) String sortDirection
    ) {
        log.info("GET /api/itineraries/{}/days/{} - Fetching day", itineraryId, dayId);
        return getService.getItineraryDay(itineraryId, dayId, sortBy, sortDirection);
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
