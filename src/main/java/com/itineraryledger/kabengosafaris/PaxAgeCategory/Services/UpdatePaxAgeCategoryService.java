package com.itineraryledger.kabengosafaris.PaxAgeCategory.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.PaxAgeCategory.DTOs.PaxAgeCategoryDTO;
import com.itineraryledger.kabengosafaris.PaxAgeCategory.DTOs.UpdatePaxAgeCategoryDTO;
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
 * UpdatePaxAgeCategoryService - Service for updating pax age categories
 */
@Service
@Slf4j
@Transactional
public class UpdatePaxAgeCategoryService {

    private final PaxAgeCategoryRepository paxAgeCategoryRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public UpdatePaxAgeCategoryService(
        PaxAgeCategoryRepository paxAgeCategoryRepository,
        IdObfuscator idObfuscator
    ) {
        this.paxAgeCategoryRepository = paxAgeCategoryRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Update a pax age category by obfuscated ID
     *
     * @param idObfuscated The obfuscated category ID
     * @param updateDTO The updated category data
     * @return ResponseEntity with ApiResponse containing the updated category
     */
    @AuditLogAnnotation(
        action = "UPDATE_PAX_AGE_CATEGORY",
        description = "Updating pax age category",
        entityType = "PaxAgeCategory",
        entityIdParamName = "idObfuscated"
    )
    public ResponseEntity<ApiResponse<?>> updatePaxAgeCategory(String idObfuscated, UpdatePaxAgeCategoryDTO updateDTO) {
        log.info("Updating pax age category with ID: {}", idObfuscated);

        try {
            // Validate that at least one field is provided for update
            ResponseEntity<ApiResponse<?>> validationError = validateAtLeastOneFieldProvided(updateDTO);
            if (validationError != null) {
                return validationError;
            }

            // Validate input fields
            validationError = validateInputFields(updateDTO);
            if (validationError != null) {
                return validationError;
            }

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

            return updatePaxAgeCategoryById(id, updateDTO);

        } catch (Exception e) {
            log.error("Error updating pax age category", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to update pax age category",
                    "PAX_AGE_CATEGORY_UPDATE_FAILED"
                )
            );
        }
    }

