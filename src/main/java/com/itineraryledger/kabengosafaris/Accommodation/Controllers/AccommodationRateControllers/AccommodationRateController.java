package com.itineraryledger.kabengosafaris.Accommodation.Controllers.AccommodationRateControllers;

import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationRateDTOs.BulkUpsertAccommodationRateDTO;
import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationRateDTOs.UpdateAccommodationRateDTO;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationRateServices.AccommodationRateDeleteService;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationRateServices.AccommodationRateGetService;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationRateServices.AccommodationRateMatrixService;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationRateServices.AccommodationRateUpsertService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AccommodationRateController - REST controller for managing accommodation rates
 *
 * Provides endpoints for:
 * - CRUD operations on rates
 * - Bulk upsert operations
 * - Rate lookups with filtering
 * - Rate matrix for UI grid rendering
 *
 * Key Features:
 * - Accommodation rates are determined by the combination of:
 *   Accommodation + Season + RoomType + RoomStandard + BoardType
 * - All four dimensions (Season, RoomType, RoomStandard, BoardType) are accommodation-specific
 * - Season, RoomType, RoomStandard, and BoardType must belong to the same accommodation
 */
@RestController
@RequestMapping("/api/accommodation-rates")
@RequiredArgsConstructor
@Slf4j
public class AccommodationRateController {

    private final AccommodationRateGetService getService;
    private final AccommodationRateDeleteService deleteService;
    private final AccommodationRateUpsertService upsertService;
    private final AccommodationRateMatrixService matrixService;

    /**
     * Get rate by ID
     */
    @GetMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_READ_ACCOMMODATION_RATE')")
    public ResponseEntity<ApiResponse<?>> getRateById(
        @PathVariable String idObfuscated,
        @RequestParam(required = false) String scopeParentId
    ) {
        log.info("GET /api/accommodation-rates/{} - Fetching rate", idObfuscated);
        return getService.getRateById(idObfuscated, scopeParentId);
    }

    /**
     * Get all rates with filtering and pagination
     *
     * @param isPerPerson Filter by rate charging model: true = Per Person Sharing (PPS), false = Per Room
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_ACCOMMODATION_RATE')")
    public ResponseEntity<ApiResponse<?>> getAllRates(
        @RequestParam(required = false) String accommodationId,
        @RequestParam(required = false) String seasonId,
        @RequestParam(required = false) String roomTypeId,
        @RequestParam(required = false) String roomStandardId,
        @RequestParam(required = false) String boardTypeId,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) Boolean isPerPerson,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/accommodation-rates - Fetching all rates with filters");
        return getService.getAllRates(accommodationId, seasonId, roomTypeId, roomStandardId, boardTypeId, isActive, isPerPerson, page, size, sortBy, sortDirection);
    }

    /**
     * Update an existing rate
     */
    @PutMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ACCOMMODATION_RATE')")
    public ResponseEntity<ApiResponse<?>> updateRate(
        @PathVariable String idObfuscated,
        @Valid @RequestBody UpdateAccommodationRateDTO updateDTO
    ) {
        log.info("PUT /api/accommodation-rates/{} - Updating rate", idObfuscated);
        return getService.updateRate(idObfuscated, updateDTO);
    }

    /**
     * Delete rates by IDs
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_ACCOMMODATION_RATE')")
    public ResponseEntity<ApiResponse<?>> deleteRates(
        @RequestBody List<String> ids
    ) {
        log.info("DELETE /api/accommodation-rates - Deleting {} rates", ids.size());
        return deleteService.deleteRates(ids);
    }

    /**
     * Bulk upsert accommodation rates
     */
    @PostMapping("/bulk-upsert")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ACCOMMODATION_RATE')")
    public ResponseEntity<ApiResponse<?>> bulkUpsertRates(
        @Valid @RequestBody List<BulkUpsertAccommodationRateDTO> requests
    ) {
        log.info("POST /api/accommodation-rates/bulk-upsert - Processing {} rates", requests.size());
        return upsertService.bulkUpsertRates(requests);
    }

    /**
     * Get rate matrix for an accommodation
     *
     * Returns data needed to render a rate input grid/matrix:
     * - Accommodation information
     * - List of accommodation-specific seasons (with exclusion support)
     * - List of room types (with exclusion support)
     * - List of room standards (with exclusion support)
     * - List of board types (with exclusion support)
     * - Existing rates filtered by the same exclusions
     *
     * Matrix structure:
     * Season x RoomStandard x RoomType x BoardType | STO Rate | Rack Rate | Currency
     *
     * Or filtered by season:
     * RoomStandard x RoomType x BoardType | STO Rate | Rack Rate | Currency
     *
     * @param accommodationId Required - The accommodation to get rates for
     * @param seasonId Optional - Filter by specific season
     * @param excludeSeasonIds Optional - Season IDs to exclude from the matrix
     * @param excludeRoomTypeIds Optional - Room type IDs to exclude
     * @param excludeRoomStandardIds Optional - Room standard IDs to exclude
     * @param excludeBoardTypeIds Optional - Board type IDs to exclude
     */
    @GetMapping("/matrix")
    @PreAuthorize("hasAuthority('PERM_READ_ACCOMMODATION_RATE_MATRIX')")
    public ResponseEntity<ApiResponse<?>> getRateMatrix(
        @RequestParam String accommodationId,
        @RequestParam(required = false) String seasonId,
        @RequestParam(required = false) List<String> excludeSeasonIds,
        @RequestParam(required = false) List<String> excludeRoomTypeIds,
        @RequestParam(required = false) List<String> excludeRoomStandardIds,
        @RequestParam(required = false) List<String> excludeBoardTypeIds
    ) {
        log.info("GET /api/accommodation-rates/matrix - Fetching rate matrix for accommodation: {}", accommodationId);
        return matrixService.getRateMatrix(accommodationId, seasonId, excludeSeasonIds, excludeRoomTypeIds, excludeRoomStandardIds, excludeBoardTypeIds);
    }
}
