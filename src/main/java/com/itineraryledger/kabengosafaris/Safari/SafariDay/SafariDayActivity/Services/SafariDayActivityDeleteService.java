package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.Services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.Entity.SafariDayActivity;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.Repository.SafariDayActivityRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * SafariDayActivityDeleteService - Service for deleting safari day activities
 *
 * Provides bulk deletion of safari day activities with automatic renumbering.
 * After deletion, remaining activities are renumbered to maintain sequential sortOrder (1, 2, 3, ...).
 * Uses a two-pass approach to avoid unique constraint violations if any exist on sortOrder.
 */
@Service
@Slf4j
@Transactional
public class SafariDayActivityDeleteService {

    private final SafariDayActivityRepository activityRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public SafariDayActivityDeleteService(
        SafariDayActivityRepository activityRepository,
        IdObfuscator idObfuscator
    ) {
        this.activityRepository = activityRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Delete safari day activities by list of obfuscated IDs
     *
     * @param safariIdObfuscated The obfuscated safari ID
     * @param dayIdObfuscated The obfuscated day ID
     * @param activityIdObfuscatedList List of obfuscated activity IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    public ResponseEntity<ApiResponse<?>> deleteSafariDayActivities(
        String safariIdObfuscated,
        String dayIdObfuscated,
        List<String> activityIdObfuscatedList
    ) {
        log.info("Deleting {} activities from day: {}", activityIdObfuscatedList.size(), dayIdObfuscated);

        try {
            // Decode safari and day IDs
            Long safariId;
            Long dayId;
            try {
                safariId = idObfuscator.decodeId(safariIdObfuscated);
                dayId = idObfuscator.decodeId(dayIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid ID format", "INVALID_ID")
                );
            }

            // Decode all activity IDs
            List<Long> activityIds = new ArrayList<>();
            for (String idObfuscated : activityIdObfuscatedList) {
                try {
                    Long id = idObfuscator.decodeId(idObfuscated);
                    activityIds.add(id);
                } catch (Exception e) {
                    log.warn("Failed to decode activity ID: {}", idObfuscated, e);
                }
            }

            return deleteActivitiesInternal(safariId, dayId, activityIds);

        } catch (Exception e) {
            log.error("Error deleting safari day activities", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete safari day activities", "SAFARI_DAY_ACTIVITIES_DELETE_FAILED")
            );
        }
    }

    /**
     * Delete activities by list of IDs (internal method)
     */
    private ResponseEntity<ApiResponse<?>> deleteActivitiesInternal(Long safariId, Long dayId, List<Long> activityIds) {
        int deletedCount = 0;
        Safari safari = null;

        for (Long activityId : activityIds) {
            try {
                SafariDayActivity activity = activityRepository.findById(activityId).orElse(null);

                if (activity == null) {
                    log.warn("Activity not found: {}", activityId);
                    continue;
                }

                // Verify activity belongs to the day
                if (!activity.getSafariDay().getId().equals(dayId)) {
                    log.warn("Activity {} does not belong to day {}", activityId, dayId);
                    continue;
                }

                // Verify day belongs to the safari
                if (!activity.getSafariDay().getSafari().getId().equals(safariId)) {
                    log.warn("Day {} does not belong to safari {}", dayId, safariId);
                    continue;
                }

                // Get safari instance for editable check
                if (safari == null) {
                    safari = activity.getSafariDay().getSafari();
                }

                // Use AopContext to get proxy and trigger AOP aspect
                ((SafariDayActivityDeleteService) AopContext.currentProxy()).deleteActivity(activityId);
                deletedCount++;
                log.info("Safari day activity deleted: {}", activityId);

            } catch (Exception e) {
                log.error("Error deleting activity: {}", activityId, e);
            }
        }

        // Check safari is editable before proceeding
        if (safari != null && !safari.isEditable()) {
            log.warn("Safari is not editable: {} (state: {})", safari.getCode(), safari.getState());
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    400,
                    "Safari cannot be edited in state: " + safari.getState().getDisplayName(),
                    "SAFARI_NOT_EDITABLE"
                )
            );
        }

        // Renumber remaining activities to maintain sequential order
        if (deletedCount > 0) {
            renumberActivitiesAfterDeletion(dayId);
        }

        return ResponseEntity.ok().body(
            ApiResponse.success(200, deletedCount + " activity(ies) deleted successfully", null)
        );
    }

    /**
     * Renumber remaining activities after deletion to maintain sequential sortOrder.
     * Uses two-pass approach to avoid potential unique constraint violations.
     *
     * @param dayId The safari day ID
     */
    private void renumberActivitiesAfterDeletion(Long dayId) {
        // Fetch remaining activities ordered by current sortOrder
        List<SafariDayActivity> remainingActivities = activityRepository.findBySafariDayIdOrderBySortOrderAsc(dayId);

        if (remainingActivities.isEmpty()) {
            return;
        }

        // Check if renumbering is needed (gaps in sortOrder)
        boolean needsRenumbering = false;
        int expectedSortOrder = 1;
        for (SafariDayActivity activity : remainingActivities) {
            if (!activity.getSortOrder().equals(expectedSortOrder)) {
                needsRenumbering = true;
                break;
            }
            expectedSortOrder++;
        }

        if (!needsRenumbering) {
            return;
        }

        log.info("Renumbering {} activities for day {}", remainingActivities.size(), dayId);

        // Pass 1: Set temporary negative sortOrder to avoid potential unique constraint violations
        int tempOrder = -1;
        for (SafariDayActivity activity : remainingActivities) {
            activity.setSortOrder(tempOrder--);
        }
        activityRepository.saveAll(remainingActivities);
        activityRepository.flush();

        // Pass 2: Set final sequential sortOrder
        int newSortOrder = 1;
        for (SafariDayActivity activity : remainingActivities) {
            activity.setSortOrder(newSortOrder++);
        }
        activityRepository.saveAll(remainingActivities);

        log.info("Activities renumbered successfully for day {}", dayId);
    }

    @AuditLogAnnotation(action = "DELETE_SAFARI_DAY_ACTIVITY", description = "Deleting safari day activity", entityType = "SafariDayActivity", entityIdParamName = "id")
    public void deleteActivity(Long id) {
        activityRepository.deleteById(id);
    }
}
