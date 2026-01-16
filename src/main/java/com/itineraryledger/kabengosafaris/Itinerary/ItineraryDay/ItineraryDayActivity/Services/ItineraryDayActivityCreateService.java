package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Activity.Activity;
import com.itineraryledger.kabengosafaris.Activity.ActivityRepository;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.ParkActivity.ParkActivityRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Entity.ItineraryDay;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Repository.ItineraryDayRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.Entity.ItineraryDayActivity;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.Repository.ItineraryDayActivityRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.DTOs.CreateItineraryDayActivityDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.DTOs.ItineraryDayActivityDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryDayActivityCreateService - Service for creating itinerary day activities
 *
 * This service creates standalone activities for itinerary days.
 * Standalone activities are activities NOT linked to any park.
 *
 * Validation rules:
 * - Activity must exist
 * - Activity must be active (isActive = true)
 * - Activity must NOT be linked to any park (not in parks_activities table)
 * - Activity must not already exist for this day
 *
 * For park-specific activities, use ItineraryDayParkActivity instead.
 */
@Service
@Slf4j
@Transactional
public class ItineraryDayActivityCreateService {

    private final ItineraryDayRepository itineraryDayRepository;
    private final ItineraryDayActivityRepository activityRepository;
    private final ActivityRepository baseActivityRepository;
    private final ParkActivityRepository parkActivityRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ItineraryDayActivityCreateService(
        ItineraryDayRepository itineraryDayRepository,
        ItineraryDayActivityRepository activityRepository,
        ActivityRepository baseActivityRepository,
        ParkActivityRepository parkActivityRepository,
        IdObfuscator idObfuscator
    ) {
        this.itineraryDayRepository = itineraryDayRepository;
        this.activityRepository = activityRepository;
        this.baseActivityRepository = baseActivityRepository;
        this.parkActivityRepository = parkActivityRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Create a new itinerary day activity
     *
     * Sort order is auto-determined based on existing activities in the day.
     * First activity = 1, subsequent activities increment from there.
     *
     * @param itineraryIdObfuscated The obfuscated itinerary ID
     * @param dayIdObfuscated The obfuscated day ID
     * @param createDTO The activity data
     * @return ResponseEntity with ApiResponse containing the created activity
     */
    @AuditLogAnnotation(action = "CREATE_ITINERARY_DAY_ACTIVITY", description = "Creating a new itinerary day activity", entityType = "ItineraryDayActivity")
    public ResponseEntity<ApiResponse<?>> createItineraryDayActivity(
        String itineraryIdObfuscated,
        String dayIdObfuscated,
        CreateItineraryDayActivityDTO createDTO
    ) {
        log.info("Creating new activity for day: {}", dayIdObfuscated);

        try {
            // Decode IDs
            Long itineraryId;
            Long dayId;
            Long baseActivityId;
            try {
                itineraryId = idObfuscator.decodeId(itineraryIdObfuscated);
                dayId = idObfuscator.decodeId(dayIdObfuscated);
                baseActivityId = idObfuscator.decodeId(createDTO.getActivityId());
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid ID format", "INVALID_ID")
                );
            }

            // Find itinerary day
            ItineraryDay day = itineraryDayRepository.findById(dayId).orElse(null);
            if (day == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Itinerary day not found", "ITINERARY_DAY_NOT_FOUND")
                );
            }

            // Verify day belongs to the itinerary
            if (!day.getItinerary().getId().equals(itineraryId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Day does not belong to this itinerary", "DAY_ITINERARY_MISMATCH")
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
            // ItineraryDayActivity is for standalone activities only.
            // Park-specific activities should use ItineraryDayParkActivity instead.
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

            // Check if activity already exists for this day
            if (activityRepository.existsByItineraryDayIdAndActivityId(dayId, baseActivityId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Activity already exists for this day", "ACTIVITY_ALREADY_EXISTS")
                );
            }

            // Count existing activities for this day to determine sortOrder
            long existingCount = activityRepository.countByItineraryDayId(dayId);
            int nextSortOrder = (int) existingCount + 1;

            // Create activity entity
            ItineraryDayActivity activity = ItineraryDayActivity.builder()
                .itineraryDay(day)
                .activity(baseActivity)
                .sortOrder(nextSortOrder)
                .durationHours(createDTO.getDurationHours())
                .startTime(createDTO.getStartTime())
                .endTime(createDTO.getEndTime())
                .notes(createDTO.getNotes())
                .isIncludedInPrice(createDTO.getIsIncludedInPrice() != null ? createDTO.getIsIncludedInPrice() : true)
                .isOptional(createDTO.getIsOptional() != null ? createDTO.getIsOptional() : false)
                .build();

            // Save activity
            activity = activityRepository.save(activity);

            // Convert to DTO
            ItineraryDayActivityDTO activityDTO = convertToDTO(activity);

            log.info("Itinerary day activity created successfully: {} (sortOrder: {}) for day {}",
                baseActivity.getName(), activity.getSortOrder(), day.getDayTag());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, "Itinerary day activity created successfully", activityDTO)
            );

        } catch (Exception e) {
            log.error("Error creating itinerary day activity", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to create itinerary day activity", "ITINERARY_DAY_ACTIVITY_CREATE_FAILED")
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
