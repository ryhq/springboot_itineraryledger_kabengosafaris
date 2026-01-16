package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Entity.ItineraryDay;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Repository.ItineraryDayRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.Entity.ItineraryDayActivity;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.Repository.ItineraryDayActivityRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.DTOs.ItineraryDayActivityDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.DTOs.ReorderItineraryDayActivitiesDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.DTOs.ReorderItineraryDayActivitiesDTO.ActivityOrderItem;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ItineraryDayActivityReorderService - Service for reordering itinerary day activities
 *
 * Handles drag-and-drop reordering from the UI with comprehensive validations:
 * - Validates all activity IDs exist and belong to the day
 * - Validates no duplicate activity IDs
 * - Validates all activities are included (no missing activities)
 * - Validates expected sort orders if provided
 * - Updates sort orders
 */
@Service
@Slf4j
@Transactional
public class ItineraryDayActivityReorderService {

    private final ItineraryDayRepository itineraryDayRepository;
    private final ItineraryDayActivityRepository activityRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ItineraryDayActivityReorderService(
        ItineraryDayRepository itineraryDayRepository,
        ItineraryDayActivityRepository activityRepository,
        IdObfuscator idObfuscator
    ) {
        this.itineraryDayRepository = itineraryDayRepository;
        this.activityRepository = activityRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Reorder itinerary day activities based on the new order provided
     *
     * @param itineraryIdObfuscated The obfuscated itinerary ID
     * @param dayIdObfuscated The obfuscated day ID
     * @param reorderDTO The reorder data containing the new activity order
     * @return ResponseEntity with ApiResponse containing the reordered activities
     */
    @AuditLogAnnotation(action = "REORDER_ITINERARY_DAY_ACTIVITIES", description = "Reordering itinerary day activities", entityType = "ItineraryDayActivity")
    public ResponseEntity<ApiResponse<?>> reorderItineraryDayActivities(
        String itineraryIdObfuscated,
        String dayIdObfuscated,
        ReorderItineraryDayActivitiesDTO reorderDTO
    ) {
        log.info("Reordering activities for day: {}", dayIdObfuscated);

        try {
            // ========================
            // DECODE IDs
            // ========================
            Long itineraryId;
            Long dayId;
            try {
                itineraryId = idObfuscator.decodeId(itineraryIdObfuscated);
                dayId = idObfuscator.decodeId(dayIdObfuscated);
            } catch (Exception e) {
                log.warn("Invalid ID format: itinerary={}, day={}", itineraryIdObfuscated, dayIdObfuscated);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid ID format", "INVALID_ID")
                );
            }

            // ========================
            // FIND ITINERARY DAY
            // ========================
            ItineraryDay day = itineraryDayRepository.findById(dayId).orElse(null);
            if (day == null) {
                log.warn("Itinerary day not found: {}", dayId);
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Itinerary day not found", "ITINERARY_DAY_NOT_FOUND")
                );
            }

            // Verify day belongs to itinerary
            if (!day.getItinerary().getId().equals(itineraryId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Day does not belong to this itinerary", "DAY_ITINERARY_MISMATCH")
                );
            }

            // ========================
            // FETCH EXISTING ACTIVITIES
            // ========================
            List<ItineraryDayActivity> existingActivities = activityRepository.findByItineraryDayIdOrderBySortOrderAsc(dayId);

            if (existingActivities.isEmpty()) {
                log.warn("No activities found for day: {}", dayId);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Day has no activities to reorder", "NO_ACTIVITIES_TO_REORDER")
                );
            }

            // ========================
            // VALIDATION: Check activity order list size matches existing activities
            // ========================
            List<ActivityOrderItem> activityOrder = reorderDTO.getActivityOrder();

