package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationBoardType;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRoomStandard;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRoomType;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationBoardTypeRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRoomStandardRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRoomTypeRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Entity.ItineraryDay;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Repository.ItineraryDayRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.Entity.ItineraryDayAccommodation;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.Repository.ItineraryDayAccommodationRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.DTOs.CreateItineraryDayAccommodationDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.DTOs.ItineraryDayAccommodationDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryDayAccommodationCreateService - Service for creating itinerary day accommodations
 */
@Service
@Slf4j
@Transactional
public class ItineraryDayAccommodationCreateService {

    private final ItineraryDayRepository itineraryDayRepository;
    private final ItineraryDayAccommodationRepository accommodationRepository;
    private final AccommodationRepository baseAccommodationRepository;
    private final AccommodationRoomTypeRepository roomTypeRepository;
    private final AccommodationRoomStandardRepository roomStandardRepository;
    private final AccommodationBoardTypeRepository boardTypeRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ItineraryDayAccommodationCreateService(
        ItineraryDayRepository itineraryDayRepository,
        ItineraryDayAccommodationRepository accommodationRepository,
        AccommodationRepository baseAccommodationRepository,
        AccommodationRoomTypeRepository roomTypeRepository,
        AccommodationRoomStandardRepository roomStandardRepository,
        AccommodationBoardTypeRepository boardTypeRepository,
        IdObfuscator idObfuscator
    ) {
        this.itineraryDayRepository = itineraryDayRepository;
        this.accommodationRepository = accommodationRepository;
        this.baseAccommodationRepository = baseAccommodationRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.roomStandardRepository = roomStandardRepository;
        this.boardTypeRepository = boardTypeRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Create a new accommodation for an itinerary day
     *
     * @param itineraryIdObfuscated The obfuscated itinerary ID
     * @param dayIdObfuscated The obfuscated day ID
     * @param createDTO The accommodation data
     * @return ResponseEntity with ApiResponse containing the created accommodation
     */
    @AuditLogAnnotation(action = "CREATE_ITINERARY_DAY_ACCOMMODATION", description = "Creating accommodation for itinerary day", entityType = "ItineraryDayAccommodation")
    public ResponseEntity<ApiResponse<?>> createItineraryDayAccommodation(
        String itineraryIdObfuscated,
        String dayIdObfuscated,
        CreateItineraryDayAccommodationDTO createDTO
    ) {
        log.info("Creating accommodation for day: {}", dayIdObfuscated);

        try {
            // Decode IDs
            Long itineraryId;
            Long dayId;
            Long accommodationId;
            try {
                itineraryId = idObfuscator.decodeId(itineraryIdObfuscated);
                dayId = idObfuscator.decodeId(dayIdObfuscated);
                accommodationId = idObfuscator.decodeId(createDTO.getAccommodationId());
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid ID", "INVALID_ID")
                );
            }

            // Find day
            ItineraryDay day = itineraryDayRepository.findById(dayId).orElse(null);
            if (day == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Itinerary day not found", "ITINERARY_DAY_NOT_FOUND")
                );
            }

            // Verify day belongs to the itinerary
            if (!day.getItinerary().getId().equals(itineraryId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Day does not belong to this itinerary", "DAY_ITINERARY_MISMATCH")
                );
            }

            // Find accommodation
            Accommodation accommodation = baseAccommodationRepository.findById(accommodationId).orElse(null);
            if (accommodation == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Accommodation not found", "ACCOMMODATION_NOT_FOUND")
                );
            }

            // Find optional entities
            AccommodationRoomType roomType = null;
            if (createDTO.getRoomTypeId() != null && !createDTO.getRoomTypeId().isEmpty()) {
                Long roomTypeId = idObfuscator.decodeId(createDTO.getRoomTypeId());
                roomType = roomTypeRepository.findById(roomTypeId).orElse(null);
            }

            AccommodationRoomStandard roomStandard = null;
            if (createDTO.getRoomStandardId() != null && !createDTO.getRoomStandardId().isEmpty()) {
                Long roomStandardId = idObfuscator.decodeId(createDTO.getRoomStandardId());
                roomStandard = roomStandardRepository.findById(roomStandardId).orElse(null);
            }

            AccommodationBoardType boardType = null;
            if (createDTO.getBoardTypeId() != null && !createDTO.getBoardTypeId().isEmpty()) {
                Long boardTypeId = idObfuscator.decodeId(createDTO.getBoardTypeId());
                boardType = boardTypeRepository.findById(boardTypeId).orElse(null);
            }

            // Create accommodation entry
            ItineraryDayAccommodation dayAccommodation = ItineraryDayAccommodation.builder()
                .itineraryDay(day)
                .accommodation(accommodation)
                .roomType(roomType)
                .roomStandard(roomStandard)
                .boardType(boardType)
                .roomCount(createDTO.getRoomCount() != null ? createDTO.getRoomCount() : 1)
                .nights(createDTO.getNights() != null ? createDTO.getNights() : 1)
                .isAlternative(createDTO.getIsAlternative() != null ? createDTO.getIsAlternative() : false)
                .notes(createDTO.getNotes())
                .build();

            // Save
            dayAccommodation = accommodationRepository.save(dayAccommodation);

            // Convert to DTO
            ItineraryDayAccommodationDTO dto = convertToDTO(dayAccommodation);

            log.info("Accommodation created: {} for day {}", accommodation.getName(), day.getDayNumber());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, "Accommodation created successfully", dto)
            );

        } catch (Exception e) {
            log.error("Error creating accommodation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to create accommodation", "ACCOMMODATION_CREATE_FAILED")
            );
        }
    }

    /**
     * Convert entity to DTO
     */
    private ItineraryDayAccommodationDTO convertToDTO(ItineraryDayAccommodation entity) {
        ItineraryDayAccommodationDTO dto = new ItineraryDayAccommodationDTO();
        dto.setId(idObfuscator.encodeId(entity.getId()));
        dto.setItineraryDayId(idObfuscator.encodeId(entity.getItineraryDay().getId()));
        dto.setAccommodationId(idObfuscator.encodeId(entity.getAccommodation().getId()));
        dto.setAccommodationName(entity.getAccommodation().getName());
        dto.setAccommodationSlug(entity.getAccommodation().getSlug());

        if (entity.getRoomType() != null) {
            dto.setRoomTypeId(idObfuscator.encodeId(entity.getRoomType().getId()));
            dto.setRoomTypeName(entity.getRoomType().getName());
        }
        if (entity.getRoomStandard() != null) {
            dto.setRoomStandardId(idObfuscator.encodeId(entity.getRoomStandard().getId()));
            dto.setRoomStandardName(entity.getRoomStandard().getName());
        }
        if (entity.getBoardType() != null) {
            dto.setBoardTypeId(idObfuscator.encodeId(entity.getBoardType().getId()));
            dto.setBoardTypeName(entity.getBoardType().getName());
        }

        dto.setRoomCount(entity.getRoomCount());
        dto.setNights(entity.getNights());
        dto.setIsAlternative(entity.getIsAlternative());
        dto.setNotes(entity.getNotes());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
