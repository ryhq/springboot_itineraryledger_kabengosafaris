package com.itineraryledger.kabengosafaris.Activity.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Activity.ChargingBasis;
import com.itineraryledger.kabengosafaris.Activity.Activity;
import com.itineraryledger.kabengosafaris.Activity.ActivityRepository;
import com.itineraryledger.kabengosafaris.Activity.DTOs.ActivityDTO;
import com.itineraryledger.kabengosafaris.Activity.DTOs.UpdateActivityDTO;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ActivityUpdateService - Service for updating activities
 */
@Service
@Slf4j
@Transactional
public class ActivityUpdateService {

    private final ActivityRepository activityRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ActivityUpdateService(
        ActivityRepository activityRepository,
        IdObfuscator idObfuscator
    ) {
        this.activityRepository = activityRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Update an activity by obfuscated ID
     *
     * @param idObfuscated The obfuscated activity ID
     * @param updateActivityDTO The updated activity data
     * @return ResponseEntity with ApiResponse containing the updated activity
     */
    @AuditLogAnnotation(action = "UPDATE_ACTIVITY", description = "Updating activity", entityType = "Activity", entityIdParamName = "idObfuscated")
    public ResponseEntity<ApiResponse<?>> updateActivity(String idObfuscated, UpdateActivityDTO updateActivityDTO) {
        log.info("Updating activity with ID: {}", idObfuscated);

        try {
            // Decode ID
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode activity ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid activity ID",
                        "INVALID_ACTIVITY_ID"
                    )
                );
            }

            return updateActivityById(id, updateActivityDTO);

        } catch (Exception e) {
            log.error("Error updating activity", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to update activity",
                    "ACTIVITY_UPDATE_FAILED"
                )
            );
        }
    }

    /**
     * Update an activity by ID (internal method)
     */
    private ResponseEntity<ApiResponse<?>> updateActivityById(Long id, UpdateActivityDTO updateActivityDTO) {
        // Find activity
        Activity activity = activityRepository.findById(id).orElse(null);
        if (activity == null) {
            return ResponseEntity.status(404).body(
                ApiResponse.error(
                    404,
                    "Activity not found",
                    "ACTIVITY_NOT_FOUND"
                )
            );
        }

        // Check if name is being changed and if it's unique
        if (updateActivityDTO.getName() != null && !updateActivityDTO.getName().equals(activity.getName())) {
            if (activityRepository.existsByName(updateActivityDTO.getName())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Activity with name '" + updateActivityDTO.getName() + "' already exists",
                        "ACTIVITY_NAME_EXISTS"
                    )
                );
            }
            activity.setName(updateActivityDTO.getName());
        }

        // Check if slug is being changed and if it's unique
        if (updateActivityDTO.getSlug() != null && !updateActivityDTO.getSlug().equals(activity.getSlug())) {
            if (activityRepository.existsBySlug(updateActivityDTO.getSlug())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Activity with slug '" + updateActivityDTO.getSlug() + "' already exists",
                        "ACTIVITY_SLUG_EXISTS"
                    )
                );
            }
            activity.setSlug(updateActivityDTO.getSlug());
        }

        // Update other fields if provided
        if (updateActivityDTO.getHasTariff() != null) {
            activity.setHasTariff(updateActivityDTO.getHasTariff());
        }
        if (updateActivityDTO.getIsWebActive() != null) {
            activity.setIsWebActive(updateActivityDTO.getIsWebActive());
        }
        if (updateActivityDTO.getChargingBasis() != null) {
            // blank clears it; anything else must be a real ChargingBasis
            String raw = updateActivityDTO.getChargingBasis().trim();
            activity.setChargingBasis(raw.isEmpty() ? null : ChargingBasis.valueOf(raw));
        }
        if (updateActivityDTO.getDescription() != null) {
            activity.setDescription(updateActivityDTO.getDescription());
        }
        if (updateActivityDTO.getDetailedDescription() != null) {
            activity.setDetailedDescription(updateActivityDTO.getDetailedDescription());
        }
        if (updateActivityDTO.getMinimumAge() != null) {
            activity.setMinimumAge(updateActivityDTO.getMinimumAge());
        }
        if (updateActivityDTO.getMaximumParticipants() != null) {
            activity.setMaximumParticipants(updateActivityDTO.getMaximumParticipants());
        }
        if (updateActivityDTO.getEquipmentRequired() != null) {
            activity.setEquipmentRequired(updateActivityDTO.getEquipmentRequired());
        }
        if (updateActivityDTO.getSeasonAvailability() != null) {
            activity.setSeasonAvailability(updateActivityDTO.getSeasonAvailability());
        }
        if (updateActivityDTO.getPrimaryImage() != null) {
            activity.setPrimaryImage(updateActivityDTO.getPrimaryImage());
        }
        if (updateActivityDTO.getTags() != null) {
            activity.setTags(updateActivityDTO.getTags());
        }
        if (updateActivityDTO.getSafetyInformation() != null) {
            activity.setSafetyInformation(updateActivityDTO.getSafetyInformation());
        }
        if (updateActivityDTO.getIsActive() != null) {
            activity.setIsActive(updateActivityDTO.getIsActive());
        }

        // Save updated activity
        activity = activityRepository.save(activity);

        // Convert to DTO
        ActivityDTO activityDTO = convertToDTO(activity);

        log.info("Activity updated successfully: {}", activity.getName());

        return ResponseEntity.ok().body(
            ApiResponse.success(
                200,
                "Activity updated successfully",
                activityDTO
            )
        );
    }

    /**
     * Convert Activity entity to ActivityDTO
     */
    private ActivityDTO convertToDTO(Activity activity) {
        ActivityDTO dto = new ActivityDTO();
        dto.setId(idObfuscator.encodeId(activity.getId()));
        dto.setName(activity.getName());
        dto.setSlug(activity.getSlug());
        dto.setHasTariff(activity.getHasTariff());
        dto.setIsWebActive(activity.getIsWebActive());
        dto.setChargingBasis(activity.getChargingBasis());
        dto.setDescription(activity.getDescription());
        dto.setDetailedDescription(activity.getDetailedDescription());
        dto.setMinimumAge(activity.getMinimumAge());
        dto.setMaximumParticipants(activity.getMaximumParticipants());
        dto.setEquipmentRequired(activity.getEquipmentRequired());
        dto.setSeasonAvailability(activity.getSeasonAvailability());
        dto.setPrimaryImage(activity.getPrimaryImage());
        dto.setTags(activity.getTags());
        dto.setSafetyInformation(activity.getSafetyInformation());
        dto.setIsActive(activity.getIsActive());
        dto.setCreatedAt(activity.getCreatedAt());
        dto.setUpdatedAt(activity.getUpdatedAt());
        return dto;
    }
}
