package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.Services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Safari.SafariDay.Repository.SafariDayRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.Entity.SafariDayAccommodation;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.Repository.SafariDayAccommodationRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.DTOs.SafariDayAccommodationDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * SafariDayAccommodationGetService - Service for retrieving safari day accommodations
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class SafariDayAccommodationGetService {

    private final SafariDayRepository safariDayRepository;
    private final SafariDayAccommodationRepository accommodationRepository;
    private final IdObfuscator idObfuscator;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "roomCount", "isAlternative", "bookingStatus", "confirmationNumber",
        "checkInTime", "checkOutTime", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    @Autowired
    public SafariDayAccommodationGetService(
        SafariDayRepository safariDayRepository,
        SafariDayAccommodationRepository accommodationRepository,
        IdObfuscator idObfuscator
    ) {
        this.safariDayRepository = safariDayRepository;
        this.accommodationRepository = accommodationRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Get all accommodations for a safari day
     *
     * @param safariIdObfuscated The obfuscated safari ID
     * @param dayIdObfuscated The obfuscated day ID
     * @return ResponseEntity with ApiResponse containing list of accommodations
     */
    public ResponseEntity<ApiResponse<?>> getSafariDayAccommodations(
        String safariIdObfuscated,
        String dayIdObfuscated,
        String sortBy,
        String sortDirection
    ) {
        log.info("Fetching accommodations for day: {}", dayIdObfuscated);

        try {
            // Decode IDs
            Long safariId;
            Long dayId;
            try {
                safariId = idObfuscator.decodeId(safariIdObfuscated);
                dayId = idObfuscator.decodeId(dayIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid ID", "INVALID_ID")
                );
            }

            // Verify day exists and belongs to safari
            var day = safariDayRepository.findById(dayId).orElse(null);
            if (day == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Safari day not found", "SAFARI_DAY_NOT_FOUND")
                );
            }
            if (!day.getSafari().getId().equals(safariId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Day does not belong to this safari", "DAY_SAFARI_MISMATCH")
                );
            }

            // Sorting with validation
            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                log.warn("Invalid sort field: {}", sortBy);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }

            // Fetch accommodations
            List<SafariDayAccommodation> accommodations = accommodationRepository.findBySafariDayId(dayId);

            // Convert to DTOs
            List<SafariDayAccommodationDTO> dtos = accommodations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            // Response
            Map<String, Object> response = new HashMap<>();
            response.put("accommodations", dtos);
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDir", sortDirection != null ? sortDirection : "desc");

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Accommodations retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error fetching accommodations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch accommodations", "ACCOMMODATIONS_FETCH_FAILED")
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

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
    }
}
