package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationBoardTypeServices;

import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationBoardTypeDTOs.AccommodationBoardTypeDTO;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationBoardType;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationBoardTypeRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
 * AccommodationBoardTypeGetService - Service for retrieving accommodation board types
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class AccommodationBoardTypeGetService {

    private final AccommodationBoardTypeRepository boardTypeRepository;

    // filter-aware prev/next + the N of M readout

    @org.springframework.beans.factory.annotation.Autowired

    private com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;

    // dashboard counters for the CURRENT filter set (see CLAUDE.md)

    @org.springframework.beans.factory.annotation.Autowired

    private com.itineraryledger.kabengosafaris.Response.ListStats listStats;
    private final IdObfuscator idObfuscator;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "name", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    @Autowired
    public AccommodationBoardTypeGetService(
        AccommodationBoardTypeRepository boardTypeRepository,
        IdObfuscator idObfuscator
    ) {
        this.boardTypeRepository = boardTypeRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Get a single accommodation board type by ID
     *
     * @param idObfuscated The obfuscated board type ID
     * @return ResponseEntity with ApiResponse containing the board type
     */
    public ResponseEntity<ApiResponse<?>> getAccommodationBoardTypeById(String idObfuscated, String scopeParentId) {
        return getAccommodationBoardTypeById(idObfuscated, scopeParentId, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * One record, plus where it sits in the set the caller was looking at.
     *
     * The list's filters and sort arrive here because paging out of a filtered
     * list must stay inside that filter, and the N of M readout must count the
     * same set. Arrows that traverse a different set are worse than no arrows.
     */
    public ResponseEntity<ApiResponse<?>> getAccommodationBoardTypeById(
        String idObfuscated,
        String scopeParentId,
        /*
         * The global list filters by accommodation through a facet, not a scope; both
         * forms have to reach the walk or paging escapes the filter on screen.
         */
        String accommodationId,
        String name,
        Boolean breakfastIncluded,
        Boolean lunchIncluded,
        Boolean dinnerIncluded,
        Boolean drinksIncluded,
        Boolean alcoholicDrinksIncluded,
        Boolean isActive,
        Boolean hasFullMealPlan,
        String keyword,
        String sortBy,
        String sortDirection
    ) {
        log.info("Fetching accommodation board type by ID: {}", idObfuscated);

        try {
            // Decode board type ID
            Long boardTypeId;
            try {
                boardTypeId = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode board type ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid board type ID",
                        "INVALID_BOARD_TYPE_ID"
                    )
                );
            }

            // Find board type
            AccommodationBoardType boardType = boardTypeRepository.findById(boardTypeId).orElse(null);
            if (boardType == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(
                        404,
                        "Board type not found",
                        "BOARD_TYPE_NOT_FOUND"
                    )
                );
            }

            // Convert to DTO
            AccommodationBoardTypeDTO boardTypeDTO = convertToDTO(boardType);

            // Decode optional scope parent ID for scoped navigation
            Long decodedParentId = null;
            if (scopeParentId != null && !scopeParentId.isEmpty()) {
                try {
                    decodedParentId = idObfuscator.decodeId(scopeParentId);
                } catch (Exception ex) {
                    log.warn("Invalid scopeParentId: {}, falling back to global navigation", scopeParentId);
                }
            }

            /*
             * Prev/next walks the SAME set the caller was looking at — this parent's
             * children when scoped, everything otherwise — and returns the position so
             * the record page can show 'N of M' with the wraparound visible.
             */
            Specification<AccommodationBoardType> navSpec = buildSpec(decodedParentId != null ? decodedParentId : decodeOrNull(accommodationId), name, breakfastIncluded, lunchIncluded, dinnerIncluded, drinksIncluded, alcoholicDrinksIncluded, isActive, hasFullMealPlan, keyword);
            String navSortBy = validateSortField(sortBy);
            if (navSortBy == null) navSortBy = DEFAULT_SORT_FIELD;
            boolean navAscending = sortDirection != null && sortDirection.equalsIgnoreCase("asc");
            java.util.Map<String, Object> nav = recordNavigation.navigate(
                AccommodationBoardType.class, navSpec, navSortBy, navAscending, boardTypeId
            );
            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("boardType", boardTypeDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));
            response.put("scopeParentId", scopeParentId);

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Board type retrieved successfully",
                    response
                )
            );

        } catch (Exception e) {
            log.error("Error fetching accommodation board type by ID", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch board type",
                    "BOARD_TYPE_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Get all accommodation board types with optional filters
     * Accommodation ID is optional
     *
     * @param accommodationId Optional accommodation ID filter
     * @param name Filter by name
     * @param breakfastIncluded Filter by breakfast included
     * @param lunchIncluded Filter by lunch included
     * @param dinnerIncluded Filter by dinner included
     * @param drinksIncluded Filter by drinks included
     * @param alcoholicDrinksIncluded Filter by alcoholic drinks included
     * @param isActive Filter by active status
     * @param hasFullMealPlan Filter board types with full meal plan
     * @param keyword Search keyword
     * @param pageable Pagination parameters
     * @return ResponseEntity with ApiResponse containing paginated board types
     */
    public ResponseEntity<ApiResponse<?>> getAllAccommodationBoardTypes(
        String accommodationId,
        String name,
        Boolean breakfastIncluded,
        Boolean lunchIncluded,
        Boolean dinnerIncluded,
        Boolean drinksIncluded,
        Boolean alcoholicDrinksIncluded,
        Boolean isActive,
        Boolean hasFullMealPlan,
        String keyword,
        String sortBy,
        String sortDirection,
        Pageable pageable
    ) {
        log.info("Fetching all accommodation board types with filters");

        try {
            // Validate sort field
            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                log.warn("Invalid sort field: {}", sortBy);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }

            // Build specification
            Long decodedAccommodationId = null;
            if (accommodationId != null && !accommodationId.isEmpty()) {
                try {
                    decodedAccommodationId = idObfuscator.decodeId(accommodationId);
                } catch (Exception e) {
                    log.warn("Failed to decode accommodation ID: {}", accommodationId, e);
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid accommodation ID", "INVALID_ACCOMMODATION_ID")
                    );
                }
            }
            Specification<AccommodationBoardType> spec = buildSpec(decodedAccommodationId, name, breakfastIncluded, lunchIncluded, dinnerIncluded, drinksIncluded, alcoholicDrinksIncluded, isActive, hasFullMealPlan, keyword);

            // Fetch paginated results
            Page<AccommodationBoardType> boardTypesPage = boardTypeRepository.findAll(spec, pageable);

            // Convert to DTOs
            List<AccommodationBoardTypeDTO> dtos = boardTypesPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            // Build response map
            Map<String, Object> response = new HashMap<>();
            response.put("boardTypes", dtos);
            response.put("currentPage", boardTypesPage.getNumber());
            response.put("totalItems", boardTypesPage.getTotalElements());
            response.put("totalPages", boardTypesPage.getTotalPages());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");
            // counters share the rows' Specification, so cards and table agree
            response.put("stats", computeStats(spec));

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Board types retrieved successfully",
                    response
                )
            );

        } catch (Exception e) {
            log.error("Error fetching all accommodation board types", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch board types",
                    "BOARD_TYPES_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Get all board types for a specific accommodation
     * Accommodation ID is required
     *
     * @param accommodationId Required accommodation ID
     * @param name Filter by name
     * @param breakfastIncluded Filter by breakfast included
     * @param lunchIncluded Filter by lunch included
     * @param dinnerIncluded Filter by dinner included
     * @param drinksIncluded Filter by drinks included
     * @param alcoholicDrinksIncluded Filter by alcoholic drinks included
     * @param isActive Filter by active status
     * @param hasFullMealPlan Filter board types with full meal plan
     * @param keyword Search keyword
     * @param pageable Pagination parameters
     * @return ResponseEntity with ApiResponse containing paginated board types
     */
    public ResponseEntity<ApiResponse<?>> getAllAccommodationsBoardTypes(
        @NotBlank(message = "Accommodation ID is required") String accommodationId,
        String name,
        Boolean breakfastIncluded,
        Boolean lunchIncluded,
        Boolean dinnerIncluded,
        Boolean drinksIncluded,
        Boolean alcoholicDrinksIncluded,
        Boolean isActive,
        Boolean hasFullMealPlan,
        String keyword,
        String sortBy,
        String sortDirection,
        Pageable pageable
    ) {
        log.info("Fetching board types for accommodation: {}", accommodationId);

        try {
            // Validate sort field
            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                log.warn("Invalid sort field: {}", sortBy);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }

            // Decode accommodation ID
            Long accId;
            try {
                accId = idObfuscator.decodeId(accommodationId);
            } catch (Exception e) {
                log.warn("Failed to decode accommodation ID: {}", accommodationId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid accommodation ID",
                        "INVALID_ACCOMMODATION_ID"
                    )
                );
            }

            // Build specification starting with accommodation ID
            Specification<AccommodationBoardType> spec = AccommodationBoardTypeSpecification.hasAccommodationId(accId);

            // Apply other filters
            if (name != null && !name.isEmpty()) {
                spec = spec.and(AccommodationBoardTypeSpecification.hasName(name));
            }
            if (breakfastIncluded != null) {
                spec = spec.and(AccommodationBoardTypeSpecification.hasBreakfastIncluded(breakfastIncluded));
            }
            if (lunchIncluded != null) {
                spec = spec.and(AccommodationBoardTypeSpecification.hasLunchIncluded(lunchIncluded));
            }
            if (dinnerIncluded != null) {
                spec = spec.and(AccommodationBoardTypeSpecification.hasDinnerIncluded(dinnerIncluded));
            }
            if (drinksIncluded != null) {
                spec = spec.and(AccommodationBoardTypeSpecification.hasDrinksIncluded(drinksIncluded));
            }
            if (alcoholicDrinksIncluded != null) {
                spec = spec.and(AccommodationBoardTypeSpecification.hasAlcoholicDrinksIncluded(alcoholicDrinksIncluded));
            }
            if (isActive != null) {
                spec = spec.and(AccommodationBoardTypeSpecification.isActive(isActive));
            }
            if (hasFullMealPlan != null && hasFullMealPlan) {
                spec = spec.and(AccommodationBoardTypeSpecification.hasFullMealPlan());
            }
            if (keyword != null && !keyword.isEmpty()) {
                spec = spec.and(AccommodationBoardTypeSpecification.searchKeyword(keyword));
            }

            // Fetch paginated results
            Page<AccommodationBoardType> boardTypesPage = boardTypeRepository.findAll(spec, pageable);

            // Convert to DTOs
            List<AccommodationBoardTypeDTO> dtos = boardTypesPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            // Build response map
            Map<String, Object> response = new HashMap<>();
            response.put("boardTypes", dtos);
            response.put("currentPage", boardTypesPage.getNumber());
            response.put("totalItems", boardTypesPage.getTotalElements());
            response.put("totalPages", boardTypesPage.getTotalPages());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Board types retrieved successfully",
                    response
                )
            );

        } catch (Exception e) {
            log.error("Error fetching accommodation board types", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch board types",
                    "BOARD_TYPES_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Get unique board types based on meal configuration
     * Returns one board type per unique meal configuration, sorted by name
     * This is useful for dropdowns where users select existing board type configurations
     *
     * @return ResponseEntity with ApiResponse containing list of unique board types
     */
    public ResponseEntity<ApiResponse<?>> getUniqueBoardTypes() {
        log.info("Fetching unique board types by meal configuration");

        try {
            // Fetch unique board types from repository
            List<AccommodationBoardType> uniqueBoardTypes = boardTypeRepository.findUniqueBoardTypesByMealConfiguration();

            // Convert to DTOs
            List<AccommodationBoardTypeDTO> dtos = uniqueBoardTypes.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Unique board types retrieved successfully",
                    dtos
                )
            );

        } catch (Exception e) {
            log.error("Error fetching unique board types", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch unique board types",
                    "UNIQUE_BOARD_TYPES_FETCH_FAILED"
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
     * Convert AccommodationBoardType entity to DTO
     */
    private AccommodationBoardTypeDTO convertToDTO(AccommodationBoardType boardType) {
        return AccommodationBoardTypeDTO.builder()
            .id(idObfuscator.encodeId(boardType.getId()))
            .accommodationId(idObfuscator.encodeId(boardType.getAccommodation().getId()))
            .accommodationName(boardType.getAccommodation().getName())
            .name(boardType.getName())
            .description(boardType.getDescription())
            .mealsIncluded(boardType.getMealsIncluded())
            .breakfastIncluded(boardType.getBreakfastIncluded())
            .lunchIncluded(boardType.getLunchIncluded())
            .dinnerIncluded(boardType.getDinnerIncluded())
            .snacksIncluded(boardType.getSnacksIncluded())
            .drinksIncluded(boardType.getDrinksIncluded())
            .alcoholicDrinksIncluded(boardType.getAlcoholicDrinksIncluded())
            .inclusions(boardType.getInclusions())
            .exclusions(boardType.getExclusions())
            .mealTimes(boardType.getMealTimes())
            .isActive(boardType.getIsActive())
            .mealCount(boardType.getMealCount())
            .isFullMealPlan(boardType.isFullMealPlan())
            .createdAt(boardType.getCreatedAt())
            .updatedAt(boardType.getUpdatedAt())
            .build();
    }

    /** Dashboard counters built from the SAME Specification as the rows. */
    private java.util.Map<String, Object> computeStats(
        org.springframework.data.jpa.domain.Specification<AccommodationBoardType> base
    ) {
        return listStats.of(AccommodationBoardType.class, base)
            .total()
            .count("active", AccommodationBoardTypeSpecification.isActive(true))
            .complement("inactive", "active")
            .recency(AccommodationBoardTypeSpecification::createdAfter)
            .build();
    }

    /**
     * The ONE place a AccommodationBoardType filter is expressed.
     *
     * The rows, the stat counters and prev/next paging all build from this, so a
     * card can never disagree with the table and the arrows can never walk a
     * different set from the one on screen.
     */
    private Specification<AccommodationBoardType> buildSpec(
        Long accommodationId,
        String name,
        Boolean breakfastIncluded,
        Boolean lunchIncluded,
        Boolean dinnerIncluded,
        Boolean drinksIncluded,
        Boolean alcoholicDrinksIncluded,
        Boolean isActive,
        Boolean hasFullMealPlan,
        String keyword
    ) {
        Specification<AccommodationBoardType> spec = Specification.unrestricted();
        if (accommodationId != null) {
            spec = spec.and(AccommodationBoardTypeSpecification.hasAccommodationId(accommodationId));
        }
    if (name != null && !name.isEmpty()) {
    spec = spec.and(AccommodationBoardTypeSpecification.hasName(name));
    }
    if (breakfastIncluded != null) {
    spec = spec.and(AccommodationBoardTypeSpecification.hasBreakfastIncluded(breakfastIncluded));
    }
    if (lunchIncluded != null) {
    spec = spec.and(AccommodationBoardTypeSpecification.hasLunchIncluded(lunchIncluded));
    }
    if (dinnerIncluded != null) {
    spec = spec.and(AccommodationBoardTypeSpecification.hasDinnerIncluded(dinnerIncluded));
    }
    if (drinksIncluded != null) {
    spec = spec.and(AccommodationBoardTypeSpecification.hasDrinksIncluded(drinksIncluded));
    }
    if (alcoholicDrinksIncluded != null) {
    spec = spec.and(AccommodationBoardTypeSpecification.hasAlcoholicDrinksIncluded(alcoholicDrinksIncluded));
    }
    if (isActive != null) {
    spec = spec.and(AccommodationBoardTypeSpecification.isActive(isActive));
    }
    if (hasFullMealPlan != null && hasFullMealPlan) {
    spec = spec.and(AccommodationBoardTypeSpecification.hasFullMealPlan());
    }
    if (keyword != null && !keyword.isEmpty()) {
    spec = spec.and(AccommodationBoardTypeSpecification.searchKeyword(keyword));
    }
        return spec;
    }

    /** Decodes an obfuscated id, or null when absent or unreadable. */
    private Long decodeOrNull(String obfuscated) {
        if (obfuscated == null || obfuscated.isBlank()) return null;
        try {
            return idObfuscator.decodeId(obfuscated);
        } catch (Exception e) {
            log.warn("Unreadable id in filter: {}", obfuscated);
            return null;
        }
    }
}
