package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.Services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationBoardType;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRoomStandard;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRoomType;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationBoardTypeRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRoomStandardRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRoomTypeRepository;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.Entity.ItineraryDayAccommodation;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.Repository.ItineraryDayAccommodationRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.DTOs.UpdateItineraryDayAccommodationDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.DTOs.ItineraryDayAccommodationDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryPax.Entity.ItineraryPax;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryPax.Repository.ItineraryPaxRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryDayAccommodationUpdateService - Service for updating itinerary day accommodations
 *
 * Allows updating room count, alternative status, and notes.
 * Core accommodation configuration cannot be changed.
 */
@Service
@Slf4j
@Transactional
public class ItineraryDayAccommodationUpdateService {

    private final ItineraryDayAccommodationRepository accommodationRepository;
    private final ItineraryPaxRepository paxRepository;
    private final AccommodationRepository baseAccommodationRepository;
    private final AccommodationRoomTypeRepository roomTypeRepository;
    private final AccommodationRoomStandardRepository roomStandardRepository;
    private final AccommodationBoardTypeRepository boardTypeRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ItineraryDayAccommodationUpdateService(
        ItineraryDayAccommodationRepository accommodationRepository,
        ItineraryPaxRepository paxRepository,
        AccommodationRepository baseAccommodationRepository,
        AccommodationRoomTypeRepository roomTypeRepository,
        AccommodationRoomStandardRepository roomStandardRepository,
        AccommodationBoardTypeRepository boardTypeRepository,
        IdObfuscator idObfuscator
    ) {
        this.accommodationRepository = accommodationRepository;
        this.paxRepository = paxRepository;
        this.baseAccommodationRepository = baseAccommodationRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.roomStandardRepository = roomStandardRepository;
        this.boardTypeRepository = boardTypeRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Update an existing accommodation for an itinerary day
     *
     * @param itineraryIdObfuscated The obfuscated itinerary ID
     * @param dayIdObfuscated The obfuscated day ID
     * @param accommodationIdObfuscated The obfuscated accommodation entry ID
     * @param updateDTO The accommodation update data
     * @return ResponseEntity with ApiResponse containing the updated accommodation
     */
    @AuditLogAnnotation(action = "UPDATE_ITINERARY_DAY_ACCOMMODATION", description = "Updating accommodation for itinerary day", entityType = "ItineraryDayAccommodation")
    public ResponseEntity<ApiResponse<?>> updateItineraryDayAccommodation(
        String itineraryIdObfuscated,
        String dayIdObfuscated,
        String accommodationIdObfuscated,
        UpdateItineraryDayAccommodationDTO updateDTO
    ) {
        log.info("Updating accommodation: {}", accommodationIdObfuscated);

        try {
            // Decode IDs
            Long itineraryId;
            Long dayId;
            Long accommodationEntryId;
            try {
                itineraryId = idObfuscator.decodeId(itineraryIdObfuscated);
                dayId = idObfuscator.decodeId(dayIdObfuscated);
                accommodationEntryId = idObfuscator.decodeId(accommodationIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid ID", "INVALID_ID")
                );
            }

            // Find accommodation entry
            ItineraryDayAccommodation dayAccommodation = accommodationRepository.findById(accommodationEntryId).orElse(null);
            if (dayAccommodation == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Accommodation not found", "ACCOMMODATION_NOT_FOUND")
                );
            }

            // Verify accommodation belongs to the specified day
            if (!dayAccommodation.getItineraryDay().getId().equals(dayId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Accommodation does not belong to this day", "ACCOMMODATION_DAY_MISMATCH")
                );
            }

            // Verify day belongs to the specified itinerary
            if (!dayAccommodation.getItineraryDay().getItinerary().getId().equals(itineraryId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Day does not belong to this itinerary", "DAY_ITINERARY_MISMATCH")
                );
            }

            // Update fields if provided
            /*
             * The room configuration, which used to be fixed after creation.
             * Order matters: the property is resolved first, because the room
             * type, standard and board are then checked against whichever
             * property the stay ends up on — a Bungalow from another lodge is
             * not a valid choice here, and silently accepting it would price
             * the night against a rate that does not exist.
             */
            if (updateDTO.getAccommodationId() != null && !updateDTO.getAccommodationId().isBlank()) {
                Long newAccommodationId;
                try {
                    newAccommodationId = idObfuscator.decodeId(updateDTO.getAccommodationId());
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid accommodation ID", "INVALID_ACCOMMODATION_ID"));
                }
                Accommodation accommodation = baseAccommodationRepository.findById(newAccommodationId).orElse(null);
                if (accommodation == null) {
                    return ResponseEntity.status(404).body(
                        ApiResponse.error(404, "Accommodation not found", "ACCOMMODATION_NOT_FOUND"));
                }
                dayAccommodation.setAccommodation(accommodation);
            }

            Long owningAccommodationId = dayAccommodation.getAccommodation().getId();
            String owningName = dayAccommodation.getAccommodation().getName();

            if (updateDTO.getRoomTypeId() != null && !updateDTO.getRoomTypeId().isBlank()) {
                AccommodationRoomType roomType = roomTypeRepository
                    .findById(idObfuscator.decodeId(updateDTO.getRoomTypeId())).orElse(null);
                if (roomType == null) {
                    return ResponseEntity.status(404).body(
                        ApiResponse.error(404, "Room type not found", "ROOM_TYPE_NOT_FOUND"));
                }
                if (!roomType.getAccommodation().getId().equals(owningAccommodationId)) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400,
                            "That room type belongs to another property, not " + owningName + ".",
                            "ROOM_TYPE_MISMATCH"));
                }
                dayAccommodation.setRoomType(roomType);
            }

            if (updateDTO.getRoomStandardId() != null && !updateDTO.getRoomStandardId().isBlank()) {
                AccommodationRoomStandard roomStandard = roomStandardRepository
                    .findById(idObfuscator.decodeId(updateDTO.getRoomStandardId())).orElse(null);
                if (roomStandard == null) {
                    return ResponseEntity.status(404).body(
                        ApiResponse.error(404, "Room standard not found", "ROOM_STANDARD_NOT_FOUND"));
                }
                if (!roomStandard.getAccommodation().getId().equals(owningAccommodationId)) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400,
                            "That room standard belongs to another property, not " + owningName + ".",
                            "ROOM_STANDARD_MISMATCH"));
                }
                dayAccommodation.setRoomStandard(roomStandard);
            }

            if (updateDTO.getBoardTypeId() != null && !updateDTO.getBoardTypeId().isBlank()) {
                AccommodationBoardType boardType = boardTypeRepository
                    .findById(idObfuscator.decodeId(updateDTO.getBoardTypeId())).orElse(null);
                if (boardType == null) {
                    return ResponseEntity.status(404).body(
                        ApiResponse.error(404, "Board type not found", "BOARD_TYPE_NOT_FOUND"));
                }
                if (!boardType.getAccommodation().getId().equals(owningAccommodationId)) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400,
                            "That board type belongs to another property, not " + owningName + ".",
                            "BOARD_TYPE_MISMATCH"));
                }
                dayAccommodation.setBoardType(boardType);
            }

            if (updateDTO.getRoomCount() != null) {
                dayAccommodation.setRoomCount(updateDTO.getRoomCount());
            }

            if (updateDTO.getIsAlternative() != null) {
                dayAccommodation.setIsAlternative(updateDTO.getIsAlternative());
            }

            // Notes can be set to null to clear it
            dayAccommodation.setNotes(updateDTO.getNotes());

            // Validate pax capacity if this is a primary (non-alternative) accommodation
            Boolean isAlternative = updateDTO.getIsAlternative() != null
                ? updateDTO.getIsAlternative()
                : dayAccommodation.getIsAlternative();

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

            log.info("Accommodation updated: {} for day {}",
                dayAccommodation.getAccommodation().getName(),
                dayAccommodation.getItineraryDay().getDayNumber());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Accommodation updated successfully", dto)
            );

        } catch (Exception e) {
            log.error("Error updating accommodation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update accommodation", "ACCOMMODATION_UPDATE_FAILED")
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
     * @param updatedAccommodation The accommodation being updated (with new values applied)
     * @return Error response if validation fails, null if valid
     */
    private ResponseEntity<ApiResponse<?>> validatePaxCapacity(
        Long itineraryId,
        Long dayId,
        ItineraryDayAccommodation updatedAccommodation
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

        // Get all primary accommodations for this day
        List<ItineraryDayAccommodation> primaryAccommodations =
            accommodationRepository.findByItineraryDayIdAndIsAlternativeFalse(dayId);

        // Calculate total minimum occupancy, replacing the updated accommodation's values
        int totalMinOccupancy = 0;

        for (ItineraryDayAccommodation acc : primaryAccommodations) {
            int roomCount;
            Integer minOccupancy;

            if (acc.getId().equals(updatedAccommodation.getId())) {
                // Use updated values
                roomCount = updatedAccommodation.getRoomCount() != null ? updatedAccommodation.getRoomCount() : 1;
                minOccupancy = updatedAccommodation.getRoomType().getMinOccupancy();
            } else {
                roomCount = acc.getRoomCount() != null ? acc.getRoomCount() : 1;
                minOccupancy = acc.getRoomType().getMinOccupancy();
            }

            if (minOccupancy != null) {
                totalMinOccupancy += roomCount * minOccupancy;
            }
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

    /**
     * Make one recorded option the booked one, and demote whatever held that place.
     *
     * One call rather than two, because the pair of writes has to land together. Done from a screen
     * as "set this one primary" then "set that one alternative", a failure between them leaves the
     * day with two primaries, which the estimator prices as two beds for one night, or with none,
     * which prices as no bed at all. Both are wrong on a quote and neither announces itself. The
     * class is @Transactional, so here they commit or neither does.
     *
     * Written to be idempotent: promoting the bed that is already primary is a no-op that still
     * reports success, since a double click on a slow connection should not be an error.
     */
    @AuditLogAnnotation(action = "MAKE_ITINERARY_DAY_ACCOMMODATION_PRIMARY",
        description = "Making an accommodation option the booked one", entityType = "ItineraryDayAccommodation")
    public ResponseEntity<ApiResponse<?>> makePrimary(
        String itineraryIdObfuscated,
        String dayIdObfuscated,
        String accommodationIdObfuscated
    ) {
        log.info("Making accommodation primary: {}", accommodationIdObfuscated);

        try {
            Long dayId;
            Long entryId;
            try {
                dayId = idObfuscator.decodeId(dayIdObfuscated);
                entryId = idObfuscator.decodeId(accommodationIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid ID", "INVALID_ID"));
            }

            ItineraryDayAccommodation target = accommodationRepository.findById(entryId).orElse(null);
            if (target == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Accommodation not found", "ACCOMMODATION_NOT_FOUND"));
            }
            if (target.getItineraryDay() == null
                || !target.getItineraryDay().getId().equals(dayId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        "That stay is not on this day", "ACCOMMODATION_NOT_ON_DAY"));
            }

            /*
             * Every sibling on the day, not just the current primary. A day can arrive with two
             * primaries from an older import or a half-finished edit, and promoting one option is
             * the moment to leave the day with exactly one.
             */
            List<ItineraryDayAccommodation> onThisDay =
                accommodationRepository.findByItineraryDayId(dayId);
            for (ItineraryDayAccommodation stay : onThisDay) {
                boolean shouldBePrimary = stay.getId().equals(entryId);
                if (Boolean.TRUE.equals(stay.getIsAlternative()) == shouldBePrimary) {
                    stay.setIsAlternative(!shouldBePrimary);
                    accommodationRepository.save(stay);
                }
            }

            log.info("Accommodation {} is now the booked stay on day {}", entryId, dayId);
            return ResponseEntity.ok(ApiResponse.success(200,
                "This stay is now the booked one for the day", null));

        } catch (Exception e) {
            log.error("Error making accommodation primary", e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Could not change the booked stay", "MAKE_PRIMARY_FAILED"));
        }
    }
}
