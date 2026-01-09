package com.itineraryledger.kabengosafaris.Season.Controllers.SeasonPeriodControllers;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Season.DTOs.SeasonPeriodDTOs.CreateSeasonPeriodDTO;
import com.itineraryledger.kabengosafaris.Season.DTOs.SeasonPeriodDTOs.UpdateSeasonPeriodDTO;
import com.itineraryledger.kabengosafaris.Season.Services.SeasonPeriodServices.CreateSeasonPeriodService;
import com.itineraryledger.kabengosafaris.Season.Services.SeasonPeriodServices.DeleteSeasonPeriodService;
import com.itineraryledger.kabengosafaris.Season.Services.SeasonPeriodServices.GetSeasonPeriodService;
import com.itineraryledger.kabengosafaris.Season.Services.SeasonPeriodServices.UpdateSeasonPeriodService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SeasonPeriodController - REST controller for managing season periods
 *
 * Provides endpoints for CRUD operations on season periods with permission-based access control
 * Season periods define specific date ranges for seasons
 */
@RestController
@RequestMapping("/api/season-periods")
@Slf4j
public class SeasonPeriodController {

    private final CreateSeasonPeriodService createSeasonPeriodService;
    private final UpdateSeasonPeriodService updateSeasonPeriodService;
    private final DeleteSeasonPeriodService deleteSeasonPeriodService;
    private final GetSeasonPeriodService getSeasonPeriodService;

    @Autowired
    public SeasonPeriodController(
        CreateSeasonPeriodService createSeasonPeriodService,
        UpdateSeasonPeriodService updateSeasonPeriodService,
        DeleteSeasonPeriodService deleteSeasonPeriodService,
        GetSeasonPeriodService getSeasonPeriodService
    ) {
        this.createSeasonPeriodService = createSeasonPeriodService;
        this.updateSeasonPeriodService = updateSeasonPeriodService;
        this.deleteSeasonPeriodService = deleteSeasonPeriodService;
        this.getSeasonPeriodService = getSeasonPeriodService;
    }

    /**
     * Create a new season period
     *
     * @param createSeasonPeriodDTO The season period data
     * @return ResponseEntity with ApiResponse containing the created season period
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_SEASON_PERIOD')")
    public ResponseEntity<ApiResponse<?>> createSeasonPeriod(
        @Valid @RequestBody CreateSeasonPeriodDTO createSeasonPeriodDTO
    ) {
        log.info("POST /api/season-periods - Creating new season period for season: {}", createSeasonPeriodDTO.getSeasonId());
        return createSeasonPeriodService.createSeasonPeriod(createSeasonPeriodDTO);
    }

    /**
     * Update an existing season period
     *
     * @param id The obfuscated season period ID
     * @param updateSeasonPeriodDTO The updated season period data
     * @return ResponseEntity with ApiResponse containing the updated season period
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_SEASON_PERIOD')")
    public ResponseEntity<ApiResponse<?>> updateSeasonPeriod(
        @PathVariable String id,
        @Valid @RequestBody UpdateSeasonPeriodDTO updateSeasonPeriodDTO
    ) {
        log.info("PUT /api/season-periods/{} - Updating season period", id);
        return updateSeasonPeriodService.updateSeasonPeriod(id, updateSeasonPeriodDTO);
    }

    /**
     * Get a single season period by ID
     *
     * @param id The obfuscated season period ID
     * @return ResponseEntity with ApiResponse containing the season period
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_SEASON_PERIOD')")
    public ResponseEntity<ApiResponse<?>> getSeasonPeriodById(
        @PathVariable String id
    ) {
        log.info("GET /api/season-periods/{} - Fetching season period by ID", id);
        return getSeasonPeriodService.getSeasonPeriodById(id);
    }

    /**
     * Get all season periods with pagination, sorting, and filtering
     *
     * @param seasonId Filter by season ID (obfuscated)
     * @param isActive Filter by active status
     * @param isSystem Filter by system status (true = system/protected, false = user-created)
     * @param year Filter by specific year
     * @param isRecurring Filter recurring periods (null year)
     * @param notes Filter by notes (partial match)
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortDirection Sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing paginated season periods
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_SEASON_PERIOD')")
    public ResponseEntity<ApiResponse<?>> getAllSeasonPeriods(
        @RequestParam(required = false) String seasonId,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) Boolean isSystem,
        @RequestParam(required = false) Integer year,
        @RequestParam(required = false) Boolean isRecurring,
        @RequestParam(required = false) String notes,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size,
        @RequestParam(required = false, defaultValue = "asc") String sortDirection
    ) {
        log.info("GET /api/season-periods - Fetching all season periods with filters");
        return getSeasonPeriodService.getAllSeasonPeriods(
            seasonId,
            isActive,
            isSystem,
            year,
            isRecurring,
            null, // startDate - not commonly filtered by users
            null, // endDate - not commonly filtered by users
            notes,
            page,
            size,
            sortDirection
        );
    }

    /**
     * Delete season periods by list of IDs
     *
     * @param ids List of obfuscated season period IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_SEASON_PERIOD')")
    public ResponseEntity<ApiResponse<?>> deleteSeasonPeriods(
        @RequestBody List<String> ids
    ) {
        log.info("DELETE /api/season-periods - Deleting {} season periods", ids.size());
        return deleteSeasonPeriodService.deleteSeasonPeriods(ids);
    }
}
