package com.itineraryledger.kabengosafaris.Flight.Controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.itineraryledger.kabengosafaris.Flight.DTOs.BulkUpsertFlightFareDTO;
import com.itineraryledger.kabengosafaris.Flight.DTOs.UpdateFlightFareDTO;
import com.itineraryledger.kabengosafaris.Flight.Services.FlightFareService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Fares: one published departure of a sector, in one contract window.
 *
 * <p>There is no create endpoint. A price list arrives as a list and is reissued annually, so the
 * way in is the bulk upsert, which updates a row already present rather than duplicating it.
 */
@RestController
@RequestMapping("/api/flight-fares")
@RequiredArgsConstructor
public class FlightFareController {

    private final FlightFareService service;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_FLIGHT_FARE')")
    public ResponseEntity<ApiResponse<?>> getAll(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String flightRouteId,
        @RequestParam(required = false) String airlineId,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) Boolean includeRetired,
        @RequestParam(required = false) Boolean missingNetFare,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "50") Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false, defaultValue = "asc") String sortDirection
    ) {
        return service.getAll(keyword, flightRouteId, airlineId, isActive, includeRetired,
            missingNetFare, page, size, sortBy, sortDirection);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_FLIGHT_FARE')")
    public ResponseEntity<ApiResponse<?>> getById(
        @PathVariable String id,
        /* The list's filters, so the record arrows walk the same set that was on screen. */
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String flightRouteId,
        @RequestParam(required = false) String airlineId,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) Boolean includeRetired,
        @RequestParam(required = false) Boolean missingNetFare,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection) {
        return service.getById(id, keyword, flightRouteId, airlineId, isActive, includeRetired, missingNetFare, sortBy, sortDirection);
    }

    /** A price list, all at once. Reports per-row outcomes so a bad line names itself. */
    @PostMapping("/bulk-upsert")
    @PreAuthorize("hasAuthority('PERM_CREATE_FLIGHT_FARE')")
    public ResponseEntity<ApiResponse<?>> bulkUpsert(@Valid @RequestBody List<BulkUpsertFlightFareDTO> rows) {
        return service.bulkUpsert(rows);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_FLIGHT_FARE')")
    public ResponseEntity<ApiResponse<?>> update(@PathVariable String id,
                                                 @Valid @RequestBody UpdateFlightFareDTO dto) {
        return service.update(id, dto);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_FLIGHT_FARE')")
    public ResponseEntity<ApiResponse<?>> delete(@RequestBody List<String> ids) {
        return service.delete(ids);
    }
}
