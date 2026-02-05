package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.Services;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.Entity.SafariDayActivity;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.Repository.SafariDayActivityRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.DTOs.SafariDayActivityDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.DTOs.UpdateSafariDayActivityDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * SafariDayActivityUpdateService - Service for updating safari day activities
 */
@Service
@Slf4j
@Transactional
public class SafariDayActivityUpdateService {

    private final SafariDayActivityRepository activityRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public SafariDayActivityUpdateService(
        SafariDayActivityRepository activityRepository,
        IdObfuscator idObfuscator
    ) {
        this.activityRepository = activityRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Update a safari day activity
     *
     * @param safariIdObfuscated The obfuscated safari ID
     * @param dayIdObfuscated The obfuscated day ID
     * @param activityIdObfuscated The obfuscated activity ID
     * @param updateDTO The updated activity data
     * @return ResponseEntity with ApiResponse containing the updated activity
     */
    @AuditLogAnnotation(action = "UPDATE_SAFARI_DAY_ACTIVITY", description = "Updating safari day activity", entityType = "SafariDayActivity", entityIdParamName = "activityIdObfuscated")
    public ResponseEntity<ApiResponse<?>> updateSafariDayActivity(
        String safariIdObfuscated,
        String dayIdObfuscated,
        String activityIdObfuscated,
        UpdateSafariDayActivityDTO updateDTO
    ) {
        log.info("Updating activity {} for day: {}", activityIdObfuscated, dayIdObfuscated);

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

            // Check if safari is editable (for non-operational updates)
            Safari safari = activity.getSafariDay().getSafari();
            boolean isOperationalUpdate = updateDTO.getIsCompleted() != null ||
                updateDTO.getActualStartTime() != null ||
                updateDTO.getActualEndTime() != null ||
                updateDTO.getFeedback() != null ||
                updateDTO.getIsSkipped() != null ||
                updateDTO.getSkipReason() != null;

            if (!isOperationalUpdate && !safari.isEditable()) {
                log.warn("Safari is not editable: {} (state: {})", safari.getCode(), safari.getState());
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Safari cannot be edited in state: " + safari.getState().getDisplayName(),
                        "SAFARI_NOT_EDITABLE"
                    )
                );
            }

            // Note: sortOrder and activityId are not updatable - use reorder endpoint for sortOrder

            // Update fields if provided
            if (updateDTO.getDurationHours() != null) {
                activity.setDurationHours(updateDTO.getDurationHours());
            }
            if (updateDTO.getStartTime() != null) {
                activity.setStartTime(updateDTO.getStartTime());
            }
            if (updateDTO.getEndTime() != null) {
                activity.setEndTime(updateDTO.getEndTime());
            }
            if (updateDTO.getNotes() != null) {
                activity.setNotes(updateDTO.getNotes());
            }
            if (updateDTO.getIsIncludedInPrice() != null) {
                activity.setIsIncludedInPrice(updateDTO.getIsIncludedInPrice());
            }
            if (updateDTO.getIsOptional() != null) {
                activity.setIsOptional(updateDTO.getIsOptional());
            }

            // Safari-specific fields
            if (updateDTO.getIsCompleted() != null) {
                activity.setIsCompleted(updateDTO.getIsCompleted());
                // Auto-set completedAt timestamp when marking as completed
                if (Boolean.TRUE.equals(updateDTO.getIsCompleted())) {
                    activity.setCompletedAt(LocalDateTime.now());
                } else {
                    activity.setCompletedAt(null);
                }
            }
            if (updateDTO.getActualStartTime() != null) {
                activity.setActualStartTime(updateDTO.getActualStartTime());
            }
            if (updateDTO.getActualEndTime() != null) {
                activity.setActualEndTime(updateDTO.getActualEndTime());
            }
            if (updateDTO.getFeedback() != null) {
                activity.setFeedback(updateDTO.getFeedback());
            }
            if (updateDTO.getIsSkipped() != null) {
                activity.setIsSkipped(updateDTO.getIsSkipped());
            }
            if (updateDTO.getSkipReason() != null) {
                activity.setSkipReason(updateDTO.getSkipReason());
            }

            // Save updated activity
            activity = activityRepository.save(activity);

            // Convert to DTO
            SafariDayActivityDTO activityDTO = convertToDTO(activity);

            log.info("Safari day activity updated successfully: {}", activity.getActivity().getName());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Safari day activity updated successfully", activityDTO)
            );

        } catch (Exception e) {
            log.error("Error updating safari day activity", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update safari day activity", "SAFARI_DAY_ACTIVITY_UPDATE_FAILED")
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
