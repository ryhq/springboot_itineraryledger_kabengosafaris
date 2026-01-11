package com.itineraryledger.kabengosafaris.PaxNationCategory.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.PaxNationCategory.DTOs.CreatePaxNationCategoryDTO;
import com.itineraryledger.kabengosafaris.PaxNationCategory.DTOs.PaxNationCategoryDTO;
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

/**
 * CreatePaxNationCategoryService - Service for creating new pax nation categories
 */
@Service
@Slf4j
public class CreatePaxNationCategoryService {

    private final PaxNationCategoryRepository paxNationCategoryRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public CreatePaxNationCategoryService(
        PaxNationCategoryRepository paxNationCategoryRepository,
        IdObfuscator idObfuscator
    ) {
        this.paxNationCategoryRepository = paxNationCategoryRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Create a new pax nation category
     *
     * @param createDTO The category data
     * @return ResponseEntity with ApiResponse containing the created category
     */
    @Transactional
    @AuditLogAnnotation(
        action = "CREATE_PAX_NATION_CATEGORY",
        description = "Creating a new pax nation category",
        entityType = "PaxNationCategory"
    )
    public ResponseEntity<ApiResponse<?>> createPaxNationCategory(CreatePaxNationCategoryDTO createDTO) {
        log.info("Creating new pax nation category: {}", createDTO.getName());

        try {
            // Validate input fields
            ResponseEntity<ApiResponse<?>> validationError = validateInputFields(createDTO);
            if (validationError != null) {
                return validationError;
            }

            // Check for duplicate name
            if (paxNationCategoryRepository.existsByNameIgnoreCase(createDTO.getName())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Pax nation category name already exists",
                        "DUPLICATE_CATEGORY_NAME"
                    )
                );
            }

            // Validate priority factor uniqueness
            if (createDTO.getPriorityFactor() != null &&
                paxNationCategoryRepository.existsByPriorityFactor(createDTO.getPriorityFactor())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Priority factor " + createDTO.getPriorityFactor() + " is already in use by another category",
                        "DUPLICATE_PRIORITY_FACTOR"
                    )
                );
            }

            // Create category entity
            PaxNationCategory category = PaxNationCategory.builder()
                .name(createDTO.getName())
                .categoryType(createDTO.getCategoryType())
                .description(createDTO.getDescription())
                .priorityFactor(createDTO.getPriorityFactor() != null ? createDTO.getPriorityFactor() : getNextPriorityFactor())
                .isActive(createDTO.getIsActive() != null ? createDTO.getIsActive() : true)
                .build();

            // Save category
            category = paxNationCategoryRepository.save(category);

            log.info("Pax nation category created successfully: {}", category.getName());

            // Convert to DTO
            PaxNationCategoryDTO categoryDTO = convertToDTO(category);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                    201,
                    "Pax nation category created successfully",
                    categoryDTO
                )
            );

        } catch (Exception e) {
            log.error("Error creating pax nation category", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to create pax nation category: " + e.getMessage(),
                    "PAX_NATION_CATEGORY_CREATE_FAILED"
                )
            );
        }
    }

    /**
     * Validate input fields for category creation
     */
    private ResponseEntity<ApiResponse<?>> validateInputFields(CreatePaxNationCategoryDTO dto) {
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

        // Validate priority factor if provided
        if (dto.getPriorityFactor() != null && dto.getPriorityFactor() < 1) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Priority factor must be at least 1", "INVALID_PRIORITY_FACTOR")
            );
        }

        return null; // No validation errors
    }

    /**
     * Get the next available priority factor
     */
    private Integer getNextPriorityFactor() {
        return (int) paxNationCategoryRepository.count() + 1;
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