            if (activityOrder.size() != existingActivities.size()) {
                log.warn("Activity order count mismatch. Expected: {}, Received: {}", existingActivities.size(), activityOrder.size());
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Activity order list must contain exactly " + existingActivities.size() + " activities. Received: " + activityOrder.size(),
                        "ACTIVITY_COUNT_MISMATCH"
                    )
                );
            }

            // ========================
            // DECODE ALL ACTIVITY IDs AND VALIDATE FORMAT
            // ========================
            Map<Long, ActivityOrderItem> decodedActivityIds = new LinkedHashMap<>();
            List<String> invalidIds = new ArrayList<>();
            List<String> duplicateIds = new ArrayList<>();

            for (ActivityOrderItem item : activityOrder) {
                if (item.getActivityId() == null || item.getActivityId().isBlank()) {
                    invalidIds.add("null/empty");
                    continue;
                }

                try {
                    Long decodedId = idObfuscator.decodeId(item.getActivityId());

                    // Check for duplicates
                    if (decodedActivityIds.containsKey(decodedId)) {
                        duplicateIds.add(item.getActivityId());
                    } else {
                        decodedActivityIds.put(decodedId, item);
                    }
                } catch (Exception e) {
                    invalidIds.add(item.getActivityId());
                }
            }

            if (!invalidIds.isEmpty()) {
                log.warn("Invalid activity ID formats: {}", invalidIds);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid activity ID format(s): " + String.join(", ", invalidIds),
                        "INVALID_ACTIVITY_ID_FORMAT"
                    )
                );
            }

            if (!duplicateIds.isEmpty()) {
                log.warn("Duplicate activity IDs in reorder list: {}", duplicateIds);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Duplicate activity ID(s) in reorder list: " + String.join(", ", duplicateIds),
                        "DUPLICATE_ACTIVITY_IDS"
                    )
                );
            }

            // ========================
            // VALIDATION: All activities belong to this day
            // ========================
            Set<Long> existingActivityIds = existingActivities.stream()
                .map(ItineraryDayActivity::getId)
                .collect(Collectors.toSet());

            Set<Long> providedActivityIds = decodedActivityIds.keySet();

            // Check for activities that don't belong to this day
            Set<Long> foreignActivities = new HashSet<>(providedActivityIds);
            foreignActivities.removeAll(existingActivityIds);

            if (!foreignActivities.isEmpty()) {
                List<String> foreignActivityObfuscated = foreignActivities.stream()
                    .map(id -> idObfuscator.encodeId(id))
                    .collect(Collectors.toList());
                log.warn("Activity IDs not belonging to day: {}", foreignActivityObfuscated);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Activity ID(s) do not belong to this day: " + String.join(", ", foreignActivityObfuscated),
                        "ACTIVITY_DAY_MISMATCH"
                    )
                );
            }

            // Check for missing activities
            Set<Long> missingActivities = new HashSet<>(existingActivityIds);
            missingActivities.removeAll(providedActivityIds);

            if (!missingActivities.isEmpty()) {
                List<String> missingActivityObfuscated = missingActivities.stream()
                    .map(id -> idObfuscator.encodeId(id))
                    .collect(Collectors.toList());
                log.warn("Missing activity IDs in reorder list: {}", missingActivityObfuscated);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Missing activity ID(s) in reorder list: " + String.join(", ", missingActivityObfuscated),
                        "MISSING_ACTIVITY_IDS"
                    )
                );
            }

            // ========================
            // VALIDATION: Expected sort orders (if provided)
            // ========================
            List<String> expectedOrderMismatches = new ArrayList<>();
            int position = 1;

            for (ActivityOrderItem item : activityOrder) {
                if (item.getExpectedSortOrder() != null && !item.getExpectedSortOrder().equals(position)) {
                    expectedOrderMismatches.add(
                        String.format("Activity %s: expected %d, but position is %d",
                            item.getActivityId(), item.getExpectedSortOrder(), position)
                    );
                }
                position++;
            }

            if (!expectedOrderMismatches.isEmpty()) {
                log.warn("Expected sort order mismatches: {}", expectedOrderMismatches);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Expected sort order mismatches: " + String.join("; ", expectedOrderMismatches),
                        "EXPECTED_ORDER_MISMATCH"
                    )
                );
            }

            // ========================
            // CREATE ACTIVITY LOOKUP MAP
            // ========================
            Map<Long, ItineraryDayActivity> activityLookup = existingActivities.stream()
                .collect(Collectors.toMap(ItineraryDayActivity::getId, activity -> activity));

            // ========================
            // CHECK IF REORDER IS ACTUALLY NEEDED
            // ========================
            boolean orderChanged = false;
            int newSortOrder = 1;

            for (Long activityId : decodedActivityIds.keySet()) {
                ItineraryDayActivity activity = activityLookup.get(activityId);
                if (!activity.getSortOrder().equals(newSortOrder)) {
                    orderChanged = true;
                    break;
                }
                newSortOrder++;
            }

            if (!orderChanged) {
                log.info("Activity order unchanged for day: {}", day.getDayTag());
                List<ItineraryDayActivityDTO> resultDTOs = existingActivities.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

                return ResponseEntity.ok().body(
                    ApiResponse.success(200, "Activity order unchanged", resultDTOs)
                );
            }

            // ========================
            // PERFORM REORDER
            // ========================
            log.info("Performing reorder for {} activities", existingActivities.size());

            // Pass 1: Set temporary negative sort orders to avoid unique constraint violations (if any)
            int tempOrder = -1;
            for (ItineraryDayActivity activity : existingActivities) {
                activity.setSortOrder(tempOrder--);
            }
            activityRepository.saveAll(existingActivities);
            activityRepository.flush();

            // Pass 2: Set final sort orders based on new order
            List<ItineraryDayActivity> reorderedActivities = new ArrayList<>();
            newSortOrder = 1;

            for (Long activityId : decodedActivityIds.keySet()) {
                ItineraryDayActivity activity = activityLookup.get(activityId);
                activity.setSortOrder(newSortOrder);
                reorderedActivities.add(activity);
                newSortOrder++;
            }

            // Save all reordered activities
            reorderedActivities = activityRepository.saveAll(reorderedActivities);

            // ========================
            // CONVERT TO DTOs AND RETURN
            // ========================
            List<ItineraryDayActivityDTO> resultDTOs = reorderedActivities.stream()
                .sorted(Comparator.comparing(ItineraryDayActivity::getSortOrder))
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            log.info("Successfully reordered {} activities for day: {}", reorderedActivities.size(), day.getDayTag());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Activities reordered successfully", resultDTOs)
            );

        } catch (Exception e) {
            log.error("Error reordering itinerary day activities", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to reorder itinerary day activities", "ITINERARY_DAY_ACTIVITIES_REORDER_FAILED")
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
