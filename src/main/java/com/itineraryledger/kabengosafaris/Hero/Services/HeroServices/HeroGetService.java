package com.itineraryledger.kabengosafaris.Hero.Services.HeroServices;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Hero.Entity.Hero;
import com.itineraryledger.kabengosafaris.Hero.Entity.HeroImage;
import com.itineraryledger.kabengosafaris.Hero.Enums.HeroPage;
import com.itineraryledger.kabengosafaris.Hero.Repository.HeroRepository;
import com.itineraryledger.kabengosafaris.Hero.Specifications.HeroSpecification;
import com.itineraryledger.kabengosafaris.Hero.DTOs.HeroDTO;
import com.itineraryledger.kabengosafaris.Hero.Services.HeroImageServices.HeroImageStorageService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * HeroGetService - Service for retrieving heroes with filtering, pagination, and sorting
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class HeroGetService {

    private final HeroRepository heroRepository;
    private final IdObfuscator idObfuscator;
    private final HeroImageStorageService storageService;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "title", "page", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    @Autowired
    public HeroGetService(
        HeroRepository heroRepository,
        IdObfuscator idObfuscator,
        HeroImageStorageService storageService
    ) {
        this.heroRepository = heroRepository;
        this.idObfuscator = idObfuscator;
        this.storageService = storageService;
    }

    /**
     * Get a single hero by obfuscated ID
     *
     * @param idObfuscated The obfuscated hero ID
     * @return ResponseEntity with ApiResponse containing the hero
     */
    public ResponseEntity<ApiResponse<?>> getHeroById(String idObfuscated) {
        log.info("Fetching hero with ID: {}", idObfuscated);

        try {
            // Decode ID
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode hero ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid hero ID",
                        "INVALID_HERO_ID"
                    )
                );
            }

            // Find hero
            Hero hero = heroRepository.findById(id).orElse(null);
            if (hero == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(
                        404,
                        "Hero not found",
                        "HERO_NOT_FOUND"
                    )
                );
            }

            // Convert to DTO
            HeroDTO heroDTO = convertToDTO(hero);

            // Circular navigation
            Long nextId = heroRepository.findNextId(id).orElse(null);
            Long previousId = heroRepository.findPreviousId(id).orElse(null);
            if (nextId == null) nextId = heroRepository.findFirstId().orElse(null);
            if (previousId == null) previousId = heroRepository.findLastId().orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("hero", heroDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Hero retrieved successfully",
                    response
                )
            );

        } catch (Exception e) {
            log.error("Error fetching hero", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch hero",
                    "HERO_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Get heroes for a specific page (for front-end display)
     *
     * @param page The page to get heroes for
     * @return ResponseEntity with ApiResponse containing the heroes
     */
    public ResponseEntity<ApiResponse<?>> getHeroesByPage(HeroPage page) {
        log.info("Fetching active heroes for page: {}", page);

        try {
            List<Hero> heroes = heroRepository.findByPageAndIsActiveTrueOrderByDisplayOrderAsc(page);

            List<HeroDTO> heroDTOs = heroes.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Heroes retrieved successfully",
                    heroDTOs
                )
            );

        } catch (Exception e) {
            log.error("Error fetching heroes by page", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch heroes",
                    "HEROES_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * List heroes with filtering, pagination, and sorting
     *
     * @param pageNumber Page number (0-indexed)
     * @param pageSize Number of items per page
     * @param sortBy Field to sort by
     * @param sortDirection Sort direction (asc/desc)
     * @param title Filter by title (partial match)
     * @param page Filter by page
     * @param isActive Filter by active status
     * @param textAlignment Filter by text alignment
     * @param createdById Filter by creator
     * @param updatedById Filter by updater
     * @return ResponseEntity with ApiResponse containing paginated heroes
     */
    public ResponseEntity<ApiResponse<?>> listHeroes(
        Integer pageNumber,
        Integer pageSize,
        String sortBy,
        String sortDirection,
        String title,
        HeroPage page,
        Boolean isActive,
        String textAlignment,
        String createdById,
        String updatedById
    ) {
        log.info("Listing heroes with filters");

        try {
            // Set defaults
            pageNumber = (pageNumber != null) ? pageNumber : 0;
            pageSize = (pageSize != null) ? pageSize : 20;
            sortDirection = (sortDirection != null && !sortDirection.isEmpty()) ? sortDirection : "asc";

            // Sorting with validation
            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                log.warn("Invalid sort field: {}", sortBy);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }

            // Create sort
            Sort sort = sortDirection.equalsIgnoreCase("desc")
                ? Sort.by(validatedSortBy).descending()
                : Sort.by(validatedSortBy).ascending();

            // Create pageable
            Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

            // Build specification
            Specification<Hero> spec = Specification.unrestricted();

            if (title != null && !title.isEmpty()) {
                spec = spec.and(HeroSpecification.byTitle(title));
            }
            if (page != null) {
                spec = spec.and(HeroSpecification.byPage(page));
            }
            if (isActive != null) {
                spec = spec.and(HeroSpecification.byIsActive(isActive));
            }
            if (textAlignment != null && !textAlignment.isEmpty()) {
                spec = spec.and(HeroSpecification.byTextAlignment(textAlignment));
            }
            if (createdById != null && !createdById.isEmpty()) {
                try {
                    Long decodedCreatedById = idObfuscator.decodeId(createdById);
                    spec = spec.and(HeroSpecification.byCreatedById(decodedCreatedById));
                } catch (Exception e) {
                    log.warn("Invalid createdById: {}", createdById);
                }
            }
            if (updatedById != null && !updatedById.isEmpty()) {
                try {
                    Long decodedUpdatedById = idObfuscator.decodeId(updatedById);
                    spec = spec.and(HeroSpecification.byUpdatedById(decodedUpdatedById));
                } catch (Exception e) {
                    log.warn("Invalid updatedById: {}", updatedById);
                }
            }

            // Execute query
            Page<Hero> heroPage = heroRepository.findAll(spec, pageable);

            // Convert to DTOs
            List<HeroDTO> heroDTOs = heroPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            // Build response with pagination metadata
            Map<String, Object> response = new HashMap<>();
            response.put("heroes", heroDTOs);
            response.put("currentPage", heroPage.getNumber());
            response.put("totalItems", heroPage.getTotalElements());
            response.put("totalPages", heroPage.getTotalPages());
            response.put("pageSize", heroPage.getSize());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", sortDirection);

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Heroes retrieved successfully",
                    response
                )
            );

        } catch (Exception e) {
            log.error("Error listing heroes", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to list heroes",
                    "HEROES_LIST_FAILED"
                )
            );
        }
    }

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
    }

    /**
     * Convert Hero entity to HeroDTO
     */
    public HeroDTO convertToDTO(Hero hero) {
        HeroDTO dto = HeroDTO.builder()
            .id(idObfuscator.encodeId(hero.getId()))
            .title(hero.getTitle())
            .subtitle(hero.getSubtitle())
            .description(hero.getDescription())
            .page(hero.getPage())
            .pageDisplayName(hero.getPage() != null ? hero.getPage().getDisplayName() : null)
            .ctaText(hero.getCtaText())
            .ctaLink(hero.getCtaLink())
            .displayOrder(hero.getDisplayOrder())
            .isActive(hero.getIsActive())
            .overlayColor(hero.getOverlayColor())
            .overlayOpacity(hero.getOverlayOpacity())
            .textAlignment(hero.getTextAlignment())
            .cssClasses(hero.getCssClasses())
            .imageCount((long) hero.getImages().size())
            .createdAt(hero.getCreatedAt())
            .updatedAt(hero.getUpdatedAt())
            .build();

        // Set audit fields
        if (hero.getCreatedBy() != null) {
            dto.setCreatedById(idObfuscator.encodeId(hero.getCreatedBy().getId()));
            dto.setCreatedByName(hero.getCreatedBy().getFirstName() + " " + hero.getCreatedBy().getLastName());
        }
        if (hero.getUpdatedBy() != null) {
            dto.setUpdatedById(idObfuscator.encodeId(hero.getUpdatedBy().getId()));
            dto.setUpdatedByName(hero.getUpdatedBy().getFirstName() + " " + hero.getUpdatedBy().getLastName());
        }

        // Set primary image URL
        HeroImage primaryImage = hero.getPrimaryImage();
        if (primaryImage != null && primaryImage.getFileName() != null) {
            dto.setPrimaryImageUrl(storageService.constructImageUrl(idObfuscator.encodeId(primaryImage.getId())));
        }

        return dto;
    }
}
