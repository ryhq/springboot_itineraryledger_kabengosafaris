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

import com.itineraryledger.kabengosafaris.Flight.DTOs.CreateFlightRouteDTO;
import com.itineraryledger.kabengosafaris.Flight.DTOs.UpdateFlightRouteDTO;
import com.itineraryledger.kabengosafaris.Flight.Services.FlightRouteService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Sectors: which airline flies from where to where. */
@RestController
@RequestMapping("/api/flight-routes")
@RequiredArgsConstructor
public class FlightRouteController {

    private final FlightRouteService service;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_FLIGHT_ROUTE')")
    public ResponseEntity<ApiResponse<?>> getAll(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String airlineId,
        @RequestParam(required = false) String originAirstripId,
        @RequestParam(required = false) String destinationAirstripId,
        @RequestParam(required = false) Boolean isOnRequest,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "50") Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false, defaultValue = "asc") String sortDirection
    ) {
        return service.getAll(keyword, airlineId, originAirstripId, destinationAirstripId,
            isOnRequest, isActive, page, size, sortBy, sortDirection);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_FLIGHT_ROUTE')")
    public ResponseEntity<ApiResponse<?>> getById(@PathVariable String id) {
        return service.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_FLIGHT_ROUTE')")
    public ResponseEntity<ApiResponse<?>> create(@Valid @RequestBody CreateFlightRouteDTO dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_FLIGHT_ROUTE')")
    public ResponseEntity<ApiResponse<?>> update(@PathVariable String id,
                                                 @Valid @RequestBody UpdateFlightRouteDTO dto) {
        return service.update(id, dto);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_FLIGHT_ROUTE')")
    public ResponseEntity<ApiResponse<?>> delete(@RequestBody List<String> ids) {
        return service.delete(ids);
    }
}
