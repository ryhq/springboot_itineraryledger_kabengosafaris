package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Entity.ItineraryDayPark;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Repository.ItineraryDayParkRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.DTOs.ItineraryDayParkActivityDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.DTOs.ReorderItineraryDayParkActivitiesDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.DTOs.ReorderItineraryDayParkActivitiesDTO.ActivityOrderItem;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.Entity.ItineraryDayParkActivity;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.Repository.ItineraryDayParkActivityRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ItineraryDayParkActivityReorderService - Service for reordering activities within a park visit
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
public class ItineraryDayParkActivityReorderService {

    private final ItineraryDayParkRepository parkVisitRepository;
    private final ItineraryDayParkActivityRepository activityRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ItineraryDayParkActivityReorderService(
        ItineraryDayParkRepository parkVisitRepository,
        ItineraryDayParkActivityRepository activityRepository,
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
    @AuditLogAnnotation(action = "REORDER_PARK_ACTIVITIES", description = "Reordering park activities", entityType = "ItineraryDayParkActivity")
    public ResponseEntity<ApiResponse<?>> reorderParkActivities(
        String parkVisitIdObfuscated,
        ReorderItineraryDayParkActivitiesDTO reorderDTO
    ) {
        log.info("Reordering park activities for park visit: {}", parkVisitIdObfuscated);

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
            ItineraryDayPark parkVisit = parkVisitRepository.findById(parkVisitId).orElse(null);
            if (parkVisit == null) {
                log.warn("Park visit not found: {}", parkVisitId);
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Park visit not found", "PARK_VISIT_NOT_FOUND")
                );
            }

            // ========================
            // FETCH EXISTING ACTIVITIES
            // ========================
            List<ItineraryDayParkActivity> existingActivities = activityRepository.findByItineraryDayParkIdOrderBySortOrderAsc(parkVisitId);

            if (existingActivities.isEmpty()) {
                log.warn("No activities found for park visit: {}", parkVisitId);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Park visit has no activities to reorder", "NO_ACTIVITIES_TO_REORDER")
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
                .map(ItineraryDayParkActivity::getId)
                .collect(Collectors.toSet());

            Set<Long> providedActivityIds = decodedActivityIds.keySet();

            // Check for activities that don't belong to this park visit
            Set<Long> foreignActivities = new HashSet<>(providedActivityIds);
            foreignActivities.removeAll(existingActivityIds);

            if (!foreignActivities.isEmpty()) {
                List<String> foreignActivityObfuscated = foreignActivities.stream()
                    .map(id -> idObfuscator.encodeId(id))
                    .collect(Collectors.toList());
                log.warn("Activity IDs not belonging to park visit: {}", foreignActivityObfuscated);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Activity ID(s) do not belong to this park visit: " + String.join(", ", foreignActivityObfuscated),
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
            Map<Long, ItineraryDayParkActivity> activityLookup = existingActivities.stream()
                .collect(Collectors.toMap(ItineraryDayParkActivity::getId, a -> a));

            // ========================
            // CHECK IF REORDER IS ACTUALLY NEEDED
            // ========================
            boolean orderChanged = false;
            int newSortOrder = 1;

            for (Long activityId : decodedActivityIds.keySet()) {
                ItineraryDayParkActivity activity = activityLookup.get(activityId);
                if (!activity.getSortOrder().equals(newSortOrder)) {
                    orderChanged = true;
                    break;
                }
                newSortOrder++;
            }

            if (!orderChanged) {
                log.info("Activity order unchanged for park visit: {}", parkVisitIdObfuscated);
                List<ItineraryDayParkActivityDTO> resultDTOs = existingActivities.stream()
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
            for (ItineraryDayParkActivity activity : existingActivities) {
                activity.setSortOrder(tempOrder--);
            }
            activityRepository.saveAll(existingActivities);
            activityRepository.flush();

            // Pass 2: Set final sort orders based on new order
            List<ItineraryDayParkActivity> reorderedActivities = new ArrayList<>();
            newSortOrder = 1;

            for (Long activityId : decodedActivityIds.keySet()) {
                ItineraryDayParkActivity activity = activityLookup.get(activityId);
                activity.setSortOrder(newSortOrder);
                reorderedActivities.add(activity);
                newSortOrder++;
            }

            // Save all reordered activities
            reorderedActivities = activityRepository.saveAll(reorderedActivities);

            // ========================
            // CONVERT TO DTOs AND RETURN
            // ========================
            List<ItineraryDayParkActivityDTO> resultDTOs = reorderedActivities.stream()
                .sorted(Comparator.comparing(ItineraryDayParkActivity::getSortOrder))
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            log.info("Successfully reordered {} activities for park visit: {}", reorderedActivities.size(), parkVisitIdObfuscated);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Activities reordered successfully", resultDTOs)
            );

        } catch (Exception e) {
            log.error("Error reordering park activities", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to reorder activities", "PARK_ACTIVITIES_REORDER_FAILED")
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
        dto.setParkId(idObfuscator.encodeId(activity.getItineraryDayPark().getPark().getId()));
        dto.setParkName(activity.getItineraryDayPark().getPark().getName());
        dto.setActivityId(idObfuscator.encodeId(activity.getParkActivity().getActivity().getId()));
        dto.setActivityName(activity.getParkActivity().getActivity().getName());
        dto.setSortOrder(activity.getSortOrder());
        dto.setDurationHours(activity.getDurationHours());
        dto.setNotes(activity.getNotes());
        dto.setIsIncludedInPrice(activity.getIsIncludedInPrice());
        dto.setCreatedAt(activity.getCreatedAt());
        return dto;
    }
}
