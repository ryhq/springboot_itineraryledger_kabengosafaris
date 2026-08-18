package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.Services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.Entity.SafariDayAccommodation;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.Repository.SafariDayAccommodationRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.DTOs.UpdateSafariDayAccommodationDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.DTOs.SafariDayAccommodationDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariPax.Entity.SafariPax;
import com.itineraryledger.kabengosafaris.Safari.SafariPax.Repository.SafariPaxRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.AvailabilityRequest.Services.AvailabilityRequestService;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * SafariDayAccommodationUpdateService - Service for updating safari day accommodations
 *
 * Allows updating room count, alternative status, notes, and Safari-specific operational fields.
 * Core accommodation configuration cannot be changed.
 *
 * Implements dual update modes:
 * - Planning updates (room count, isAlternative, notes) require editable safari state
 * - Operational updates (booking confirmation, check-in/out, feedback) can be done anytime
 */
@Service
@Slf4j
@Transactional
public class SafariDayAccommodationUpdateService {

    private final SafariDayAccommodationRepository accommodationRepository;
    private final AvailabilityRequestService availabilityRequestService;
    private final SafariPaxRepository paxRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public SafariDayAccommodationUpdateService(
        SafariDayAccommodationRepository accommodationRepository,
        SafariPaxRepository paxRepository,
        IdObfuscator idObfuscator,
        AvailabilityRequestService availabilityRequestService
    ) {
        this.accommodationRepository = accommodationRepository;
        this.paxRepository = paxRepository;
        this.idObfuscator = idObfuscator;
        this.availabilityRequestService = availabilityRequestService;
    }

