package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Services;

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
    private final com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;
    private final ItineraryDayRepository itineraryDayRepository;
    private final IdObfuscator idObfuscator;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "dayNumber", "title", "dayTag", "startLocation", "endLocation",
        "distanceKm", "isOvernight", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "dayNumber";

    @Autowired
    public ItineraryDayGetService(
        ItineraryRepository itineraryRepository,
        ItineraryDayRepository itineraryDayRepository,
        IdObfuscator idObfuscator
    ,
        com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation
    ) {
        this.itineraryRepository = itineraryRepository;
        this.itineraryDayRepository = itineraryDayRepository;
        this.idObfuscator = idObfuscator;
        this.recordNavigation = recordNavigation;
    }

    /**
     * Get all days for an itinerary
     *
     * @param itineraryIdObfuscated The obfuscated itinerary ID
     * @param sortBy The field to sort by
     * @param sortDirection The sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing list of days
     */
    public ResponseEntity<ApiResponse<?>> getItineraryDays(
            String itineraryIdObfuscated,
            String sortBy,
            String sortDirection
    ) {
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

            // Sorting with validation
            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                log.warn("Invalid sort field: {}", sortBy);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }

            // Fetch days
            List<ItineraryDay> days = itineraryDayRepository.findByItineraryIdOrderByDayNumberAsc(itineraryId);

            // Convert to DTOs
            List<ItineraryDayDTO> dayDTOs = days.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            // Response
            Map<String, Object> response = new HashMap<>();
            response.put("itineraryDays", dayDTOs);
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", sortDirection != null ? sortDirection : "asc");

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Itinerary days retrieved successfully", response)
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
    public ResponseEntity<ApiResponse<?>> getItineraryDay(String itineraryIdObfuscated, String dayIdObfuscated,
        /* the sort travels with the record so its arrows keep the list's order */
        String sortBy,
        String sortDirection
    ) {
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

            /*
             * Parent-scoped circular navigation, in the ORDER THE LIST USED. The repository
             * walk this replaces stepped by id whatever the sort was, so the arrows moved
             * through a different sequence from the one on screen — and could not say where
             * in it you were.
             */
            Long parentId = day.getItinerary().getId();
            String validatedSortBy = validateSortField(sortBy);
            Map<String, Object> nav = recordNavigation.navigate(
                ItineraryDay.class,
                (root, query, cb) -> cb.equal(root.get("itinerary").get("id"), parentId),
                validatedSortBy != null ? validatedSortBy : DEFAULT_SORT_FIELD,
                !"desc".equalsIgnoreCase(sortDirection),
                dayId
            );
            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("itineraryDay", dayDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Itinerary day retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error fetching itinerary day", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch itinerary day", "ITINERARY_DAY_FETCH_FAILED")
            );
        }
    }

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
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
