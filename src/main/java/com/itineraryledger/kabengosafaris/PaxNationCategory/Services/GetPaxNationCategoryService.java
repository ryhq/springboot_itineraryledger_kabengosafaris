package com.itineraryledger.kabengosafaris.PaxNationCategory.Services;

import com.itineraryledger.kabengosafaris.PaxNationCategory.DTOs.PaxNationCategoryDTO;
import com.itineraryledger.kabengosafaris.PaxNationCategory.PaxNationCategory;
import com.itineraryledger.kabengosafaris.PaxNationCategory.Repositories.PaxNationCategoryRepository;
import com.itineraryledger.kabengosafaris.PaxNationCategory.Specifications.PaxNationCategorySpecification;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.extern.slf4j.Slf4j;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * GetPaxNationCategoryService - Service for retrieving pax nation categories
 */
@Service
@Slf4j
public class GetPaxNationCategoryService {

    private final PaxNationCategoryRepository paxNationCategoryRepository;
    private final IdObfuscator idObfuscator;

    // filter-aware prev/next + the N of M readout, and declarative counters
    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;

    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Response.ListStats listStats;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "name", "categoryType", "priorityFactor", "isActive", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
    }

    @Autowired
    public GetPaxNationCategoryService(
        PaxNationCategoryRepository paxNationCategoryRepository,
        IdObfuscator idObfuscator
    ) {
        this.paxNationCategoryRepository = paxNationCategoryRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Get a pax nation category by ID
     *
     * @param id The obfuscated category ID
     * @return ResponseEntity with ApiResponse containing the category
     */
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> getPaxNationCategoryById(String id) {
        return getPaxNationCategoryById(id, null, null, null, null, null, null, null, null);
    }

    /**
     * One record, plus where it sits in the set the caller was looking at.
     *
     * The list's filters and sort arrive here because paging out of a filtered list
     * must stay inside that filter, and the N of M readout must count the same set.
     */
    public ResponseEntity<ApiResponse<?>> getPaxNationCategoryById(
        String id,
        String name, PaxNationCategory.CategoryType categoryType, Boolean isActive, Boolean isSystem, Integer priorityFactor, String keyword,
        String sortBy,
        String sortDirection
    ) {
        log.info("Fetching pax nation category by ID: {}", id);

        try {
            // Decode ID
            Long decodedId = idObfuscator.decodeId(id);
            if (decodedId == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid pax nation category ID", "INVALID_PAX_NATION_CATEGORY_ID")
                );
            }

            // Find category
            Optional<PaxNationCategory> categoryOpt = paxNationCategoryRepository.findById(decodedId);
            if (categoryOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Pax nation category not found", "PAX_NATION_CATEGORY_NOT_FOUND")
                );
            }

            PaxNationCategoryDTO categoryDTO = convertToDTO(categoryOpt.get());

            /*
             * Prev/next walks the SAME filtered, sorted set the list showed; the old
             * pair walked raw id order over every record regardless of the filter.
             */
            Specification<PaxNationCategory> navSpec = buildSpec(name, categoryType, isActive, isSystem, priorityFactor, keyword);
            String navSortBy = validateSortField(sortBy);
            if (navSortBy == null) navSortBy = DEFAULT_SORT_FIELD;
            boolean navAscending = sortDirection == null || sortDirection.equalsIgnoreCase("asc");
            Map<String, Object> nav = recordNavigation.navigate(
                PaxNationCategory.class, navSpec, navSortBy, navAscending, decodedId
            );
            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("paxNationCategory", categoryDTO);
            responseMap.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            responseMap.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            responseMap.put("position", nav.get("position"));
            responseMap.put("total", nav.get("total"));

            return ResponseEntity.ok(
                ApiResponse.success(
                    200,
                    "Pax nation category retrieved successfully",
                    responseMap
                )
            );

        } catch (Exception e) {
            log.error("Error fetching pax nation category by ID", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch pax nation category: " + e.getMessage(),
                    "PAX_NATION_CATEGORY_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Get all pax nation categories with pagination, sorting, and filtering
     *
     * @param name Filter by name (partial match)
     * @param categoryType Filter by category type
     * @param isActive Filter by active status
     * @param isSystem Filter by system status
     * @param priorityFactor Filter by exact priority factor
     * @param keyword Search keyword
     * @param page Page number
     * @param size Page size
     * @param sortDirection Sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing paginated categories
     */
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> getAllPaxNationCategories(
        String name,
        PaxNationCategory.CategoryType categoryType,
        Boolean isActive,
        Boolean isSystem,
        Integer priorityFactor,
        String keyword,
        Boolean includeStats,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
    ) {
        log.info("Fetching all pax nation categories with filters");

        try {
            // Validate sort field
            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                log.warn("Invalid sort field: {}", sortBy);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }
            Specification<PaxNationCategory> spec = buildSpec(name, categoryType, isActive, isSystem, priorityFactor, keyword);

            // Build pageable with sorting
            Sort sort = Sort.by(
                "asc".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC,
                validatedSortBy
            );
            Pageable pageable = PageRequest.of(page, size, sort);

            // Fetch page
            Page<PaxNationCategory> categoryPage = paxNationCategoryRepository.findAll(spec, pageable);

            // Convert to DTOs
            List<PaxNationCategoryDTO> categoryDTOs = categoryPage.getContent().stream()
                .map(this::convertToDTO)
                .toList();

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("paxNationCategories", categoryDTOs);
            response.put("currentPage", categoryPage.getNumber());
            response.put("totalItems", categoryPage.getTotalElements());
            response.put("totalPages", categoryPage.getTotalPages());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");
            // counters share the rows' Specification, so cards and table agree
            if (includeStats == null || includeStats) {
                response.put("stats", computeStats(spec));
            }

            return ResponseEntity.ok(
                ApiResponse.success(
                    200,
                    "Pax nation categories retrieved successfully",
                    response
                )
            );

        } catch (Exception e) {
            log.error("Error fetching pax nation categories", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch pax nation categories: " + e.getMessage(),
                    "PAX_NATION_CATEGORIES_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Convert PaxNationCategory entity to DTO
     */
    private PaxNationCategoryDTO convertToDTO(PaxNationCategory category) {
        return PaxNationCategoryDTO.builder()
            .id(idObfuscator.encodeId(category.getId()))
            .name(category.getName())
            .categoryType(category.getCategoryType())
            .categoryTypeDisplayName(category.getCategoryType() != null ? category.getCategoryType().getDisplayName() : null)
            .categoryTypeDescription(category.getCategoryType() != null ? category.getCategoryType().getDescription() : null)
            .description(category.getDescription())
            .priorityFactor(category.getPriorityFactor())
            .priorityDisplay(category.getPriorityDisplay())
            .isActive(category.getIsActive())
            .isSystem(category.getIsSystem())
            .createdAt(category.getCreatedAt())
            .updatedAt(category.getUpdatedAt())
            .build();
    }

    /**
     * The ONE place this module's filter is expressed — rows, counters and prev/next
     * all build from it, so a card can never disagree with the table and the arrows
     * can never walk a different set from the one on screen.
     */
    private Specification<PaxNationCategory> buildSpec(String name, PaxNationCategory.CategoryType categoryType, Boolean isActive, Boolean isSystem, Integer priorityFactor, String keyword) {
        Specification<PaxNationCategory> spec = Specification.unrestricted();
        if (name != null && !name.trim().isEmpty()) spec = spec.and(PaxNationCategorySpecification.nameLike(name));
        if (categoryType != null) spec = spec.and(PaxNationCategorySpecification.hasCategoryType(categoryType));
        if (isActive != null) spec = spec.and(PaxNationCategorySpecification.isActive(isActive));
        if (isSystem != null) spec = spec.and(PaxNationCategorySpecification.isSystem(isSystem));
        if (priorityFactor != null) spec = spec.and(PaxNationCategorySpecification.hasPriorityFactor(priorityFactor));
        if (keyword != null && !keyword.trim().isEmpty()) spec = spec.and(PaxNationCategorySpecification.searchKeyword(keyword));
        return spec;
    }

    /** Counters built from the SAME Specification as the rows. */
    private Map<String, Object> computeStats(Specification<PaxNationCategory> base) {
        return listStats.of(PaxNationCategory.class, base)
            .total()
            .count("active", PaxNationCategorySpecification.isActive(true))
            .complement("inactive", "active")
            .count("system", PaxNationCategorySpecification.isSystem(true))
            .breakdown("byType", PaxNationCategory.CategoryType.values(), PaxNationCategorySpecification::hasCategoryType)
            .recency(PaxNationCategorySpecification::createdAfter)
            .build();
    }
}
