package com.itineraryledger.kabengosafaris.PaxAgeCategory.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.PaxAgeCategory.DTOs.CreatePaxAgeCategoryDTO;
import com.itineraryledger.kabengosafaris.PaxAgeCategory.DTOs.PaxAgeCategoryDTO;
import com.itineraryledger.kabengosafaris.PaxAgeCategory.PaxAgeCategory;
import com.itineraryledger.kabengosafaris.PaxAgeCategory.Repositories.PaxAgeCategoryRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CreatePaxAgeCategoryService - Service for creating new pax age categories
 */
@Service
@Slf4j
public class CreatePaxAgeCategoryService {

    private final PaxAgeCategoryRepository paxAgeCategoryRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public CreatePaxAgeCategoryService(
        PaxAgeCategoryRepository paxAgeCategoryRepository,
        IdObfuscator idObfuscator
    ) {
        this.paxAgeCategoryRepository = paxAgeCategoryRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Create a new pax age category
     *
     * @param createDTO The category data
     * @return ResponseEntity with ApiResponse containing the created category
     */
    @Transactional
    @AuditLogAnnotation(
        action = "CREATE_PAX_AGE_CATEGORY",
        description = "Creating a new pax age category",
        entityType = "PaxAgeCategory"
    )
    public ResponseEntity<ApiResponse<?>> createPaxAgeCategory(CreatePaxAgeCategoryDTO createDTO) {
        log.info("Creating new pax age category: {}", createDTO.getName());

        try {
            // Validate input fields
            ResponseEntity<ApiResponse<?>> validationError = validateInputFields(createDTO);
            if (validationError != null) {
                return validationError;
            }

            // Check for duplicate name
            if (paxAgeCategoryRepository.existsByNameIgnoreCase(createDTO.getName())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Pax age category name already exists",
                        "DUPLICATE_CATEGORY_NAME"
                    )
                );
            }

            // Validate no overlap and no gap with existing categories
            validationError = validateNoOverlapOrGap(createDTO.getMinAge(), createDTO.getMaxAge(), null);
            if (validationError != null) {
                return validationError;
            }

            // Create category entity
            PaxAgeCategory category = PaxAgeCategory.builder()
                .name(createDTO.getName())
                .categoryType(createDTO.getCategoryType())
                .minAge(createDTO.getMinAge())
                .maxAge(createDTO.getMaxAge())
                .description(createDTO.getDescription())
                .isActive(createDTO.getIsActive() != null ? createDTO.getIsActive() : true)
                .build();

            // Save category
            category = paxAgeCategoryRepository.save(category);

            log.info("Pax age category created successfully: {}", category.getName());

            // Convert to DTO
            PaxAgeCategoryDTO categoryDTO = convertToDTO(category);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                    201,
                    "Pax age category created successfully",
                    categoryDTO
                )
            );

        } catch (Exception e) {
            log.error("Error creating pax age category", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to create pax age category: " + e.getMessage(),
                    "PAX_AGE_CATEGORY_CREATE_FAILED"
                )
            );
        }
    }

    /**
     * Validate input fields for category creation
     */
    private ResponseEntity<ApiResponse<?>> validateInputFields(CreatePaxAgeCategoryDTO dto) {
        // Validate and sanitize name
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Category name cannot be empty", "INVALID_NAME")
            );
        }

        String trimmedName = dto.getName().trim();
        if (trimmedName.length() > 100) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Category name cannot exceed 100 characters", "NAME_TOO_LONG")
            );
        }
        dto.setName(trimmedName);

        // Validate minAge
        if (dto.getMinAge() == null) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Minimum age is required", "MISSING_MIN_AGE")
            );
        }
        if (dto.getMinAge() < 0) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Minimum age cannot be negative", "INVALID_MIN_AGE")
            );
        }

        // Validate maxAge
        if (dto.getMaxAge() == null) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Maximum age is required", "MISSING_MAX_AGE")
            );
        }
        if (dto.getMaxAge() < 0) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Maximum age cannot be negative", "INVALID_MAX_AGE")
            );
        }

        // Validate age range
        if (dto.getMinAge() > dto.getMaxAge()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Minimum age cannot be greater than maximum age", "INVALID_AGE_RANGE")
            );
        }

        return null; // No validation errors
    }

    /**
     * Validate that the new category does not overlap with existing categories
     * and does not create gaps in the age range coverage.
     *
     * Expected valid pattern:
     * 0-5 (Child)
     * 6-14 (Youth)
     * 15-150 (Adult)
     *
     * @param minAge The minimum age of the new category
     * @param maxAge The maximum age of the new category
     * @param excludeId ID to exclude (for updates), null for creates
     * @return ResponseEntity with error if validation fails, null if valid
     */
    protected ResponseEntity<ApiResponse<?>> validateNoOverlapOrGap(Integer minAge, Integer maxAge, Long excludeId) {
        // Get all existing categories sorted by minAge
        List<PaxAgeCategory> existingCategories = paxAgeCategoryRepository.findAll(Sort.by(Sort.Direction.ASC, "minAge"));

        // Filter out the category being updated (if any)
        if (excludeId != null) {
            existingCategories = existingCategories.stream()
                .filter(c -> !c.getId().equals(excludeId))
                .toList();
        }

        // If no existing categories, the new one must start at 0
        if (existingCategories.isEmpty()) {
            if (minAge != 0) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "First age category must start at age 0. Got minAge: " + minAge,
                        "AGE_GAP_FROM_ZERO"
                    )
                );
            }
            return null; // Valid - first category starting at 0
        }

        // Check for overlaps with existing categories
        for (PaxAgeCategory existing : existingCategories) {
            // Check if ranges overlap: overlap exists if NOT (new ends before existing starts OR new starts after existing ends)
            boolean overlaps = !(maxAge < existing.getMinAge() || minAge > existing.getMaxAge());
            if (overlaps) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Age range " + minAge + "-" + maxAge + " overlaps with existing category '" +
                        existing.getName() + "' (" + existing.getMinAge() + "-" + existing.getMaxAge() + ")",
                        "AGE_RANGE_OVERLAP"
                    )
                );
            }
        }

        // Check for gaps - the new category must connect seamlessly with existing ones
        // Build the expected continuous range including the new category
        int lowestExisting = existingCategories.get(0).getMinAge();
        int highestExisting = existingCategories.stream()
            .mapToInt(PaxAgeCategory::getMaxAge)
            .max()
            .orElse(0);

        // New category can extend below (must start at 0 or connect to lowest)
        // New category can extend above (must connect to highest)
        // New category can fill a gap in between

        // Check if new category fills a gap or extends the range properly
        boolean connectsToExisting = false;

        // Check if it extends below
        if (maxAge == lowestExisting - 1) {
            // Must start at 0 if extending below
            if (minAge != 0) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "New category extending below existing range must start at age 0. Got minAge: " + minAge,
                        "AGE_GAP_FROM_ZERO"
                    )
                );
            }
            connectsToExisting = true;
        }

        // Check if it extends above
        if (minAge == highestExisting + 1) {
            connectsToExisting = true;
        }

        // Check if it fills a gap between existing categories
        for (int i = 0; i < existingCategories.size() - 1; i++) {
            PaxAgeCategory current = existingCategories.get(i);
            PaxAgeCategory next = existingCategories.get(i + 1);

            // Check if there's a gap between current and next
            if (current.getMaxAge() + 1 < next.getMinAge()) {
                // There's a gap - check if new category fills it exactly
                if (minAge == current.getMaxAge() + 1 && maxAge == next.getMinAge() - 1) {
                    connectsToExisting = true;
                    break;
                }
            }
        }

        // If not connecting to existing range, it's creating a gap
        if (!connectsToExisting) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    400,
                    "Age range " + minAge + "-" + maxAge + " would create a gap in age coverage. " +
                    "Current coverage: " + lowestExisting + "-" + highestExisting + ". " +
                    "New category must connect seamlessly (e.g., extend from " + (highestExisting + 1) + " or fill an existing gap)",
                    "AGE_GAP_DETECTED"
                )
            );
        }

        return null; // Valid
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
