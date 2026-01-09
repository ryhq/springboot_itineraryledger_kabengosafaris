package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationRoomStandardServices;

import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationRoomStandardDTOs.AccommodationRoomStandardDTO;
import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationRoomStandardDTOs.CreateAccommodationRoomStandardDTO;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRoomStandard;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRepository;
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
 * CreateAccommodationRoomStandardService - Service for creating accommodation room standards
 */
@Service
@Slf4j
@Transactional
public class CreateAccommodationRoomStandardService {

    private final AccommodationRoomStandardRepository roomStandardRepository;
    private final AccommodationRepository accommodationRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public CreateAccommodationRoomStandardService(
        AccommodationRoomStandardRepository roomStandardRepository,
        AccommodationRepository accommodationRepository,
        IdObfuscator idObfuscator
    ) {
        this.roomStandardRepository = roomStandardRepository;
        this.accommodationRepository = accommodationRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Create a new accommodation room standard
     *
     * @param createDTO The room standard data
     * @return ResponseEntity with ApiResponse containing the created room standard
     */
    @AuditLogAnnotation(
        action = "CREATE_ACCOMMODATION_ROOM_STANDARD",
        description = "Creating accommodation room standard",
        entityType = "AccommodationRoomStandard"
    )
    public ResponseEntity<ApiResponse<?>> createAccommodationRoomStandard(CreateAccommodationRoomStandardDTO createDTO) {
        log.info("Creating new accommodation room standard: {}", createDTO.getName());

        try {
            // Validate input fields
            ResponseEntity<ApiResponse<?>> validationError = validateInputFields(createDTO);
            if (validationError != null) {
                return validationError;
            }

            // Decode accommodation ID
            Long accommodationId;
            try {
                accommodationId = idObfuscator.decodeId(createDTO.getAccommodationId());
            } catch (Exception e) {
                log.warn("Failed to decode accommodation ID: {}", createDTO.getAccommodationId(), e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid accommodation ID",
                        "INVALID_ACCOMMODATION_ID"
                    )
                );
            }

            // Check if accommodation exists
            Accommodation accommodation = accommodationRepository.findById(accommodationId).orElse(null);
            if (accommodation == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(
                        404,
                        "Accommodation not found",
                        "ACCOMMODATION_NOT_FOUND"
                    )
                );
            }

            // Check for duplicate room standard name for this accommodation
            if (roomStandardRepository.existsByAccommodationIdAndName(accommodationId, createDTO.getName())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Room standard name already exists for this accommodation",
                        "DUPLICATE_ROOM_STANDARD_NAME"
                    )
                );
            }

            // Create room standard entity
            AccommodationRoomStandard roomStandard = AccommodationRoomStandard.builder()
                .accommodation(accommodation)
                .name(createDTO.getName())
                .description(createDTO.getDescription())
                .maxOccupancy(createDTO.getMaxOccupancy())
                .amenities(createDTO.getAmenities())
                .viewType(createDTO.getViewType())
                .floorLevel(createDTO.getFloorLevel())
                .isActive(createDTO.getIsActive() != null ? createDTO.getIsActive() : true)
                .build();

            // Save room standard
            roomStandard = roomStandardRepository.save(roomStandard);

            // Convert to DTO
            AccommodationRoomStandardDTO roomStandardDTO = convertToDTO(roomStandard);

            log.info("Accommodation room standard created successfully: {}", roomStandard.getName());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                    201,
                    "Accommodation room standard created successfully",
                    roomStandardDTO
                )
            );

        } catch (Exception e) {
            log.error("Error creating accommodation room standard", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to create accommodation room standard",
                    "ACCOMMODATION_ROOM_STANDARD_CREATE_FAILED"
                )
            );
        }
    }

    /**
     * Validate input fields for room standard creation
     */
    private ResponseEntity<ApiResponse<?>> validateInputFields(CreateAccommodationRoomStandardDTO dto) {
        // Validate and sanitize name
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Room standard name cannot be empty", "INVALID_NAME")
            );
        }

        String trimmedName = dto.getName().trim();
        if (trimmedName.length() > 100) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Room standard name cannot exceed 100 characters", "NAME_TOO_LONG")
            );
        }
        dto.setName(trimmedName);

        // Validate viewType length if provided
        if (dto.getViewType() != null && dto.getViewType().length() > 100) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "View type cannot exceed 100 characters", "VIEW_TYPE_TOO_LONG")
            );
        }

        // Validate floorLevel length if provided
        if (dto.getFloorLevel() != null && dto.getFloorLevel().length() > 50) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Floor level cannot exceed 50 characters", "FLOOR_LEVEL_TOO_LONG")
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

        return null; // No validation errors
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
