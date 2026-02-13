package com.itineraryledger.kabengosafaris.Hero.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Hero.DTOs.CreateHeroDTO;
import com.itineraryledger.kabengosafaris.Hero.DTOs.ReorderHeroDTO;
import com.itineraryledger.kabengosafaris.Hero.DTOs.UpdateHeroDTO;
import com.itineraryledger.kabengosafaris.Hero.Enums.HeroPage;
import com.itineraryledger.kabengosafaris.Hero.Services.HeroServices.HeroCreateService;
import com.itineraryledger.kabengosafaris.Hero.Services.HeroServices.HeroDeleteService;
import com.itineraryledger.kabengosafaris.Hero.Services.HeroServices.HeroGetService;
import com.itineraryledger.kabengosafaris.Hero.Services.HeroServices.HeroReorderService;
import com.itineraryledger.kabengosafaris.Hero.Services.HeroServices.HeroUpdateService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * HeroController - REST controller for managing website hero sections
 *
 * Provides endpoints for CRUD operations on heroes with permission-based access control
 */
@RestController
@RequestMapping("/api/heroes")
@Tag(name = "Hero Management", description = "APIs for managing website hero sections")
@Slf4j
public class HeroController {

    private final HeroCreateService heroCreateService;
    private final HeroUpdateService heroUpdateService;
    private final HeroDeleteService heroDeleteService;
    private final HeroGetService heroGetService;
    private final HeroReorderService heroReorderService;

    @Autowired
    public HeroController(
        HeroCreateService heroCreateService,
        HeroUpdateService heroUpdateService,
        HeroDeleteService heroDeleteService,
        HeroGetService heroGetService,
        HeroReorderService heroReorderService
    ) {
        this.heroCreateService = heroCreateService;
        this.heroUpdateService = heroUpdateService;
        this.heroDeleteService = heroDeleteService;
        this.heroGetService = heroGetService;
        this.heroReorderService = heroReorderService;
    }

    /**
     * Create a new hero section
     *
     * @param createHeroDTO The hero data
     * @return ResponseEntity with ApiResponse containing the created hero
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_HERO')")
    public ResponseEntity<ApiResponse<?>> createHero(
        @Valid @RequestBody CreateHeroDTO createHeroDTO
    ) {
        log.info("POST /api/heroes - Creating new hero: {}", createHeroDTO.getTitle());
        return heroCreateService.createHero(createHeroDTO);
    }

    /**
     * Update an existing hero section
     *
     * @param idObfuscated The obfuscated hero ID
     * @param updateHeroDTO The updated hero data
     * @return ResponseEntity with ApiResponse containing the updated hero
     */
    @PutMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_HERO')")
    public ResponseEntity<ApiResponse<?>> updateHero(
        @Parameter(description = "Obfuscated hero ID") @PathVariable String idObfuscated,
        @Valid @RequestBody UpdateHeroDTO updateHeroDTO
    ) {
        log.info("PUT /api/heroes/{} - Updating hero", idObfuscated);
        return heroUpdateService.updateHero(idObfuscated, updateHeroDTO);
    }

    /**
     * Delete heroes by list of IDs
     *
     * @param idObfuscatedList List of obfuscated hero IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_HERO')")
    public ResponseEntity<ApiResponse<?>> deleteHeroes(
        @Parameter(description = "List of obfuscated hero IDs") @RequestBody List<String> idObfuscatedList
    ) {
        log.info("DELETE /api/heroes - Deleting {} heroes", idObfuscatedList.size());
        return heroDeleteService.deleteHeroes(idObfuscatedList);
    }

    /**
     * Get a single hero by ID
     *
     * @param idObfuscated The obfuscated hero ID
     * @return ResponseEntity with ApiResponse containing the hero
     */
    @GetMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_READ_HERO')")
    public ResponseEntity<ApiResponse<?>> getHeroById(
        @Parameter(description = "Obfuscated hero ID") @PathVariable String idObfuscated
    ) {
        log.info("GET /api/heroes/{} - Fetching hero by ID", idObfuscated);
        return heroGetService.getHeroById(idObfuscated);
    }

    /**
     * Get heroes for a specific page (for front-end display)
     *
     * @param page The page to get heroes for
     * @return ResponseEntity with ApiResponse containing the heroes
     */
    @GetMapping("/page/{page}")
    public ResponseEntity<ApiResponse<?>> getHeroesByPage(
        @Parameter(description = "Page name") @PathVariable HeroPage page
    ) {
        log.info("GET /api/heroes/page/{} - Fetching heroes for page", page);
        return heroGetService.getHeroesByPage(page);
    }

    /**
     * Get all heroes with pagination, sorting, and filtering
     *
     * @param title Filter by title (partial match)
     * @param page Filter by page
     * @param isActive Filter by active status
     * @param textAlignment Filter by text alignment
     * @param createdById Filter by creator (obfuscated ID)
     * @param updatedById Filter by updater (obfuscated ID)
     * @param pageNumber Page number (0-indexed)
     * @param pageSize Page size
     * @param sortBy Field to sort by
     * @param sortDirection Sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing paginated heroes
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_HERO')")
    public ResponseEntity<ApiResponse<?>> getAllHeroes(
        @Parameter(description = "Filter by title (partial match)") @RequestParam(required = false) String title,
        @Parameter(description = "Filter by page") @RequestParam(required = false) HeroPage page,
        @Parameter(description = "Filter by active status") @RequestParam(required = false) Boolean isActive,
        @Parameter(description = "Filter by text alignment") @RequestParam(required = false) String textAlignment,
        @Parameter(description = "Filter by creator ID") @RequestParam(required = false) String createdById,
        @Parameter(description = "Filter by updater ID") @RequestParam(required = false) String updatedById,
        @Parameter(description = "Page number (0-indexed)") @RequestParam(required = false, defaultValue = "0") Integer pageNumber,
        @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "20") Integer pageSize,
        @Parameter(description = "Sort by field") @RequestParam(required = false, defaultValue = "displayOrder") String sortBy,
        @Parameter(description = "Sort direction (asc/desc)") @RequestParam(required = false, defaultValue = "asc") String sortDirection
    ) {
        log.info("GET /api/heroes - Fetching all heroes with filters");
        return heroGetService.listHeroes(
            pageNumber,
            pageSize,
            sortBy,
            sortDirection,
            title,
            page,
            isActive,
            textAlignment,
            createdById,
            updatedById
        );
    }

    /**
     * Reorder heroes within a page (drag & drop support)
     *
     * @param reorderDTO The reorder data containing page and new order
     * @return ResponseEntity with ApiResponse containing the reordered heroes
     */
    @PostMapping("/reorder")
    @PreAuthorize("hasAuthority('PERM_UPDATE_HERO')")
    public ResponseEntity<ApiResponse<?>> reorderHeroes(
        @Valid @RequestBody ReorderHeroDTO reorderDTO
    ) {
        log.info("POST /api/heroes/reorder - Reordering heroes for page: {}", reorderDTO.getPage());
        return heroReorderService.reorderHeroes(reorderDTO);
    }
}
