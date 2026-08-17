package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Services;

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

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Repository.ItineraryDayRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Entity.ItineraryDayPark;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Repository.ItineraryDayParkRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.DTOs.ItineraryDayParkDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryDayParkGetService - Service for retrieving itinerary day park visits
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class ItineraryDayParkGetService {

    private final ItineraryDayRepository itineraryDayRepository;
    private final com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;
    private final ItineraryDayParkRepository itineraryDayParkRepository;
    private final IdObfuscator idObfuscator;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "sortOrder", "entryType", "arrivalTime", "departureTime", "createdAt"
    );
    private static final String DEFAULT_SORT_FIELD = "sortOrder";

    @Autowired
    public ItineraryDayParkGetService(
        ItineraryDayRepository itineraryDayRepository,
        ItineraryDayParkRepository itineraryDayParkRepository,
        IdObfuscator idObfuscator
    ,
        com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation
    ) {
        this.itineraryDayRepository = itineraryDayRepository;
        this.itineraryDayParkRepository = itineraryDayParkRepository;
        this.idObfuscator = idObfuscator;
        this.recordNavigation = recordNavigation;
    }

    /**
     * Get all park visits for an itinerary day
     *
     * @param itineraryIdObfuscated The obfuscated itinerary ID
     * @param dayIdObfuscated The obfuscated day ID
     * @param sortBy The field to sort by
     * @param sortDirection The sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing list of park visits
     */
    public ResponseEntity<ApiResponse<?>> getItineraryDayParks(
            String itineraryIdObfuscated,
            String dayIdObfuscated,
            String sortBy,
            String sortDirection
    ) {
        log.info("Fetching parks for day: {}", dayIdObfuscated);

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

            // Sorting with validation
            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                log.warn("Invalid sort field: {}", sortBy);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }

            // Fetch park visits
            List<ItineraryDayPark> parks = itineraryDayParkRepository.findByItineraryDayIdOrderBySortOrderAsc(dayId);

            // Convert to DTOs
            List<ItineraryDayParkDTO> parkDTOs = parks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            // Response
            Map<String, Object> response = new HashMap<>();
            response.put("parkVisits", parkDTOs);
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", sortDirection != null ? sortDirection : "asc");

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Park visits retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error fetching park visits", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch park visits", "ITINERARY_DAY_PARKS_FETCH_FAILED")
            );
        }
    }

    /**
     * Get a single park visit by ID
     *
     * @param itineraryIdObfuscated The obfuscated itinerary ID
     * @param dayIdObfuscated The obfuscated day ID
     * @param parkVisitIdObfuscated The obfuscated park visit ID
     * @return ResponseEntity with ApiResponse containing the park visit
     */
    public ResponseEntity<ApiResponse<?>> getItineraryDayPark(
        String itineraryIdObfuscated,
        String dayIdObfuscated,
        String parkVisitIdObfuscated,
        /* the sort travels with the record so its arrows keep the list's order */
        String sortBy,
        String sortDirection
    ) {
        log.info("Fetching park visit: {}", parkVisitIdObfuscated);

        try {
            // Decode IDs
            Long itineraryId;
            Long dayId;
            Long parkVisitId;
            try {
                itineraryId = idObfuscator.decodeId(itineraryIdObfuscated);
                dayId = idObfuscator.decodeId(dayIdObfuscated);
                parkVisitId = idObfuscator.decodeId(parkVisitIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid ID", "INVALID_ID")
                );
            }

            // Find park visit
            ItineraryDayPark parkVisit = itineraryDayParkRepository.findById(parkVisitId).orElse(null);
            if (parkVisit == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Park visit not found", "PARK_VISIT_NOT_FOUND")
                );
            }

            // Verify ownership chain
            if (!parkVisit.getItineraryDay().getId().equals(dayId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Park visit does not belong to this day", "OWNERSHIP_MISMATCH")
                );
            }
            if (!parkVisit.getItineraryDay().getItinerary().getId().equals(itineraryId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Day does not belong to this itinerary", "OWNERSHIP_MISMATCH")
                );
            }

            // Convert to DTO
            ItineraryDayParkDTO parkVisitDTO = convertToDTO(parkVisit);

            /*
             * Parent-scoped circular navigation, in the ORDER THE LIST USED. The repository
             * walk this replaces stepped by id whatever the sort was, so the arrows moved
             * through a different sequence from the one on screen — and could not say where
             * in it you were.
             */
            Long parentId = parkVisit.getItineraryDay().getId();
            String validatedSortBy = validateSortField(sortBy);
            Map<String, Object> nav = recordNavigation.navigate(
                ItineraryDayPark.class,
                (root, query, cb) -> cb.equal(root.get("itineraryDay").get("id"), parentId),
                validatedSortBy != null ? validatedSortBy : DEFAULT_SORT_FIELD,
                !"desc".equalsIgnoreCase(sortDirection),
                parkVisitId
            );
            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("parkVisit", parkVisitDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Park visit retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error fetching park visit", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch park visit", "PARK_VISIT_FETCH_FAILED")
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
     * Convert ItineraryDayPark entity to DTO
     */
    private ItineraryDayParkDTO convertToDTO(ItineraryDayPark dayPark) {
        ItineraryDayParkDTO dto = new ItineraryDayParkDTO();
        dto.setId(idObfuscator.encodeId(dayPark.getId()));
        dto.setItineraryDayId(idObfuscator.encodeId(dayPark.getItineraryDay().getId()));
        dto.setParkId(idObfuscator.encodeId(dayPark.getPark().getId()));
        dto.setParkName(dayPark.getPark().getName());
        dto.setParkSlug(dayPark.getPark().getSlug());
        dto.setEntryType(dayPark.getEntryType());
        dto.setEntryTypeDisplayName(dayPark.getEntryType().getDisplayName());
        dto.setSortOrder(dayPark.getSortOrder());
        dto.setArrivalTime(dayPark.getArrivalTime());
        dto.setDepartureTime(dayPark.getDepartureTime());
        dto.setNotes(dayPark.getNotes());
        dto.setCreatedAt(dayPark.getCreatedAt());
        return dto;
    }
}
