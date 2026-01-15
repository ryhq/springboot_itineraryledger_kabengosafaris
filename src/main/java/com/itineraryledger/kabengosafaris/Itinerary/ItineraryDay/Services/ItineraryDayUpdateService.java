package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Entity.ItineraryDay;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Repository.ItineraryDayRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.DTOs.ItineraryDayDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.DTOs.UpdateItineraryDayDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryDayUpdateService - Service for updating itinerary days
 */
@Service
@Slf4j
@Transactional
public class ItineraryDayUpdateService {

    private final ItineraryDayRepository itineraryDayRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ItineraryDayUpdateService(
        ItineraryDayRepository itineraryDayRepository,
        IdObfuscator idObfuscator
    ) {
        this.itineraryDayRepository = itineraryDayRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Update an itinerary day
     *
     * @param itineraryIdObfuscated The obfuscated itinerary ID
     * @param dayIdObfuscated The obfuscated day ID
     * @param updateDTO The updated day data
     * @return ResponseEntity with ApiResponse containing the updated day
     */
    @AuditLogAnnotation(action = "UPDATE_ITINERARY_DAY", description = "Updating itinerary day", entityType = "ItineraryDay", entityIdParamName = "dayIdObfuscated")
    public ResponseEntity<ApiResponse<?>> updateItineraryDay(
        String itineraryIdObfuscated,
        String dayIdObfuscated,
        UpdateItineraryDayDTO updateDTO
    ) {
        log.info("Updating day {} for itinerary: {}", dayIdObfuscated, itineraryIdObfuscated);

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

            // Check if day number is being changed and if it's unique
            if (updateDTO.getDayNumber() != null && !updateDTO.getDayNumber().equals(day.getDayNumber())) {
                if (itineraryDayRepository.existsByItineraryIdAndDayNumberAndIdNot(itineraryId, updateDTO.getDayNumber(), dayId)) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                            400,
                            "Day " + updateDTO.getDayNumber() + " already exists in this itinerary",
                            "DAY_NUMBER_EXISTS"
                        )
                    );
                }
                // Validate against total days
                if (updateDTO.getDayNumber() > day.getItinerary().getTotalDays()) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                            400,
                            "Day number cannot exceed itinerary's total days",
                            "DAY_NUMBER_EXCEEDS_TOTAL"
                        )
                    );
                }
                day.setDayNumber(updateDTO.getDayNumber());
            }

            // Update other fields if provided
            if (updateDTO.getDayTag() != null) {
                day.setDayTag(updateDTO.getDayTag());
            }
            if (updateDTO.getTitle() != null) {
                day.setTitle(updateDTO.getTitle());
            }
            if (updateDTO.getDescription() != null) {
                day.setDescription(updateDTO.getDescription());
            }
            if (updateDTO.getMorningActivities() != null) {
                day.setMorningActivities(updateDTO.getMorningActivities());
            }
            if (updateDTO.getAfternoonActivities() != null) {
                day.setAfternoonActivities(updateDTO.getAfternoonActivities());
            }
            if (updateDTO.getEveningActivities() != null) {
                day.setEveningActivities(updateDTO.getEveningActivities());
            }
            if (updateDTO.getWildlifeHighlights() != null) {
                day.setWildlifeHighlights(updateDTO.getWildlifeHighlights());
            }
            if (updateDTO.getScenicHighlights() != null) {
                day.setScenicHighlights(updateDTO.getScenicHighlights());
            }
            if (updateDTO.getSpecialNotes() != null) {
                day.setSpecialNotes(updateDTO.getSpecialNotes());
            }
            if (updateDTO.getStartLocation() != null) {
                day.setStartLocation(updateDTO.getStartLocation());
            }
            if (updateDTO.getEndLocation() != null) {
                day.setEndLocation(updateDTO.getEndLocation());
            }
            if (updateDTO.getDistanceKm() != null) {
                day.setDistanceKm(updateDTO.getDistanceKm());
            }
            if (updateDTO.getIsOvernight() != null) {
                day.setIsOvernight(updateDTO.getIsOvernight());
            }
            if (updateDTO.getMealsIncluded() != null) {
                day.setMealsIncluded(updateDTO.getMealsIncluded());
            }

            // Save updated day
            day = itineraryDayRepository.save(day);

            // Convert to DTO
            ItineraryDayDTO dayDTO = convertToDTO(day);

            log.info("Itinerary day updated successfully: Day {}", day.getDayNumber());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Itinerary day updated successfully", dayDTO)
            );

        } catch (Exception e) {
            log.error("Error updating itinerary day", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update itinerary day", "ITINERARY_DAY_UPDATE_FAILED")
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
