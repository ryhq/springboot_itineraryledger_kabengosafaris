package com.itineraryledger.kabengosafaris.Season.Controllers.SeasonControllers;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Season.DTOs.SeasonDTOs.CreateSeasonDTO;
import com.itineraryledger.kabengosafaris.Season.DTOs.SeasonDTOs.UpdateSeasonDTO;
import com.itineraryledger.kabengosafaris.Season.Season;
import com.itineraryledger.kabengosafaris.Season.Services.SeasonServices.CreateSeasonService;
import com.itineraryledger.kabengosafaris.Season.Services.SeasonServices.DeleteSeasonService;
import com.itineraryledger.kabengosafaris.Season.Services.SeasonServices.GetSeasonService;
import com.itineraryledger.kabengosafaris.Season.Services.SeasonServices.UpdateSeasonService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SeasonController - REST controller for managing seasons
 *
 * Provides endpoints for CRUD operations on seasons with permission-based access control
 * Handles both global seasons and accommodation-specific seasons
 */
@RestController
@RequestMapping("/api/seasons")
@Slf4j
public class SeasonController {

    private final CreateSeasonService createSeasonService;
    private final UpdateSeasonService updateSeasonService;
    private final DeleteSeasonService deleteSeasonService;
    private final GetSeasonService getSeasonService;

    @Autowired
    public SeasonController(
        CreateSeasonService createSeasonService,
        UpdateSeasonService updateSeasonService,
        DeleteSeasonService deleteSeasonService,
        GetSeasonService getSeasonService
    ) {
        this.createSeasonService = createSeasonService;
        this.updateSeasonService = updateSeasonService;
        this.deleteSeasonService = deleteSeasonService;
        this.getSeasonService = getSeasonService;
    }

    /**
     * Create a new season
     *
     * @param createSeasonDTO The season data
     * @return ResponseEntity with ApiResponse containing the created season
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_SEASON')")
    public ResponseEntity<ApiResponse<?>> createSeason(
        @Valid @RequestBody CreateSeasonDTO createSeasonDTO
    ) {
        log.info("POST /api/seasons - Creating new season: {}", createSeasonDTO.getName());
        return createSeasonService.createSeason(createSeasonDTO);
    }

    /**
     * Update an existing season
     *
     * @param id The obfuscated season ID
     * @param updateSeasonDTO The updated season data
     * @return ResponseEntity with ApiResponse containing the updated season
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_SEASON')")
    public ResponseEntity<ApiResponse<?>> updateSeason(
        @PathVariable String id,
        @Valid @RequestBody UpdateSeasonDTO updateSeasonDTO
    ) {
        log.info("PUT /api/seasons/{} - Updating season", id);
        return updateSeasonService.updateSeason(id, updateSeasonDTO);
    }

    /**
     * Get a single season by ID
     *
     * @param id The obfuscated season ID
     * @return ResponseEntity with ApiResponse containing the season
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_SEASON')")
    public ResponseEntity<ApiResponse<?>> getSeasonById(
        @PathVariable String id,
        // the list's whole filter set, so prev/next walks the SAME set and N of M counts it
        @RequestParam(required = false) String name,
        @RequestParam(required = false) Season.SeasonType seasonType,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) Boolean isGlobal,
        @RequestParam(required = false) Boolean isSystem,
        @RequestParam(required = false) String accommodationId,
        @RequestParam(required = false) String description,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection
    ) {
        log.info("GET /api/seasons/{} - Fetching season by ID", id);
        return getSeasonService.getSeasonById(
            id, name, seasonType, isActive, isGlobal, isSystem, accommodationId, description, keyword,
            sortBy, sortDirection
        );
    }

    /**
     * Get all seasons with pagination, sorting, and filtering
     *
     * @param name Filter by name (partial match)
     * @param seasonType Filter by season type
     * @param isActive Filter by active status
     * @param isGlobal Filter by global status (true = global, false = accommodation-specific)
     * @param isSystem Filter by system status (true = system/protected, false = user-created)
     * @param accommodationId Filter by accommodation ID (for accommodation-specific seasons)
     * @param description Filter by description (partial match)
     * @param keyword Search keyword across name and description
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortDirection Sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing paginated seasons
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_SEASON')")
    public ResponseEntity<ApiResponse<?>> getAllSeasons(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) Season.SeasonType seasonType,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) Boolean isGlobal,
        @RequestParam(required = false) Boolean isSystem,
        @RequestParam(required = false) String accommodationId,
        @RequestParam(required = false) String description,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Boolean includeStats,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/seasons - Fetching all seasons with filters");
        return getSeasonService.getAllSeasons(
            name,
            seasonType,
            isActive,
            isGlobal,
            isSystem,
            accommodationId,
            description,
            keyword,
            includeStats,
            page,
            size,
            sortBy,
            sortDirection
        );
    }

    /**
     * Delete seasons by list of IDs
     * All associated season periods will be deleted as well
     *
     * @param ids List of obfuscated season IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_SEASON')")
    public ResponseEntity<ApiResponse<?>> deleteSeasons(
        @RequestBody List<String> ids
    ) {
        log.info("DELETE /api/seasons - Deleting {} seasons", ids.size());
        return deleteSeasonService.deleteSeasons(ids);
    }

    /**
     * Get unique seasons based on season type
     * Returns one season per unique season type, sorted by name
     * Useful for dropdowns where users select existing season type configurations
     *
     * @return ResponseEntity with ApiResponse containing list of unique seasons
     */
    @GetMapping("/unique")
    @PreAuthorize("hasAuthority('PERM_READ_SEASON')")
    public ResponseEntity<ApiResponse<?>> getUniqueSeasons() {
        log.info("GET /api/seasons/unique - Fetching unique seasons");
        return getSeasonService.getUniqueSeasons();
    }
    // shared bulk-flag endpoint (see Response/BulkFlags)
    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Response.BulkFlags bulkFlags;

    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Season.Repositories.SeasonRepository bulkFlagsRepository;

    /**
     * PATCH /bulk — one request for a whole selection.
     *
     * Activating fifty rows one request at a time is slow and can leave the set
     * half-changed; this applies only the flags present in the body and reports
     * per-id outcomes, so the UI can say what did not change and why.
     */
    @PatchMapping("/bulk")
    @PreAuthorize("hasAuthority('PERM_UPDATE_SEASON')")
    public ResponseEntity<?> bulkFlags(
        @RequestBody com.itineraryledger.kabengosafaris.Response.BulkFlags.Request request
    ) {
        return bulkFlags.apply("season", bulkFlagsRepository, request, entity -> {
            if (request.getIsActive() != null) entity.setIsActive(request.getIsActive());
        });
    }
}
