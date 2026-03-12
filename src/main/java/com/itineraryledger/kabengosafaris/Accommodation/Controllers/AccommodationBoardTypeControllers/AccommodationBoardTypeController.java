package com.itineraryledger.kabengosafaris.Accommodation.Controllers.AccommodationBoardTypeControllers;

import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationBoardTypeDTOs.CreateAccommodationBoardTypeDTO;
import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationBoardTypeDTOs.UpdateAccommodationBoardTypeDTO;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationBoardTypeServices.AccommodationBoardTypeGetService;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationBoardTypeServices.CreateAccommodationBoardTypeService;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationBoardTypeServices.DeleteAccommodationBoardTypeService;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationBoardTypeServices.UpdateAccommodationBoardTypeService;
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
 * AccommodationBoardTypeController - REST controller for managing accommodation board types
 *
 * Provides endpoints for CRUD operations on accommodation board types with permission-based access control
 */
@RestController
@RequestMapping("/api/accommodation-board-types")
@Slf4j
public class AccommodationBoardTypeController {

    private final CreateAccommodationBoardTypeService createAccommodationBoardTypeService;
    private final UpdateAccommodationBoardTypeService updateAccommodationBoardTypeService;
    private final DeleteAccommodationBoardTypeService deleteAccommodationBoardTypeService;
    private final AccommodationBoardTypeGetService accommodationBoardTypeGetService;

    @Autowired
    public AccommodationBoardTypeController(
        CreateAccommodationBoardTypeService createAccommodationBoardTypeService,
        UpdateAccommodationBoardTypeService updateAccommodationBoardTypeService,
        DeleteAccommodationBoardTypeService deleteAccommodationBoardTypeService,
        AccommodationBoardTypeGetService accommodationBoardTypeGetService
    ) {
        this.createAccommodationBoardTypeService = createAccommodationBoardTypeService;
        this.updateAccommodationBoardTypeService = updateAccommodationBoardTypeService;
        this.deleteAccommodationBoardTypeService = deleteAccommodationBoardTypeService;
        this.accommodationBoardTypeGetService = accommodationBoardTypeGetService;
    }

    /**
     * Create a new accommodation board type
     *
     * @param createAccommodationBoardTypeDTO The board type data
     * @return ResponseEntity with ApiResponse containing the created board type
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_ACCOMMODATION_BOARD_TYPE')")
    public ResponseEntity<ApiResponse<?>> createAccommodationBoardType(
        @Valid @RequestBody CreateAccommodationBoardTypeDTO createAccommodationBoardTypeDTO
    ) {
        log.info("POST /api/accommodation-board-types - Creating new board type: {}", createAccommodationBoardTypeDTO.getName());
        return createAccommodationBoardTypeService.createAccommodationBoardType(createAccommodationBoardTypeDTO);
    }

    /**
     * Update an existing accommodation board type
     *
     * @param idObfuscated The obfuscated board type ID
     * @param updateAccommodationBoardTypeDTO The updated board type data
     * @return ResponseEntity with ApiResponse containing the updated board type
     */
    @PutMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ACCOMMODATION_BOARD_TYPE')")
    public ResponseEntity<ApiResponse<?>> updateAccommodationBoardType(
        @PathVariable String idObfuscated,
        @Valid @RequestBody UpdateAccommodationBoardTypeDTO updateAccommodationBoardTypeDTO
    ) {
        log.info("PUT /api/accommodation-board-types/{} - Updating board type", idObfuscated);
        return updateAccommodationBoardTypeService.updateAccommodationBoardType(idObfuscated, updateAccommodationBoardTypeDTO);
    }

    /**
     * Delete accommodation board types by list of IDs
     *
     * @param idObfuscatedList List of obfuscated board type IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_ACCOMMODATION_BOARD_TYPE')")
    public ResponseEntity<ApiResponse<?>> deleteAccommodationBoardTypes(
        @RequestBody List<String> idObfuscatedList
    ) {
        log.info("DELETE /api/accommodation-board-types - Deleting {} board types", idObfuscatedList.size());
        return deleteAccommodationBoardTypeService.deleteAccommodationBoardTypes(idObfuscatedList);
    }

    /**
     * Get a single accommodation board type by ID
     *
     * @param idObfuscated The obfuscated board type ID
     * @return ResponseEntity with ApiResponse containing the board type
     */
    @GetMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_READ_ACCOMMODATION_BOARD_TYPE')")
    public ResponseEntity<ApiResponse<?>> getAccommodationBoardTypeById(
        @PathVariable String idObfuscated,
        @RequestParam(required = false) String scopeParentId
    ) {
        log.info("GET /api/accommodation-board-types/{} - Fetching board type by ID", idObfuscated);
        return accommodationBoardTypeGetService.getAccommodationBoardTypeById(idObfuscated, scopeParentId);
    }

