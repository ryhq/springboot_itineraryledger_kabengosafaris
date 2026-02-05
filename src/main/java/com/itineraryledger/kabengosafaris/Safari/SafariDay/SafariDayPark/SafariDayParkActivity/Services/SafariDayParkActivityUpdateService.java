package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.Services;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.DTOs.SafariDayParkActivityDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.DTOs.UpdateSafariDayParkActivityDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.Entity.SafariDayParkActivity;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.Repository.SafariDayParkActivityRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * SafariDayParkActivityUpdateService - Service for updating safari day park activities
 *
 * Implements dual update modes:
 * - Planning updates (duration, times, notes, pricing) require editable safari state
 * - Operational updates (completion, sightings, feedback) allowed anytime
 */
@Service
@Slf4j
@Transactional
public class SafariDayParkActivityUpdateService {

    private final SafariDayParkActivityRepository safariDayParkActivityRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public SafariDayParkActivityUpdateService(
        SafariDayParkActivityRepository safariDayParkActivityRepository,
        IdObfuscator idObfuscator
    ) {
        this.safariDayParkActivityRepository = safariDayParkActivityRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Update a safari day park activity
     *
     * @param parkVisitIdObfuscated The obfuscated park visit ID
     * @param activityIdObfuscated The obfuscated activity ID
     * @param updateDTO The update data
     * @return ResponseEntity with ApiResponse containing the updated activity
     */
    @AuditLogAnnotation(action = "UPDATE_SAFARI_DAY_PARK_ACTIVITY", description = "Updating safari park activity", entityType = "SafariDayParkActivity")
    public ResponseEntity<ApiResponse<?>> updateParkActivity(
        String parkVisitIdObfuscated,
        String activityIdObfuscated,
        UpdateSafariDayParkActivityDTO updateDTO
    ) {
        log.info("Updating safari park activity: {} in park visit: {}", activityIdObfuscated, parkVisitIdObfuscated);

        try {
            // Decode IDs
            Long parkVisitId;
            Long activityId;
            try {
                parkVisitId = idObfuscator.decodeId(parkVisitIdObfuscated);
                activityId = idObfuscator.decodeId(activityIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid ID format", "INVALID_ID")
                );
            }

            // Find activity
            SafariDayParkActivity activity = safariDayParkActivityRepository.findById(activityId).orElse(null);
            if (activity == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Safari park activity not found", "SAFARI_PARK_ACTIVITY_NOT_FOUND")
                );
            }

            // Verify ownership chain
            if (!activity.getSafariDayPark().getId().equals(parkVisitId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Activity does not belong to this park visit", "ACTIVITY_PARK_VISIT_MISMATCH")
                );
            }

            // Determine if this is an operational update
            boolean isOperationalUpdate = updateDTO.getIsCompleted() != null ||
                updateDTO.getActualDurationHours() != null ||
                updateDTO.getSightingsNotes() != null ||
                updateDTO.getGuestExperience() != null ||
                updateDTO.getIsSkipped() != null ||
                updateDTO.getSkipReason() != null;

            // Check if safari is editable for planning updates
            Safari safari = activity.getSafariDayPark().getSafariDay().getSafari();
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

            // Update planning fields if provided (sortOrder is handled by reorder methods)
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

            // Update Safari-specific operational fields
            if (updateDTO.getIsCompleted() != null) {
                activity.setIsCompleted(updateDTO.getIsCompleted());
                // Auto-set completedAt timestamp when marking as completed
                if (Boolean.TRUE.equals(updateDTO.getIsCompleted())) {
                    activity.setCompletedAt(LocalDateTime.now());
                } else {
                    activity.setCompletedAt(null);
                }
            }

            if (updateDTO.getActualDurationHours() != null) {
                activity.setActualDurationHours(updateDTO.getActualDurationHours());
            }

            if (updateDTO.getSightingsNotes() != null) {
                activity.setSightingsNotes(updateDTO.getSightingsNotes());
            }

            if (updateDTO.getGuestExperience() != null) {
                activity.setGuestExperience(updateDTO.getGuestExperience());
            }

            if (updateDTO.getIsSkipped() != null) {
                activity.setIsSkipped(updateDTO.getIsSkipped());
            }

            if (updateDTO.getSkipReason() != null) {
                activity.setSkipReason(updateDTO.getSkipReason());
            }

            // Save
            activity = safariDayParkActivityRepository.save(activity);

            // Convert to DTO
            SafariDayParkActivityDTO dto = convertToDTO(activity);

            log.info("Safari park activity updated successfully: {}", activityId);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Safari park activity updated successfully", dto)
            );

        } catch (Exception e) {
            log.error("Error updating safari park activity", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update safari park activity", "SAFARI_DAY_PARK_ACTIVITY_UPDATE_FAILED")
            );
        }
    }

    /**
     * Convert SafariDayParkActivity entity to DTO
     */
    private SafariDayParkActivityDTO convertToDTO(SafariDayParkActivity activity) {
        SafariDayParkActivityDTO dto = new SafariDayParkActivityDTO();
        dto.setId(idObfuscator.encodeId(activity.getId()));
        dto.setSafariDayParkId(idObfuscator.encodeId(activity.getSafariDayPark().getId()));
        dto.setParkId(idObfuscator.encodeId(activity.getParkActivity().getPark().getId()));
        dto.setParkName(activity.getParkActivity().getPark().getName());
        dto.setActivityId(idObfuscator.encodeId(activity.getParkActivity().getActivity().getId()));
        dto.setActivityName(activity.getParkActivity().getActivity().getName());
        dto.setSortOrder(activity.getSortOrder());
        dto.setDurationHours(activity.getDurationHours());
        dto.setStartTime(activity.getStartTime());
        dto.setEndTime(activity.getEndTime());
        dto.setNotes(activity.getNotes());
        dto.setIsIncludedInPrice(activity.getIsIncludedInPrice());

        // Safari-specific fields
        dto.setIsCompleted(activity.getIsCompleted());
        dto.setCompletedAt(activity.getCompletedAt());
        dto.setActualDurationHours(activity.getActualDurationHours());
        dto.setSightingsNotes(activity.getSightingsNotes());
        dto.setGuestExperience(activity.getGuestExperience());
        dto.setIsSkipped(activity.getIsSkipped());
        dto.setSkipReason(activity.getSkipReason());

        dto.setCreatedAt(activity.getCreatedAt());
        return dto;
    }
}
