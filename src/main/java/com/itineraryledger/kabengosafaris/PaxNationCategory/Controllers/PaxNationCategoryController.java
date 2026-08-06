package com.itineraryledger.kabengosafaris.PaxNationCategory.Controllers;

import com.itineraryledger.kabengosafaris.PaxNationCategory.DTOs.CreatePaxNationCategoryDTO;
import com.itineraryledger.kabengosafaris.PaxNationCategory.DTOs.UpdatePaxNationCategoryDTO;
import com.itineraryledger.kabengosafaris.PaxNationCategory.PaxNationCategory;
import com.itineraryledger.kabengosafaris.PaxNationCategory.Services.CreatePaxNationCategoryService;
import com.itineraryledger.kabengosafaris.PaxNationCategory.Services.DeletePaxNationCategoryService;
import com.itineraryledger.kabengosafaris.PaxNationCategory.Services.GetPaxNationCategoryService;
import com.itineraryledger.kabengosafaris.PaxNationCategory.Services.UpdatePaxNationCategoryService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * PaxNationCategoryController - REST controller for managing pax nation categories
 *
 * Provides endpoints for CRUD operations on pax nation categories with permission-based access control
 * Nation categories define nationality-based pricing (e.g., Resident, Expatriate, East African, Non-Resident)
 */
@RestController
@RequestMapping("/api/pax-nation-categories")
@Slf4j
public class PaxNationCategoryController {

    private final CreatePaxNationCategoryService createPaxNationCategoryService;
    private final UpdatePaxNationCategoryService updatePaxNationCategoryService;
    private final DeletePaxNationCategoryService deletePaxNationCategoryService;
    private final GetPaxNationCategoryService getPaxNationCategoryService;

    @Autowired
    public PaxNationCategoryController(
        CreatePaxNationCategoryService createPaxNationCategoryService,
        UpdatePaxNationCategoryService updatePaxNationCategoryService,
        DeletePaxNationCategoryService deletePaxNationCategoryService,
        GetPaxNationCategoryService getPaxNationCategoryService
    ) {
        this.createPaxNationCategoryService = createPaxNationCategoryService;
        this.updatePaxNationCategoryService = updatePaxNationCategoryService;
        this.deletePaxNationCategoryService = deletePaxNationCategoryService;
        this.getPaxNationCategoryService = getPaxNationCategoryService;
    }

    /**
     * Create a new pax nation category
     *
     * @param createDTO The pax nation category data
     * @return ResponseEntity with ApiResponse containing the created category
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_PAX_NATION_CATEGORY')")
    public ResponseEntity<ApiResponse<?>> createPaxNationCategory(
        @Valid @RequestBody CreatePaxNationCategoryDTO createDTO
    ) {
        log.info("POST /api/pax-nation-categories - Creating new pax nation category: {}", createDTO.getName());
        return createPaxNationCategoryService.createPaxNationCategory(createDTO);
    }

    /**
     * Update an existing pax nation category
     *
     * @param id The obfuscated category ID
     * @param updateDTO The updated category data
     * @return ResponseEntity with ApiResponse containing the updated category
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_PAX_NATION_CATEGORY')")
    public ResponseEntity<ApiResponse<?>> updatePaxNationCategory(
        @PathVariable String id,
        @Valid @RequestBody UpdatePaxNationCategoryDTO updateDTO
    ) {
        log.info("PUT /api/pax-nation-categories/{} - Updating pax nation category", id);
        return updatePaxNationCategoryService.updatePaxNationCategory(id, updateDTO);
    }

    /**
     * Get a single pax nation category by ID
     *
     * @param id The obfuscated category ID
     * @return ResponseEntity with ApiResponse containing the category
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_PAX_NATION_CATEGORY')")
    public ResponseEntity<ApiResponse<?>> getPaxNationCategoryById(
        @PathVariable String id,
        // the list's filters and sort, so prev/next stays inside the set on screen
        @RequestParam(required = false) String name,
        @RequestParam(required = false) PaxNationCategory.CategoryType categoryType,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) Boolean isSystem,
        @RequestParam(required = false) Integer priorityFactor,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection
    ) {
        log.info("GET /api/pax-nation-categories/{} - Fetching pax nation category by ID", id);
        return getPaxNationCategoryService.getPaxNationCategoryById(id, name, categoryType, isActive, isSystem, priorityFactor, keyword, sortBy, sortDirection);
    }

    /**
     * Get all pax nation categories with pagination, sorting, and filtering
     *
     * @param name Filter by name (partial match)
     * @param categoryType Filter by category type (RESIDENT, EXPATRIATE, EAST_AFRICAN, NON_RESIDENT, CUSTOM)
     * @param isActive Filter by active status
     * @param isSystem Filter by system status (true = system/protected, false = user-created)
     * @param priorityFactor Filter by exact priority factor
     * @param keyword Search keyword across name and description
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortDirection Sort direction (asc/desc) by priority factor
     * @return ResponseEntity with ApiResponse containing paginated categories
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_PAX_NATION_CATEGORY')")
    public ResponseEntity<ApiResponse<?>> getAllPaxNationCategories(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) PaxNationCategory.CategoryType categoryType,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) Boolean isSystem,
        @RequestParam(required = false) Integer priorityFactor,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Boolean includeStats,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false, defaultValue = "asc") String sortDirection
    ) {
        log.info("GET /api/pax-nation-categories - Fetching all pax nation categories with filters");
        return getPaxNationCategoryService.getAllPaxNationCategories(
            name,
            categoryType,
            isActive,
            isSystem,
            priorityFactor,
            keyword,
            includeStats,
            page,
            size,
            sortBy,
            sortDirection
        );
    }

    /**
     * Delete pax nation categories by list of IDs
     * System categories cannot be deleted - only user-created categories can be deleted
     *
     * @param ids List of obfuscated category IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_PAX_NATION_CATEGORY')")
    public ResponseEntity<ApiResponse<?>> deletePaxNationCategories(
        @RequestBody List<String> ids
    ) {
        log.info("DELETE /api/pax-nation-categories - Deleting {} pax nation categories", ids.size());
        return deletePaxNationCategoryService.deletePaxNationCategories(ids);
    }

    // shared bulk-flag endpoint (see Response/BulkFlags)
    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Response.BulkFlags bulkFlags;

    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.PaxNationCategory.Repositories.PaxNationCategoryRepository bulkFlagsRepository;

    /** PATCH /bulk — activate or withdraw a whole selection in one request. */
    @PatchMapping("/bulk")
    @PreAuthorize("hasAuthority('PERM_UPDATE_PAX_NATION_CATEGORY')")
    public ResponseEntity<?> bulkFlags(
        @RequestBody com.itineraryledger.kabengosafaris.Response.BulkFlags.Request request
    ) {
        return bulkFlags.apply("pax nation category", bulkFlagsRepository, request, entity -> {
            if (request.getIsActive() != null) entity.setIsActive(request.getIsActive());
        });
    }
}
