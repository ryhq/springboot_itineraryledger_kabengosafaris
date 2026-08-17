package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.Services;

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

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.Entity.ItineraryDayParkActivity;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.Repository.ItineraryDayParkActivityRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.DTOs.ItineraryDayParkActivityDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryDayParkActivityGetService - Service for retrieving park activities within a park visit
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class ItineraryDayParkActivityGetService {

    private final ItineraryDayParkActivityRepository parkActivityRepository;
    private final com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;
    private final IdObfuscator idObfuscator;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "sortOrder", "durationHours", "startTime", "endTime",
        "isIncludedInPrice", "createdAt"
    );
    private static final String DEFAULT_SORT_FIELD = "sortOrder";

    @Autowired
    public ItineraryDayParkActivityGetService(
        ItineraryDayParkActivityRepository parkActivityRepository,
        IdObfuscator idObfuscator
    ,
        com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation
    ) {
        this.parkActivityRepository = parkActivityRepository;
        this.idObfuscator = idObfuscator;
        this.recordNavigation = recordNavigation;
    }

    /**
     * Get all activities for a park visit
     *
     * @param parkVisitIdObfuscated The obfuscated park visit ID
     * @param sortBy The field to sort by
     * @param sortDirection The sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing list of activities
     */
    public ResponseEntity<ApiResponse<?>> getParkActivities(
            String parkVisitIdObfuscated,
            String sortBy,
            String sortDirection
    ) {
        log.info("Fetching activities for park visit: {}", parkVisitIdObfuscated);

        try {
            Long parkVisitId;
            try {
                parkVisitId = idObfuscator.decodeId(parkVisitIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid park visit ID", "INVALID_ID")
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

            List<ItineraryDayParkActivity> activities = parkActivityRepository.findByItineraryDayParkIdOrderBySortOrderAsc(parkVisitId);
            List<ItineraryDayParkActivityDTO> dtos = activities.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            // Response
            Map<String, Object> response = new HashMap<>();
            response.put("parkActivities", dtos);
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", sortDirection != null ? sortDirection : "asc");

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Park activities retrieved", response)
            );

        } catch (Exception e) {
            log.error("Error fetching park activities", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch park activities", "FETCH_FAILED")
            );
        }
    }

    /**
     * Get a single activity by ID
     *
     * @param parkVisitIdObfuscated The obfuscated park visit ID
     * @param activityIdObfuscated The obfuscated activity entry ID
     * @return ResponseEntity with ApiResponse containing the activity
     */
    public ResponseEntity<ApiResponse<?>> getParkActivity(
        String parkVisitIdObfuscated,
        String activityIdObfuscated,
        /* the sort travels with the record so its arrows keep the list's order */
        String sortBy,
        String sortDirection
    ) {
        log.info("Fetching activity {} for park visit: {}", activityIdObfuscated, parkVisitIdObfuscated);

        try {
            Long parkVisitId;
            Long activityId;
            try {
                parkVisitId = idObfuscator.decodeId(parkVisitIdObfuscated);
                activityId = idObfuscator.decodeId(activityIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid ID", "INVALID_ID")
                );
            }

            ItineraryDayParkActivity activity = parkActivityRepository.findById(activityId).orElse(null);
            if (activity == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Park activity not found", "PARK_ACTIVITY_NOT_FOUND")
                );
            }

            // Verify ownership
            if (!activity.getItineraryDayPark().getId().equals(parkVisitId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Activity does not belong to this park visit", "OWNERSHIP_MISMATCH")
                );
            }

            // Convert to DTO
            ItineraryDayParkActivityDTO activityDTO = convertToDTO(activity);

            /*
             * Parent-scoped circular navigation, in the ORDER THE LIST USED. The repository
             * walk this replaces stepped by id whatever the sort was, so the arrows moved
             * through a different sequence from the one on screen — and could not say where
             * in it you were.
             */
            Long parentId = activity.getItineraryDayPark().getId();
            String validatedSortBy = validateSortField(sortBy);
            Map<String, Object> nav = recordNavigation.navigate(
                ItineraryDayParkActivity.class,
                (root, query, cb) -> cb.equal(root.get("itineraryDayPark").get("id"), parentId),
                validatedSortBy != null ? validatedSortBy : DEFAULT_SORT_FIELD,
                !"desc".equalsIgnoreCase(sortDirection),
                activityId
            );
            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("parkActivity", activityDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Park activity retrieved", response)
            );

        } catch (Exception e) {
            log.error("Error fetching park activity", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch park activity", "FETCH_FAILED")
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

    private ItineraryDayParkActivityDTO convertToDTO(ItineraryDayParkActivity entity) {
        ItineraryDayParkActivityDTO dto = new ItineraryDayParkActivityDTO();
        dto.setId(idObfuscator.encodeId(entity.getId()));
        dto.setItineraryDayParkId(idObfuscator.encodeId(entity.getItineraryDayPark().getId()));
        dto.setParkId(idObfuscator.encodeId(entity.getParkActivity().getPark().getId()));
        dto.setParkName(entity.getParkActivity().getPark().getName());
        dto.setActivityId(idObfuscator.encodeId(entity.getParkActivity().getActivity().getId()));
        dto.setActivityName(entity.getParkActivity().getActivity().getName());
        dto.setSortOrder(entity.getSortOrder());
        dto.setDurationHours(entity.getDurationHours());
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());
        dto.setNotes(entity.getNotes());
        dto.setIsIncludedInPrice(entity.getIsIncludedInPrice());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
