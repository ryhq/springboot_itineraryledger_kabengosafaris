package com.itineraryledger.kabengosafaris.Accommodation.Controllers.AccommodationRoomStandardControllers;

import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationRoomStandardDTOs.CreateAccommodationRoomStandardDTO;
import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationRoomStandardDTOs.UpdateAccommodationRoomStandardDTO;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationRoomStandardServices.AccommodationRoomStandardGetService;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationRoomStandardServices.CreateAccommodationRoomStandardService;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationRoomStandardServices.DeleteAccommodationRoomStandardService;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationRoomStandardServices.UpdateAccommodationRoomStandardService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AccommodationRoomStandardController - REST controller for managing accommodation room standards
 *
 * Provides endpoints for CRUD operations on accommodation room standards with permission-based access control
 */
@RestController
@RequestMapping("/api/accommodation-room-standards")
@Slf4j
public class AccommodationRoomStandardController {

    private final CreateAccommodationRoomStandardService createAccommodationRoomStandardService;
    private final UpdateAccommodationRoomStandardService updateAccommodationRoomStandardService;
    private final DeleteAccommodationRoomStandardService deleteAccommodationRoomStandardService;
    private final AccommodationRoomStandardGetService accommodationRoomStandardGetService;

    @Autowired
    public AccommodationRoomStandardController(
        CreateAccommodationRoomStandardService createAccommodationRoomStandardService,
        UpdateAccommodationRoomStandardService updateAccommodationRoomStandardService,
        DeleteAccommodationRoomStandardService deleteAccommodationRoomStandardService,
        AccommodationRoomStandardGetService accommodationRoomStandardGetService
    ) {
        this.createAccommodationRoomStandardService = createAccommodationRoomStandardService;
        this.updateAccommodationRoomStandardService = updateAccommodationRoomStandardService;
        this.deleteAccommodationRoomStandardService = deleteAccommodationRoomStandardService;
        this.accommodationRoomStandardGetService = accommodationRoomStandardGetService;
    }

    /**
     * Create a new accommodation room standard
     *
     * @param createAccommodationRoomStandardDTO The room standard data
     * @return ResponseEntity with ApiResponse containing the created room standard
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_ACCOMMODATION_ROOM_STANDARD')")
    public ResponseEntity<ApiResponse<?>> createAccommodationRoomStandard(
        @Valid @RequestBody CreateAccommodationRoomStandardDTO createAccommodationRoomStandardDTO
    ) {
        log.info("POST /api/accommodation-room-standards - Creating new room standard: {}", createAccommodationRoomStandardDTO.getName());
        return createAccommodationRoomStandardService.createAccommodationRoomStandard(createAccommodationRoomStandardDTO);
    }

    /**
     * Update an existing accommodation room standard
     *
     * @param idObfuscated The obfuscated room standard ID
     * @param updateAccommodationRoomStandardDTO The updated room standard data
     * @return ResponseEntity with ApiResponse containing the updated room standard
     */
    @PutMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ACCOMMODATION_ROOM_STANDARD')")
    public ResponseEntity<ApiResponse<?>> updateAccommodationRoomStandard(
        @PathVariable String idObfuscated,
        @Valid @RequestBody UpdateAccommodationRoomStandardDTO updateAccommodationRoomStandardDTO
    ) {
        log.info("PUT /api/accommodation-room-standards/{} - Updating room standard", idObfuscated);
        return updateAccommodationRoomStandardService.updateAccommodationRoomStandard(idObfuscated, updateAccommodationRoomStandardDTO);
    }

