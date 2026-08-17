package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.Services;

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
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.Entity.SafariDayPark;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.Repository.SafariDayParkRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.DTOs.SafariDayParkDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * SafariDayParkGetService - Service for retrieving safari day park visits
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class SafariDayParkGetService {

    private final SafariDayRepository safariDayRepository;
    private final com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;
    private final SafariDayParkRepository safariDayParkRepository;
    private final IdObfuscator idObfuscator;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "sortOrder", "entryType", "arrivalTime", "departureTime",
        "feesPaid", "weatherConditions", "createdAt"
    );
    private static final String DEFAULT_SORT_FIELD = "sortOrder";

    @Autowired
    public SafariDayParkGetService(
        SafariDayRepository safariDayRepository,
        SafariDayParkRepository safariDayParkRepository,
        IdObfuscator idObfuscator
    ,
        com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation
    ) {
        this.safariDayRepository = safariDayRepository;
        this.safariDayParkRepository = safariDayParkRepository;
        this.idObfuscator = idObfuscator;
        this.recordNavigation = recordNavigation;
    }

    /**
     * Get all park visits for a safari day
     *
     * @param safariIdObfuscated The obfuscated safari ID
     * @param dayIdObfuscated The obfuscated day ID
     * @return ResponseEntity with ApiResponse containing list of park visits
     */
    public ResponseEntity<ApiResponse<?>> getSafariDayParks(String safariIdObfuscated, String dayIdObfuscated, String sortBy, String sortDirection) {
        log.info("Fetching parks for safari day: {}", dayIdObfuscated);

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

            // Fetch park visits
            List<SafariDayPark> parks = safariDayParkRepository.findBySafariDayIdOrderBySortOrderAsc(dayId);

            // Convert to DTOs
            List<SafariDayParkDTO> parkDTOs = parks.stream()
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
            log.error("Error fetching safari day park visits", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch park visits", "SAFARI_DAY_PARKS_FETCH_FAILED")
            );
        }
    }

    /**
     * Get a specific park visit by ID
     *
     * @param safariIdObfuscated The obfuscated safari ID
     * @param dayIdObfuscated The obfuscated day ID
     * @param parkVisitIdObfuscated The obfuscated park visit ID
     * @return ResponseEntity with ApiResponse containing the park visit
     */
    public ResponseEntity<ApiResponse<?>> getSafariDayPark(String safariIdObfuscated, String dayIdObfuscated, String parkVisitIdObfuscated,
        /* the sort travels with the record so its arrows keep the list's order */
        String sortBy,
        String sortDirection
    ) {
        log.info("Fetching park visit {} for safari day: {}", parkVisitIdObfuscated, dayIdObfuscated);

        try {
            // Decode IDs
            Long safariId;
            Long dayId;
            Long parkVisitId;
            try {
                safariId = idObfuscator.decodeId(safariIdObfuscated);
                dayId = idObfuscator.decodeId(dayIdObfuscated);
                parkVisitId = idObfuscator.decodeId(parkVisitIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid ID", "INVALID_ID")
                );
            }

            // Find park visit
            SafariDayPark parkVisit = safariDayParkRepository.findById(parkVisitId).orElse(null);
            if (parkVisit == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Park visit not found", "SAFARI_DAY_PARK_NOT_FOUND")
                );
            }

            // Verify park visit belongs to the safari day
            if (!parkVisit.getSafariDay().getId().equals(dayId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Park visit does not belong to this safari day", "PARK_VISIT_DAY_MISMATCH")
                );
            }

            // Verify day belongs to the safari
            if (!parkVisit.getSafariDay().getSafari().getId().equals(safariId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Safari day does not belong to this safari", "DAY_SAFARI_MISMATCH")
                );
            }

            // Convert to DTO
            SafariDayParkDTO parkDTO = convertToDTO(parkVisit);

            /*
             * Parent-scoped circular navigation, in the ORDER THE LIST USED. The repository
             * walk this replaces stepped by id whatever the sort was, so the arrows moved
             * through a different sequence from the one on screen — and could not say where
             * in it you were.
             */
            Long parentId = parkVisit.getSafariDay().getId();
            String validatedSortBy = validateSortField(sortBy);
            Map<String, Object> nav = recordNavigation.navigate(
                SafariDayPark.class,
                (root, query, cb) -> cb.equal(root.get("safariDay").get("id"), parentId),
                validatedSortBy != null ? validatedSortBy : DEFAULT_SORT_FIELD,
                !"desc".equalsIgnoreCase(sortDirection),
                parkVisitId
            );
            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("parkVisit", parkDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Park visit retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error fetching safari day park visit", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch park visit", "SAFARI_DAY_PARK_FETCH_FAILED")
            );
        }
    }

    /**
     * Convert SafariDayPark entity to SafariDayParkDTO
     */
    private SafariDayParkDTO convertToDTO(SafariDayPark parkVisit) {
        SafariDayParkDTO dto = new SafariDayParkDTO();
        dto.setId(idObfuscator.encodeId(parkVisit.getId()));
        dto.setSafariDayId(idObfuscator.encodeId(parkVisit.getSafariDay().getId()));
        dto.setParkId(idObfuscator.encodeId(parkVisit.getPark().getId()));
        dto.setParkName(parkVisit.getPark().getName());
        dto.setParkSlug(parkVisit.getPark().getSlug());
        dto.setEntryType(parkVisit.getEntryType());
        dto.setEntryTypeDisplayName(parkVisit.getEntryType().getDisplayName());
        dto.setSortOrder(parkVisit.getSortOrder());
        dto.setArrivalTime(parkVisit.getArrivalTime());
        dto.setDepartureTime(parkVisit.getDepartureTime());
        dto.setNotes(parkVisit.getNotes());

        // Safari-specific fields
        dto.setActualArrivalTime(parkVisit.getActualArrivalTime());
        dto.setActualDepartureTime(parkVisit.getActualDepartureTime());
        dto.setEntryReceiptNumber(parkVisit.getEntryReceiptNumber());
        dto.setWildlifeSightings(parkVisit.getWildlifeSightings());
        dto.setVisitNotes(parkVisit.getVisitNotes());
        dto.setFeesPaid(parkVisit.getFeesPaid());
        dto.setFeesPaidAt(parkVisit.getFeesPaidAt());
        dto.setWeatherConditions(parkVisit.getWeatherConditions());

        dto.setCreatedAt(parkVisit.getCreatedAt());

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
