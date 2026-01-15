package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Itinerary.Repository.ItineraryRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Entity.ItineraryDay;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Repository.ItineraryDayRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.DTOs.ItineraryDayDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryDayGetService - Service for retrieving itinerary days
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class ItineraryDayGetService {

    private final ItineraryRepository itineraryRepository;
    private final ItineraryDayRepository itineraryDayRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ItineraryDayGetService(
        ItineraryRepository itineraryRepository,
        ItineraryDayRepository itineraryDayRepository,
        IdObfuscator idObfuscator
    ) {
        this.itineraryRepository = itineraryRepository;
        this.itineraryDayRepository = itineraryDayRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Get all days for an itinerary
     *
     * @param itineraryIdObfuscated The obfuscated itinerary ID
     * @return ResponseEntity with ApiResponse containing list of days
     */
    public ResponseEntity<ApiResponse<?>> getItineraryDays(String itineraryIdObfuscated) {
        log.info("Fetching days for itinerary: {}", itineraryIdObfuscated);

        try {
            // Decode itinerary ID
            Long itineraryId;
            try {
                itineraryId = idObfuscator.decodeId(itineraryIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid itinerary ID", "INVALID_ITINERARY_ID")
                );
            }

            // Verify itinerary exists
            if (!itineraryRepository.existsById(itineraryId)) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Itinerary not found", "ITINERARY_NOT_FOUND")
                );
            }

            // Fetch days
            List<ItineraryDay> days = itineraryDayRepository.findByItineraryIdOrderByDayNumberAsc(itineraryId);

            // Convert to DTOs
            List<ItineraryDayDTO> dayDTOs = days.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Itinerary days retrieved successfully", dayDTOs)
            );

        } catch (Exception e) {
            log.error("Error fetching itinerary days", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch itinerary days", "ITINERARY_DAYS_FETCH_FAILED")
            );
        }
    }

    /**
     * Get a specific day by ID
     *
     * @param itineraryIdObfuscated The obfuscated itinerary ID
     * @param dayIdObfuscated The obfuscated day ID
     * @return ResponseEntity with ApiResponse containing the day
     */
    public ResponseEntity<ApiResponse<?>> getItineraryDay(String itineraryIdObfuscated, String dayIdObfuscated) {
        log.info("Fetching day {} for itinerary: {}", dayIdObfuscated, itineraryIdObfuscated);

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

            // Convert to DTO
            ItineraryDayDTO dayDTO = convertToDTO(day);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Itinerary day retrieved successfully", dayDTO)
            );

        } catch (Exception e) {
            log.error("Error fetching itinerary day", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch itinerary day", "ITINERARY_DAY_FETCH_FAILED")
            );
        }
    }

    /**
     * Get a specific day by day number
     *
     * @param itineraryIdObfuscated The obfuscated itinerary ID
     * @param dayNumber The day number
     * @return ResponseEntity with ApiResponse containing the day
     */
    public ResponseEntity<ApiResponse<?>> getItineraryDayByNumber(String itineraryIdObfuscated, Integer dayNumber) {
        log.info("Fetching day number {} for itinerary: {}", dayNumber, itineraryIdObfuscated);

        try {
            // Decode itinerary ID
            Long itineraryId;
            try {
                itineraryId = idObfuscator.decodeId(itineraryIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid itinerary ID", "INVALID_ITINERARY_ID")
                );
            }

            // Find day by itinerary and day number
            ItineraryDay day = itineraryDayRepository.findByItineraryIdAndDayNumber(itineraryId, dayNumber).orElse(null);
            if (day == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Day " + dayNumber + " not found", "ITINERARY_DAY_NOT_FOUND")
                );
            }

            // Convert to DTO
            ItineraryDayDTO dayDTO = convertToDTO(day);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Itinerary day retrieved successfully", dayDTO)
            );

        } catch (Exception e) {
            log.error("Error fetching itinerary day by number", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch itinerary day", "ITINERARY_DAY_FETCH_FAILED")
            );
        }
    }

    /**
     * Convert ItineraryDay entity to ItineraryDayDTO
     */
    private ItineraryDayDTO convertToDTO(ItineraryDay day) {
        ItineraryDayDTO dto = new ItineraryDayDTO();
        dto.setId(idObfuscator.encodeId(day.getId()));
        dto.setItineraryId(idObfuscator.encodeId(day.getItinerary().getId()));
        dto.setDayNumber(day.getDayNumber());
        dto.setDayTag(day.getDayTag());
        dto.setTitle(day.getTitle());
        dto.setDescription(day.getDescription());
        dto.setMorningActivities(day.getMorningActivities());
        dto.setAfternoonActivities(day.getAfternoonActivities());
        dto.setEveningActivities(day.getEveningActivities());
        dto.setWildlifeHighlights(day.getWildlifeHighlights());
        dto.setScenicHighlights(day.getScenicHighlights());
        dto.setSpecialNotes(day.getSpecialNotes());
        dto.setStartLocation(day.getStartLocation());
        dto.setEndLocation(day.getEndLocation());
        dto.setDistanceKm(day.getDistanceKm());
        dto.setIsOvernight(day.getIsOvernight());
        dto.setMealsIncluded(day.getMealsIncluded());
        dto.setCreatedAt(day.getCreatedAt());
        dto.setUpdatedAt(day.getUpdatedAt());
        return dto;
    }
}