    /**
     * Update an existing accommodation for a safari day
     *
     * @param safariIdObfuscated The obfuscated safari ID
     * @param dayIdObfuscated The obfuscated day ID
     * @param accommodationIdObfuscated The obfuscated accommodation entry ID
     * @param updateDTO The accommodation update data
     * @return ResponseEntity with ApiResponse containing the updated accommodation
     */
    @AuditLogAnnotation(action = "UPDATE_SAFARI_DAY_ACCOMMODATION", description = "Updating accommodation for safari day", entityType = "SafariDayAccommodation")
    public ResponseEntity<ApiResponse<?>> updateSafariDayAccommodation(
        String safariIdObfuscated,
        String dayIdObfuscated,
        String accommodationIdObfuscated,
        UpdateSafariDayAccommodationDTO updateDTO
    ) {
        log.info("Updating accommodation: {}", accommodationIdObfuscated);

        try {
            // Decode IDs
            Long safariId;
            Long dayId;
            Long accommodationEntryId;
            try {
                safariId = idObfuscator.decodeId(safariIdObfuscated);
                dayId = idObfuscator.decodeId(dayIdObfuscated);
                accommodationEntryId = idObfuscator.decodeId(accommodationIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid ID", "INVALID_ID")
                );
            }

            // Find accommodation entry
            SafariDayAccommodation dayAccommodation = accommodationRepository.findById(accommodationEntryId).orElse(null);
            if (dayAccommodation == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Accommodation not found", "ACCOMMODATION_NOT_FOUND")
                );
            }

            // Verify accommodation belongs to the specified day
            if (!dayAccommodation.getSafariDay().getId().equals(dayId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Accommodation does not belong to this day", "ACCOMMODATION_DAY_MISMATCH")
                );
            }

            // Verify day belongs to the specified safari
            if (!dayAccommodation.getSafariDay().getSafari().getId().equals(safariId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Day does not belong to this safari", "DAY_SAFARI_MISMATCH")
                );
            }

            // Determine if this is an operational update
            boolean isOperationalUpdate = updateDTO.getConfirmationNumber() != null ||
                updateDTO.getCheckInTime() != null ||
                updateDTO.getCheckOutTime() != null ||
                updateDTO.getRoomNumbers() != null ||
                updateDTO.getGuestFeedback() != null ||
                updateDTO.getSpecialArrangements() != null ||
                updateDTO.getBookingStatus() != null;

            // Check if safari is editable for planning updates
            Safari safari = dayAccommodation.getSafariDay().getSafari();
            if (!isOperationalUpdate && !safari.isEditable()) {
                log.warn("Safari is not editable: {} (state: {})", safari.getCode(), safari.getState());
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Safari cannot be edited in state: " + safari.getState().getDisplayName(),
                        "SAFARI_NOT_EDITABLE"
                    )
                );
            }

            // Update planning fields if provided
            if (updateDTO.getRoomCount() != null) {
                dayAccommodation.setRoomCount(updateDTO.getRoomCount());
            }

            if (updateDTO.getIsAlternative() != null) {
                dayAccommodation.setIsAlternative(updateDTO.getIsAlternative());
            }

            // Notes can be set to null to clear it
            dayAccommodation.setNotes(updateDTO.getNotes());

            // Update Safari-specific operational fields
            if (updateDTO.getConfirmationNumber() != null) {
                dayAccommodation.setConfirmationNumber(updateDTO.getConfirmationNumber());
                // Auto-set confirmedAt if confirmation number is being added
                if (dayAccommodation.getConfirmedAt() == null) {
                    dayAccommodation.setConfirmedAt(LocalDateTime.now());
                }
            }

            if (updateDTO.getCheckInTime() != null) {
                dayAccommodation.setCheckInTime(updateDTO.getCheckInTime());
            }

            if (updateDTO.getCheckOutTime() != null) {
                dayAccommodation.setCheckOutTime(updateDTO.getCheckOutTime());
            }

            if (updateDTO.getRoomNumbers() != null) {
                dayAccommodation.setRoomNumbers(updateDTO.getRoomNumbers());
            }

            if (updateDTO.getGuestFeedback() != null) {
                dayAccommodation.setGuestFeedback(updateDTO.getGuestFeedback());
            }

            if (updateDTO.getSpecialArrangements() != null) {
                dayAccommodation.setSpecialArrangements(updateDTO.getSpecialArrangements());
            }

            boolean justConfirmed = false;

            if (updateDTO.getBookingStatus() != null) {
                try {
                    SafariDayAccommodation.BookingStatus status =
                        SafariDayAccommodation.BookingStatus.valueOf(updateDTO.getBookingStatus().toUpperCase());
                    justConfirmed = status == SafariDayAccommodation.BookingStatus.CONFIRMED
                        && dayAccommodation.getBookingStatus() != SafariDayAccommodation.BookingStatus.CONFIRMED;
                    dayAccommodation.setBookingStatus(status);
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400,
                            "Invalid booking status. Valid values: PENDING, CONFIRMED, CANCELLED, NO_SHOW, COMPLETED",
                            "INVALID_BOOKING_STATUS")
                    );
                }
            }

            // Validate pax capacity if this is a primary (non-alternative) accommodation and planning fields were updated
            Boolean isAlternative = updateDTO.getIsAlternative() != null
                ? updateDTO.getIsAlternative()
                : dayAccommodation.getIsAlternative();

            if (!isAlternative && updateDTO.getRoomCount() != null) {
                ResponseEntity<ApiResponse<?>> capacityError = validatePaxCapacity(
                    safariId, dayId, dayAccommodation);
                if (capacityError != null) {
                    return capacityError;
                }
            }

            // Save
            dayAccommodation = accommodationRepository.save(dayAccommodation);

            /*
             * A confirmed night finishes whatever ask was waiting on it.
             *
             * Nobody goes back to close an availability request by hand once the booking is in, and
             * an ask left open would sit on the chase list for a night already secured. Best effort
             * by design: the confirmation is the fact, the bookkeeping is not allowed to undo it.
             */
            if (justConfirmed) {
                availabilityRequestService.noticeStayConfirmed(dayAccommodation.getId());
            }

            // Convert to DTO
            SafariDayAccommodationDTO dto = convertToDTO(dayAccommodation);

            log.info("Accommodation updated: {} for day {}",
                dayAccommodation.getAccommodation().getName(),
                dayAccommodation.getSafariDay().getDayNumber());

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
     * @param updatedAccommodation The accommodation being updated (with new values applied)
     * @return Error response if validation fails, null if valid
     */
    private ResponseEntity<ApiResponse<?>> validatePaxCapacity(
        Long safariId,
        Long dayId,
        SafariDayAccommodation updatedAccommodation
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

        // Get all primary accommodations for this day
        List<SafariDayAccommodation> primaryAccommodations =
            accommodationRepository.findPrimaryAccommodationsByDayId(dayId);

        // Calculate total minimum occupancy, replacing the updated accommodation's values
        int totalMinOccupancy = 0;

        for (SafariDayAccommodation acc : primaryAccommodations) {
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
}
