package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.DTOs.ItineraryDayParkActivityDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.DTOs.UpdateItineraryDayParkActivityDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.Entity.ItineraryDayParkActivity;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.Repository.ItineraryDayParkActivityRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryDayParkActivityUpdateService - Service for updating itinerary day park activities
 */
@Service
@Slf4j
@Transactional
public class ItineraryDayParkActivityUpdateService {

    private final ItineraryDayParkActivityRepository itineraryDayParkActivityRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ItineraryDayParkActivityUpdateService(
        ItineraryDayParkActivityRepository itineraryDayParkActivityRepository,
        IdObfuscator idObfuscator
    ) {
        this.itineraryDayParkActivityRepository = itineraryDayParkActivityRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Update an itinerary day park activity
     *
     * @param parkVisitIdObfuscated The obfuscated park visit ID
     * @param activityIdObfuscated The obfuscated activity ID
     * @param updateDTO The update data
     * @return ResponseEntity with ApiResponse containing the updated activity
     */
    @AuditLogAnnotation(action = "UPDATE_ITINERARY_DAY_PARK_ACTIVITY", description = "Updating park activity", entityType = "ItineraryDayParkActivity")
    public ResponseEntity<ApiResponse<?>> updateParkActivity(
        String parkVisitIdObfuscated,
        String activityIdObfuscated,
        UpdateItineraryDayParkActivityDTO updateDTO
    ) {
        log.info("Updating park activity: {} in park visit: {}", activityIdObfuscated, parkVisitIdObfuscated);

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
            ItineraryDayParkActivity activity = itineraryDayParkActivityRepository.findById(activityId).orElse(null);
            if (activity == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Park activity not found", "PARK_ACTIVITY_NOT_FOUND")
                );
            }

            // Verify ownership chain
            if (!activity.getItineraryDayPark().getId().equals(parkVisitId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Activity does not belong to this park visit", "ACTIVITY_PARK_VISIT_MISMATCH")
                );
            }

            // Update fields if provided (sortOrder is handled by reorder methods)
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

            // Save
            activity = itineraryDayParkActivityRepository.save(activity);

            // Convert to DTO
            ItineraryDayParkActivityDTO dto = convertToDTO(activity);

            log.info("Park activity updated successfully: {}", activityId);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Park activity updated successfully", dto)
            );

        } catch (Exception e) {
            log.error("Error updating park activity", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update park activity", "ITINERARY_DAY_PARK_ACTIVITY_UPDATE_FAILED")
            );
        }
    }

    /**
     * Convert ItineraryDayParkActivity entity to DTO
     */
    private ItineraryDayParkActivityDTO convertToDTO(ItineraryDayParkActivity activity) {
        ItineraryDayParkActivityDTO dto = new ItineraryDayParkActivityDTO();
        dto.setId(idObfuscator.encodeId(activity.getId()));
        dto.setItineraryDayParkId(idObfuscator.encodeId(activity.getItineraryDayPark().getId()));
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
        dto.setCreatedAt(activity.getCreatedAt());
        return dto;
    }
}
