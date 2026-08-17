package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkTariff.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkTariff.DTOs.CreateSafariDayParkTariffDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkTariff.DTOs.UpdateSafariDayParkTariffDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkTariff.Services.SafariDayParkTariffCreateService;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkTariff.Services.SafariDayParkTariffDeleteService;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkTariff.Services.SafariDayParkTariffGetService;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkTariff.Services.SafariDayParkTariffUpdateService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * SafariDayParkTariffController - REST controller for managing tariffs within a safari park visit
 */
@RestController
@RequestMapping("/api/safaris/{safariId}/days/{dayId}/parks/{parkVisitId}/tariffs")
@Slf4j
public class SafariDayParkTariffController {

    private final SafariDayParkTariffCreateService createService;
    private final SafariDayParkTariffGetService getService;
    private final SafariDayParkTariffUpdateService updateService;
    private final SafariDayParkTariffDeleteService deleteService;

    @Autowired
    public SafariDayParkTariffController(
        SafariDayParkTariffCreateService createService,
        SafariDayParkTariffGetService getService,
        SafariDayParkTariffUpdateService updateService,
        SafariDayParkTariffDeleteService deleteService
    ) {
        this.createService = createService;
        this.getService = getService;
        this.updateService = updateService;
        this.deleteService = deleteService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_UPDATE_SAFARI_DAY_PARK_TARIFF')")
    public ResponseEntity<ApiResponse<?>> addParkTariffs(
        @PathVariable String safariId,
        @PathVariable String dayId,
        @PathVariable String parkVisitId,
        @Valid @RequestBody List<CreateSafariDayParkTariffDTO> createDTOs
    ) {
        log.info("POST /api/safaris/{}/days/{}/parks/{}/tariffs - Adding {} tariffs",
            safariId, dayId, parkVisitId, createDTOs.size());
        return createService.addParkTariffs(parkVisitId, createDTOs);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_SAFARI_DAY_PARK_TARIFF')")
    public ResponseEntity<ApiResponse<?>> getParkTariffs(
        @PathVariable String safariId,
        @PathVariable String dayId,
        @PathVariable String parkVisitId,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection
    ) {
        log.info("GET /api/safaris/{}/days/{}/parks/{}/tariffs - Fetching tariffs",
            safariId, dayId, parkVisitId);
        return getService.getParkTariffs(parkVisitId, sortBy, sortDirection);
    }

    @GetMapping("/{tariffId}")
    @PreAuthorize("hasAuthority('PERM_READ_SAFARI_DAY_PARK_TARIFF')")
    public ResponseEntity<ApiResponse<?>> getParkTariff(
        @PathVariable String safariId,
        @PathVariable String dayId,
        @PathVariable String parkVisitId,
        @PathVariable String tariffId
    ,
        /* the sort travels with the record so its arrows keep the list's order */
        @RequestParam(required = false) String sortBy
        , @RequestParam(required = false) String sortDirection
    ) {
        log.info("GET /api/safaris/{}/days/{}/parks/{}/tariffs/{} - Fetching tariff",
            safariId, dayId, parkVisitId, tariffId);
        return getService.getParkTariff(parkVisitId, tariffId, sortBy, sortDirection);
    }

    @PutMapping("/{tariffId}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_SAFARI_DAY_PARK_TARIFF')")
    public ResponseEntity<ApiResponse<?>> updateParkTariff(
        @PathVariable String safariId,
        @PathVariable String dayId,
        @PathVariable String parkVisitId,
        @PathVariable String tariffId,
        @Valid @RequestBody UpdateSafariDayParkTariffDTO updateDTO
    ) {
        log.info("PUT /api/safaris/{}/days/{}/parks/{}/tariffs/{} - Updating tariff",
            safariId, dayId, parkVisitId, tariffId);
        return updateService.updateParkTariff(parkVisitId, tariffId, updateDTO);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_UPDATE_SAFARI_DAY_PARK_TARIFF')")
    public ResponseEntity<ApiResponse<?>> deleteParkTariffs(
        @PathVariable String safariId,
        @PathVariable String dayId,
        @PathVariable String parkVisitId,
        @RequestBody List<String> tariffIds
    ) {
        log.info("DELETE /api/safaris/{}/days/{}/parks/{}/tariffs - Deleting {} tariffs",
            safariId, dayId, parkVisitId, tariffIds.size());
        return deleteService.deleteParkTariffs(parkVisitId, tariffIds);
    }
}
