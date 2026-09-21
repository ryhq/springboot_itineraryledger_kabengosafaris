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

import com.itineraryledger.kabengosafaris.Flight.DTOs.CreateAirstripDTO;
import com.itineraryledger.kabengosafaris.Flight.DTOs.UpdateAirstripDTO;
import com.itineraryledger.kabengosafaris.Flight.Services.AirstripService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Airstrips: the places flights go between. */
@RestController
@RequestMapping("/api/airstrips")
@RequiredArgsConstructor
public class AirstripController {

    private final AirstripService service;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_AIRSTRIP')")
    public ResponseEntity<ApiResponse<?>> getAll(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String region,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "50") Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false, defaultValue = "asc") String sortDirection
    ) {
        return service.getAll(keyword, region, isActive, page, size, sortBy, sortDirection);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_AIRSTRIP')")
    public ResponseEntity<ApiResponse<?>> getById(
        @PathVariable String id,
        /* The list's filters, so the record arrows walk the same set that was on screen. */
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String region,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection) {
        return service.getById(id, keyword, region, isActive, sortBy, sortDirection);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_AIRSTRIP')")
    public ResponseEntity<ApiResponse<?>> create(@Valid @RequestBody CreateAirstripDTO dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_AIRSTRIP')")
    public ResponseEntity<ApiResponse<?>> update(@PathVariable String id,
                                                 @Valid @RequestBody UpdateAirstripDTO dto) {
        return service.update(id, dto);
    }

    /** A bare array of ids, which is the house contract for a bulk delete. */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_AIRSTRIP')")
    public ResponseEntity<ApiResponse<?>> delete(@RequestBody List<String> ids) {
        return service.delete(ids);
    }
}
