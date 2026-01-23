package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.Services;

import java.util.List;

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
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryPax.Entity.ItineraryPax;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryPax.Repository.ItineraryPaxRepository;
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
    private final ItineraryPaxRepository paxRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ItineraryDayAccommodationCreateService(
        ItineraryDayRepository itineraryDayRepository,
        ItineraryDayAccommodationRepository accommodationRepository,
        AccommodationRepository baseAccommodationRepository,
        AccommodationRoomTypeRepository roomTypeRepository,
        AccommodationRoomStandardRepository roomStandardRepository,
        AccommodationBoardTypeRepository boardTypeRepository,
        ItineraryPaxRepository paxRepository,
        IdObfuscator idObfuscator
    ) {
        this.itineraryDayRepository = itineraryDayRepository;
        this.accommodationRepository = accommodationRepository;
        this.baseAccommodationRepository = baseAccommodationRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.roomStandardRepository = roomStandardRepository;
        this.boardTypeRepository = boardTypeRepository;
        this.paxRepository = paxRepository;
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

            // Find required room configuration entities
            Long roomTypeId = idObfuscator.decodeId(createDTO.getRoomTypeId());
            AccommodationRoomType roomType = roomTypeRepository.findById(roomTypeId).orElse(null);
            if (roomType == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Room type not found", "ROOM_TYPE_NOT_FOUND")
                );
            }

            Long roomStandardId = idObfuscator.decodeId(createDTO.getRoomStandardId());
            AccommodationRoomStandard roomStandard = roomStandardRepository.findById(roomStandardId).orElse(null);
            if (roomStandard == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Room standard not found", "ROOM_STANDARD_NOT_FOUND")
                );
            }

            Long boardTypeId = idObfuscator.decodeId(createDTO.getBoardTypeId());
            AccommodationBoardType boardType = boardTypeRepository.findById(boardTypeId).orElse(null);
            if (boardType == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Board type not found", "BOARD_TYPE_NOT_FOUND")
                );
            }

            // Create accommodation entry
            int roomCount = createDTO.getRoomCount() != null ? createDTO.getRoomCount() : 1;
            boolean isAlternative = createDTO.getIsAlternative() != null ? createDTO.getIsAlternative() : false;

            ItineraryDayAccommodation dayAccommodation = ItineraryDayAccommodation.builder()
                .itineraryDay(day)
                .accommodation(accommodation)
                .roomType(roomType)
                .roomStandard(roomStandard)
                .boardType(boardType)
                .roomCount(roomCount)
                .isAlternative(isAlternative)
                .notes(createDTO.getNotes())
                .build();

            // Validate pax capacity if this is a primary (non-alternative) accommodation
            if (!isAlternative) {
                ResponseEntity<ApiResponse<?>> capacityError = validatePaxCapacity(
                    itineraryId, dayId, dayAccommodation);
                if (capacityError != null) {
                    return capacityError;
                }
            }

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

        dto.setRoomTypeId(idObfuscator.encodeId(entity.getRoomType().getId()));
        dto.setRoomTypeName(entity.getRoomType().getName());
        dto.setRoomStandardId(idObfuscator.encodeId(entity.getRoomStandard().getId()));
        dto.setRoomStandardName(entity.getRoomStandard().getName());
        dto.setBoardTypeId(idObfuscator.encodeId(entity.getBoardType().getId()));
        dto.setBoardTypeName(entity.getBoardType().getName());

        dto.setRoomCount(entity.getRoomCount());
        dto.setIsAlternative(entity.getIsAlternative());
        dto.setNotes(entity.getNotes());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    /**
     * Validate that primary accommodations don't exceed reasonable capacity for total pax.
     * Prevents overbooking (booking more rooms than needed for passengers).
     *
     * Note: Underbooking validation (capacity < pax) is NOT checked here because
     * users may need to add multiple accommodations to reach required capacity.
     * Underbooking should be validated at itinerary level (e.g., when publishing).
     *
     * @param itineraryId The itinerary ID
     * @param dayId The day ID
     * @param newAccommodation The new accommodation being created
     * @return Error response if validation fails, null if valid
     */
    private ResponseEntity<ApiResponse<?>> validatePaxCapacity(
        Long itineraryId,
        Long dayId,
        ItineraryDayAccommodation newAccommodation
    ) {
        // Get total pax count for the itinerary
        List<ItineraryPax> paxList = paxRepository.findByItineraryId(itineraryId);
        int totalPax = paxList.stream()
            .mapToInt(pax -> pax.getCount() != null ? pax.getCount() : 0)
            .sum();

        // If no pax configured, skip validation
        if (totalPax == 0) {
            return null;
        }

        // Get existing primary accommodations for this day
        List<ItineraryDayAccommodation> existingPrimaryAccommodations =
            accommodationRepository.findByItineraryDayIdAndIsAlternativeFalse(dayId);

        // Calculate total minimum occupancy including the new accommodation
        int totalMinOccupancy = 0;

        // Add min occupancy from existing accommodations
        for (ItineraryDayAccommodation acc : existingPrimaryAccommodations) {
            int roomCount = acc.getRoomCount() != null ? acc.getRoomCount() : 1;
            Integer minOccupancy = acc.getRoomType().getMinOccupancy();
            if (minOccupancy != null) {
                totalMinOccupancy += roomCount * minOccupancy;
            }
        }

        // Add min occupancy from new accommodation
        int newRoomCount = newAccommodation.getRoomCount() != null ? newAccommodation.getRoomCount() : 1;
        Integer newMinOccupancy = newAccommodation.getRoomType().getMinOccupancy();
        if (newMinOccupancy != null) {
            totalMinOccupancy += newRoomCount * newMinOccupancy;
        }

        // Validate against overbooking (minimum occupancy exceeds pax count)
        if (totalMinOccupancy > totalPax) {
            String message = String.format(
                "Accommodation overbooking detected. Total pax: %d, Minimum room occupancy required: %d. " +
                "You are booking more rooms than needed for your passengers. Please reduce room count.",
                totalPax, totalMinOccupancy);
            log.warn("Overbooking validation failed: {}", message);
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, message, "ACCOMMODATION_OVERBOOKING")
            );
        }

        return null;
    }
}
