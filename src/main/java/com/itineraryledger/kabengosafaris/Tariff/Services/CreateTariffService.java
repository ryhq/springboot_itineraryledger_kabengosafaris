package com.itineraryledger.kabengosafaris.Tariff.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Tariff.DTOs.CreateTariffDTO;
import com.itineraryledger.kabengosafaris.Tariff.DTOs.TariffDTO;
import com.itineraryledger.kabengosafaris.Tariff.Repositories.TariffRepository;
import com.itineraryledger.kabengosafaris.Tariff.Tariff;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CreateTariffService - Service for creating new tariffs
 *
 * Handles tariff creation with validation for:
 * - Name uniqueness (case-insensitive)
 * - Slug uniqueness
 * - Required fields validation
 */
@Service
@Slf4j
public class CreateTariffService {

    private final TariffRepository tariffRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public CreateTariffService(
        TariffRepository tariffRepository,
        IdObfuscator idObfuscator
    ) {
        this.tariffRepository = tariffRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Create a new tariff
     *
     * @param createDTO The tariff data
     * @return ResponseEntity with ApiResponse containing the created tariff
     */
    @Transactional
    @AuditLogAnnotation(
        action = "CREATE_TARIFF",
        description = "Creating a new tariff",
        entityType = "Tariff"
    )
    public ResponseEntity<ApiResponse<?>> createTariff(CreateTariffDTO createDTO) {
        log.info("Creating tariff: {}", createDTO.getName());

        try {
            // Validate required fields
            ResponseEntity<ApiResponse<?>> validationError = validateCreateInput(createDTO);
            if (validationError != null) {
                return validationError;
            }

            // Build tariff entity
            Tariff tariff = Tariff.builder()
                .name(createDTO.getName().trim())
                .slug(createDTO.getSlug() != null ? createDTO.getSlug().trim() : null)
                .chargingBasis(createDTO.getChargingBasis())
                .description(createDTO.getDescription())
                .internalNotes(createDTO.getInternalNotes())
                .isActive(createDTO.getIsActive() != null ? createDTO.getIsActive() : true)
                .isSystem(false) // User-created tariffs are never system tariffs
                .build();

            // Save tariff
            tariff = tariffRepository.save(tariff);

            log.info("Tariff created successfully: {} (ID: {})", tariff.getName(), tariff.getId());

            // Convert to DTO
            TariffDTO tariffDTO = convertToDTO(tariff);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                    201,
                    "Tariff created successfully",
                    tariffDTO
                )
            );

        } catch (Exception e) {
            log.error("Error creating tariff", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to create tariff: " + e.getMessage(),
                    "TARIFF_CREATE_FAILED"
                )
            );
        }
    }

    /**
     * Validate create input
     */
    private ResponseEntity<ApiResponse<?>> validateCreateInput(CreateTariffDTO dto) {
        // Validate name
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Tariff name is required", "INVALID_NAME")
            );
        }

        String trimmedName = dto.getName().trim();
        if (trimmedName.length() > 150) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Tariff name cannot exceed 150 characters", "NAME_TOO_LONG")
            );
        }

        // Check for duplicate name
        if (tariffRepository.existsByNameIgnoreCase(trimmedName)) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Tariff with this name already exists", "DUPLICATE_TARIFF_NAME")
            );
        }

        // Validate charging basis
        if (dto.getChargingBasis() == null) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Charging basis is required", "INVALID_CHARGING_BASIS")
            );
        }

        // Validate slug if provided
        if (dto.getSlug() != null && !dto.getSlug().trim().isEmpty()) {
            String trimmedSlug = dto.getSlug().trim();
            if (trimmedSlug.length() > 200) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Slug cannot exceed 200 characters", "SLUG_TOO_LONG")
                );
            }
            if (tariffRepository.existsBySlug(trimmedSlug)) {
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
