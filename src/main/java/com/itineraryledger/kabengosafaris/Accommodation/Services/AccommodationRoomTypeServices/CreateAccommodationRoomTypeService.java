package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationRoomTypeServices;

import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationRoomTypeDTOs.AccommodationRoomTypeDTO;
import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationRoomTypeDTOs.CreateAccommodationRoomTypeDTO;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRoomType;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRepository;
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
 * CreateAccommodationRoomTypeService - Service for creating accommodation room types
 */
@Service
@Slf4j
@Transactional
public class CreateAccommodationRoomTypeService {

    private final AccommodationRoomTypeRepository roomTypeRepository;
    private final AccommodationRepository accommodationRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public CreateAccommodationRoomTypeService(
        AccommodationRoomTypeRepository roomTypeRepository,
        AccommodationRepository accommodationRepository,
        IdObfuscator idObfuscator
    ) {
        this.roomTypeRepository = roomTypeRepository;
        this.accommodationRepository = accommodationRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Create a new accommodation room type
     *
     * @param createDTO The room type data
     * @return ResponseEntity with ApiResponse containing the created room type
     */
    @AuditLogAnnotation(
        action = "CREATE_ACCOMMODATION_ROOM_TYPE",
        description = "Creating accommodation room type",
        entityType = "AccommodationRoomType"
    )
    public ResponseEntity<ApiResponse<?>> createAccommodationRoomType(CreateAccommodationRoomTypeDTO createDTO) {
        log.info("Creating new accommodation room type: {}", createDTO.getName());

        try {
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

            // Check for duplicate room type name for this accommodation
            if (roomTypeRepository.existsByAccommodationIdAndName(accommodationId, createDTO.getName())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Room type name already exists for this accommodation",
                        "DUPLICATE_ROOM_TYPE_NAME"
                    )
                );
            }

            // Create room type entity
            AccommodationRoomType roomType = AccommodationRoomType.builder()
                .accommodation(accommodation)
                .name(createDTO.getName())
                .bedConfiguration(createDTO.getBedConfiguration())
                .maxOccupancy(createDTO.getMaxOccupancy())
                .minOccupancy(createDTO.getMinOccupancy() != null ? createDTO.getMinOccupancy() : 1)
                .description(createDTO.getDescription())
                .isActive(createDTO.getIsActive() != null ? createDTO.getIsActive() : true)
                .build();

            // Save room type
            roomType = roomTypeRepository.save(roomType);

            // Convert to DTO
            AccommodationRoomTypeDTO roomTypeDTO = convertToDTO(roomType);

            log.info("Accommodation room type created successfully: {}", roomType.getName());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                    201,
                    "Accommodation room type created successfully",
                    roomTypeDTO
                )
            );

        } catch (Exception e) {
            log.error("Error creating accommodation room type", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to create accommodation room type",
                    "ACCOMMODATION_ROOM_TYPE_CREATE_FAILED"
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
