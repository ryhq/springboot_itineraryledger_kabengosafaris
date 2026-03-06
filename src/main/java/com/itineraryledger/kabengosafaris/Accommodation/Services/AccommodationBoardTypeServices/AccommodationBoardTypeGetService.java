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
    public ResponseEntity<ApiResponse<?>> getAccommodationBoardTypeById(String idObfuscated) {
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

            // Circular navigation
            Long nextId = boardTypeRepository.findNextId(boardTypeId).orElse(null);
            Long previousId = boardTypeRepository.findPreviousId(boardTypeId).orElse(null);
            if (nextId == null) nextId = boardTypeRepository.findFirstId().orElse(null);
            if (previousId == null) previousId = boardTypeRepository.findLastId().orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("boardType", boardTypeDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);

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
            Specification<AccommodationBoardType> spec = Specification.unrestricted();

            // Filter by accommodation ID if provided
            if (accommodationId != null && !accommodationId.isEmpty()) {
                try {
                    Long accId = idObfuscator.decodeId(accommodationId);
                    spec = spec.and(AccommodationBoardTypeSpecification.hasAccommodationId(accId));
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
            }

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
            response.put("currentSortDir", sortDirection != null ? sortDirection : "desc");

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
            response.put("currentSortDir", sortDirection != null ? sortDirection : "desc");

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
}
