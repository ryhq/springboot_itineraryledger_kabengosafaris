package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.Services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Repository.ItineraryDayRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.Entity.ItineraryDayAccommodation;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.Repository.ItineraryDayAccommodationRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.DTOs.ItineraryDayAccommodationDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryDayAccommodationGetService - Service for retrieving itinerary day accommodations
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class ItineraryDayAccommodationGetService {

    private final ItineraryDayRepository itineraryDayRepository;
    private final ItineraryDayAccommodationRepository accommodationRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ItineraryDayAccommodationGetService(
        ItineraryDayRepository itineraryDayRepository,
        ItineraryDayAccommodationRepository accommodationRepository,
        IdObfuscator idObfuscator
    ) {
        this.itineraryDayRepository = itineraryDayRepository;
        this.accommodationRepository = accommodationRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Get all accommodations for an itinerary day
     *
     * @param itineraryIdObfuscated The obfuscated itinerary ID
     * @param dayIdObfuscated The obfuscated day ID
     * @return ResponseEntity with ApiResponse containing list of accommodations
     */
    public ResponseEntity<ApiResponse<?>> getItineraryDayAccommodations(
        String itineraryIdObfuscated,
        String dayIdObfuscated
    ) {
        log.info("Fetching accommodations for day: {}", dayIdObfuscated);

        try {
            // Decode IDs
            Long itineraryId;
            Long dayId;
            try {
                itineraryId = idObfuscator.decodeId(itineraryIdObfuscated);
                dayId = idObfuscator.decodeId(dayIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid ID", "INVALID_ID")
                );
            }

            // Verify day exists and belongs to itinerary
            var day = itineraryDayRepository.findById(dayId).orElse(null);
            if (day == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Itinerary day not found", "ITINERARY_DAY_NOT_FOUND")
                );
            }
            if (!day.getItinerary().getId().equals(itineraryId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Day does not belong to this itinerary", "DAY_ITINERARY_MISMATCH")
                );
            }

            // Fetch accommodations
            List<ItineraryDayAccommodation> accommodations = accommodationRepository.findByItineraryDayId(dayId);

            // Convert to DTOs
            List<ItineraryDayAccommodationDTO> dtos = accommodations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Accommodations retrieved successfully", dtos)
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
}
