package com.itineraryledger.kabengosafaris.Itinerary.ItineraryPax.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryPax.DTOs.UpsertItineraryPaxDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryPax.Services.ItineraryPaxDeleteService;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryPax.Services.ItineraryPaxGetService;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryPax.Services.ItineraryPaxUpsertService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryPaxController - REST controller for managing itinerary passenger categories
 */
@RestController
@RequestMapping("/api/itineraries/{itineraryId}/pax")
@Slf4j
public class ItineraryPaxController {

    private final ItineraryPaxUpsertService upsertService;
    private final ItineraryPaxGetService getService;
    private final ItineraryPaxDeleteService deleteService;

    @Autowired
    public ItineraryPaxController(
        ItineraryPaxUpsertService upsertService,
        ItineraryPaxGetService getService,
        ItineraryPaxDeleteService deleteService
    ) {
        this.upsertService = upsertService;
        this.getService = getService;
        this.deleteService = deleteService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_UPDATE_ITINERARY_PAX')")
    public ResponseEntity<ApiResponse<?>> upsertPax(
        @PathVariable String itineraryId,
        @Valid @RequestBody List<UpsertItineraryPaxDTO> upsertDTOs
    ) {
        log.info("POST /api/itineraries/{}/pax - Upserting {} pax entries", itineraryId, upsertDTOs.size());
        return upsertService.upsertItineraryPax(itineraryId, upsertDTOs);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_ITINERARY_PAX')")
    public ResponseEntity<ApiResponse<?>> getPax(
        @PathVariable String itineraryId
    ) {
        log.info("GET /api/itineraries/{}/pax - Fetching pax entries", itineraryId);
        return getService.getItineraryPax(itineraryId);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_ITINERARY_PAX')")
    public ResponseEntity<ApiResponse<?>> deletePax(
        @PathVariable String itineraryId,
        @RequestBody List<String> paxIds
    ) {
        log.info("DELETE /api/itineraries/{}/pax - Deleting {} pax entries", itineraryId, paxIds.size());
        return deleteService.deleteItineraryPax(itineraryId, paxIds);
    }
}
