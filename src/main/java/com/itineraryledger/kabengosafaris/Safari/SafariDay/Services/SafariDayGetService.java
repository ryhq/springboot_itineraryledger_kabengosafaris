package com.itineraryledger.kabengosafaris.Safari.SafariDay.Services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Safari.Repository.SafariRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.Entity.SafariDay;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.Repository.SafariDayRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.DTOs.SafariDayDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * SafariDayGetService - Service for retrieving safari days
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class SafariDayGetService {

    private final SafariRepository safariRepository;
    private final SafariDayRepository safariDayRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public SafariDayGetService(
        SafariRepository safariRepository,
        SafariDayRepository safariDayRepository,
        IdObfuscator idObfuscator
    ) {
        this.safariRepository = safariRepository;
        this.safariDayRepository = safariDayRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Get all days for a safari
     *
     * @param safariIdObfuscated The obfuscated safari ID
     * @return ResponseEntity with ApiResponse containing list of days
     */
    public ResponseEntity<ApiResponse<?>> getSafariDays(String safariIdObfuscated) {
        log.info("Fetching days for safari: {}", safariIdObfuscated);

        try {
            // Decode safari ID
            Long safariId;
            try {
                safariId = idObfuscator.decodeId(safariIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid safari ID", "INVALID_SAFARI_ID")
                );
            }

            // Verify safari exists
            if (!safariRepository.existsById(safariId)) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Safari not found", "SAFARI_NOT_FOUND")
                );
            }

            // Fetch days
            List<SafariDay> days = safariDayRepository.findBySafariIdOrderByDayNumberAsc(safariId);

            // Convert to DTOs
            List<SafariDayDTO> dayDTOs = days.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Safari days retrieved successfully", dayDTOs)
            );

        } catch (Exception e) {
            log.error("Error fetching safari days", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch safari days", "SAFARI_DAYS_FETCH_FAILED")
            );
        }
    }

    /**
     * Get a specific day by ID
     *
     * @param safariIdObfuscated The obfuscated safari ID
     * @param dayIdObfuscated The obfuscated day ID
     * @return ResponseEntity with ApiResponse containing the day
     */
    public ResponseEntity<ApiResponse<?>> getSafariDay(String safariIdObfuscated, String dayIdObfuscated) {
        log.info("Fetching day {} for safari: {}", dayIdObfuscated, safariIdObfuscated);

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

            // Convert to DTO
            SafariDayDTO dayDTO = convertToDTO(day);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Safari day retrieved successfully", dayDTO)
            );

        } catch (Exception e) {
            log.error("Error fetching safari day", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch safari day", "SAFARI_DAY_FETCH_FAILED")
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
        dto.setActualDate(day.getActualDate());
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
        dto.setInternalNotes(day.getInternalNotes());

        // Safari-specific fields
        dto.setWeatherNotes(day.getWeatherNotes());
        dto.setActualStartTime(day.getActualStartTime());
        dto.setActualEndTime(day.getActualEndTime());
        dto.setDriverNotes(day.getDriverNotes());

        // Calculate date flags using entity's transient methods
        dto.setIsPast(day.isPast());
        dto.setIsToday(day.isToday());
        dto.setIsFuture(day.isFuture());

        dto.setCreatedAt(day.getCreatedAt());
        dto.setUpdatedAt(day.getUpdatedAt());

        return dto;
    }
}
