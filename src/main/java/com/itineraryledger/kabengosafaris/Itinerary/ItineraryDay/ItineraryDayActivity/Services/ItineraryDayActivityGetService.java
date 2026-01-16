package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.Services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Repository.ItineraryDayRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.Entity.ItineraryDayActivity;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.Repository.ItineraryDayActivityRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.DTOs.ItineraryDayActivityDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryDayActivityGetService - Service for retrieving itinerary day activities
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class ItineraryDayActivityGetService {

    private final ItineraryDayRepository itineraryDayRepository;
    private final ItineraryDayActivityRepository activityRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ItineraryDayActivityGetService(
        ItineraryDayRepository itineraryDayRepository,
        ItineraryDayActivityRepository activityRepository,
        IdObfuscator idObfuscator
    ) {
        this.itineraryDayRepository = itineraryDayRepository;
        this.activityRepository = activityRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Get all activities for an itinerary day
     *
     * @param itineraryIdObfuscated The obfuscated itinerary ID
     * @param dayIdObfuscated The obfuscated day ID
     * @return ResponseEntity with ApiResponse containing list of activities
     */
    public ResponseEntity<ApiResponse<?>> getItineraryDayActivities(
        String itineraryIdObfuscated,
        String dayIdObfuscated
    ) {
        log.info("Fetching activities for day: {}", dayIdObfuscated);

        try {
            // Decode IDs
            Long itineraryId;
            Long dayId;
            try {
                itineraryId = idObfuscator.decodeId(itineraryIdObfuscated);
                dayId = idObfuscator.decodeId(dayIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid ID format", "INVALID_ID")
                );
            }

            // Verify day exists and belongs to itinerary
            var dayOpt = itineraryDayRepository.findById(dayId);
            if (dayOpt.isEmpty()) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Itinerary day not found", "ITINERARY_DAY_NOT_FOUND")
                );
            }

            if (!dayOpt.get().getItinerary().getId().equals(itineraryId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Day does not belong to this itinerary", "DAY_ITINERARY_MISMATCH")
                );
            }

            // Fetch activities
            List<ItineraryDayActivity> activities = activityRepository.findByItineraryDayIdOrderBySortOrderAsc(dayId);

            // Convert to DTOs
            List<ItineraryDayActivityDTO> activityDTOs = activities.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Itinerary day activities retrieved successfully", activityDTOs)
            );

        } catch (Exception e) {
            log.error("Error fetching itinerary day activities", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch itinerary day activities", "ITINERARY_DAY_ACTIVITIES_FETCH_FAILED")
            );
        }
    }

    /**
     * Get a specific activity by ID
     *
     * @param itineraryIdObfuscated The obfuscated itinerary ID
     * @param dayIdObfuscated The obfuscated day ID
     * @param activityIdObfuscated The obfuscated activity ID
     * @return ResponseEntity with ApiResponse containing the activity
     */
    public ResponseEntity<ApiResponse<?>> getItineraryDayActivity(
        String itineraryIdObfuscated,
        String dayIdObfuscated,
        String activityIdObfuscated
    ) {
        log.info("Fetching activity {} for day: {}", activityIdObfuscated, dayIdObfuscated);

        try {
            // Decode IDs
            Long itineraryId;
            Long dayId;
            Long activityId;
            try {
                itineraryId = idObfuscator.decodeId(itineraryIdObfuscated);
                dayId = idObfuscator.decodeId(dayIdObfuscated);
                activityId = idObfuscator.decodeId(activityIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid ID format", "INVALID_ID")
                );
            }

            // Find activity
            ItineraryDayActivity activity = activityRepository.findById(activityId).orElse(null);
            if (activity == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Itinerary day activity not found", "ITINERARY_DAY_ACTIVITY_NOT_FOUND")
                );
            }

            // Verify activity belongs to the day
            if (!activity.getItineraryDay().getId().equals(dayId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Activity does not belong to this day", "ACTIVITY_DAY_MISMATCH")
                );
            }

            // Verify day belongs to the itinerary
            if (!activity.getItineraryDay().getItinerary().getId().equals(itineraryId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Day does not belong to this itinerary", "DAY_ITINERARY_MISMATCH")
                );
            }

            // Convert to DTO
            ItineraryDayActivityDTO activityDTO = convertToDTO(activity);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Itinerary day activity retrieved successfully", activityDTO)
            );

        } catch (Exception e) {
            log.error("Error fetching itinerary day activity", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch itinerary day activity", "ITINERARY_DAY_ACTIVITY_FETCH_FAILED")
            );
        }
    }

    /**
     * Convert ItineraryDayActivity entity to ItineraryDayActivityDTO
     */
    private ItineraryDayActivityDTO convertToDTO(ItineraryDayActivity activity) {
        ItineraryDayActivityDTO dto = new ItineraryDayActivityDTO();
        dto.setId(idObfuscator.encodeId(activity.getId()));
        dto.setItineraryDayId(idObfuscator.encodeId(activity.getItineraryDay().getId()));
        dto.setActivityId(idObfuscator.encodeId(activity.getActivity().getId()));
        dto.setActivityName(activity.getActivity().getName());
        dto.setActivitySlug(activity.getActivity().getSlug());
        dto.setSortOrder(activity.getSortOrder());
        dto.setDurationHours(activity.getDurationHours());
        dto.setStartTime(activity.getStartTime());
        dto.setEndTime(activity.getEndTime());
        dto.setNotes(activity.getNotes());
        dto.setIsIncludedInPrice(activity.getIsIncludedInPrice());
        dto.setIsOptional(activity.getIsOptional());
        dto.setCreatedAt(activity.getCreatedAt());
        return dto;
    }
}