    /**
     * Get all accommodation board types with optional filters
     * Accommodation ID is optional
     *
     * @param accommodationId Optional accommodation ID filter
     * @param name Filter by name (partial match)
     * @param breakfastIncluded Filter by breakfast included
     * @param lunchIncluded Filter by lunch included
     * @param dinnerIncluded Filter by dinner included
     * @param drinksIncluded Filter by drinks included
     * @param alcoholicDrinksIncluded Filter by alcoholic drinks included
     * @param isActive Filter by active status
     * @param hasFullMealPlan Filter board types with full meal plan
     * @param keyword Search keyword
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortDirection Sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing paginated board types
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_ACCOMMODATION_BOARD_TYPE')")
    public ResponseEntity<ApiResponse<?>> getAllAccommodationBoardTypes(
        @RequestParam(required = false) String accommodationId,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) Boolean breakfastIncluded,
        @RequestParam(required = false) Boolean lunchIncluded,
        @RequestParam(required = false) Boolean dinnerIncluded,
        @RequestParam(required = false) Boolean drinksIncluded,
        @RequestParam(required = false) Boolean alcoholicDrinksIncluded,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) Boolean hasFullMealPlan,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/accommodation-board-types - Fetching all board types with filters");
        String sortField = (sortBy != null && !sortBy.isBlank()) ? sortBy : "createdAt";
        Sort sort = sortDirection.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return accommodationBoardTypeGetService.getAllAccommodationBoardTypes(
            accommodationId,
            name,
            breakfastIncluded,
            lunchIncluded,
            dinnerIncluded,
            drinksIncluded,
            alcoholicDrinksIncluded,
            isActive,
            hasFullMealPlan,
            keyword,
            sortBy,
            sortDirection,
            pageable
        );
    }

    /**
     * Get all board types for a specific accommodation
     * Accommodation ID is required as path parameter
     *
     * @param accommodationId Required accommodation ID
     * @param name Filter by name (partial match)
     * @param breakfastIncluded Filter by breakfast included
     * @param lunchIncluded Filter by lunch included
     * @param dinnerIncluded Filter by dinner included
     * @param drinksIncluded Filter by drinks included
     * @param alcoholicDrinksIncluded Filter by alcoholic drinks included
     * @param isActive Filter by active status
     * @param hasFullMealPlan Filter board types with full meal plan
     * @param keyword Search keyword
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortDirection Sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing paginated board types
     */
    @GetMapping("/accommodation/{accommodationId}")
    @PreAuthorize("hasAuthority('PERM_READ_ACCOMMODATION_BOARD_TYPE')")
    public ResponseEntity<ApiResponse<?>> getAllAccommodationsBoardTypes(
        @PathVariable String accommodationId,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) Boolean breakfastIncluded,
        @RequestParam(required = false) Boolean lunchIncluded,
        @RequestParam(required = false) Boolean dinnerIncluded,
        @RequestParam(required = false) Boolean drinksIncluded,
        @RequestParam(required = false) Boolean alcoholicDrinksIncluded,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) Boolean hasFullMealPlan,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/accommodation-board-types/accommodation/{} - Fetching board types for accommodation", accommodationId);
        String sortField = (sortBy != null && !sortBy.isBlank()) ? sortBy : "createdAt";
        Sort sort = sortDirection.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return accommodationBoardTypeGetService.getAllAccommodationsBoardTypes(
            accommodationId,
            name,
            breakfastIncluded,
            lunchIncluded,
            dinnerIncluded,
            drinksIncluded,
            alcoholicDrinksIncluded,
            isActive,
            hasFullMealPlan,
            keyword,
            sortBy,
            sortDirection,
            pageable
        );
    }

    /**
     * Get unique board types based on meal configuration
     * Returns one board type per unique meal configuration, sorted by name
     * Useful for dropdowns where users select existing board type configurations
     *
     * @return ResponseEntity with ApiResponse containing list of unique board types
     */
    @GetMapping("/unique")
    @PreAuthorize("hasAuthority('PERM_READ_ACCOMMODATION_BOARD_TYPE')")
    public ResponseEntity<ApiResponse<?>> getUniqueBoardTypes() {
        log.info("GET /api/accommodation-board-types/unique - Fetching unique board types");
        return accommodationBoardTypeGetService.getUniqueBoardTypes();
    }
}
