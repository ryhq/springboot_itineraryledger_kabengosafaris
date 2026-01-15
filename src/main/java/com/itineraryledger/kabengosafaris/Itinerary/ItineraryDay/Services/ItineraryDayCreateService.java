package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.Repository.ItineraryRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Entity.ItineraryDay;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Repository.ItineraryDayRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.DTOs.CreateItineraryDayDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.DTOs.ItineraryDayDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryDayCreateService - Service for creating itinerary days
 */
@Service
@Slf4j
@Transactional
public class ItineraryDayCreateService {

    private final ItineraryRepository itineraryRepository;
    private final ItineraryDayRepository itineraryDayRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ItineraryDayCreateService(
        ItineraryRepository itineraryRepository,
        ItineraryDayRepository itineraryDayRepository,
        IdObfuscator idObfuscator
    ) {
        this.itineraryRepository = itineraryRepository;
        this.itineraryDayRepository = itineraryDayRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Create a new itinerary day
     *
     * @param itineraryIdObfuscated The obfuscated itinerary ID
     * @param createDTO The day data
     * @return ResponseEntity with ApiResponse containing the created day
     */
    @AuditLogAnnotation(action = "CREATE_ITINERARY_DAY", description = "Creating a new itinerary day", entityType = "ItineraryDay")
    public ResponseEntity<ApiResponse<?>> createItineraryDay(String itineraryIdObfuscated, CreateItineraryDayDTO createDTO) {
        log.info("Creating day {} for itinerary: {}", createDTO.getDayNumber(), itineraryIdObfuscated);

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

            // Find itinerary
            Itinerary itinerary = itineraryRepository.findById(itineraryId).orElse(null);
            if (itinerary == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Itinerary not found", "ITINERARY_NOT_FOUND")
                );
            }

            // Check if day number already exists
            if (itineraryDayRepository.existsByItineraryIdAndDayNumber(itineraryId, createDTO.getDayNumber())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Day " + createDTO.getDayNumber() + " already exists in this itinerary",
                        "DAY_NUMBER_EXISTS"
                    )
                );
            }

            // Validate day number against itinerary's total days
            if (createDTO.getDayNumber() > itinerary.getTotalDays()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Day number cannot exceed itinerary's total days (" + itinerary.getTotalDays() + ")",
                        "DAY_NUMBER_EXCEEDS_TOTAL"
                    )
                );
            }

            // Create day entity
            ItineraryDay day = ItineraryDay.builder()
                .itinerary(itinerary)
                .dayNumber(createDTO.getDayNumber())
                .dayTag(createDTO.getDayTag())
                .title(createDTO.getTitle())
                .description(createDTO.getDescription())
                .morningActivities(createDTO.getMorningActivities())
                .afternoonActivities(createDTO.getAfternoonActivities())
                .eveningActivities(createDTO.getEveningActivities())
                .wildlifeHighlights(createDTO.getWildlifeHighlights())
                .scenicHighlights(createDTO.getScenicHighlights())
                .specialNotes(createDTO.getSpecialNotes())
                .startLocation(createDTO.getStartLocation())
                .endLocation(createDTO.getEndLocation())
                .distanceKm(createDTO.getDistanceKm())
                .isOvernight(createDTO.getIsOvernight() != null ? createDTO.getIsOvernight() : true)
                .mealsIncluded(createDTO.getMealsIncluded())
                .build();

            // Save day
            day = itineraryDayRepository.save(day);

            // Convert to DTO
            ItineraryDayDTO dayDTO = convertToDTO(day);

            log.info("Itinerary day created successfully: Day {} for itinerary {}", day.getDayNumber(), itinerary.getCode());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, "Itinerary day created successfully", dayDTO)
            );

        } catch (Exception e) {
            log.error("Error creating itinerary day", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to create itinerary day", "ITINERARY_DAY_CREATE_FAILED")
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
