package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.Entity.ItineraryDayActivity;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.Repository.ItineraryDayActivityRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.DTOs.ItineraryDayActivityDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.DTOs.UpdateItineraryDayActivityDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryDayActivityUpdateService - Service for updating itinerary day activities
 */
@Service
@Slf4j
@Transactional
public class ItineraryDayActivityUpdateService {

    private final ItineraryDayActivityRepository activityRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ItineraryDayActivityUpdateService(
        ItineraryDayActivityRepository activityRepository,
        IdObfuscator idObfuscator
    ) {
        this.activityRepository = activityRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Update an itinerary day activity
     *
     * @param itineraryIdObfuscated The obfuscated itinerary ID
     * @param dayIdObfuscated The obfuscated day ID
     * @param activityIdObfuscated The obfuscated activity ID
     * @param updateDTO The updated activity data
     * @return ResponseEntity with ApiResponse containing the updated activity
     */
    @AuditLogAnnotation(action = "UPDATE_ITINERARY_DAY_ACTIVITY", description = "Updating itinerary day activity", entityType = "ItineraryDayActivity", entityIdParamName = "activityIdObfuscated")
    public ResponseEntity<ApiResponse<?>> updateItineraryDayActivity(
        String itineraryIdObfuscated,
        String dayIdObfuscated,
        String activityIdObfuscated,
        UpdateItineraryDayActivityDTO updateDTO
    ) {
        log.info("Updating activity {} for day: {}", activityIdObfuscated, dayIdObfuscated);

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

            // Save updated activity
            activity = activityRepository.save(activity);

            // Convert to DTO
            ItineraryDayActivityDTO activityDTO = convertToDTO(activity);

            log.info("Itinerary day activity updated successfully: {}", activity.getActivity().getName());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Itinerary day activity updated successfully", activityDTO)
            );

        } catch (Exception e) {
            log.error("Error updating itinerary day activity", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update itinerary day activity", "ITINERARY_DAY_ACTIVITY_UPDATE_FAILED")
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
