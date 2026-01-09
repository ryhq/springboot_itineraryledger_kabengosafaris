package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationRoomTypeServices;

import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationRoomTypeDTOs.AccommodationRoomTypeDTO;
import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationRoomTypeDTOs.UpdateAccommodationRoomTypeDTO;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRoomType;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRoomTypeRepository;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UpdateAccommodationRoomTypeService - Service for updating accommodation room types
 */
@Service
@Slf4j
@Transactional
public class UpdateAccommodationRoomTypeService {

    private final AccommodationRoomTypeRepository roomTypeRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public UpdateAccommodationRoomTypeService(
        AccommodationRoomTypeRepository roomTypeRepository,
        IdObfuscator idObfuscator
    ) {
        this.roomTypeRepository = roomTypeRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Update an existing accommodation room type
     *
     * @param idObfuscated The obfuscated room type ID
     * @param updateDTO The update data
     * @return ResponseEntity with ApiResponse containing the updated room type
     */
    @AuditLogAnnotation(
        action = "UPDATE_ACCOMMODATION_ROOM_TYPE",
        description = "Updating accommodation room type",
        entityType = "AccommodationRoomType"
    )
    public ResponseEntity<ApiResponse<?>> updateAccommodationRoomType(
        String idObfuscated,
        UpdateAccommodationRoomTypeDTO updateDTO
    ) {
        log.info("Updating accommodation room type: {}", idObfuscated);

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

            // Decode room type ID
            Long roomTypeId;
            try {
                roomTypeId = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode room type ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid room type ID",
                        "INVALID_ROOM_TYPE_ID"
                    )
                );
            }

            // Find existing room type
            AccommodationRoomType roomType = roomTypeRepository.findById(roomTypeId).orElse(null);
            if (roomType == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(
                        404,
                        "Room type not found",
                        "ROOM_TYPE_NOT_FOUND"
                    )
                );
            }

            // Validate occupancy logic against existing entity
            Integer finalMinOccupancy = updateDTO.getMinOccupancy() != null
                ? updateDTO.getMinOccupancy()
                : roomType.getMinOccupancy();
            Integer finalMaxOccupancy = updateDTO.getMaxOccupancy() != null
                ? updateDTO.getMaxOccupancy()
                : roomType.getMaxOccupancy();

            if (finalMinOccupancy != null && finalMaxOccupancy != null && finalMinOccupancy > finalMaxOccupancy) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Min occupancy cannot be greater than max occupancy",
                        "INVALID_OCCUPANCY_LOGIC"
                    )
                );
            }

            // Check for duplicate name if name is being updated
            if (updateDTO.getName() != null && !updateDTO.getName().equals(roomType.getName())) {
                Long accommodationId = roomType.getAccommodation().getId();
                if (roomTypeRepository.existsByAccommodationIdAndNameExcludingId(accommodationId, updateDTO.getName(), roomTypeId)) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                            400,
                            "Room type name already exists for this accommodation",
                            "DUPLICATE_ROOM_TYPE_NAME"
                        )
                    );
                }
                roomType.setName(updateDTO.getName());
            }

            // Update other fields if provided
            if (updateDTO.getBedConfiguration() != null) {
                roomType.setBedConfiguration(updateDTO.getBedConfiguration());
            }
            if (updateDTO.getMaxOccupancy() != null) {
                roomType.setMaxOccupancy(updateDTO.getMaxOccupancy());
            }
            if (updateDTO.getMinOccupancy() != null) {
                roomType.setMinOccupancy(updateDTO.getMinOccupancy());
            }
            if (updateDTO.getDescription() != null) {
                roomType.setDescription(updateDTO.getDescription());
            }
            if (updateDTO.getIsActive() != null) {
                roomType.setIsActive(updateDTO.getIsActive());
            }

            // Save updated room type
            roomType = roomTypeRepository.save(roomType);

            // Convert to DTO
            AccommodationRoomTypeDTO roomTypeDTO = convertToDTO(roomType);

            log.info("Accommodation room type updated successfully: {}", roomType.getName());

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Accommodation room type updated successfully",
                    roomTypeDTO
                )
            );

        } catch (Exception e) {
            log.error("Error updating accommodation room type", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to update accommodation room type",
                    "ACCOMMODATION_ROOM_TYPE_UPDATE_FAILED"
                )
            );
        }
    }

    /**
     * Validate that at least one field is provided for update
     */
    private ResponseEntity<ApiResponse<?>> validateAtLeastOneFieldProvided(UpdateAccommodationRoomTypeDTO dto) {
        boolean hasUpdate =
            dto.getName() != null ||
            dto.getBedConfiguration() != null ||
            dto.getMaxOccupancy() != null ||
            dto.getMinOccupancy() != null ||
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
     * Validate input fields for room type update
     */
    private ResponseEntity<ApiResponse<?>> validateInputFields(UpdateAccommodationRoomTypeDTO dto) {
        // Validate and sanitize name if provided
        if (dto.getName() != null) {
            String trimmedName = dto.getName().trim();
            if (trimmedName.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Room type name cannot be empty", "INVALID_NAME")
                );
            }
            if (trimmedName.length() > 100) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Room type name cannot exceed 100 characters", "NAME_TOO_LONG")
                );
            }
            dto.setName(trimmedName);
        }

        // Validate bedConfiguration length if provided
        if (dto.getBedConfiguration() != null && dto.getBedConfiguration().length() > 100) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Bed configuration cannot exceed 100 characters", "BED_CONFIGURATION_TOO_LONG")
            );
        }

        // Validate maxOccupancy if provided
        if (dto.getMaxOccupancy() != null && dto.getMaxOccupancy() < 1) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Max occupancy must be at least 1", "INVALID_MAX_OCCUPANCY")
            );
        }

        if (dto.getMaxOccupancy() != null && dto.getMaxOccupancy() > 50) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Max occupancy cannot exceed 50", "MAX_OCCUPANCY_TOO_HIGH")
            );
        }

        // Validate minOccupancy if provided
        if (dto.getMinOccupancy() != null && dto.getMinOccupancy() < 1) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Min occupancy must be at least 1", "INVALID_MIN_OCCUPANCY")
            );
        }

        if (dto.getMinOccupancy() != null && dto.getMinOccupancy() > 50) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Min occupancy cannot exceed 50", "MIN_OCCUPANCY_TOO_HIGH")
            );
        }

        // Validate occupancy logic if both are being updated
        if (dto.getMinOccupancy() != null && dto.getMaxOccupancy() != null) {
            if (dto.getMinOccupancy() > dto.getMaxOccupancy()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Min occupancy cannot be greater than max occupancy", "INVALID_OCCUPANCY_LOGIC")
                );
            }
        }

        return null; // No validation errors
    }

    /**
     * Convert AccommodationRoomType entity to DTO
     */
    private AccommodationRoomTypeDTO convertToDTO(AccommodationRoomType roomType) {
        return AccommodationRoomTypeDTO.builder()
            .id(idObfuscator.encodeId(roomType.getId()))
            .accommodationId(idObfuscator.encodeId(roomType.getAccommodation().getId()))
            .accommodationName(roomType.getAccommodation().getName())
            .name(roomType.getName())
            .bedConfiguration(roomType.getBedConfiguration())
            .maxOccupancy(roomType.getMaxOccupancy())
            .minOccupancy(roomType.getMinOccupancy())
            .description(roomType.getDescription())
            .isActive(roomType.getIsActive())
            .createdAt(roomType.getCreatedAt())
            .updatedAt(roomType.getUpdatedAt())
            .build();
    }
}
