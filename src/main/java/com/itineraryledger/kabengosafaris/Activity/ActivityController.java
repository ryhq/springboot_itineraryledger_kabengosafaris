package com.itineraryledger.kabengosafaris.Activity;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Activity.DTOs.CreateActivityDTO;
import com.itineraryledger.kabengosafaris.Activity.DTOs.UpdateActivityDTO;
import com.itineraryledger.kabengosafaris.Activity.Services.ActivityCreateService;
import com.itineraryledger.kabengosafaris.Activity.Services.ActivityDeleteService;
import com.itineraryledger.kabengosafaris.Activity.Services.ActivityGetService;
import com.itineraryledger.kabengosafaris.Activity.Services.ActivityUpdateService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * ActivityController - REST controller for managing activities
 *
 * Provides endpoints for CRUD operations on activities with permission-based access control
 */
@RestController
@RequestMapping("/api/activities")
@Tag(name = "Activity Management", description = "APIs for managing tourism activities")
@Slf4j
public class ActivityController {

    private final ActivityCreateService activityCreateService;
    private final ActivityUpdateService activityUpdateService;
    private final ActivityDeleteService activityDeleteService;
    private final ActivityGetService activityGetService;

    @Autowired
    public ActivityController(
        ActivityCreateService activityCreateService,
        ActivityUpdateService activityUpdateService,
        ActivityDeleteService activityDeleteService,
        ActivityGetService activityGetService
    ) {
        this.activityCreateService = activityCreateService;
        this.activityUpdateService = activityUpdateService;
        this.activityDeleteService = activityDeleteService;
        this.activityGetService = activityGetService;
    }

    /**
     * Create a new activity
     *
     * @param createActivityDTO The activity data
     * @return ResponseEntity with ApiResponse containing the created activity
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> createActivity(
        @Valid @RequestBody CreateActivityDTO createActivityDTO
    ) {
        log.info("POST /api/activities - Creating new activity: {}", createActivityDTO.getName());
        return activityCreateService.createActivity(createActivityDTO);
    }

    /**
     * Update an existing activity
     *
     * @param idObfuscated The obfuscated activity ID
     * @param updateActivityDTO The updated activity data
     * @return ResponseEntity with ApiResponse containing the updated activity
     */
    @PutMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> updateActivity(
        @Parameter(description = "Obfuscated activity ID") @PathVariable String idObfuscated,
        @Valid @RequestBody UpdateActivityDTO updateActivityDTO
    ) {
        log.info("PUT /api/activities/{} - Updating activity", idObfuscated);
        return activityUpdateService.updateActivity(idObfuscated, updateActivityDTO);
    }

    /**
     * Delete activities by list of IDs
     *
     * @param idObfuscatedList List of obfuscated activity IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> deleteActivities(
        @Parameter(description = "List of obfuscated activity IDs") @RequestBody List<String> idObfuscatedList
    ) {
        log.info("DELETE /api/activities - Deleting {} activities", idObfuscatedList.size());
        return activityDeleteService.deleteActivities(idObfuscatedList);
    }

    /**
     * Get a single activity by ID
     *
     * @param idObfuscated The obfuscated activity ID
     * @return ResponseEntity with ApiResponse containing the activity
     */
    @GetMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_READ_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> getActivityById(
        @Parameter(description = "Obfuscated activity ID") @PathVariable String idObfuscated
    ) {
        log.info("GET /api/activities/{} - Fetching activity by ID", idObfuscated);
        return activityGetService.getActivityById(idObfuscated);
    }

    /**
     * Get a single activity by slug
     *
     * @param slug The activity slug
     * @return ResponseEntity with ApiResponse containing the activity
     */
    @GetMapping("/slug/{slug}")
    @PreAuthorize("hasAuthority('PERM_READ_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> getActivityBySlug(
        @Parameter(description = "Activity slug") @PathVariable String slug
    ) {
        log.info("GET /api/activities/slug/{} - Fetching activity by slug", slug);
        return activityGetService.getActivityBySlug(slug);
    }

