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
