package com.itineraryledger.kabengosafaris.Accommodation.Controllers.AccommodationControllers;

import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationDTOs.CreateAccommodationDTO;
import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationDTOs.UpdateAccommodationDTO;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationCategory;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationType;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationServices.AccommodationGetService;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationServices.CreateAccommodationService;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationServices.DeleteAccommodationService;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationServices.UpdateAccommodationService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AccommodationController - REST controller for managing accommodations
 *
 * Provides endpoints for CRUD operations on accommodations with permission-based access control
 */
@RestController
@RequestMapping("/api/accommodations")
@Slf4j
public class AccommodationController {

    private final CreateAccommodationService createAccommodationService;
    private final UpdateAccommodationService updateAccommodationService;
    private final DeleteAccommodationService deleteAccommodationService;
    private final AccommodationGetService accommodationGetService;

    @Autowired
    public AccommodationController(
        CreateAccommodationService createAccommodationService,
        UpdateAccommodationService updateAccommodationService,
        DeleteAccommodationService deleteAccommodationService,
        AccommodationGetService accommodationGetService
    ) {
        this.createAccommodationService = createAccommodationService;
        this.updateAccommodationService = updateAccommodationService;
        this.deleteAccommodationService = deleteAccommodationService;
        this.accommodationGetService = accommodationGetService;
    }

    /**
     * Create a new accommodation
     *
     * @param createAccommodationDTO The accommodation data
     * @return ResponseEntity with ApiResponse containing the created accommodation
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_ACCOMMODATION')")
    public ResponseEntity<ApiResponse<?>> createAccommodation(
        @Valid @RequestBody CreateAccommodationDTO createAccommodationDTO
    ) {
        log.info("POST /api/accommodations - Creating new accommodation: {}", createAccommodationDTO.getName());
        return createAccommodationService.createAccommodation(createAccommodationDTO);
    }

    /**
     * Update an existing accommodation
     *
     * @param idObfuscated The obfuscated accommodation ID
     * @param updateAccommodationDTO The updated accommodation data
     * @return ResponseEntity with ApiResponse containing the updated accommodation
     */
    @PutMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ACCOMMODATION')")
    public ResponseEntity<ApiResponse<?>> updateAccommodation(
        @PathVariable String idObfuscated,
        @Valid @RequestBody UpdateAccommodationDTO updateAccommodationDTO
    ) {
        log.info("PUT /api/accommodations/{} - Updating accommodation", idObfuscated);
        return updateAccommodationService.updateAccommodation(idObfuscated, updateAccommodationDTO);
    }

    /**
     * Delete accommodations by list of IDs
     * If an accommodation has branches, all branches will be deleted as well
     *
     * @param idObfuscatedList List of obfuscated accommodation IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_ACCOMMODATION')")
    public ResponseEntity<ApiResponse<?>> deleteAccommodations(
        @RequestBody List<String> idObfuscatedList
    ) {
        log.info("DELETE /api/accommodations - Deleting {} accommodations", idObfuscatedList.size());
        return deleteAccommodationService.deleteAccommodations(idObfuscatedList);
    }

    /**
     * Get a single accommodation by ID
     *
     * @param idObfuscated The obfuscated accommodation ID
     * @return ResponseEntity with ApiResponse containing the accommodation
     */
    @GetMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_READ_ACCOMMODATION')")
    public ResponseEntity<ApiResponse<?>> getAccommodationById(
        @PathVariable String idObfuscated
    ) {
        log.info("GET /api/accommodations/{} - Fetching accommodation by ID", idObfuscated);
        return accommodationGetService.getAccommodationById(idObfuscated);
    }

    /**
     * Get a single accommodation by slug
     *
     * @param slug The accommodation slug
     * @return ResponseEntity with ApiResponse containing the accommodation
     */
    @GetMapping("/slug/{slug}")
    @PreAuthorize("hasAuthority('PERM_READ_ACCOMMODATION')")
    public ResponseEntity<ApiResponse<?>> getAccommodationBySlug(
        @PathVariable String slug
    ) {
        log.info("GET /api/accommodations/slug/{} - Fetching accommodation by slug", slug);
        return accommodationGetService.getAccommodationBySlug(slug);
    }

