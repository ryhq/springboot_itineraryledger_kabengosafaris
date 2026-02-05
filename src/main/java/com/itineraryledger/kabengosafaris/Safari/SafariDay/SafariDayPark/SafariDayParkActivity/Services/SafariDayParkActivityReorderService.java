package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.Entity.SafariDayPark;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.Repository.SafariDayParkRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.DTOs.SafariDayParkActivityDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.DTOs.ReorderSafariDayParkActivitiesDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.DTOs.ReorderSafariDayParkActivitiesDTO.ActivityOrderItem;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.Entity.SafariDayParkActivity;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.Repository.SafariDayParkActivityRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SafariDayParkActivityReorderService - Service for reordering activities within a safari park visit
 *
 * Handles drag-and-drop reordering from the UI with comprehensive validations:
 * - Validates all activity IDs exist and belong to the park visit
 * - Validates no duplicate IDs
 * - Validates all activities are included (no missing)
 * - Validates expected sort orders if provided
 * - Updates sort orders using two-pass approach to avoid unique constraint violations
 */
@Service
@Slf4j
@Transactional
public class SafariDayParkActivityReorderService {

    private final SafariDayParkRepository parkVisitRepository;
    private final SafariDayParkActivityRepository activityRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public SafariDayParkActivityReorderService(
        SafariDayParkRepository parkVisitRepository,
        SafariDayParkActivityRepository activityRepository,
        IdObfuscator idObfuscator
    ) {
        this.parkVisitRepository = parkVisitRepository;
        this.activityRepository = activityRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Reorder activities based on the new order provided
     *
     * @param parkVisitIdObfuscated The obfuscated park visit ID
     * @param reorderDTO The reorder data containing the new activity order
     * @return ResponseEntity with ApiResponse containing the reordered activities
     */
    @AuditLogAnnotation(action = "REORDER_SAFARI_PARK_ACTIVITIES", description = "Reordering safari park activities", entityType = "SafariDayParkActivity")
    public ResponseEntity<ApiResponse<?>> reorderParkActivities(
        String parkVisitIdObfuscated,
        ReorderSafariDayParkActivitiesDTO reorderDTO
    ) {
        log.info("Reordering safari park activities for park visit: {}", parkVisitIdObfuscated);

        try {
            // ========================
            // DECODE PARK VISIT ID
            // ========================
            Long parkVisitId;
            try {
                parkVisitId = idObfuscator.decodeId(parkVisitIdObfuscated);
            } catch (Exception e) {
                log.warn("Invalid park visit ID format: {}", parkVisitIdObfuscated);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid park visit ID format", "INVALID_PARK_VISIT_ID")
                );
            }

            // ========================
            // FIND PARK VISIT
            // ========================
            SafariDayPark parkVisit = parkVisitRepository.findById(parkVisitId).orElse(null);
            if (parkVisit == null) {
                log.warn("Safari park visit not found: {}", parkVisitId);
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Safari park visit not found", "SAFARI_PARK_VISIT_NOT_FOUND")
                );
            }

            // Check if safari is editable
            Safari safari = parkVisit.getSafariDay().getSafari();
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
            List<SafariDayParkActivity> existingActivities = activityRepository.findBySafariDayParkIdOrderBySortOrderAsc(parkVisitId);

            if (existingActivities.isEmpty()) {
                log.warn("No activities found for safari park visit: {}", parkVisitId);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Safari park visit has no activities to reorder", "NO_ACTIVITIES_TO_REORDER")
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
            // VALIDATION: All activities belong to this park visit
            // ========================
            Set<Long> existingActivityIds = existingActivities.stream()
                .map(SafariDayParkActivity::getId)
                .collect(Collectors.toSet());

            Set<Long> providedActivityIds = decodedActivityIds.keySet();

            // Check for activities that don't belong to this park visit
            Set<Long> foreignActivities = new HashSet<>(providedActivityIds);
            foreignActivities.removeAll(existingActivityIds);

            if (!foreignActivities.isEmpty()) {
                List<String> foreignActivityObfuscated = foreignActivities.stream()
                    .map(id -> idObfuscator.encodeId(id))
                    .collect(Collectors.toList());
                log.warn("Activity IDs not belonging to safari park visit: {}", foreignActivityObfuscated);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Activity ID(s) do not belong to this safari park visit: " + String.join(", ", foreignActivityObfuscated),
                        "ACTIVITY_PARK_VISIT_MISMATCH"
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
            Map<Long, SafariDayParkActivity> activityLookup = existingActivities.stream()
                .collect(Collectors.toMap(SafariDayParkActivity::getId, a -> a));

            // ========================
            // CHECK IF REORDER IS ACTUALLY NEEDED
            // ========================
            boolean orderChanged = false;
            int newSortOrder = 1;

            for (Long activityId : decodedActivityIds.keySet()) {
                SafariDayParkActivity activity = activityLookup.get(activityId);
                if (!activity.getSortOrder().equals(newSortOrder)) {
                    orderChanged = true;
                    break;
                }
                newSortOrder++;
            }

            if (!orderChanged) {
                log.info("Activity order unchanged for safari park visit: {}", parkVisitIdObfuscated);
                List<SafariDayParkActivityDTO> resultDTOs = existingActivities.stream()
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
            for (SafariDayParkActivity activity : existingActivities) {
                activity.setSortOrder(tempOrder--);
            }
            activityRepository.saveAll(existingActivities);
            activityRepository.flush();

            // Pass 2: Set final sort orders based on new order
            List<SafariDayParkActivity> reorderedActivities = new ArrayList<>();
            newSortOrder = 1;

            for (Long activityId : decodedActivityIds.keySet()) {
                SafariDayParkActivity activity = activityLookup.get(activityId);
                activity.setSortOrder(newSortOrder);
                reorderedActivities.add(activity);
                newSortOrder++;
            }

            // Save all reordered activities
            reorderedActivities = activityRepository.saveAll(reorderedActivities);

            // ========================
            // CONVERT TO DTOs AND RETURN
            // ========================
            List<SafariDayParkActivityDTO> resultDTOs = reorderedActivities.stream()
                .sorted(Comparator.comparing(SafariDayParkActivity::getSortOrder))
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            log.info("Successfully reordered {} activities for safari park visit: {}", reorderedActivities.size(), parkVisitIdObfuscated);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Activities reordered successfully", resultDTOs)
            );

        } catch (Exception e) {
            log.error("Error reordering safari park activities", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to reorder activities", "SAFARI_PARK_ACTIVITIES_REORDER_FAILED")
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
        dto.setParkId(idObfuscator.encodeId(activity.getSafariDayPark().getPark().getId()));
        dto.setParkName(activity.getSafariDayPark().getPark().getName());
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
