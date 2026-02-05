package com.itineraryledger.kabengosafaris.Safari.SafariDay.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.Repository.SafariRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.Entity.SafariDay;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.Repository.SafariDayRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.DTOs.SafariDayDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.DTOs.UpdateSafariDayDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * SafariDayUpdateService - Service for updating safari days
 */
@Service
@Slf4j
@Transactional
public class SafariDayUpdateService {

    private final SafariDayRepository safariDayRepository;
    private final SafariRepository safariRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public SafariDayUpdateService(
        SafariDayRepository safariDayRepository,
        SafariRepository safariRepository,
        IdObfuscator idObfuscator
    ) {
        this.safariDayRepository = safariDayRepository;
        this.safariRepository = safariRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Update a safari day
     *
     * @param safariIdObfuscated The obfuscated safari ID
     * @param dayIdObfuscated The obfuscated day ID
     * @param updateDTO The updated day data
     * @return ResponseEntity with ApiResponse containing the updated day
     */
    @AuditLogAnnotation(action = "UPDATE_SAFARI_DAY", description = "Updating safari day", entityType = "SafariDay", entityIdParamName = "dayIdObfuscated")
    public ResponseEntity<ApiResponse<?>> updateSafariDay(
        String safariIdObfuscated,
        String dayIdObfuscated,
        UpdateSafariDayDTO updateDTO
    ) {
        log.info("Updating day {} for safari: {}", dayIdObfuscated, safariIdObfuscated);

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

            // Find safari
            Safari safari = safariRepository.findById(safariId).orElse(null);
            if (safari == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Safari not found", "SAFARI_NOT_FOUND")
                );
            }

            // Check if safari is editable
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

            // Note: dayNumber, dayTag, and actualDate are not updatable - they are auto-managed

            // Update fields if provided
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
            if (updateDTO.getInternalNotes() != null) {
                day.setInternalNotes(updateDTO.getInternalNotes());
            }

            // Safari-specific fields
            if (updateDTO.getWeatherNotes() != null) {
                day.setWeatherNotes(updateDTO.getWeatherNotes());
            }
            if (updateDTO.getActualStartTime() != null) {
                day.setActualStartTime(updateDTO.getActualStartTime());
            }
            if (updateDTO.getActualEndTime() != null) {
                day.setActualEndTime(updateDTO.getActualEndTime());
            }
            if (updateDTO.getDriverNotes() != null) {
                day.setDriverNotes(updateDTO.getDriverNotes());
            }

            // Save updated day
            day = safariDayRepository.save(day);

            // Convert to DTO
            SafariDayDTO dayDTO = convertToDTO(day);

            log.info("Safari day updated successfully: Day {} for safari {}", day.getDayNumber(), safari.getCode());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Safari day updated successfully", dayDTO)
            );

        } catch (Exception e) {
            log.error("Error updating safari day", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update safari day", "SAFARI_DAY_UPDATE_FAILED")
            );
        }
    }

    /**
     * Convert SafariDay entity to SafariDayDTO
     */
    private SafariDayDTO convertToDTO(SafariDay day) {
        SafariDayDTO dto = new SafariDayDTO();
        dto.setId(idObfuscator.encodeId(day.getId()));
        dto.setSafariId(idObfuscator.encodeId(day.getSafari().getId()));
        dto.setDayNumber(day.getDayNumber());
        dto.setDayTag(day.getDayTag());
        dto.setTitle(day.getTitle());
        dto.setActualDate(day.getActualDate());
        dto.setIsPast(day.isPast());
        dto.setIsToday(day.isToday());
        dto.setIsFuture(day.isFuture());
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
        dto.setInternalNotes(day.getInternalNotes());
        dto.setWeatherNotes(day.getWeatherNotes());
        dto.setActualStartTime(day.getActualStartTime());
        dto.setActualEndTime(day.getActualEndTime());
        dto.setDriverNotes(day.getDriverNotes());
        dto.setActivitiesCount(day.getActivities() != null ? day.getActivities().size() : 0);
        dto.setParksCount(day.getParks() != null ? day.getParks().size() : 0);
        dto.setAccommodationsCount(day.getAccommodations() != null ? day.getAccommodations().size() : 0);
        dto.setCreatedAt(day.getCreatedAt());
        dto.setUpdatedAt(day.getUpdatedAt());
        return dto;
    }
}
