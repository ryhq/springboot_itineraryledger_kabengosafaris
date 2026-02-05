package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Activity.Activity;
import com.itineraryledger.kabengosafaris.Activity.ActivityRepository;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.ParkActivity.ParkActivityRepository;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.Repository.SafariRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.Entity.SafariDay;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.Repository.SafariDayRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.Entity.SafariDayActivity;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.Repository.SafariDayActivityRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.DTOs.CreateSafariDayActivityDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.DTOs.SafariDayActivityDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * SafariDayActivityCreateService - Service for creating safari day activities
 *
 * This service creates standalone activities for safari days.
 * Standalone activities are activities NOT linked to any park.
 *
 * Validation rules:
 * - Safari must be editable (not COMPLETED, CANCELLED, etc.)
 * - Activity must exist
 * - Activity must be active (isActive = true)
 * - Activity must NOT be linked to any park (not in parks_activities table)
 *
 * Note: The same activity can be added multiple times per day (e.g., morning and evening game drives).
 * For park-specific activities, use SafariDayParkActivity instead.
 */
@Service
@Slf4j
@Transactional
public class SafariDayActivityCreateService {

    private final SafariDayRepository safariDayRepository;
    private final SafariRepository safariRepository;
    private final SafariDayActivityRepository activityRepository;
    private final ActivityRepository baseActivityRepository;
    private final ParkActivityRepository parkActivityRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public SafariDayActivityCreateService(
        SafariDayRepository safariDayRepository,
        SafariRepository safariRepository,
        SafariDayActivityRepository activityRepository,
        ActivityRepository baseActivityRepository,
        ParkActivityRepository parkActivityRepository,
        IdObfuscator idObfuscator
    ) {
        this.safariDayRepository = safariDayRepository;
        this.safariRepository = safariRepository;
        this.activityRepository = activityRepository;
        this.baseActivityRepository = baseActivityRepository;
        this.parkActivityRepository = parkActivityRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Create a new safari day activity
     *
     * Sort order is auto-determined based on existing activities in the day.
     * First activity = 1, subsequent activities increment from there.
     *
     * @param safariIdObfuscated The obfuscated safari ID
     * @param dayIdObfuscated The obfuscated day ID
     * @param createDTO The activity data
     * @return ResponseEntity with ApiResponse containing the created activity
     */
    @AuditLogAnnotation(action = "CREATE_SAFARI_DAY_ACTIVITY", description = "Creating a new safari day activity", entityType = "SafariDayActivity")
    public ResponseEntity<ApiResponse<?>> createSafariDayActivity(
        String safariIdObfuscated,
        String dayIdObfuscated,
        CreateSafariDayActivityDTO createDTO
    ) {
        log.info("Creating new activity for day: {}", dayIdObfuscated);

        try {
            // Decode IDs
            Long safariId;
            Long dayId;
            Long baseActivityId;
            try {
                safariId = idObfuscator.decodeId(safariIdObfuscated);
                dayId = idObfuscator.decodeId(dayIdObfuscated);
                baseActivityId = idObfuscator.decodeId(createDTO.getActivityId());
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid ID format", "INVALID_ID")
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

            // Find safari day
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

            // Find base activity
            Activity baseActivity = baseActivityRepository.findById(baseActivityId).orElse(null);
            if (baseActivity == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Activity not found", "ACTIVITY_NOT_FOUND")
                );
            }

            // Validate activity is active
            if (!Boolean.TRUE.equals(baseActivity.getIsActive())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Activity '" + baseActivity.getName() + "' is not active",
                        "ACTIVITY_NOT_ACTIVE"
                    )
                );
            }

            // Validate activity is standalone (not linked to any park)
            // SafariDayActivity is for standalone activities only.
            // Park-specific activities should use SafariDayParkActivity instead.
            long parkLinkCount = parkActivityRepository.countByActivityId(baseActivityId);
            if (parkLinkCount > 0) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Activity '" + baseActivity.getName() + "' is a park activity. Use park activities endpoint instead.",
                        "ACTIVITY_IS_PARK_LINKED"
                    )
                );
            }

            // Count existing activities for this day to determine sortOrder
            long existingCount = activityRepository.countBySafariDayId(dayId);
            int nextSortOrder = (int) existingCount + 1;

            // Create activity entity
            SafariDayActivity activity = SafariDayActivity.builder()
                .safariDay(day)
                .activity(baseActivity)
                .sortOrder(nextSortOrder)
                .durationHours(createDTO.getDurationHours())
                .startTime(createDTO.getStartTime())
                .endTime(createDTO.getEndTime())
                .notes(createDTO.getNotes())
                .isIncludedInPrice(createDTO.getIsIncludedInPrice() != null ? createDTO.getIsIncludedInPrice() : true)
                .isOptional(createDTO.getIsOptional() != null ? createDTO.getIsOptional() : false)
                .isCompleted(false)
                .isSkipped(false)
                .build();

            // Save activity
            activity = activityRepository.save(activity);

            // Convert to DTO
            SafariDayActivityDTO activityDTO = convertToDTO(activity);

            log.info("Safari day activity created successfully: {} (sortOrder: {}) for day {}",
                baseActivity.getName(), activity.getSortOrder(), day.getDayTag());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, "Safari day activity created successfully", activityDTO)
            );

        } catch (Exception e) {
            log.error("Error creating safari day activity", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to create safari day activity", "SAFARI_DAY_ACTIVITY_CREATE_FAILED")
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
