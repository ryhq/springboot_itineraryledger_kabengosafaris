package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.Services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Safari.SafariDay.Repository.SafariDayRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.Entity.SafariDayActivity;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.Repository.SafariDayActivityRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.DTOs.SafariDayActivityDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * SafariDayActivityGetService - Service for retrieving safari day activities
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class SafariDayActivityGetService {

    private final SafariDayRepository safariDayRepository;
    private final SafariDayActivityRepository activityRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public SafariDayActivityGetService(
        SafariDayRepository safariDayRepository,
        SafariDayActivityRepository activityRepository,
        IdObfuscator idObfuscator
    ) {
        this.safariDayRepository = safariDayRepository;
        this.activityRepository = activityRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Get all activities for a safari day
     *
     * @param safariIdObfuscated The obfuscated safari ID
     * @param dayIdObfuscated The obfuscated day ID
     * @return ResponseEntity with ApiResponse containing list of activities
     */
    public ResponseEntity<ApiResponse<?>> getSafariDayActivities(
        String safariIdObfuscated,
        String dayIdObfuscated
    ) {
        log.info("Fetching activities for day: {}", dayIdObfuscated);

        try {
            // Decode IDs
            Long safariId;
            Long dayId;
            try {
                safariId = idObfuscator.decodeId(safariIdObfuscated);
                dayId = idObfuscator.decodeId(dayIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid ID format", "INVALID_ID")
                );
            }

            // Verify day exists and belongs to safari
            var dayOpt = safariDayRepository.findById(dayId);
            if (dayOpt.isEmpty()) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Safari day not found", "SAFARI_DAY_NOT_FOUND")
                );
            }

            if (!dayOpt.get().getSafari().getId().equals(safariId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Day does not belong to this safari", "DAY_SAFARI_MISMATCH")
                );
            }

            // Fetch activities
            List<SafariDayActivity> activities = activityRepository.findBySafariDayIdOrderBySortOrderAsc(dayId);

            // Convert to DTOs
            List<SafariDayActivityDTO> activityDTOs = activities.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Safari day activities retrieved successfully", activityDTOs)
            );

        } catch (Exception e) {
            log.error("Error fetching safari day activities", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch safari day activities", "SAFARI_DAY_ACTIVITIES_FETCH_FAILED")
            );
        }
    }

    /**
     * Get a specific activity by ID
     *
     * @param safariIdObfuscated The obfuscated safari ID
     * @param dayIdObfuscated The obfuscated day ID
     * @param activityIdObfuscated The obfuscated activity ID
     * @return ResponseEntity with ApiResponse containing the activity
     */
    public ResponseEntity<ApiResponse<?>> getSafariDayActivity(
        String safariIdObfuscated,
        String dayIdObfuscated,
        String activityIdObfuscated
    ) {
        log.info("Fetching activity {} for day: {}", activityIdObfuscated, dayIdObfuscated);

        try {
            // Decode IDs
            Long safariId;
            Long dayId;
            Long activityId;
            try {
                safariId = idObfuscator.decodeId(safariIdObfuscated);
                dayId = idObfuscator.decodeId(dayIdObfuscated);
                activityId = idObfuscator.decodeId(activityIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid ID format", "INVALID_ID")
                );
            }

            // Find activity
            SafariDayActivity activity = activityRepository.findById(activityId).orElse(null);
            if (activity == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Safari day activity not found", "SAFARI_DAY_ACTIVITY_NOT_FOUND")
                );
            }

            // Verify activity belongs to the day
            if (!activity.getSafariDay().getId().equals(dayId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Activity does not belong to this day", "ACTIVITY_DAY_MISMATCH")
                );
            }

            // Verify day belongs to the safari
            if (!activity.getSafariDay().getSafari().getId().equals(safariId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Day does not belong to this safari", "DAY_SAFARI_MISMATCH")
                );
            }

            // Convert to DTO
            SafariDayActivityDTO activityDTO = convertToDTO(activity);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Safari day activity retrieved successfully", activityDTO)
            );

        } catch (Exception e) {
            log.error("Error fetching safari day activity", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch safari day activity", "SAFARI_DAY_ACTIVITY_FETCH_FAILED")
            );
        }
    }

    /**
     * Convert SafariDayActivity entity to SafariDayActivityDTO
     */
    private SafariDayActivityDTO convertToDTO(SafariDayActivity activity) {
        SafariDayActivityDTO dto = new SafariDayActivityDTO();
        dto.setId(idObfuscator.encodeId(activity.getId()));
        dto.setSafariDayId(idObfuscator.encodeId(activity.getSafariDay().getId()));
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
        dto.setIsCompleted(activity.getIsCompleted());
        dto.setCompletedAt(activity.getCompletedAt());
        dto.setActualStartTime(activity.getActualStartTime());
        dto.setActualEndTime(activity.getActualEndTime());
        dto.setFeedback(activity.getFeedback());
        dto.setIsSkipped(activity.getIsSkipped());
        dto.setSkipReason(activity.getSkipReason());
        dto.setCreatedAt(activity.getCreatedAt());
        return dto;
    }
}
