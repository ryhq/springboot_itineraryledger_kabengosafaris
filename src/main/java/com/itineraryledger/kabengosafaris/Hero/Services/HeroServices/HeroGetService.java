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
import com.itineraryledger.kabengosafaris.Hero.Specifications.HeroFilter;
import com.itineraryledger.kabengosafaris.Hero.Specifications.HeroSpecification;
import com.itineraryledger.kabengosafaris.Hero.DTOs.HeroDTO;
import com.itineraryledger.kabengosafaris.Hero.Services.HeroImageServices.HeroImageStorageService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Response.ListStats;
import com.itineraryledger.kabengosafaris.Response.RecordNavigation;
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
    private final ListStats listStats;
    private final RecordNavigation recordNavigation;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "displayOrder", "title", "page", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    @Autowired
    public HeroGetService(
        HeroRepository heroRepository,
        IdObfuscator idObfuscator,
        HeroImageStorageService storageService,
        ListStats listStats,
        RecordNavigation recordNavigation
    ) {
        this.heroRepository = heroRepository;
        this.idObfuscator = idObfuscator;
        this.storageService = storageService;
        this.listStats = listStats;
        this.recordNavigation = recordNavigation;
    }

    /**
     * Get a single hero by obfuscated ID
     *
     * @param idObfuscated The obfuscated hero ID
     * @return ResponseEntity with ApiResponse containing the hero
     */
    public ResponseEntity<ApiResponse<?>> getHeroById(String idObfuscated) {
        return getHeroById(idObfuscated, null, null, null);
    }

    /**
     * One hero, plus where it sits in the set the caller was looking at.
     *
     * The filter and sort come from the list page, so prev/next walks the same rows that
     * were on screen — page into a banner from the Home-page list and the arrows stay
     * among Home-page banners instead of wandering the whole table.
     */
    public ResponseEntity<ApiResponse<?>> getHeroById(
        String idObfuscated,
        HeroFilter filter,
        String sortBy,
        String sortDirection
    ) {
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

            /*
             * The walk runs over the SAME specification the list used, in the same order.
             * It used to page over every hero by id, so the arrows left the filtered set
             * the moment anybody used a filter.
             */
            Specification<Hero> navSpec = buildSpec(filter != null ? filter : new HeroFilter());
            String navSortBy = validateSortField(sortBy) != null ? validateSortField(sortBy) : DEFAULT_SORT_FIELD;
            boolean ascending = !"desc".equalsIgnoreCase(sortDirection);

            Map<String, Object> nav = recordNavigation.navigate(Hero.class, navSpec, navSortBy, ascending, id);
            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("hero", heroDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

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
    /** Kept so any caller still passing loose parameters keeps working. */
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
        HeroFilter filter = new HeroFilter();
        filter.setTitle(title);
        filter.setHeroPage(page);
        filter.setIsActive(isActive);
        filter.setTextAlignment(textAlignment);
        filter.setCreatedById(createdById);
        filter.setUpdatedById(updatedById);
        return listHeroes(filter, null, pageNumber, pageSize, sortBy, sortDirection);
    }

    public ResponseEntity<ApiResponse<?>> listHeroes(
        HeroFilter filter,
        Boolean includeStats,
        Integer pageNumber,
        Integer pageSize,
        String sortBy,
        String sortDirection
    ) {
        HeroFilter active = filter != null ? filter : new HeroFilter();
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

            Specification<Hero> spec = buildSpec(active);

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
            /*
             * Counters for the WHOLE filtered set, from the same specification as the rows,
             * so a card and the table under it cannot disagree.
             */
            if (!Boolean.FALSE.equals(includeStats)) {
                response.put("stats", buildStats(spec));
            }

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

    /**
     * One specification, used by the rows, the cards and the record walk.
     *
     * Sharing it is the whole point: a card counts what the table would show, and prev/next
     * stays inside the same set. OR inside a dimension, AND across dimensions.
     */
    private Specification<Hero> buildSpec(HeroFilter filter) {
        Specification<Hero> spec = Specification.unrestricted();

        if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
            spec = spec.and(HeroSpecification.searchKeyword(filter.getKeyword()));
        }
        if (filter.getTitle() != null && !filter.getTitle().isBlank()) {
            spec = spec.and(HeroSpecification.byTitle(filter.getTitle()));
        }
        if (!filter.allPages().isEmpty()) {
            spec = spec.and(HeroSpecification.pageIn(filter.allPages()));
        }
        if (!filter.allAlignments().isEmpty()) {
            spec = spec.and(HeroSpecification.textAlignmentIn(filter.allAlignments()));
        }
        Boolean active = filter.resolvedActive();
        if (active != null) {
            spec = spec.and(HeroSpecification.byIsActive(active));
        }
        if (filter.wants("noImage")) {
            spec = spec.and(HeroSpecification.hasNoImages());
        }
        if (filter.wants("brokenCta")) {
            spec = spec.and(HeroSpecification.hasBrokenCta());
        }
        if (filter.getCreatedAfter() != null) {
            spec = spec.and(HeroSpecification.createdAfter(filter.getCreatedAfter()));
        }
        // an id that will not decode is a filter nobody asked for, not a 500
        if (filter.getCreatedById() != null && !filter.getCreatedById().isBlank()) {
            try {
                spec = spec.and(HeroSpecification.byCreatedById(idObfuscator.decodeId(filter.getCreatedById())));
            } catch (Exception e) {
                log.warn("Invalid createdById: {}", filter.getCreatedById());
            }
        }
        if (filter.getUpdatedById() != null && !filter.getUpdatedById().isBlank()) {
            try {
                spec = spec.and(HeroSpecification.byUpdatedById(idObfuscator.decodeId(filter.getUpdatedById())));
            } catch (Exception e) {
                log.warn("Invalid updatedById: {}", filter.getUpdatedById());
            }
        }
        return spec;
    }

    /**
     * The cards over the hero list.
     *
     * Every figure here is reachable as a filter and every filter has a figure — the two
     * data-quality counters especially, because a banner with no image and a button with no
     * link are the failures that look perfectly healthy in every other column.
     */
    private Map<String, Object> buildStats(Specification<Hero> spec) {
        return listStats.of(Hero.class, spec)
            .total()
            .count("active", HeroSpecification.byIsActive(true))
            .complement("inactive", "active")
            .count("noImage", HeroSpecification.hasNoImages())
            .count("brokenCta", HeroSpecification.hasBrokenCta())
            .breakdown("byPage", HeroPage.values(), page -> HeroSpecification.pageIn(List.of(page)))
            .recency(HeroSpecification::createdAfter)
            .build();
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
