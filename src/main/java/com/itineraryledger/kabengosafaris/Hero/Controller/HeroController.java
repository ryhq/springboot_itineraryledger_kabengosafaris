package com.itineraryledger.kabengosafaris.Hero.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Hero.DTOs.CreateHeroDTO;
import com.itineraryledger.kabengosafaris.Hero.DTOs.ReorderHeroDTO;
import com.itineraryledger.kabengosafaris.Hero.DTOs.UpdateHeroDTO;
import com.itineraryledger.kabengosafaris.Hero.Specifications.HeroFilter;
import com.itineraryledger.kabengosafaris.Hero.Services.HeroServices.HeroCreateService;
import com.itineraryledger.kabengosafaris.Hero.Services.HeroServices.HeroDeleteService;
import com.itineraryledger.kabengosafaris.Hero.Services.HeroServices.HeroGetService;
import com.itineraryledger.kabengosafaris.Hero.Services.HeroServices.HeroReorderService;
import com.itineraryledger.kabengosafaris.Hero.Services.HeroServices.HeroUpdateService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * HeroController - REST controller for managing website hero sections
 *
 * Provides endpoints for CRUD operations on heroes with permission-based access control
 */
@RestController
@RequestMapping("/api/heroes")
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
        @PathVariable String idObfuscated,
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
        @RequestBody List<String> idObfuscatedList
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
        @PathVariable String idObfuscated,
        // the list's filter and sort, so prev/next stays inside the set on screen
        @ModelAttribute HeroFilter filter,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection
    ) {
        log.info("GET /api/heroes/{} - Fetching hero by ID", idObfuscated);
        return heroGetService.getHeroById(idObfuscated, filter, sortBy, sortDirection);
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
        /*
         * Every parameter the old signature took is still spelled the same on the wire —
         * @ModelAttribute binds them onto the filter — plus the multi-value forms
         * (heroPages, statuses, qualities) and a keyword.
         */
        @ModelAttribute HeroFilter filter,
        @RequestParam(required = false) Boolean includeStats,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "20") Integer size,
        @RequestParam(required = false, defaultValue = "displayOrder") String sortBy,
        @RequestParam(required = false, defaultValue = "asc") String sortDirection
    ) {
        log.info("GET /api/heroes - Fetching all heroes with filters");
        return heroGetService.listHeroes(filter, includeStats, page, size, sortBy, sortDirection);
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
