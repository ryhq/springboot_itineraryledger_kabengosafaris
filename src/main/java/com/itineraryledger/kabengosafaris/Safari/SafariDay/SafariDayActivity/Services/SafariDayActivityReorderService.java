package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.Entity.SafariDay;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.Repository.SafariDayRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.Entity.SafariDayActivity;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.Repository.SafariDayActivityRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.DTOs.SafariDayActivityDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.DTOs.ReorderSafariDayActivitiesDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.DTOs.ReorderSafariDayActivitiesDTO.ActivityOrderItem;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SafariDayActivityReorderService - Service for reordering safari day activities
 *
 * Handles drag-and-drop reordering from the UI with comprehensive validations:
 * - Validates safari is editable
 * - Validates all activity IDs exist and belong to the day
 * - Validates no duplicate activity IDs
 * - Validates all activities are included (no missing activities)
 * - Validates expected sort orders if provided
 * - Updates sort orders
 */
@Service
@Slf4j
@Transactional
public class SafariDayActivityReorderService {

    private final SafariDayRepository safariDayRepository;
    private final SafariDayActivityRepository activityRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public SafariDayActivityReorderService(
        SafariDayRepository safariDayRepository,
        SafariDayActivityRepository activityRepository,
        IdObfuscator idObfuscator
    ) {
        this.safariDayRepository = safariDayRepository;
        this.activityRepository = activityRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Reorder safari day activities based on the new order provided
     *
     * @param safariIdObfuscated The obfuscated safari ID
     * @param dayIdObfuscated The obfuscated day ID
     * @param reorderDTO The reorder data containing the new activity order
     * @return ResponseEntity with ApiResponse containing the reordered activities
     */
    @AuditLogAnnotation(action = "REORDER_SAFARI_DAY_ACTIVITIES", description = "Reordering safari day activities", entityType = "SafariDayActivity")
    public ResponseEntity<ApiResponse<?>> reorderSafariDayActivities(
        String safariIdObfuscated,
        String dayIdObfuscated,
        ReorderSafariDayActivitiesDTO reorderDTO
    ) {
        log.info("Reordering activities for day: {}", dayIdObfuscated);

        try {
            // ========================
            // DECODE IDs
            // ========================
            Long safariId;
            Long dayId;
            try {
                safariId = idObfuscator.decodeId(safariIdObfuscated);
                dayId = idObfuscator.decodeId(dayIdObfuscated);
            } catch (Exception e) {
                log.warn("Invalid ID format: safari={}, day={}", safariIdObfuscated, dayIdObfuscated);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid ID format", "INVALID_ID")
                );
            }

            // ========================
            // FIND SAFARI DAY
            // ========================
            SafariDay day = safariDayRepository.findById(dayId).orElse(null);
            if (day == null) {
                log.warn("Safari day not found: {}", dayId);
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Safari day not found", "SAFARI_DAY_NOT_FOUND")
                );
            }

            // Verify day belongs to safari
            if (!day.getSafari().getId().equals(safariId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Day does not belong to this safari", "DAY_SAFARI_MISMATCH")
                );
            }

            // Check if safari is editable
            Safari safari = day.getSafari();
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

            // ========================
            // FETCH EXISTING ACTIVITIES
            // ========================
            List<SafariDayActivity> existingActivities = activityRepository.findBySafariDayIdOrderBySortOrderAsc(dayId);

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
                .map(SafariDayActivity::getId)
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
            Map<Long, SafariDayActivity> activityLookup = existingActivities.stream()
                .collect(Collectors.toMap(SafariDayActivity::getId, activity -> activity));

            // ========================
            // CHECK IF REORDER IS ACTUALLY NEEDED
            // ========================
            boolean orderChanged = false;
            int newSortOrder = 1;

            for (Long activityId : decodedActivityIds.keySet()) {
                SafariDayActivity activity = activityLookup.get(activityId);
                if (!activity.getSortOrder().equals(newSortOrder)) {
                    orderChanged = true;
                    break;
                }
                newSortOrder++;
            }

            if (!orderChanged) {
                log.info("Activity order unchanged for day: {}", day.getDayTag());
                List<SafariDayActivityDTO> resultDTOs = existingActivities.stream()
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
            for (SafariDayActivity activity : existingActivities) {
                activity.setSortOrder(tempOrder--);
            }
            activityRepository.saveAll(existingActivities);
            activityRepository.flush();

            // Pass 2: Set final sort orders based on new order
            List<SafariDayActivity> reorderedActivities = new ArrayList<>();
            newSortOrder = 1;

            for (Long activityId : decodedActivityIds.keySet()) {
                SafariDayActivity activity = activityLookup.get(activityId);
                activity.setSortOrder(newSortOrder);
                reorderedActivities.add(activity);
                newSortOrder++;
            }

            // Save all reordered activities
            reorderedActivities = activityRepository.saveAll(reorderedActivities);

            // ========================
            // CONVERT TO DTOs AND RETURN
            // ========================
            List<SafariDayActivityDTO> resultDTOs = reorderedActivities.stream()
                .sorted(Comparator.comparing(SafariDayActivity::getSortOrder))
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            log.info("Successfully reordered {} activities for day: {}", reorderedActivities.size(), day.getDayTag());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Activities reordered successfully", resultDTOs)
            );

        } catch (Exception e) {
            log.error("Error reordering safari day activities", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to reorder safari day activities", "SAFARI_DAY_ACTIVITIES_REORDER_FAILED")
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
