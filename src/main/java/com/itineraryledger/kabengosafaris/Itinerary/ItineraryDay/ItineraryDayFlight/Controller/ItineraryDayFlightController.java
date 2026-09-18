package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayFlight.Controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayFlight.DTOs.CreateItineraryDayFlightDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayFlight.DTOs.UpdateItineraryDayFlightDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayFlight.Services.ItineraryDayFlightService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Flights on a day of an itinerary.
 *
 * <p>Nested under the day like accommodations and park visits, and guarded by the same permission
 * shape.
 */
@RestController
@RequestMapping("/api/itineraries/{itineraryId}/days/{dayId}/flights")
@RequiredArgsConstructor
public class ItineraryDayFlightController {

    private final ItineraryDayFlightService service;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_ITINERARY_DAY_FLIGHT')")
    public ResponseEntity<ApiResponse<?>> getAll(@PathVariable String itineraryId,
                                                 @PathVariable String dayId) {
        return service.getAll(dayId);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_ITINERARY_DAY_FLIGHT')")
    public ResponseEntity<ApiResponse<?>> create(@PathVariable String itineraryId,
                                                 @PathVariable String dayId,
                                                 @Valid @RequestBody CreateItineraryDayFlightDTO dto) {
        return service.create(dayId, dto);
    }

    @PutMapping("/{flightId}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ITINERARY_DAY_FLIGHT')")
    public ResponseEntity<ApiResponse<?>> update(@PathVariable String itineraryId,
                                                 @PathVariable String dayId,
                                                 @PathVariable String flightId,
                                                 @Valid @RequestBody UpdateItineraryDayFlightDTO dto) {
        return service.update(flightId, dto);
    }

    /** Make this the day's primary flight and demote the rest, atomically. */
    @PostMapping("/{flightId}/make-primary")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ITINERARY_DAY_FLIGHT')")
    public ResponseEntity<ApiResponse<?>> makePrimary(@PathVariable String itineraryId,
                                                      @PathVariable String dayId,
                                                      @PathVariable String flightId) {
        return service.makePrimary(flightId);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_ITINERARY_DAY_FLIGHT')")
    public ResponseEntity<ApiResponse<?>> delete(@PathVariable String itineraryId,
                                                 @PathVariable String dayId,
                                                 @RequestBody List<String> ids) {
        return service.delete(ids);
    }
}