    /**
     * Update a pax age category by ID (internal method)
     */
    private ResponseEntity<ApiResponse<?>> updatePaxAgeCategoryById(Long id, UpdatePaxAgeCategoryDTO updateDTO) {
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

        // Check for duplicate name if name is being updated
        if (updateDTO.getName() != null && !updateDTO.getName().equalsIgnoreCase(category.getName())) {
            if (paxAgeCategoryRepository.existsByNameIgnoreCaseAndIdNot(updateDTO.getName(), id)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Pax age category name already exists",
                        "DUPLICATE_CATEGORY_NAME"
                    )
                );
            }
        }

        // Check if age range is being updated - validate no overlap/gap
        Integer newMinAge = updateDTO.getMinAge() != null ? updateDTO.getMinAge() : category.getMinAge();
        Integer newMaxAge = updateDTO.getMaxAge() != null ? updateDTO.getMaxAge() : category.getMaxAge();

        // Only validate if age range is actually changing
        if ((updateDTO.getMinAge() != null && !updateDTO.getMinAge().equals(category.getMinAge())) ||
            (updateDTO.getMaxAge() != null && !updateDTO.getMaxAge().equals(category.getMaxAge()))) {

            // Validate the new age range
            if (newMinAge > newMaxAge) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Minimum age cannot be greater than maximum age", "INVALID_AGE_RANGE")
                );
            }

            // Validate no overlap or gap with other categories (excluding this one)
            ResponseEntity<ApiResponse<?>> validationError = validateNoOverlapOrGap(newMinAge, newMaxAge, id);
            if (validationError != null) {
                return validationError;
            }
        }

        // Update fields if provided
        if (updateDTO.getName() != null) {
            category.setName(updateDTO.getName());
        }
        if (updateDTO.getCategoryType() != null) {
            category.setCategoryType(updateDTO.getCategoryType().isBlank() ? null : com.itineraryledger.kabengosafaris.PaxAgeCategory.PaxAgeCategory.CategoryType.valueOf(updateDTO.getCategoryType().trim()));
        }
        if (updateDTO.getMinAge() != null) {
            category.setMinAge(updateDTO.getMinAge());
        }
        if (updateDTO.getMaxAge() != null) {
            category.setMaxAge(updateDTO.getMaxAge());
        }
        if (updateDTO.getDescription() != null) {
            category.setDescription(updateDTO.getDescription());
        }
        if (updateDTO.getIsActive() != null) {
            category.setIsActive(updateDTO.getIsActive());
        }

        // Save updated category
        category = paxAgeCategoryRepository.save(category);

        // Convert to DTO
        PaxAgeCategoryDTO categoryDTO = convertToDTO(category);

        log.info("Pax age category updated successfully: {}", category.getName());

        return ResponseEntity.ok().body(
            ApiResponse.success(
                200,
                "Pax age category updated successfully",
                categoryDTO
            )
        );
    }

    /**
     * Validate that at least one field is provided for update
     */
    private ResponseEntity<ApiResponse<?>> validateAtLeastOneFieldProvided(UpdatePaxAgeCategoryDTO dto) {
        boolean hasUpdate =
            dto.getName() != null ||
            dto.getCategoryType() != null ||
            dto.getMinAge() != null ||
            dto.getMaxAge() != null ||
            dto.getDescription() != null ||
            dto.getIsActive() != null;

        if (!hasUpdate) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "At least one field must be provided for update", "NO_FIELDS_TO_UPDATE")
            );
        }

        return null;
    }

    /**
     * Validate input fields for category update
     */
    private ResponseEntity<ApiResponse<?>> validateInputFields(UpdatePaxAgeCategoryDTO dto) {
        // Validate and sanitize name if provided
        if (dto.getName() != null) {
            String trimmedName = dto.getName().trim();
            if (trimmedName.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Category name cannot be empty", "INVALID_NAME")
                );
            }
            if (trimmedName.length() > 100) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Category name cannot exceed 100 characters", "NAME_TOO_LONG")
                );
            }
            dto.setName(trimmedName);
        }

        // Validate minAge if provided
        if (dto.getMinAge() != null && dto.getMinAge() < 0) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Minimum age cannot be negative", "INVALID_MIN_AGE")
            );
        }

        // Validate maxAge if provided
        if (dto.getMaxAge() != null && dto.getMaxAge() < 0) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Maximum age cannot be negative", "INVALID_MAX_AGE")
            );
        }

        return null; // No validation errors
    }

    /**
     * Validate that the updated category does not overlap with existing categories
     * and does not create gaps in the age range coverage.
     *
     * Expected valid pattern:
     * 0-5 (Child)
     * 6-14 (Youth)
     * 15-150 (Adult)
     *
     * @param minAge The minimum age of the updated category
     * @param maxAge The maximum age of the updated category
     * @param excludeId ID to exclude (the category being updated)
     * @return ResponseEntity with error if validation fails, null if valid
     */
    private ResponseEntity<ApiResponse<?>> validateNoOverlapOrGap(Integer minAge, Integer maxAge, Long excludeId) {
        // Get all existing categories sorted by minAge
        List<PaxAgeCategory> existingCategories = paxAgeCategoryRepository.findAll(Sort.by(Sort.Direction.ASC, "minAge"));

        // Filter out the category being updated
        existingCategories = existingCategories.stream()
            .filter(c -> !c.getId().equals(excludeId))
            .toList();

        // If no other categories exist, the updated one must start at 0
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
            return null; // Valid - only category starting at 0
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

        // Check for gaps - the updated category must connect seamlessly with existing ones
        int lowestExisting = existingCategories.get(0).getMinAge();
        int highestExisting = existingCategories.stream()
            .mapToInt(PaxAgeCategory::getMaxAge)
            .max()
            .orElse(0);

        // Check if updated category fills a gap or extends the range properly
        boolean connectsToExisting = false;

        // Check if it extends below
        if (maxAge == lowestExisting - 1) {
            // Must start at 0 if extending below
            if (minAge != 0) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Category extending below existing range must start at age 0. Got minAge: " + minAge,
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
                // There's a gap - check if updated category fills it exactly
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
                    "Updated category must connect seamlessly (e.g., extend from " + (highestExisting + 1) + " or fill an existing gap)",
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
