package com.itineraryledger.kabengosafaris.PaxAgeCategory.Controllers;

import com.itineraryledger.kabengosafaris.PaxAgeCategory.DTOs.CreatePaxAgeCategoryDTO;
import com.itineraryledger.kabengosafaris.PaxAgeCategory.DTOs.UpdatePaxAgeCategoryDTO;
import com.itineraryledger.kabengosafaris.PaxAgeCategory.PaxAgeCategory;
import com.itineraryledger.kabengosafaris.PaxAgeCategory.Services.CreatePaxAgeCategoryService;
import com.itineraryledger.kabengosafaris.PaxAgeCategory.Services.DeletePaxAgeCategoryService;
import com.itineraryledger.kabengosafaris.PaxAgeCategory.Services.GetPaxAgeCategoryService;
import com.itineraryledger.kabengosafaris.PaxAgeCategory.Services.UpdatePaxAgeCategoryService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * PaxAgeCategoryController - REST controller for managing pax age categories
 *
 * Provides endpoints for CRUD operations on pax age categories with permission-based access control
 * Age categories define passenger age ranges (e.g., Child 0-5, Youth 6-14, Adult 15+)
 */
@RestController
@RequestMapping("/api/pax-age-categories")
@Slf4j
public class PaxAgeCategoryController {

    private final CreatePaxAgeCategoryService createPaxAgeCategoryService;
    private final UpdatePaxAgeCategoryService updatePaxAgeCategoryService;
    private final DeletePaxAgeCategoryService deletePaxAgeCategoryService;
    private final GetPaxAgeCategoryService getPaxAgeCategoryService;

    @Autowired
    public PaxAgeCategoryController(
        CreatePaxAgeCategoryService createPaxAgeCategoryService,
        UpdatePaxAgeCategoryService updatePaxAgeCategoryService,
        DeletePaxAgeCategoryService deletePaxAgeCategoryService,
        GetPaxAgeCategoryService getPaxAgeCategoryService
    ) {
        this.createPaxAgeCategoryService = createPaxAgeCategoryService;
        this.updatePaxAgeCategoryService = updatePaxAgeCategoryService;
        this.deletePaxAgeCategoryService = deletePaxAgeCategoryService;
        this.getPaxAgeCategoryService = getPaxAgeCategoryService;
    }

    /**
     * Create a new pax age category
     *
     * @param createDTO The pax age category data
     * @return ResponseEntity with ApiResponse containing the created category
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_PAX_AGE_CATEGORY')")
    public ResponseEntity<ApiResponse<?>> createPaxAgeCategory(
        @Valid @RequestBody CreatePaxAgeCategoryDTO createDTO
    ) {
        log.info("POST /api/pax-age-categories - Creating new pax age category: {}", createDTO.getName());
        return createPaxAgeCategoryService.createPaxAgeCategory(createDTO);
    }

    /**
     * Update an existing pax age category
     *
     * @param id The obfuscated category ID
     * @param updateDTO The updated category data
     * @return ResponseEntity with ApiResponse containing the updated category
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_PAX_AGE_CATEGORY')")
    public ResponseEntity<ApiResponse<?>> updatePaxAgeCategory(
        @PathVariable String id,
        @Valid @RequestBody UpdatePaxAgeCategoryDTO updateDTO
    ) {
        log.info("PUT /api/pax-age-categories/{} - Updating pax age category", id);
        return updatePaxAgeCategoryService.updatePaxAgeCategory(id, updateDTO);
    }

    /**
     * Get a single pax age category by ID
     *
     * @param id The obfuscated category ID
     * @return ResponseEntity with ApiResponse containing the category
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_PAX_AGE_CATEGORY')")
    public ResponseEntity<ApiResponse<?>> getPaxAgeCategoryById(
        @PathVariable String id
    ) {
        log.info("GET /api/pax-age-categories/{} - Fetching pax age category by ID", id);
        return getPaxAgeCategoryService.getPaxAgeCategoryById(id);
    }

    /**
     * Get all pax age categories with pagination, sorting, and filtering
     *
     * @param name Filter by name (partial match)
     * @param categoryType Filter by category type (CHILD, YOUTH, ADULT, CUSTOM)
     * @param isActive Filter by active status
     * @param isSystem Filter by system status (true = system/protected, false = user-created)
     * @param minAge Filter by minimum age
     * @param maxAge Filter by maximum age
     * @param age Filter categories that include this specific age
     * @param keyword Search keyword across name and description
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortDirection Sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing paginated categories
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_PAX_AGE_CATEGORY')")
    public ResponseEntity<ApiResponse<?>> getAllPaxAgeCategories(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) PaxAgeCategory.CategoryType categoryType,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) Boolean isSystem,
        @RequestParam(required = false) Integer minAge,
        @RequestParam(required = false) Integer maxAge,
        @RequestParam(required = false) Integer age,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false, defaultValue = "asc") String sortDirection
    ) {
        log.info("GET /api/pax-age-categories - Fetching all pax age categories with filters");
        return getPaxAgeCategoryService.getAllPaxAgeCategories(
            name,
            categoryType,
            isActive,
            isSystem,
            minAge,
            maxAge,
            age,
            keyword,
            page,
            size,
            sortBy,
            sortDirection
        );
    }

    /**
     * Delete pax age categories by list of IDs
     * System categories cannot be deleted - only user-created categories can be deleted
     *
     * @param ids List of obfuscated category IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_PAX_AGE_CATEGORY')")
    public ResponseEntity<ApiResponse<?>> deletePaxAgeCategories(
        @RequestBody List<String> ids
    ) {
        log.info("DELETE /api/pax-age-categories - Deleting {} pax age categories", ids.size());
        return deletePaxAgeCategoryService.deletePaxAgeCategories(ids);
    }
}
