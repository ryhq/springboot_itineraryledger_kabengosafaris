package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.Services;

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
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.Entity.SafariDay;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.Repository.SafariDayRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.Entity.SafariDayAccommodation;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.Repository.SafariDayAccommodationRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.DTOs.CreateSafariDayAccommodationDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.DTOs.SafariDayAccommodationDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariPax.Entity.SafariPax;
import com.itineraryledger.kabengosafaris.Safari.SafariPax.Repository.SafariPaxRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * SafariDayAccommodationCreateService - Service for creating safari day accommodations
 */
@Service
@Slf4j
@Transactional
public class SafariDayAccommodationCreateService {

    private final SafariDayRepository safariDayRepository;
    private final SafariDayAccommodationRepository accommodationRepository;
    private final AccommodationRepository baseAccommodationRepository;
    private final AccommodationRoomTypeRepository roomTypeRepository;
    private final AccommodationRoomStandardRepository roomStandardRepository;
    private final AccommodationBoardTypeRepository boardTypeRepository;
    private final SafariPaxRepository paxRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public SafariDayAccommodationCreateService(
        SafariDayRepository safariDayRepository,
        SafariDayAccommodationRepository accommodationRepository,
        AccommodationRepository baseAccommodationRepository,
        AccommodationRoomTypeRepository roomTypeRepository,
        AccommodationRoomStandardRepository roomStandardRepository,
        AccommodationBoardTypeRepository boardTypeRepository,
        SafariPaxRepository paxRepository,
        IdObfuscator idObfuscator
    ) {
        this.safariDayRepository = safariDayRepository;
        this.accommodationRepository = accommodationRepository;
        this.baseAccommodationRepository = baseAccommodationRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.roomStandardRepository = roomStandardRepository;
        this.boardTypeRepository = boardTypeRepository;
        this.paxRepository = paxRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Create a new accommodation for a safari day
     *
     * @param safariIdObfuscated The obfuscated safari ID
     * @param dayIdObfuscated The obfuscated day ID
     * @param createDTO The accommodation data
     * @return ResponseEntity with ApiResponse containing the created accommodation
     */
    @AuditLogAnnotation(action = "CREATE_SAFARI_DAY_ACCOMMODATION", description = "Creating accommodation for safari day", entityType = "SafariDayAccommodation")
    public ResponseEntity<ApiResponse<?>> createSafariDayAccommodation(
        String safariIdObfuscated,
        String dayIdObfuscated,
        CreateSafariDayAccommodationDTO createDTO
    ) {
        log.info("Creating accommodation for day: {}", dayIdObfuscated);

        try {
            // Decode IDs
            Long safariId;
            Long dayId;
            Long accommodationId;
            try {
                safariId = idObfuscator.decodeId(safariIdObfuscated);
                dayId = idObfuscator.decodeId(dayIdObfuscated);
                accommodationId = idObfuscator.decodeId(createDTO.getAccommodationId());
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid ID", "INVALID_ID")
                );
            }

            // Find day
            SafariDay day = safariDayRepository.findById(dayId).orElse(null);
            if (day == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Safari day not found", "SAFARI_DAY_NOT_FOUND")
                );
            }

            // Verify day belongs to the safari
            if (!day.getSafari().getId().equals(safariId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Day does not belong to this safari", "DAY_SAFARI_MISMATCH")
                );
            }

            // Check if safari is editable
            Safari safari = day.getSafari();
            if (!safari.isEditable()) {
                log.warn("Safari is not editable: {} (state: {})", safari.getCode(), safari.getState());
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Safari cannot be edited in state: " + safari.getState().getDisplayName(),
                        "SAFARI_NOT_EDITABLE"
                    )
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

            SafariDayAccommodation dayAccommodation = SafariDayAccommodation.builder()
                .safariDay(day)
                .accommodation(accommodation)
                .roomType(roomType)
                .roomStandard(roomStandard)
                .boardType(boardType)
                .roomCount(roomCount)
                .isAlternative(isAlternative)
                .notes(createDTO.getNotes())
                .bookingStatus(SafariDayAccommodation.BookingStatus.PENDING)
                .build();

            // Validate pax capacity if this is a primary (non-alternative) accommodation
            if (!isAlternative) {
                ResponseEntity<ApiResponse<?>> capacityError = validatePaxCapacity(
                    safariId, dayId, dayAccommodation);
                if (capacityError != null) {
                    return capacityError;
                }
            }

            // Save
            dayAccommodation = accommodationRepository.save(dayAccommodation);

            // Convert to DTO
            SafariDayAccommodationDTO dto = convertToDTO(dayAccommodation);

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
    private SafariDayAccommodationDTO convertToDTO(SafariDayAccommodation entity) {
        SafariDayAccommodationDTO dto = new SafariDayAccommodationDTO();
        dto.setId(idObfuscator.encodeId(entity.getId()));
        dto.setSafariDayId(idObfuscator.encodeId(entity.getSafariDay().getId()));
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

        // Safari-specific fields
        dto.setConfirmationNumber(entity.getConfirmationNumber());
        dto.setConfirmedAt(entity.getConfirmedAt());
        dto.setCheckInTime(entity.getCheckInTime());
        dto.setCheckOutTime(entity.getCheckOutTime());
        dto.setRoomNumbers(entity.getRoomNumbers());
        dto.setGuestFeedback(entity.getGuestFeedback());
        dto.setSpecialArrangements(entity.getSpecialArrangements());
        dto.setBookingStatus(entity.getBookingStatus() != null ? entity.getBookingStatus().name() : null);

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
     * Underbooking should be validated at safari level (e.g., when publishing).
     *
     * @param safariId The safari ID
     * @param dayId The day ID
     * @param newAccommodation The new accommodation being created
     * @return Error response if validation fails, null if valid
     */
    private ResponseEntity<ApiResponse<?>> validatePaxCapacity(
        Long safariId,
        Long dayId,
        SafariDayAccommodation newAccommodation
    ) {
        // Get total pax count for the safari
        List<SafariPax> paxList = paxRepository.findBySafariId(safariId);
        int totalPax = paxList.stream()
            .mapToInt(pax -> pax.getCount() != null ? pax.getCount() : 0)
            .sum();

        // If no pax configured, skip validation
        if (totalPax == 0) {
            return null;
        }

        // Get existing primary accommodations for this day
        List<SafariDayAccommodation> existingPrimaryAccommodations =
            accommodationRepository.findPrimaryAccommodationsByDayId(dayId);

        // Calculate total minimum occupancy including the new accommodation
        int totalMinOccupancy = 0;

        // Add min occupancy from existing accommodations
        for (SafariDayAccommodation acc : existingPrimaryAccommodations) {
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
