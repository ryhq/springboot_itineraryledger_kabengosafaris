package com.itineraryledger.kabengosafaris.PaxAgeCategory.Services;

import com.itineraryledger.kabengosafaris.PaxAgeCategory.DTOs.PaxAgeCategoryDTO;
import com.itineraryledger.kabengosafaris.PaxAgeCategory.PaxAgeCategory;
import com.itineraryledger.kabengosafaris.PaxAgeCategory.Repositories.PaxAgeCategoryRepository;
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
import java.util.stream.Collectors;

/**
 * GetPaxAgeCategoryService - Service for retrieving pax age categories with filtering, pagination, and sorting
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class GetPaxAgeCategoryService {

    private final PaxAgeCategoryRepository paxAgeCategoryRepository;
    private final IdObfuscator idObfuscator;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "name", "categoryType", "minAge", "maxAge", "isActive", "createdAt", "updatedAt"
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
    public GetPaxAgeCategoryService(
        PaxAgeCategoryRepository paxAgeCategoryRepository,
        IdObfuscator idObfuscator
    ) {
        this.paxAgeCategoryRepository = paxAgeCategoryRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Get a single pax age category by obfuscated ID
     *
     * @param idObfuscated The obfuscated category ID
     * @return ResponseEntity with ApiResponse containing the category
     */
    public ResponseEntity<ApiResponse<?>> getPaxAgeCategoryById(String idObfuscated) {
        log.info("Fetching pax age category with ID: {}", idObfuscated);

        try {
            // Decode ID
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode pax age category ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid pax age category ID",
                        "INVALID_PAX_AGE_CATEGORY_ID"
                    )
                );
            }

            // Find category
            PaxAgeCategory category = paxAgeCategoryRepository.findById(id).orElse(null);
            if (category == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(
                        404,
                        "Pax age category not found",
                        "PAX_AGE_CATEGORY_NOT_FOUND"
                    )
                );
            }

            // Convert to DTO
            PaxAgeCategoryDTO categoryDTO = convertToDTO(category);

            // Navigation IDs
            Long nextId = paxAgeCategoryRepository.findNextId(id).orElse(null);
            Long previousId = paxAgeCategoryRepository.findPreviousId(id).orElse(null);
            if (nextId == null) nextId = paxAgeCategoryRepository.findFirstId().orElse(null);
            if (previousId == null) previousId = paxAgeCategoryRepository.findLastId().orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("paxAgeCategory", categoryDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Pax age category retrieved successfully",
                    response
                )
            );

        } catch (Exception e) {
            log.error("Error fetching pax age category", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch pax age category",
                    "PAX_AGE_CATEGORY_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Get all pax age categories with pagination, sorting, and filtering
     *
     * @param name Filter by name (partial match)
     * @param categoryType Filter by category type
     * @param isActive Filter by active status
     * @param isSystem Filter by system status
     * @param minAge Filter by minimum age
     * @param maxAge Filter by maximum age
     * @param age Filter categories that include this specific age
     * @param keyword Search keyword across name and description
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortDirection Sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing paginated categories
     */
    public ResponseEntity<ApiResponse<?>> getAllPaxAgeCategories(
        String name,
        PaxAgeCategory.CategoryType categoryType,
        Boolean isActive,
        Boolean isSystem,
        Integer minAge,
        Integer maxAge,
        Integer age,
        String keyword,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
    ) {
        log.info("Fetching all pax age categories with filters");

        try {
            // Validate sort field
            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                log.warn("Invalid sort field: {}", sortBy);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }
            // Build specification for filtering
            Specification<PaxAgeCategory> spec = Specification.unrestricted();

            if (name != null && !name.isEmpty()) {
                spec = spec.and(PaxAgeCategorySpecification.nameLike(name));
            }
            if (categoryType != null) {
                spec = spec.and(PaxAgeCategorySpecification.hasCategoryType(categoryType));
            }
            if (isActive != null) {
                spec = spec.and(PaxAgeCategorySpecification.isActive(isActive));
            }
            if (isSystem != null) {
                spec = spec.and(PaxAgeCategorySpecification.isSystem(isSystem));
            }
            if (minAge != null) {
                spec = spec.and(PaxAgeCategorySpecification.hasMinAge(minAge));
            }
            if (maxAge != null) {
                spec = spec.and(PaxAgeCategorySpecification.hasMaxAge(maxAge));
            }
            if (age != null) {
                spec = spec.and(PaxAgeCategorySpecification.includesAge(age));
            }
            if (keyword != null && !keyword.isEmpty()) {
                spec = spec.and(PaxAgeCategorySpecification.searchKeyword(keyword));
            }

            // Set default pagination values
            int pageNumber = (page != null && page >= 0) ? page : 0;
            int pageSize = (size != null && size > 0) ? size : 10;

            // Set default sorting (always by minAge to show categories in order)
            Sort.Direction direction = Sort.Direction.ASC;
            if (sortDirection != null && sortDirection.equalsIgnoreCase("desc")) {
                direction = Sort.Direction.DESC;
            }

            // Create pageable
            Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(direction, validatedSortBy));

            // Fetch categories
            Page<PaxAgeCategory> categoryPage = paxAgeCategoryRepository.findAll(spec, pageable);

            // Convert to DTOs
            List<PaxAgeCategoryDTO> categoryDTOs = categoryPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            // Create response with pagination metadata
            Map<String, Object> response = new HashMap<>();
            response.put("paxAgeCategories", categoryDTOs);
            response.put("currentPage", categoryPage.getNumber());
            response.put("totalItems", categoryPage.getTotalElements());
            response.put("totalPages", categoryPage.getTotalPages());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDir", sortDirection != null ? sortDirection : "desc");

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Pax age categories retrieved successfully",
                    response
                )
            );

        } catch (Exception e) {
            log.error("Error fetching pax age categories", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch pax age categories",
                    "PAX_AGE_CATEGORIES_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Convert PaxAgeCategory entity to DTO
     */
    private PaxAgeCategoryDTO convertToDTO(PaxAgeCategory category) {
        return PaxAgeCategoryDTO.builder()
            .id(idObfuscator.encodeId(category.getId()))
            .name(category.getName())
            .categoryType(category.getCategoryType())
            .categoryTypeDisplayName(category.getCategoryType() != null ? category.getCategoryType().getDisplayName() : null)
            .categoryTypeDescription(category.getCategoryType() != null ? category.getCategoryType().getDescription() : null)
            .minAge(category.getMinAge())
            .maxAge(category.getMaxAge())
            .ageRangeDisplay(category.getAgeRangeDisplay())
            .description(category.getDescription())
            .isActive(category.getIsActive())
            .isSystem(category.getIsSystem())
            .createdAt(category.getCreatedAt())
            .updatedAt(category.getUpdatedAt())
            .build();
    }
}
