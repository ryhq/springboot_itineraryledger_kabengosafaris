package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayFlight.Controller;

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

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayFlight.DTOs.CreateSafariDayFlightDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayFlight.DTOs.UpdateSafariDayFlightDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayFlight.Services.SafariDayFlightService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Flights on a day of a safari.
 *
 * <p>Nested under the safari and the day, matching the safari's other day children.
 */
@RestController
@RequestMapping("/api/safaris/{safariId}/days/{dayId}/flights")
@RequiredArgsConstructor
public class SafariDayFlightController {

    private final SafariDayFlightService service;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_SAFARI_DAY_FLIGHT')")
    public ResponseEntity<ApiResponse<?>> list(@PathVariable String safariId,
                                               @PathVariable String dayId) {
        return service.getAll(dayId);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_SAFARI_DAY_FLIGHT')")
    public ResponseEntity<ApiResponse<?>> create(@PathVariable String safariId,
                                                 @PathVariable String dayId,
                                                 @Valid @RequestBody CreateSafariDayFlightDTO dto) {
        return service.create(dayId, dto);
    }

    @GetMapping("/{flightId}")
    @PreAuthorize("hasAuthority('PERM_READ_SAFARI_DAY_FLIGHT')")
    public ResponseEntity<ApiResponse<?>> getById(@PathVariable String safariId,
                                                  @PathVariable String dayId,
                                                  @PathVariable String flightId) {
        return service.getById(flightId);
    }

    @PutMapping("/{flightId}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_SAFARI_DAY_FLIGHT')")
    public ResponseEntity<ApiResponse<?>> update(@PathVariable String safariId,
                                                 @PathVariable String dayId,
                                                 @PathVariable String flightId,
                                                 @Valid @RequestBody UpdateSafariDayFlightDTO dto) {
        return service.update(flightId, dto);
    }

    /** Make this the day's primary flight and demote the rest, atomically. */
    @PostMapping("/{flightId}/make-primary")
    @PreAuthorize("hasAuthority('PERM_UPDATE_SAFARI_DAY_FLIGHT')")
    public ResponseEntity<ApiResponse<?>> makePrimary(@PathVariable String safariId,
                                                      @PathVariable String dayId,
                                                      @PathVariable String flightId) {
        return service.makePrimary(flightId);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_SAFARI_DAY_FLIGHT')")
    public ResponseEntity<ApiResponse<?>> delete(@PathVariable String safariId,
                                                 @PathVariable String dayId,
                                                 @RequestBody List<String> ids) {
        return service.delete(ids);
    }
}