    /**
     * Get lightweight list of accommodations for dropdown/select purposes
     * Returns only essential fields: id, name, location, region, isActive
     * Useful for UI components when creating emails, phones, etc.
     *
     * @param hasBranch Optional filter for accommodations with branches
     * @return ResponseEntity with ApiResponse containing list of accommodations
     */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('PERM_READ_ACCOMMODATION')")
    public ResponseEntity<ApiResponse<?>> getAccommodationsList(
        @RequestParam(required = false) Boolean hasBranch
    ) {
        log.info("GET /api/accommodations/list - Fetching accommodations for dropdown (hasBranch: {})", hasBranch);
        return accommodationGetService.getAccommodationsList(hasBranch);
    }

    /**
     * Get all accommodations with pagination, sorting, and filtering
     *
     * @param name Filter by name (partial match)
     * @param slug Filter by slug (partial match)
     * @param accommodationType Filter by accommodation type
     * @param category Filter by category
     * @param region Filter by region (partial match)
     * @param exactRegion Filter by exact region
     * @param district Filter by district (partial match)
     * @param exactDistrict Filter by exact district
     * @param starRating Filter by exact star rating
     * @param minStarRating Filter by minimum star rating
     * @param maxStarRating Filter by maximum star rating
     * @param isActive Filter by active status
     * @param isHeadquarters Filter by headquarters status
     * @param hasBranch Filter by has branch status
     * @param parentId Filter by parent accommodation ID
     * @param hasNoParent Filter accommodations with no parent
     * @param minRooms Filter by minimum rooms
     * @param maxRooms Filter by maximum rooms
     * @param minGuests Filter by minimum guest capacity
     * @param maxGuests Filter by maximum guest capacity
     * @param tags Filter by tags (partial match)
     * @param amenities Filter by amenities (partial match)
     * @param hasImages Filter accommodations with images
     * @param hasRates Filter accommodations with rates
     * @param hasCoordinates Filter accommodations with GPS coordinates
     * @param keyword Search keyword across multiple fields
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortDirection Sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing paginated accommodations
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_ACCOMMODATION')")
    public ResponseEntity<ApiResponse<?>> getAllAccommodations(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String slug,
        @RequestParam(required = false) AccommodationType accommodationType,
        @RequestParam(required = false) AccommodationCategory category,
        @RequestParam(required = false) String region,
        @RequestParam(required = false) String exactRegion,
        @RequestParam(required = false) String district,
        @RequestParam(required = false) String exactDistrict,
        @RequestParam(required = false) Integer starRating,
        @RequestParam(required = false) Integer minStarRating,
        @RequestParam(required = false) Integer maxStarRating,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) Boolean isHeadquarters,
        @RequestParam(required = false) Boolean hasBranch,
        @RequestParam(required = false) String parentId,
        @RequestParam(required = false) Boolean hasNoParent,
        @RequestParam(required = false) Integer minRooms,
        @RequestParam(required = false) Integer maxRooms,
        @RequestParam(required = false) Integer minGuests,
        @RequestParam(required = false) Integer maxGuests,
        @RequestParam(required = false) String tags,
        @RequestParam(required = false) String amenities,
        @RequestParam(required = false) Boolean hasImages,
        @RequestParam(required = false) Boolean hasRates,
        @RequestParam(required = false) Boolean hasCoordinates,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/accommodations - Fetching all accommodations with filters");
        return accommodationGetService.getAllAccommodations(
            name,
            slug,
            accommodationType,
            category,
            region,
            exactRegion,
            district,
            exactDistrict,
            starRating,
            minStarRating,
            maxStarRating,
            isActive,
            isHeadquarters,
            hasBranch,
            parentId,
            hasNoParent,
            minRooms,
            maxRooms,
            minGuests,
            maxGuests,
            tags,
            amenities,
            hasImages,
            hasRates,
            hasCoordinates,
            keyword,
            page,
            size,
            sortDirection
        );
    }
}
