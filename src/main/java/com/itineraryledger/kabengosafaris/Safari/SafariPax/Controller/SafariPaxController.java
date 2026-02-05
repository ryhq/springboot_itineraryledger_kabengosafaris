package com.itineraryledger.kabengosafaris.Safari.SafariPax.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.SafariPax.DTOs.UpsertSafariPaxDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariPax.Services.SafariPaxDeleteService;
import com.itineraryledger.kabengosafaris.Safari.SafariPax.Services.SafariPaxGetService;
import com.itineraryledger.kabengosafaris.Safari.SafariPax.Services.SafariPaxUpsertService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * SafariPaxController - REST controller for managing safari passenger categories
 */
@RestController
@RequestMapping("/api/safaris/{safariId}/pax")
@Slf4j
public class SafariPaxController {

    private final SafariPaxUpsertService upsertService;
    private final SafariPaxGetService getService;
    private final SafariPaxDeleteService deleteService;

    @Autowired
    public SafariPaxController(
        SafariPaxUpsertService upsertService,
        SafariPaxGetService getService,
        SafariPaxDeleteService deleteService
    ) {
        this.upsertService = upsertService;
        this.getService = getService;
        this.deleteService = deleteService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_UPDATE_SAFARI_PAX')")
    public ResponseEntity<ApiResponse<?>> upsertPax(
        @PathVariable String safariId,
        @Valid @RequestBody List<UpsertSafariPaxDTO> upsertDTOs
    ) {
        log.info("POST /api/safaris/{}/pax - Upserting {} pax entries", safariId, upsertDTOs.size());
        return upsertService.upsertSafariPax(safariId, upsertDTOs);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_SAFARI_PAX')")
    public ResponseEntity<ApiResponse<?>> getPax(
        @PathVariable String safariId
    ) {
        log.info("GET /api/safaris/{}/pax - Fetching pax entries", safariId);
        return getService.getSafariPax(safariId);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_SAFARI_PAX')")
    public ResponseEntity<ApiResponse<?>> deletePax(
        @PathVariable String safariId,
        @RequestBody List<String> paxIds
    ) {
        log.info("DELETE /api/safaris/{}/pax - Deleting {} pax entries", safariId, paxIds.size());
        return deleteService.deleteSafariPax(safariId, paxIds);
    }
}
