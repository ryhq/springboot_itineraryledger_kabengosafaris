package com.itineraryledger.kabengosafaris.ActivityTariffRate;

import com.itineraryledger.kabengosafaris.ActivityTariffRate.DTOs.BulkUpsertActivityRateDTO;
import com.itineraryledger.kabengosafaris.ActivityTariffRate.DTOs.UpdateActivityTariffRateDTO;
import com.itineraryledger.kabengosafaris.ActivityTariffRate.Services.ActivityTariffRateDeleteService;
import com.itineraryledger.kabengosafaris.ActivityTariffRate.Services.ActivityTariffRateGetService;
import com.itineraryledger.kabengosafaris.ActivityTariffRate.Services.ActivityTariffRateMatrixService;
import com.itineraryledger.kabengosafaris.ActivityTariffRate.Services.ActivityTariffRateUpsertService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ActivityTariffRateController - REST controller for managing activity tariff rates
 *
 * Provides endpoints for:
 * - CRUD operations on activity rates
 * - Bulk upsert operations
 * - Rate lookups with filtering
 *
 * Key Features:
 * - Supports global rates (no park) and park-specific rates
 * - Handles PER_PERSON activities (with age category) and non-PER_PERSON (without)
 * - Validates that activities can only have rates if hasTariff=true AND chargingBasis is not null
 */
import org.springframework.web.bind.annotation.PatchMapping;

@RestController
@RequestMapping("/api/activity-tariff-rates")
@RequiredArgsConstructor
@Slf4j
public class ActivityTariffRateController {

    private final ActivityTariffRateGetService getService;
    private final ActivityTariffRateDeleteService deleteService;
    private final ActivityTariffRateUpsertService upsertService;
    private final ActivityTariffRateMatrixService matrixService;

    /**
     * Get rate by ID
     */
    @GetMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_READ_ACTIVITY_TARIFF_RATE')")
    public ResponseEntity<ApiResponse<?>> getRateById(
        @PathVariable String idObfuscated,
        // the list's filters and sort, so prev/next stays inside the set on screen
        @RequestParam(required = false) String activityId,
        @RequestParam(required = false) String parkId,
        @RequestParam(required = false) String seasonId,
        @RequestParam(required = false) String nationCategoryId,
        @RequestParam(required = false) String ageCategoryId,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) Boolean globalOnly,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String sortBy
    ) {
        log.info("GET /api/activity-tariff-rates/{} - Fetching rate", idObfuscated);
        return getService.getRateById(
            idObfuscated, activityId, parkId, seasonId, nationCategoryId, ageCategoryId, isActive, globalOnly, keyword, sortBy
        );
    }

    /**
     * Get all rates with filtering and pagination
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_ACTIVITY_TARIFF_RATE')")
    public ResponseEntity<ApiResponse<?>> getAllRates(
        @RequestParam(required = false) String activityId,
        @RequestParam(required = false) String parkId,
        @RequestParam(required = false) Boolean globalOnly,
        @RequestParam(required = false) String seasonId,
        @RequestParam(required = false) String nationCategoryId,
        @RequestParam(required = false) String ageCategoryId,
        @RequestParam(required = false) Boolean isActive,
        // the list's search box: the module had no keyword param, so it did nothing
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Boolean includeStats,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/activity-tariff-rates - Fetching all rates with filters");
        return getService.getAllRates(activityId, parkId, globalOnly, seasonId, nationCategoryId, ageCategoryId, isActive, keyword, includeStats, page, size, sortBy, sortDirection);
    }

    /**
     * Update an existing rate
     */
    @PutMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ACTIVITY_TARIFF_RATE')")
    public ResponseEntity<ApiResponse<?>> updateRate(
        @PathVariable String idObfuscated,
        @Valid @RequestBody UpdateActivityTariffRateDTO updateDTO
    ) {
        log.info("PUT /api/activity-tariff-rates/{} - Updating rate", idObfuscated);
        return getService.updateRate(idObfuscated, updateDTO);
    }

    /**
     * Delete rates by IDs
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_ACTIVITY_TARIFF_RATE')")
    public ResponseEntity<ApiResponse<?>> deleteRates(
        @RequestBody List<String> ids
    ) {
        log.info("DELETE /api/activity-tariff-rates - Deleting {} rates", ids.size());
        return deleteService.deleteRates(ids);
    }

    /**
     * Bulk upsert activity rates
     */
    @PostMapping("/bulk-upsert")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ACTIVITY_TARIFF_RATE')")
    public ResponseEntity<ApiResponse<?>> bulkUpsertRates(
        @Valid @RequestBody List<BulkUpsertActivityRateDTO> requests
    ) {
        log.info("POST /api/activity-tariff-rates/bulk-upsert - Processing {} rates", requests.size());
        return upsertService.bulkUpsertRates(requests);
    }

    /**
     * Get rate matrix for an activity
     *
     * Returns data needed to render a rate input grid/matrix:
     * - List of active global seasons (with exclusion support)
     * - List of active nation categories (with exclusion support)
     * - List of active age categories (only for PER_PERSON activities)
     * - Existing rates filtered by the same exclusions
     *
     * @param activityId Required - The activity to get rates for
     * @param parkId Optional - The park to get rates for (null for global rates)
     * @param excludeSeasonIds Optional - Season IDs to exclude from the matrix
     * @param excludeNationCategoryIds Optional - Nation category IDs to exclude
     * @param excludeAgeCategoryIds Optional - Age category IDs to exclude (only for PER_PERSON)
     */
    @GetMapping("/matrix")
    @PreAuthorize("hasAuthority('PERM_READ_ACTIVITY_TARIFF_RATE_MATRIX')")
    public ResponseEntity<ApiResponse<?>> getRateMatrix(
        @RequestParam String activityId,
        @RequestParam(required = false) String parkId,
        @RequestParam(required = false) List<String> excludeSeasonIds,
        @RequestParam(required = false) List<String> excludeNationCategoryIds,
        @RequestParam(required = false) List<String> excludeAgeCategoryIds
    ) {
        log.info("GET /api/activity-tariff-rates/matrix - Fetching rate matrix for activity: {}", activityId);
        return matrixService.getRateMatrix(activityId, parkId, excludeSeasonIds, excludeNationCategoryIds, excludeAgeCategoryIds);
    }

    // shared bulk-flag endpoint (see Response/BulkFlags)
    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Response.BulkFlags bulkFlags;

    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.ActivityTariffRate.Repositories.ActivityTariffRateRepository bulkFlagsRepository;

    /** PATCH /bulk — activate or withdraw a whole selection in one request. */
    @PatchMapping("/bulk")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ACTIVITY_TARIFF_RATE')")
    public ResponseEntity<?> bulkFlags(
        @RequestBody com.itineraryledger.kabengosafaris.Response.BulkFlags.Request request
    ) {
        return bulkFlags.apply("activity tariff rate", bulkFlagsRepository, request, entity -> {
            if (request.getIsActive() != null) entity.setIsActive(request.getIsActive());
        });
    }
}
