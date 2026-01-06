package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationRoomStandardServices;

import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationRoomStandardDTOs.AccommodationRoomStandardDTO;
import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationRoomStandardDTOs.UpdateAccommodationRoomStandardDTO;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRoomStandard;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRoomStandardRepository;
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
 * UpdateAccommodationRoomStandardService - Service for updating accommodation room standards
 */
@Service
@Slf4j
@Transactional
public class UpdateAccommodationRoomStandardService {

    private final AccommodationRoomStandardRepository roomStandardRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public UpdateAccommodationRoomStandardService(
        AccommodationRoomStandardRepository roomStandardRepository,
        IdObfuscator idObfuscator
    ) {
        this.roomStandardRepository = roomStandardRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Update an existing accommodation room standard
     *
     * @param idObfuscated The obfuscated room standard ID
     * @param updateDTO The update data
     * @return ResponseEntity with ApiResponse containing the updated room standard
     */
    @AuditLogAnnotation(
        action = "UPDATE_ACCOMMODATION_ROOM_STANDARD",
        description = "Updating accommodation room standard",
        entityType = "AccommodationRoomStandard"
    )
    public ResponseEntity<ApiResponse<?>> updateAccommodationRoomStandard(
        String idObfuscated,
        UpdateAccommodationRoomStandardDTO updateDTO
    ) {
        log.info("Updating accommodation room standard: {}", idObfuscated);

        try {
            // Decode room standard ID
            Long roomStandardId;
            try {
                roomStandardId = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode room standard ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid room standard ID",
                        "INVALID_ROOM_STANDARD_ID"
                    )
                );
            }

            // Find existing room standard
            AccommodationRoomStandard roomStandard = roomStandardRepository.findById(roomStandardId).orElse(null);
            if (roomStandard == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(
                        404,
                        "Room standard not found",
                        "ROOM_STANDARD_NOT_FOUND"
                    )
                );
            }

            // Check for duplicate name if name is being updated
            if (updateDTO.getName() != null && !updateDTO.getName().equals(roomStandard.getName())) {
                Long accommodationId = roomStandard.getAccommodation().getId();
                if (roomStandardRepository.existsByAccommodationIdAndNameExcludingId(accommodationId, updateDTO.getName(), roomStandardId)) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                            400,
                            "Room standard name already exists for this accommodation",
                            "DUPLICATE_ROOM_STANDARD_NAME"
                        )
                    );
                }
                roomStandard.setName(updateDTO.getName());
            }

            // Update other fields if provided
            if (updateDTO.getDescription() != null) {
                roomStandard.setDescription(updateDTO.getDescription());
            }
            if (updateDTO.getMaxOccupancy() != null) {
                roomStandard.setMaxOccupancy(updateDTO.getMaxOccupancy());
            }
            if (updateDTO.getAmenities() != null) {
                roomStandard.setAmenities(updateDTO.getAmenities());
            }
            if (updateDTO.getViewType() != null) {
                roomStandard.setViewType(updateDTO.getViewType());
            }
            if (updateDTO.getFloorLevel() != null) {
                roomStandard.setFloorLevel(updateDTO.getFloorLevel());
            }
            if (updateDTO.getIsActive() != null) {
                roomStandard.setIsActive(updateDTO.getIsActive());
            }

            // Save updated room standard
            roomStandard = roomStandardRepository.save(roomStandard);

            // Convert to DTO
            AccommodationRoomStandardDTO roomStandardDTO = convertToDTO(roomStandard);

            log.info("Accommodation room standard updated successfully: {}", roomStandard.getName());

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Accommodation room standard updated successfully",
                    roomStandardDTO
                )
            );

        } catch (Exception e) {
            log.error("Error updating accommodation room standard", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to update accommodation room standard",
                    "ACCOMMODATION_ROOM_STANDARD_UPDATE_FAILED"
                )
            );
        }
    }

    /**
     * Convert AccommodationRoomStandard entity to DTO
     */
    private AccommodationRoomStandardDTO convertToDTO(AccommodationRoomStandard roomStandard) {
        return AccommodationRoomStandardDTO.builder()
            .id(idObfuscator.encodeId(roomStandard.getId()))
            .accommodationId(idObfuscator.encodeId(roomStandard.getAccommodation().getId()))
            .accommodationName(roomStandard.getAccommodation().getName())
            .name(roomStandard.getName())
            .description(roomStandard.getDescription())
            .maxOccupancy(roomStandard.getMaxOccupancy())
            .amenities(roomStandard.getAmenities())
            .viewType(roomStandard.getViewType())
            .floorLevel(roomStandard.getFloorLevel())
            .isActive(roomStandard.getIsActive())
            .createdAt(roomStandard.getCreatedAt())
            .updatedAt(roomStandard.getUpdatedAt())
            .build();
    }
}