    /**
     * Get all activities with pagination, sorting, and filtering
     *
     * @param name Filter by name (partial match)
     * @param slug Filter by slug (partial match)
     * @param hasTariff Filter by tariff status
     * @param isWebActive Filter by web active status
     * @param chargingBasis Filter by charging basis
     * @param isActive Filter by active status
     * @param isStandalone Filter by standalone status (true = not linked to any park, false = linked to at least one park)
     * @param keyword Search keyword across multiple fields
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortDirection Sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing paginated activities
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> getAllActivities(
        @Parameter(description = "Filter by name (partial match)") @RequestParam(required = false) String name,
        @Parameter(description = "Filter by slug (partial match)") @RequestParam(required = false) String slug,
        @Parameter(description = "Filter by tariff status") @RequestParam(required = false) Boolean hasTariff,
        @Parameter(description = "Filter by web active status") @RequestParam(required = false) Boolean isWebActive,
        @Parameter(description = "Filter by charging basis") @RequestParam(required = false) ChargingBasis chargingBasis,
        @Parameter(description = "Filter by active status") @RequestParam(required = false) Boolean isActive,
        @Parameter(description = "Filter by standalone status (true = not linked to any park)") @RequestParam(required = false) Boolean isStandalone,
        @Parameter(description = "Search keyword across multiple fields") @RequestParam(required = false) String keyword,
        @Parameter(description = "Charging bases (OR within the dimension)") @RequestParam(required = false) java.util.List<ChargingBasis> chargingBases,
        @Parameter(description = "Statuses: active, inactive") @RequestParam(required = false) java.util.List<String> statuses,
        @Parameter(description = "Website visibility: on-web, off-web") @RequestParam(required = false) java.util.List<String> visibilities,
        @Parameter(description = "Data-quality gaps: no-description, no-tariff, no-safety") @RequestParam(required = false) java.util.List<String> qualities,
        @Parameter(description = "Created on or after (ISO date-time)") @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime createdAfter,
        @Parameter(description = "Created on or before (ISO date-time)") @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime createdBefore,
        @Parameter(description = "Include dashboard stats (default true)") @RequestParam(required = false) Boolean includeStats,
        @Parameter(description = "Page number (0-indexed)") @RequestParam(required = false, defaultValue = "0") Integer page,
        @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "10") Integer size,
        @Parameter(description = "Sort by field") @RequestParam(required = false) String sortBy,
        @Parameter(description = "Sort direction (asc/desc)") @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/activities - Fetching all activities with filters");
        return activityGetService.getAllActivities(
            name,
            slug,
            hasTariff,
            isWebActive,
            chargingBasis,
            isActive,
            isStandalone,
            keyword,
            chargingBases,
            statuses,
            visibilities,
            qualities,
            createdAfter,
            createdBefore,
            includeStats,
            page,
            size,
            sortBy,
            sortDirection
        );
    }

    // shared bulk-flag endpoint (see Response/BulkFlags)
    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Response.BulkFlags bulkFlags;

    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Activity.ActivityRepository bulkFlagsRepository;

    /**
     * PATCH /bulk — one request for a whole selection.
     *
     * Only the flags present in the body apply, so the same endpoint serves
     * activate, deactivate, publish, unpublish. Returns per-id
     * outcomes rather than a bare 200 that hides what did not change.
     */
    @PatchMapping("/bulk")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ACTIVITY')")
    public ResponseEntity<?> bulkFlags(
        @RequestBody com.itineraryledger.kabengosafaris.Response.BulkFlags.Request request
    ) {
        return bulkFlags.apply("activity", bulkFlagsRepository, request, entity -> {
            if (request.getIsActive() != null) entity.setIsActive(request.getIsActive());
            if (request.getIsWebActive() != null) entity.setIsWebActive(request.getIsWebActive());
        });
    }
}
