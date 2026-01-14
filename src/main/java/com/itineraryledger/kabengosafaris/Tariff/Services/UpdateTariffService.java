package com.itineraryledger.kabengosafaris.Tariff.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Tariff.DTOs.TariffDTO;
import com.itineraryledger.kabengosafaris.Tariff.DTOs.UpdateTariffDTO;
import com.itineraryledger.kabengosafaris.Tariff.Repositories.TariffRepository;
import com.itineraryledger.kabengosafaris.Tariff.Tariff;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * UpdateTariffService - Service for updating tariffs
 *
 * Handles tariff updates with validation for:
 * - Name uniqueness (case-insensitive, excluding current tariff)
 * - Slug uniqueness (excluding current tariff)
 * - At least one field must be provided for update
 */
@Service
@Slf4j
public class UpdateTariffService {

    private final TariffRepository tariffRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public UpdateTariffService(
        TariffRepository tariffRepository,
        IdObfuscator idObfuscator
    ) {
        this.tariffRepository = tariffRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Update an existing tariff
     *
     * @param id The obfuscated tariff ID
     * @param updateDTO The updated tariff data
     * @return ResponseEntity with ApiResponse containing the updated tariff
     */
    @Transactional
    @AuditLogAnnotation(
        action = "UPDATE_TARIFF",
        description = "Updating a tariff",
        entityType = "Tariff"
    )
    public ResponseEntity<ApiResponse<?>> updateTariff(String id, UpdateTariffDTO updateDTO) {
        log.info("Updating tariff: {}", id);

        try {
            // Decode ID
            Long decodedId = idObfuscator.decodeId(id);
            if (decodedId == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid tariff ID", "INVALID_TARIFF_ID")
                );
            }

            // Find existing tariff
            Optional<Tariff> tariffOpt = tariffRepository.findById(decodedId);
            if (tariffOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Tariff not found", "TARIFF_NOT_FOUND")
                );
            }

            Tariff tariff = tariffOpt.get();

            // Check if at least one field is provided for update
            if (!hasFieldsToUpdate(updateDTO)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "At least one field must be provided for update", "NO_FIELDS_TO_UPDATE")
                );
            }

            // Validate input fields
            ResponseEntity<ApiResponse<?>> validationError = validateUpdateInput(updateDTO, decodedId);
            if (validationError != null) {
                return validationError;
            }

            // Update fields
            if (updateDTO.getName() != null) {
                tariff.setName(updateDTO.getName().trim());
            }
            if (updateDTO.getSlug() != null) {
                tariff.setSlug(updateDTO.getSlug().trim());
            }
            if (updateDTO.getChargingBasis() != null) {
                tariff.setChargingBasis(updateDTO.getChargingBasis());
            }
            if (updateDTO.getDescription() != null) {
                tariff.setDescription(updateDTO.getDescription());
            }
            if (updateDTO.getInternalNotes() != null) {
                tariff.setInternalNotes(updateDTO.getInternalNotes());
            }
            if (updateDTO.getIsActive() != null) {
                tariff.setIsActive(updateDTO.getIsActive());
            }

            // Save updated tariff
            tariff = tariffRepository.save(tariff);

            log.info("Tariff updated successfully: {}", tariff.getName());

            // Convert to DTO
            TariffDTO tariffDTO = convertToDTO(tariff);

            return ResponseEntity.ok(
                ApiResponse.success(
                    200,
                    "Tariff updated successfully",
                    tariffDTO
                )
            );

        } catch (Exception e) {
            log.error("Error updating tariff", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to update tariff: " + e.getMessage(),
                    "TARIFF_UPDATE_FAILED"
                )
            );
        }
    }

    /**
     * Check if there are fields to update
     */
    private boolean hasFieldsToUpdate(UpdateTariffDTO dto) {
        return dto.getName() != null ||
               dto.getSlug() != null ||
               dto.getChargingBasis() != null ||
               dto.getDescription() != null ||
               dto.getInternalNotes() != null ||
               dto.getIsActive() != null;
    }

    /**
     * Validate update input fields
     */
    private ResponseEntity<ApiResponse<?>> validateUpdateInput(UpdateTariffDTO dto, Long excludeId) {
        // Validate name if provided
        if (dto.getName() != null) {
            if (dto.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Tariff name cannot be empty", "INVALID_NAME")
                );
            }

            String trimmedName = dto.getName().trim();
            if (trimmedName.length() > 150) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Tariff name cannot exceed 150 characters", "NAME_TOO_LONG")
                );
            }

            // Check for duplicate name (excluding current tariff)
            if (tariffRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, excludeId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Tariff with this name already exists", "DUPLICATE_TARIFF_NAME")
                );
            }
        }

        // Validate slug if provided
        if (dto.getSlug() != null && !dto.getSlug().trim().isEmpty()) {
            String trimmedSlug = dto.getSlug().trim();
            if (trimmedSlug.length() > 200) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Slug cannot exceed 200 characters", "SLUG_TOO_LONG")
                );
            }

            // Check for duplicate slug (excluding current tariff)
            if (tariffRepository.existsBySlugAndIdNot(trimmedSlug, excludeId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Tariff with this slug already exists", "DUPLICATE_SLUG")
                );
            }
        }

        return null; // No validation errors
    }

    /**
     * Convert Tariff entity to DTO
     */
    private TariffDTO convertToDTO(Tariff tariff) {
        return TariffDTO.builder()
            .id(idObfuscator.encodeId(tariff.getId()))
            .name(tariff.getName())
            .slug(tariff.getSlug())
            .chargingBasis(tariff.getChargingBasis())
            .chargingBasisDisplayName(tariff.getChargingBasisDisplay())
            .description(tariff.getDescription())
            .requiresAgeCategory(tariff.requiresAgeCategory())
            .isActive(tariff.getIsActive())
            .isSystem(tariff.getIsSystem())
            .createdAt(tariff.getCreatedAt())
            .updatedAt(tariff.getUpdatedAt())
            .build();
    }
}
