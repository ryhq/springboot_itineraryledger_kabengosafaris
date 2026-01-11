package com.itineraryledger.kabengosafaris.PaxNationCategory.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.PaxNationCategory.DTOs.PaxNationCategoryDTO;
import com.itineraryledger.kabengosafaris.PaxNationCategory.DTOs.UpdatePaxNationCategoryDTO;
import com.itineraryledger.kabengosafaris.PaxNationCategory.PaxNationCategory;
import com.itineraryledger.kabengosafaris.PaxNationCategory.Repositories.PaxNationCategoryRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * UpdatePaxNationCategoryService - Service for updating pax nation categories
 */
@Service
@Slf4j
public class UpdatePaxNationCategoryService {

    private final PaxNationCategoryRepository paxNationCategoryRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public UpdatePaxNationCategoryService(
        PaxNationCategoryRepository paxNationCategoryRepository,
        IdObfuscator idObfuscator
    ) {
        this.paxNationCategoryRepository = paxNationCategoryRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Update an existing pax nation category
     *
     * @param id The obfuscated category ID
     * @param updateDTO The updated category data
     * @return ResponseEntity with ApiResponse containing the updated category
     */
    @Transactional
    @AuditLogAnnotation(
        action = "UPDATE_PAX_NATION_CATEGORY",
        description = "Updating a pax nation category",
        entityType = "PaxNationCategory"
    )
    public ResponseEntity<ApiResponse<?>> updatePaxNationCategory(String id, UpdatePaxNationCategoryDTO updateDTO) {
        log.info("Updating pax nation category: {}", id);

        try {
            // Decode ID
            Long decodedId = idObfuscator.decodeId(id);
            if (decodedId == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid pax nation category ID", "INVALID_PAX_NATION_CATEGORY_ID")
                );
            }

            // Find existing category
            Optional<PaxNationCategory> categoryOpt = paxNationCategoryRepository.findById(decodedId);
            if (categoryOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Pax nation category not found", "PAX_NATION_CATEGORY_NOT_FOUND")
                );
            }

            PaxNationCategory category = categoryOpt.get();

            // Check if at least one field is provided for update
            if (!hasFieldsToUpdate(updateDTO)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "At least one field must be provided for update", "NO_FIELDS_TO_UPDATE")
                );
            }

            // Validate input fields
            ResponseEntity<ApiResponse<?>> validationError = validateInputFields(updateDTO, decodedId);
            if (validationError != null) {
                return validationError;
            }

            // Update fields
            if (updateDTO.getName() != null) {
                category.setName(updateDTO.getName().trim());
            }
            if (updateDTO.getCategoryType() != null) {
                category.setCategoryType(updateDTO.getCategoryType());
            }
            if (updateDTO.getDescription() != null) {
                category.setDescription(updateDTO.getDescription());
            }
            if (updateDTO.getPriorityFactor() != null) {
                category.setPriorityFactor(updateDTO.getPriorityFactor());
            }
            if (updateDTO.getIsActive() != null) {
                category.setIsActive(updateDTO.getIsActive());
            }

            // Save updated category
            category = paxNationCategoryRepository.save(category);

            log.info("Pax nation category updated successfully: {}", category.getName());

            // Convert to DTO
            PaxNationCategoryDTO categoryDTO = convertToDTO(category);

            return ResponseEntity.ok(
                ApiResponse.success(
                    200,
                    "Pax nation category updated successfully",
                    categoryDTO
                )
            );

        } catch (Exception e) {
            log.error("Error updating pax nation category", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to update pax nation category: " + e.getMessage(),
                    "PAX_NATION_CATEGORY_UPDATE_FAILED"
                )
            );
        }
    }

    /**
     * Check if there are fields to update
     */
    private boolean hasFieldsToUpdate(UpdatePaxNationCategoryDTO dto) {
        return dto.getName() != null ||
               dto.getCategoryType() != null ||
               dto.getDescription() != null ||
               dto.getPriorityFactor() != null ||
               dto.getIsActive() != null;
    }

    /**
     * Validate input fields for category update
     */
    private ResponseEntity<ApiResponse<?>> validateInputFields(UpdatePaxNationCategoryDTO dto, Long excludeId) {
        // Validate name if provided
        if (dto.getName() != null) {
            if (dto.getName().trim().isEmpty()) {
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

            // Check for duplicate name (excluding current category)
            if (paxNationCategoryRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, excludeId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Pax nation category name already exists", "DUPLICATE_CATEGORY_NAME")
                );
            }
        }

        // Validate priority factor if provided
        if (dto.getPriorityFactor() != null) {
            if (dto.getPriorityFactor() < 1) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Priority factor must be at least 1", "INVALID_PRIORITY_FACTOR")
                );
            }

            // Check for duplicate priority factor (excluding current category)
            if (paxNationCategoryRepository.existsByPriorityFactorAndIdNot(dto.getPriorityFactor(), excludeId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Priority factor " + dto.getPriorityFactor() + " is already in use by another category",
                        "DUPLICATE_PRIORITY_FACTOR"
                    )
                );
            }
        }

        return null; // No validation errors
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
}
