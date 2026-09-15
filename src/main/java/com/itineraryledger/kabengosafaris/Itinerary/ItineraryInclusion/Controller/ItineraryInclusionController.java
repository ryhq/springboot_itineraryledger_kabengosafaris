package com.itineraryledger.kabengosafaris.Itinerary.ItineraryInclusion.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryInclusion.DTOs.SetItineraryInclusionsDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryInclusion.Services.ItineraryInclusionService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * What one itinerary's price covers.
 *
 * <p>Guarded by the itinerary's own permissions rather than a new entity of its own: this is a
 * property panel on the itinerary, not a browsable list, and the permission catalogue is already
 * near five hundred rows.
 */
@RestController
@RequestMapping("/api/itineraries/{itineraryId}/inclusions")
@RequiredArgsConstructor
public class ItineraryInclusionController {

    private final ItineraryInclusionService service;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_ITINERARY')")
    public ResponseEntity<ApiResponse<?>> get(@PathVariable String itineraryId) {
        return service.get(itineraryId);
    }

    /** The whole ordered set at once — position in the array is the print order. */
    @PutMapping
    @PreAuthorize("hasAuthority('PERM_UPDATE_ITINERARY')")
    public ResponseEntity<ApiResponse<?>> set(
        @PathVariable String itineraryId,
        @Valid @RequestBody SetItineraryInclusionsDTO dto
    ) {
        return service.set(itineraryId, dto);
    }
}
