package com.itineraryledger.kabengosafaris.Tariff;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Activity.ChargingBasis;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Tariff.DTOs.CreateTariffDTO;
import com.itineraryledger.kabengosafaris.Tariff.DTOs.UpdateTariffDTO;
import com.itineraryledger.kabengosafaris.Tariff.Services.CreateTariffService;
import com.itineraryledger.kabengosafaris.Tariff.Services.DeleteTariffService;
import com.itineraryledger.kabengosafaris.Tariff.Services.GetTariffService;
import com.itineraryledger.kabengosafaris.Tariff.Services.UpdateTariffService;


import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * TariffController - REST controller for managing tariffs
 *
 * Provides endpoints for CRUD operations on tariffs with permission-based access control.
 * Tariffs are base definitions (e.g., "Park Entry Fee", "Conservation Fee") that can be
 * linked to parks via the ParkTariff join entity.
 */
@RestController
@RequestMapping("/api/tariffs")
@Slf4j
public class TariffController {

    private final CreateTariffService createTariffService;
    private final UpdateTariffService updateTariffService;
    private final DeleteTariffService deleteTariffService;
    private final GetTariffService getTariffService;

    @Autowired
    public TariffController(
        CreateTariffService createTariffService,
        UpdateTariffService updateTariffService,
        DeleteTariffService deleteTariffService,
        GetTariffService getTariffService
    ) {
        this.createTariffService = createTariffService;
        this.updateTariffService = updateTariffService;
        this.deleteTariffService = deleteTariffService;
        this.getTariffService = getTariffService;
    }

    /**
     * Create a new tariff
     *
     * @param createTariffDTO The tariff data
     * @return ResponseEntity with ApiResponse containing the created tariff
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_TARIFF')")
    public ResponseEntity<ApiResponse<?>> createTariff(
        @Valid @RequestBody CreateTariffDTO createTariffDTO
    ) {
        log.info("POST /api/tariffs - Creating new tariff: {}", createTariffDTO.getName());
        return createTariffService.createTariff(createTariffDTO);
    }

    /**
     * Update an existing tariff
     *
     * @param idObfuscated The obfuscated tariff ID
     * @param updateTariffDTO The updated tariff data
     * @return ResponseEntity with ApiResponse containing the updated tariff
     */
    @PutMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_TARIFF')")
    public ResponseEntity<ApiResponse<?>> updateTariff(
        @PathVariable String idObfuscated,
        @Valid @RequestBody UpdateTariffDTO updateTariffDTO
    ) {
        log.info("PUT /api/tariffs/{} - Updating tariff", idObfuscated);
        return updateTariffService.updateTariff(idObfuscated, updateTariffDTO);
    }

    /**
     * Delete tariffs by list of IDs
     *
     * @param idObfuscatedList List of obfuscated tariff IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_TARIFF')")
    public ResponseEntity<ApiResponse<?>> deleteTariffs(
        @RequestBody List<String> idObfuscatedList
    ) {
        log.info("DELETE /api/tariffs - Deleting {} tariffs", idObfuscatedList.size());
        return deleteTariffService.deleteTariffs(idObfuscatedList);
    }

    /**
     * Get a single tariff by ID
     *
     * @param idObfuscated The obfuscated tariff ID
     * @return ResponseEntity with ApiResponse containing the tariff
     */
    @GetMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_READ_TARIFF')")
    public ResponseEntity<ApiResponse<?>> getTariffById(
        @PathVariable String idObfuscated
    ) {
        log.info("GET /api/tariffs/{} - Fetching tariff by ID", idObfuscated);
        return getTariffService.getTariffById(idObfuscated);
    }

    /**
     * Get a single tariff by slug
     *
     * @param slug The tariff slug
     * @return ResponseEntity with ApiResponse containing the tariff
     */
    @GetMapping("/slug/{slug}")
    @PreAuthorize("hasAuthority('PERM_READ_TARIFF')")
    public ResponseEntity<ApiResponse<?>> getTariffBySlug(
        @PathVariable String slug
    ) {
        log.info("GET /api/tariffs/slug/{} - Fetching tariff by slug", slug);
        return getTariffService.getTariffBySlug(slug);
    }

    /**
     * Get all tariffs with pagination, sorting, and filtering
     *
     * @param name Filter by name (partial match)
     * @param slug Filter by slug (partial match)
     * @param chargingBasis Filter by charging basis
     * @param isActive Filter by active status
     * @param isSystem Filter by system status
     * @param keyword Search keyword across multiple fields
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortDirection Sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing paginated tariffs
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_TARIFF')")
    public ResponseEntity<ApiResponse<?>> getAllTariffs(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String slug,
        @RequestParam(required = false) ChargingBasis chargingBasis,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) Boolean isSystem,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/tariffs - Fetching all tariffs with filters");
        return getTariffService.getAllTariffs(
            name,
            slug,
            chargingBasis,
            isActive,
            isSystem,
            keyword,
            page,
            size,
            sortBy,
            sortDirection
        );
    }
}
