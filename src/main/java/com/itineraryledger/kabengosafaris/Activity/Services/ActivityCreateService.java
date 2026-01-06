package com.itineraryledger.kabengosafaris.Activity.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Activity.Activity;
import com.itineraryledger.kabengosafaris.Activity.ActivityRepository;
import com.itineraryledger.kabengosafaris.Activity.DTOs.ActivityDTO;
import com.itineraryledger.kabengosafaris.Activity.DTOs.CreateActivityDTO;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ActivityCreateService - Service for creating activities
 */
@Service
@Slf4j
@Transactional
public class ActivityCreateService {

    private final ActivityRepository activityRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ActivityCreateService(
        ActivityRepository activityRepository,
        IdObfuscator idObfuscator
    ) {
        this.activityRepository = activityRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Create a new activity
     *
     * @param createActivityDTO The activity data
     * @return ResponseEntity with ApiResponse containing the created activity
     */
    @AuditLogAnnotation(action = "CREATE_ACTIVITY", description = "Creating a new activity", entityType = "Activity")
    public ResponseEntity<ApiResponse<?>> createActivity(CreateActivityDTO createActivityDTO) {
        log.info("Creating new activity: {}", createActivityDTO.getName());

        try {
            // Validate name uniqueness
            if (activityRepository.existsByName(createActivityDTO.getName())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Activity with name '" + createActivityDTO.getName() + "' already exists",
                        "ACTIVITY_NAME_EXISTS"
                    )
                );
            }

            // Generate slug if not provided
            String slug = createActivityDTO.getSlug();
            if (slug == null || slug.isEmpty()) {
                slug = generateSlug(createActivityDTO.getName());
            }

            // Validate slug uniqueness
            if (activityRepository.existsBySlug(slug)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Activity with slug '" + slug + "' already exists",
                        "ACTIVITY_SLUG_EXISTS"
                    )
                );
            }

            // Create activity entity
            Activity activity = Activity.builder()
                .name(createActivityDTO.getName())
                .slug(slug)
                .hasTariff(createActivityDTO.getHasTariff() != null ? createActivityDTO.getHasTariff() : false)
                .isWebActive(createActivityDTO.getIsWebActive() != null ? createActivityDTO.getIsWebActive() : true)
                .chargingBasis(createActivityDTO.getChargingBasis())
                .description(createActivityDTO.getDescription())
                .detailedDescription(createActivityDTO.getDetailedDescription())
                .minimumAge(createActivityDTO.getMinimumAge())
                .maximumParticipants(createActivityDTO.getMaximumParticipants())
                .equipmentRequired(createActivityDTO.getEquipmentRequired())
                .seasonAvailability(createActivityDTO.getSeasonAvailability())
                .primaryImage(createActivityDTO.getPrimaryImage())
                .tags(createActivityDTO.getTags())
                .safetyInformation(createActivityDTO.getSafetyInformation())
                .isActive(createActivityDTO.getIsActive() != null ? createActivityDTO.getIsActive() : true)
                .build();

            // Save activity
            activity = activityRepository.save(activity);

            // Convert to DTO
            ActivityDTO activityDTO = convertToDTO(activity);

            log.info("Activity created successfully: {}", activity.getName());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                    201,
                    "Activity created successfully",
                    activityDTO
                )
            );

        } catch (Exception e) {
            log.error("Error creating activity", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to create activity",
                    "ACTIVITY_CREATE_FAILED"
                )
            );
        }
    }

    /**
     * Generate URL-friendly slug from activity name
     */
    private String generateSlug(String name) {
        return name.toLowerCase()
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-")
            .trim();
    }

    /**
     * Convert Activity entity to ActivityDTO
     */
    private ActivityDTO convertToDTO(Activity activity) {
        ActivityDTO dto = new ActivityDTO();
        dto.setIdObfuscated(idObfuscator.encodeId(activity.getId()));
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
