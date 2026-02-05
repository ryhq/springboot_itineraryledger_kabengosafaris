package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.DTOs.CreateSafariDayAccommodationDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.DTOs.UpdateSafariDayAccommodationDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.Services.SafariDayAccommodationCreateService;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.Services.SafariDayAccommodationDeleteService;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.Services.SafariDayAccommodationGetService;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.Services.SafariDayAccommodationUpdateService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * SafariDayAccommodationController - REST controller for managing accommodations in safari days
 */
@RestController
@RequestMapping("/api/safaris/{safariId}/days/{dayId}/accommodations")
@Slf4j
public class SafariDayAccommodationController {

    private final SafariDayAccommodationCreateService createService;
    private final SafariDayAccommodationGetService getService;
    private final SafariDayAccommodationUpdateService updateService;
    private final SafariDayAccommodationDeleteService deleteService;

    @Autowired
    public SafariDayAccommodationController(
        SafariDayAccommodationCreateService createService,
        SafariDayAccommodationGetService getService,
        SafariDayAccommodationUpdateService updateService,
        SafariDayAccommodationDeleteService deleteService
    ) {
        this.createService = createService;
        this.getService = getService;
        this.updateService = updateService;
        this.deleteService = deleteService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_SAFARI_DAY_ACCOMMODATION')")
    public ResponseEntity<ApiResponse<?>> createAccommodation(
        @PathVariable String safariId,
        @PathVariable String dayId,
        @Valid @RequestBody CreateSafariDayAccommodationDTO createDTO
    ) {
        log.info("POST /api/safaris/{}/days/{}/accommodations - Creating accommodation", safariId, dayId);
        return createService.createSafariDayAccommodation(safariId, dayId, createDTO);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_SAFARI_DAY_ACCOMMODATION')")
    public ResponseEntity<ApiResponse<?>> getAccommodations(
        @PathVariable String safariId,
        @PathVariable String dayId
    ) {
        log.info("GET /api/safaris/{}/days/{}/accommodations - Fetching accommodations", safariId, dayId);
        return getService.getSafariDayAccommodations(safariId, dayId);
    }

    @PutMapping("/{accommodationId}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_SAFARI_DAY_ACCOMMODATION')")
    public ResponseEntity<ApiResponse<?>> updateAccommodation(
        @PathVariable String safariId,
        @PathVariable String dayId,
        @PathVariable String accommodationId,
        @Valid @RequestBody UpdateSafariDayAccommodationDTO updateDTO
    ) {
        log.info("PUT /api/safaris/{}/days/{}/accommodations/{} - Updating accommodation", safariId, dayId, accommodationId);
        return updateService.updateSafariDayAccommodation(safariId, dayId, accommodationId, updateDTO);
    }

    @DeleteMapping("/{accommodationId}")
    @PreAuthorize("hasAuthority('PERM_DELETE_SAFARI_DAY_ACCOMMODATION')")
    public ResponseEntity<ApiResponse<?>> deleteAccommodation(
        @PathVariable String safariId,
        @PathVariable String dayId,
        @PathVariable String accommodationId
    ) {
        log.info("DELETE /api/safaris/{}/days/{}/accommodations/{} - Deleting accommodation", safariId, dayId, accommodationId);
        return deleteService.deleteSafariDayAccommodation(safariId, dayId, accommodationId);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_SAFARI_DAY_ACCOMMODATION')")
    public ResponseEntity<ApiResponse<?>> deleteAccommodations(
        @PathVariable String safariId,
        @PathVariable String dayId,
        @RequestBody List<String> accommodationIds
    ) {
        log.info("DELETE /api/safaris/{}/days/{}/accommodations - Deleting {} accommodations", safariId, dayId, accommodationIds.size());
        return deleteService.deleteSafariDayAccommodations(safariId, dayId, accommodationIds);
    }
}
