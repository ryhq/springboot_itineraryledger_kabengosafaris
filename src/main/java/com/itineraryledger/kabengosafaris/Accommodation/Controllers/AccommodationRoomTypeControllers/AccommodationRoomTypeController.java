package com.itineraryledger.kabengosafaris.Accommodation.Controllers.AccommodationRoomTypeControllers;

import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationRoomTypeDTOs.CreateAccommodationRoomTypeDTO;
import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationRoomTypeDTOs.UpdateAccommodationRoomTypeDTO;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationRoomTypeServices.AccommodationRoomTypeGetService;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationRoomTypeServices.CreateAccommodationRoomTypeService;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationRoomTypeServices.DeleteAccommodationRoomTypeService;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationRoomTypeServices.UpdateAccommodationRoomTypeService;
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
 * AccommodationRoomTypeController - REST controller for managing accommodation room types
 *
 * Provides endpoints for CRUD operations on accommodation room types with permission-based access control
 */
@RestController
@RequestMapping("/api/accommodation-room-types")
@Slf4j
public class AccommodationRoomTypeController {

    private final CreateAccommodationRoomTypeService createAccommodationRoomTypeService;
    private final UpdateAccommodationRoomTypeService updateAccommodationRoomTypeService;
    private final DeleteAccommodationRoomTypeService deleteAccommodationRoomTypeService;
    private final AccommodationRoomTypeGetService accommodationRoomTypeGetService;

    @Autowired
    public AccommodationRoomTypeController(
        CreateAccommodationRoomTypeService createAccommodationRoomTypeService,
        UpdateAccommodationRoomTypeService updateAccommodationRoomTypeService,
        DeleteAccommodationRoomTypeService deleteAccommodationRoomTypeService,
        AccommodationRoomTypeGetService accommodationRoomTypeGetService
    ) {
        this.createAccommodationRoomTypeService = createAccommodationRoomTypeService;
        this.updateAccommodationRoomTypeService = updateAccommodationRoomTypeService;
        this.deleteAccommodationRoomTypeService = deleteAccommodationRoomTypeService;
        this.accommodationRoomTypeGetService = accommodationRoomTypeGetService;
    }

    /**
     * Create a new accommodation room type
     *
     * @param createAccommodationRoomTypeDTO The room type data
     * @return ResponseEntity with ApiResponse containing the created room type
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_ACCOMMODATION_ROOM_TYPE')")
    public ResponseEntity<ApiResponse<?>> createAccommodationRoomType(
        @Valid @RequestBody CreateAccommodationRoomTypeDTO createAccommodationRoomTypeDTO
    ) {
        log.info("POST /api/accommodation-room-types - Creating new room type: {}", createAccommodationRoomTypeDTO.getName());
        return createAccommodationRoomTypeService.createAccommodationRoomType(createAccommodationRoomTypeDTO);
    }

    /**
     * Update an existing accommodation room type
     *
     * @param idObfuscated The obfuscated room type ID
     * @param updateAccommodationRoomTypeDTO The updated room type data
     * @return ResponseEntity with ApiResponse containing the updated room type
     */
    @PutMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ACCOMMODATION_ROOM_TYPE')")
    public ResponseEntity<ApiResponse<?>> updateAccommodationRoomType(
        @PathVariable String idObfuscated,
        @Valid @RequestBody UpdateAccommodationRoomTypeDTO updateAccommodationRoomTypeDTO
    ) {
        log.info("PUT /api/accommodation-room-types/{} - Updating room type", idObfuscated);
        return updateAccommodationRoomTypeService.updateAccommodationRoomType(idObfuscated, updateAccommodationRoomTypeDTO);
    }

    /**
     * Delete accommodation room types by list of IDs
     *
     * @param idObfuscatedList List of obfuscated room type IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_ACCOMMODATION_ROOM_TYPE')")
    public ResponseEntity<ApiResponse<?>> deleteAccommodationRoomTypes(
        @RequestBody List<String> idObfuscatedList
    ) {
        log.info("DELETE /api/accommodation-room-types - Deleting {} room types", idObfuscatedList.size());
        return deleteAccommodationRoomTypeService.deleteAccommodationRoomTypes(idObfuscatedList);
    }

    /**
     * Get a single accommodation room type by ID
     *
     * @param idObfuscated The obfuscated room type ID
     * @return ResponseEntity with ApiResponse containing the room type
     */
    @GetMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_READ_ACCOMMODATION_ROOM_TYPE')")
    public ResponseEntity<ApiResponse<?>> getAccommodationRoomTypeById(
        @PathVariable String idObfuscated
    ) {
        log.info("GET /api/accommodation-room-types/{} - Fetching room type by ID", idObfuscated);
        return accommodationRoomTypeGetService.getAccommodationRoomTypeById(idObfuscated);
    }

    /**
     * Get all accommodation room types with optional filters
     * Accommodation ID is optional
     *
     * @param accommodationId Optional accommodation ID filter
     * @param name Filter by name (partial match)
     * @param minOccupancy Filter by minimum occupancy
     * @param maxOccupancy Filter by maximum occupancy
     * @param isActive Filter by active status
     * @param keyword Search keyword
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortDir Sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing paginated room types
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_ACCOMMODATION_ROOM_TYPE')")
    public ResponseEntity<ApiResponse<?>> getAllAccommodationRoomTypes(
        @RequestParam(required = false) String accommodationId,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) Integer minOccupancy,
        @RequestParam(required = false) Integer maxOccupancy,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size,
        @RequestParam(required = false, defaultValue = "desc") String sortDir
    ) {
        log.info("GET /api/accommodation-room-types - Fetching all room types with filters");
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by("createdAt").ascending() : Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return accommodationRoomTypeGetService.getAllAccommodationRoomTypes(
            accommodationId,
            name,
            minOccupancy,
            maxOccupancy,
            isActive,
            keyword,
            pageable
        );
    }

    /**
     * Get all room types for a specific accommodation
     * Accommodation ID is required as path parameter
     *
     * @param accommodationId Required accommodation ID
     * @param name Filter by name (partial match)
     * @param minOccupancy Filter by minimum occupancy
     * @param maxOccupancy Filter by maximum occupancy
     * @param isActive Filter by active status
     * @param keyword Search keyword
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortDir Sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing paginated room types
     */
    @GetMapping("/accommodation/{accommodationId}")
    @PreAuthorize("hasAuthority('PERM_READ_ACCOMMODATION_ROOM_TYPE')")
    public ResponseEntity<ApiResponse<?>> getAllAccommodationsRoomTypes(
        @PathVariable String accommodationId,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) Integer minOccupancy,
        @RequestParam(required = false) Integer maxOccupancy,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size,
        @RequestParam(required = false, defaultValue = "desc") String sortDir
    ) {
        log.info("GET /api/accommodation-room-types/accommodation/{} - Fetching room types for accommodation", accommodationId);
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by("createdAt").ascending() : Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return accommodationRoomTypeGetService.getAllAccommodationsRoomTypes(
            accommodationId,
            name,
            minOccupancy,
            maxOccupancy,
            isActive,
            keyword,
            pageable
        );
    }
}