    /**
     * Delete accommodation room standards by list of IDs
     *
     * @param idObfuscatedList List of obfuscated room standard IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_ACCOMMODATION_ROOM_STANDARD')")
    public ResponseEntity<ApiResponse<?>> deleteAccommodationRoomStandards(
        @RequestBody List<String> idObfuscatedList
    ) {
        log.info("DELETE /api/accommodation-room-standards - Deleting {} room standards", idObfuscatedList.size());
        return deleteAccommodationRoomStandardService.deleteAccommodationRoomStandards(idObfuscatedList);
    }

    /**
     * Get a single accommodation room standard by ID
     *
     * @param idObfuscated The obfuscated room standard ID
     * @return ResponseEntity with ApiResponse containing the room standard
     */
    @GetMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_READ_ACCOMMODATION_ROOM_STANDARD')")
    public ResponseEntity<ApiResponse<?>> getAccommodationRoomStandardById(
        @PathVariable String idObfuscated
    ) {
        log.info("GET /api/accommodation-room-standards/{} - Fetching room standard by ID", idObfuscated);
        return accommodationRoomStandardGetService.getAccommodationRoomStandardById(idObfuscated);
    }

    /**
     * Get all accommodation room standards with optional filters
     * Accommodation ID is optional
     *
     * @param accommodationId Optional accommodation ID filter
     * @param name Filter by name (partial match)
     * @param viewType Filter by view type (partial match)
     * @param floorLevel Filter by floor level (partial match)
     * @param minOccupancy Filter by minimum occupancy
     * @param maxOccupancy Filter by maximum occupancy
     * @param isActive Filter by active status
     * @param keyword Search keyword
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortDir Sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing paginated room standards
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_ACCOMMODATION_ROOM_STANDARD')")
    public ResponseEntity<ApiResponse<?>> getAllAccommodationRoomStandards(
        @RequestParam(required = false) String accommodationId,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String viewType,
        @RequestParam(required = false) String floorLevel,
        @RequestParam(required = false) Integer minOccupancy,
        @RequestParam(required = false) Integer maxOccupancy,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size,
        @RequestParam(required = false, defaultValue = "desc") String sortDir
    ) {
        log.info("GET /api/accommodation-room-standards - Fetching all room standards with filters");
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by("createdAt").ascending() : Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return accommodationRoomStandardGetService.getAllAccommodationRoomStandards(
            accommodationId,
            name,
            viewType,
            floorLevel,
            minOccupancy,
            maxOccupancy,
            isActive,
            keyword,
            pageable
        );
    }

    /**
     * Get all room standards for a specific accommodation
     * Accommodation ID is required as path parameter
     *
     * @param accommodationId Required accommodation ID
     * @param name Filter by name (partial match)
     * @param viewType Filter by view type (partial match)
     * @param floorLevel Filter by floor level (partial match)
     * @param minOccupancy Filter by minimum occupancy
     * @param maxOccupancy Filter by maximum occupancy
     * @param isActive Filter by active status
     * @param keyword Search keyword
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortDir Sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing paginated room standards
     */
    @GetMapping("/accommodation/{accommodationId}")
    @PreAuthorize("hasAuthority('PERM_READ_ACCOMMODATION_ROOM_STANDARD')")
    public ResponseEntity<ApiResponse<?>> getAllAccommodationsRoomStandards(
        @PathVariable String accommodationId,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String viewType,
        @RequestParam(required = false) String floorLevel,
        @RequestParam(required = false) Integer minOccupancy,
        @RequestParam(required = false) Integer maxOccupancy,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size,
        @RequestParam(required = false, defaultValue = "desc") String sortDir
    ) {
        log.info("GET /api/accommodation-room-standards/accommodation/{} - Fetching room standards for accommodation", accommodationId);
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by("createdAt").ascending() : Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return accommodationRoomStandardGetService.getAllAccommodationsRoomStandards(
            accommodationId,
            name,
            viewType,
            floorLevel,
            minOccupancy,
            maxOccupancy,
            isActive,
            keyword,
            pageable
        );
    }

    /**
     * Get unique room standards based on name
     * Returns one room standard per unique name, sorted alphabetically
     * Useful for dropdowns where users select existing room standard names
     *
     * @return ResponseEntity with ApiResponse containing list of unique room standards
     */
    @GetMapping("/unique")
    @PreAuthorize("hasAuthority('PERM_READ_ACCOMMODATION_ROOM_STANDARD')")
    public ResponseEntity<ApiResponse<?>> getUniqueRoomStandards() {
        log.info("GET /api/accommodation-room-standards/unique - Fetching unique room standards");
        return accommodationRoomStandardGetService.getUniqueRoomStandards();
    }
}
