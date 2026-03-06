package com.itineraryledger.kabengosafaris.Park;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Park.DTOs.CreateParkDTO;
import com.itineraryledger.kabengosafaris.Park.DTOs.UpdateParkDTO;
import com.itineraryledger.kabengosafaris.Park.Services.ParkCreateService;
import com.itineraryledger.kabengosafaris.Park.Services.ParkDeleteService;
import com.itineraryledger.kabengosafaris.Park.Services.ParkGetService;
import com.itineraryledger.kabengosafaris.Park.Services.ParkUpdateService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * ParkController - REST controller for managing parks
 *
 * Provides endpoints for CRUD operations on parks with permission-based access control
 */
@RestController
@RequestMapping("/api/parks")
@Tag(name = "Park Management", description = "APIs for managing Tanzania National Parks and Reserves")
@Slf4j
public class ParkController {

    private final ParkCreateService parkCreateService;
    private final ParkUpdateService parkUpdateService;
    private final ParkDeleteService parkDeleteService;
    private final ParkGetService parkGetService;

    @Autowired
    public ParkController(
        ParkCreateService parkCreateService,
        ParkUpdateService parkUpdateService,
        ParkDeleteService parkDeleteService,
        ParkGetService parkGetService
    ) {
        this.parkCreateService = parkCreateService;
        this.parkUpdateService = parkUpdateService;
        this.parkDeleteService = parkDeleteService;
        this.parkGetService = parkGetService;
    }

    /**
     * Create a new park
     *
     * @param createParkDTO The park data
     * @return ResponseEntity with ApiResponse containing the created park
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_PARK')")
    public ResponseEntity<ApiResponse<?>> createPark(
        @Valid @RequestBody CreateParkDTO createParkDTO
    ) {
        log.info("POST /api/parks - Creating new park: {}", createParkDTO.getName());
        return parkCreateService.createPark(createParkDTO);
    }

    /**
     * Update an existing park
     *
     * @param idObfuscated The obfuscated park ID
     * @param updateParkDTO The updated park data
     * @return ResponseEntity with ApiResponse containing the updated park
     */
    @PutMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_PARK')")
    public ResponseEntity<ApiResponse<?>> updatePark(
        @Parameter(description = "Obfuscated park ID") @PathVariable String idObfuscated,
        @Valid @RequestBody UpdateParkDTO updateParkDTO
    ) {
        log.info("PUT /api/parks/{} - Updating park", idObfuscated);
        return parkUpdateService.updatePark(idObfuscated, updateParkDTO);
    }

    /**
     * Delete parks by list of IDs
     *
     * @param idObfuscatedList List of obfuscated park IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_PARK')")
    public ResponseEntity<ApiResponse<?>> deleteParks(
        @Parameter(description = "List of obfuscated park IDs") @RequestBody List<String> idObfuscatedList
    ) {
        log.info("DELETE /api/parks - Deleting {} parks", idObfuscatedList.size());
        return parkDeleteService.deleteParks(idObfuscatedList);
    }

    /**
     * Get a single park by ID
     *
     * @param idObfuscated The obfuscated park ID
     * @return ResponseEntity with ApiResponse containing the park
     */
    @GetMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_READ_PARK')")
    public ResponseEntity<ApiResponse<?>> getParkById(
        @Parameter(description = "Obfuscated park ID") @PathVariable String idObfuscated
    ) {
        log.info("GET /api/parks/{} - Fetching park by ID", idObfuscated);
        return parkGetService.getParkById(idObfuscated);
    }

    /**
     * Get a single park by slug
     *
     * @param slug The park slug
     * @return ResponseEntity with ApiResponse containing the park
     */
    @GetMapping("/slug/{slug}")
    @PreAuthorize("hasAuthority('PERM_READ_PARK')")
    public ResponseEntity<ApiResponse<?>> getParkBySlug(
        @Parameter(description = "Park slug") @PathVariable String slug
    ) {
        log.info("GET /api/parks/slug/{} - Fetching park by slug", slug);
        return parkGetService.getParkBySlug(slug);
    }

    /**
     * Get all parks with pagination, sorting, and filtering
     *
     * @param name Filter by name (partial match)
     * @param slug Filter by slug (partial match)
     * @param parkType Filter by park type
     * @param region Filter by region (partial match)
     * @param district Filter by district (partial match)
     * @param location Filter by location (partial match)
     * @param isActive Filter by active status
     * @param keyword Search keyword across multiple fields
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortDirection Sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing paginated parks
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_PARK')")
    public ResponseEntity<ApiResponse<?>> getAllParks(
        @Parameter(description = "Filter by name (partial match)") @RequestParam(required = false) String name,
        @Parameter(description = "Filter by slug (partial match)") @RequestParam(required = false) String slug,
        @Parameter(description = "Filter by park type") @RequestParam(required = false) ParkType parkType,
        @Parameter(description = "Filter by region (partial match)") @RequestParam(required = false) String region,
        @Parameter(description = "Filter by district (partial match)") @RequestParam(required = false) String district,
        @Parameter(description = "Filter by location (partial match)") @RequestParam(required = false) String location,
        @Parameter(description = "Filter by park size (partial match)") @RequestParam(required = false) String parkSize,
        @Parameter(description = "Filter by active status") @RequestParam(required = false) Boolean isActive,
        @Parameter(description = "Search keyword across multiple fields") @RequestParam(required = false) String keyword,
        @Parameter(description = "Page number (0-indexed)") @RequestParam(required = false, defaultValue = "0") Integer page,
        @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "10") Integer size,
        @Parameter(description = "Sort by field") @RequestParam(required = false) String sortBy,
        @Parameter(description = "Sort direction (asc/desc)") @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/parks - Fetching all parks with filters");
        return parkGetService.getAllParks(
            name,
            slug,
            parkType,
            region,
            district,
            location,
            parkSize,
            isActive,
            keyword,
            page,
            size,
            sortBy,
            sortDirection
        );
    }
}
