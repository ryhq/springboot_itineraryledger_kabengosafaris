package com.itineraryledger.kabengosafaris.ParkTariffRate;

import com.itineraryledger.kabengosafaris.ParkTariffRate.DTOs.BulkUpsertParkRateDTO;
import com.itineraryledger.kabengosafaris.ParkTariffRate.DTOs.UpdateParkTariffRateDTO;
import com.itineraryledger.kabengosafaris.ParkTariffRate.Services.ParkTariffRateDeleteService;
import com.itineraryledger.kabengosafaris.ParkTariffRate.Services.ParkTariffRateGetService;
import com.itineraryledger.kabengosafaris.ParkTariffRate.Services.ParkTariffRateMatrixService;
import com.itineraryledger.kabengosafaris.ParkTariffRate.Services.ParkTariffRateUpsertService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ParkTariffRateController - REST controller for managing park tariff rates
 *
 * Provides endpoints for:
 * - CRUD operations on rates
 * - Bulk upsert operations
 * - Rate lookups with filtering
 * - Rate matrix for UI grid rendering
 *
 * Key Features:
 * - Park tariff rates are always park-specific (no global rates)
 * - Handles PER_PERSON tariffs (with age category) and non-PER_PERSON (without)
 * - Validates that tariffs can only have rates if chargingBasis is set
 */
import org.springframework.web.bind.annotation.PatchMapping;

@RestController
@RequestMapping("/api/park-tariff-rates")
@RequiredArgsConstructor
@Slf4j
public class ParkTariffRateController {

    private final ParkTariffRateGetService getService;
    private final ParkTariffRateDeleteService deleteService;
    private final ParkTariffRateUpsertService upsertService;
    private final ParkTariffRateMatrixService matrixService;

    /**
     * Get rate by ID
     */
    @GetMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_READ_PARK_TARIFF_RATE')")
    public ResponseEntity<ApiResponse<?>> getRateById(
        @PathVariable String idObfuscated,
        // the list's filters and sort, so prev/next stays inside the set on screen
        @RequestParam(required = false) String parkId,
        @RequestParam(required = false) String tariffId,
        @RequestParam(required = false) String seasonId,
        @RequestParam(required = false) String nationCategoryId,
        @RequestParam(required = false) String ageCategoryId,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) String sortBy
    ) {
        log.info("GET /api/park-tariff-rates/{} - Fetching rate", idObfuscated);
        return getService.getRateById(
            idObfuscated, parkId, tariffId, seasonId, nationCategoryId, ageCategoryId, isActive, sortBy
        );
    }

    /**
     * Get all rates with filtering and pagination
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_PARK_TARIFF_RATE')")
    public ResponseEntity<ApiResponse<?>> getAllRates(
        @RequestParam(required = false) String parkId,
        @RequestParam(required = false) String tariffId,
        @RequestParam(required = false) String seasonId,
        @RequestParam(required = false) String nationCategoryId,
        @RequestParam(required = false) String ageCategoryId,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) Boolean includeStats,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/park-tariff-rates - Fetching all rates with filters");
        return getService.getAllRates(parkId, tariffId, seasonId, nationCategoryId, ageCategoryId, isActive, includeStats, page, size, sortBy, sortDirection);
    }

    /**
     * Update an existing rate
     */
    @PutMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_PARK_TARIFF_RATE')")
    public ResponseEntity<ApiResponse<?>> updateRate(
        @PathVariable String idObfuscated,
        @Valid @RequestBody UpdateParkTariffRateDTO updateDTO
    ) {
        log.info("PUT /api/park-tariff-rates/{} - Updating rate", idObfuscated);
        return getService.updateRate(idObfuscated, updateDTO);
    }

    /**
     * Delete rates by IDs
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_PARK_TARIFF_RATE')")
    public ResponseEntity<ApiResponse<?>> deleteRates(
        @RequestBody List<String> ids
    ) {
        log.info("DELETE /api/park-tariff-rates - Deleting {} rates", ids.size());
        return deleteService.deleteRates(ids);
    }

    /**
     * Bulk upsert park tariff rates
     */
    @PostMapping("/bulk-upsert")
    @PreAuthorize("hasAuthority('PERM_UPDATE_PARK_TARIFF_RATE')")
    public ResponseEntity<ApiResponse<?>> bulkUpsertRates(
        @Valid @RequestBody List<BulkUpsertParkRateDTO> requests
    ) {
        log.info("POST /api/park-tariff-rates/bulk-upsert - Processing {} rates", requests.size());
        return upsertService.bulkUpsertRates(requests);
    }

    /**
     * Get rate matrix for a park-tariff combination
     *
     * Returns data needed to render a rate input grid/matrix:
     * - Park and Tariff information
     * - List of active global seasons (with exclusion support)
     * - List of active nation categories (with exclusion support)
     * - List of active age categories (only for PER_PERSON tariffs)
     * - Existing rates filtered by the same exclusions
     *
     * @param tariffId Required - The tariff to get rates for
     * @param parkId Optional - The park to get rates for (auto-selects first available if not provided)
     * @param excludeSeasonIds Optional - Season IDs to exclude from the matrix
     * @param excludeNationCategoryIds Optional - Nation category IDs to exclude
     * @param excludeAgeCategoryIds Optional - Age category IDs to exclude (only for PER_PERSON)
     */
    @GetMapping("/matrix")
    @PreAuthorize("hasAuthority('PERM_READ_PARK_TARIFF_RATE_MATRIX')")
    public ResponseEntity<ApiResponse<?>> getRateMatrix(
        @RequestParam String tariffId,
        @RequestParam(required = false) String parkId,
        @RequestParam(required = false) List<String> excludeSeasonIds,
        @RequestParam(required = false) List<String> excludeNationCategoryIds,
        @RequestParam(required = false) List<String> excludeAgeCategoryIds
    ) {
        log.info("GET /api/park-tariff-rates/matrix - Fetching rate matrix for tariff: {}, park: {}", tariffId, parkId);
        return matrixService.getRateMatrix(tariffId, parkId, excludeSeasonIds, excludeNationCategoryIds, excludeAgeCategoryIds);
    }

    // shared bulk-flag endpoint (see Response/BulkFlags)
    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Response.BulkFlags bulkFlags;

    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.ParkTariffRate.Repositories.ParkTariffRateRepository bulkFlagsRepository;

    /** PATCH /bulk — activate or withdraw a whole selection in one request. */
    @PatchMapping("/bulk")
    @PreAuthorize("hasAuthority('PERM_UPDATE_PARK_TARIFF_RATE')")
    public ResponseEntity<?> bulkFlags(
        @RequestBody com.itineraryledger.kabengosafaris.Response.BulkFlags.Request request
    ) {
        return bulkFlags.apply("park tariff rate", bulkFlagsRepository, request, entity -> {
            if (request.getIsActive() != null) entity.setIsActive(request.getIsActive());
        });
    }
}
