package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.Services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.Entity.ItineraryDayActivity;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.Repository.ItineraryDayActivityRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryDayActivityDeleteService - Service for deleting itinerary day activities
 *
 * Provides bulk deletion of itinerary day activities with automatic renumbering.
 * After deletion, remaining activities are renumbered to maintain sequential sortOrder (1, 2, 3, ...).
 * Uses a two-pass approach to avoid unique constraint violations if any exist on sortOrder.
 */
@Service
@Slf4j
@Transactional
public class ItineraryDayActivityDeleteService {

    private final ItineraryDayActivityRepository activityRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ItineraryDayActivityDeleteService(
        ItineraryDayActivityRepository activityRepository,
        IdObfuscator idObfuscator
    ) {
        this.activityRepository = activityRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Delete itinerary day activities by list of obfuscated IDs
     *
     * @param itineraryIdObfuscated The obfuscated itinerary ID
     * @param dayIdObfuscated The obfuscated day ID
     * @param activityIdObfuscatedList List of obfuscated activity IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    public ResponseEntity<ApiResponse<?>> deleteItineraryDayActivities(
        String itineraryIdObfuscated,
        String dayIdObfuscated,
        List<String> activityIdObfuscatedList
    ) {
        log.info("Deleting {} activities from day: {}", activityIdObfuscatedList.size(), dayIdObfuscated);

        try {
            // Decode itinerary and day IDs
            Long itineraryId;
            Long dayId;
            try {
                itineraryId = idObfuscator.decodeId(itineraryIdObfuscated);
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

            return deleteActivitiesInternal(itineraryId, dayId, activityIds);

        } catch (Exception e) {
            log.error("Error deleting itinerary day activities", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete itinerary day activities", "ITINERARY_DAY_ACTIVITIES_DELETE_FAILED")
            );
        }
    }

    /**
     * Delete activities by list of IDs (internal method)
     */
    private ResponseEntity<ApiResponse<?>> deleteActivitiesInternal(Long itineraryId, Long dayId, List<Long> activityIds) {
        int deletedCount = 0;

        for (Long activityId : activityIds) {
            try {
                ItineraryDayActivity activity = activityRepository.findById(activityId).orElse(null);

                if (activity == null) {
                    log.warn("Activity not found: {}", activityId);
                    continue;
                }

                // Verify activity belongs to the day
                if (!activity.getItineraryDay().getId().equals(dayId)) {
                    log.warn("Activity {} does not belong to day {}", activityId, dayId);
                    continue;
                }

                // Verify day belongs to the itinerary
                if (!activity.getItineraryDay().getItinerary().getId().equals(itineraryId)) {
                    log.warn("Day {} does not belong to itinerary {}", dayId, itineraryId);
                    continue;
                }

                // Use AopContext to get proxy and trigger AOP aspect
                ((ItineraryDayActivityDeleteService) AopContext.currentProxy()).deleteActivity(activityId);
                deletedCount++;
                log.info("Itinerary day activity deleted: {}", activityId);

            } catch (Exception e) {
                log.error("Error deleting activity: {}", activityId, e);
            }
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
     * @param dayId The itinerary day ID
     */
    private void renumberActivitiesAfterDeletion(Long dayId) {
        // Fetch remaining activities ordered by current sortOrder
        List<ItineraryDayActivity> remainingActivities = activityRepository.findByItineraryDayIdOrderBySortOrderAsc(dayId);

        if (remainingActivities.isEmpty()) {
            return;
        }

        // Check if renumbering is needed (gaps in sortOrder)
        boolean needsRenumbering = false;
        int expectedSortOrder = 1;
        for (ItineraryDayActivity activity : remainingActivities) {
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
        for (ItineraryDayActivity activity : remainingActivities) {
            activity.setSortOrder(tempOrder--);
        }
        activityRepository.saveAll(remainingActivities);
        activityRepository.flush();

        // Pass 2: Set final sequential sortOrder
        int newSortOrder = 1;
        for (ItineraryDayActivity activity : remainingActivities) {
            activity.setSortOrder(newSortOrder++);
        }
        activityRepository.saveAll(remainingActivities);

        log.info("Activities renumbered successfully for day {}", dayId);
    }

    @AuditLogAnnotation(action = "DELETE_ITINERARY_DAY_ACTIVITY", description = "Deleting itinerary day activity", entityType = "ItineraryDayActivity", entityIdParamName = "id")
    public void deleteActivity(Long id) {
        activityRepository.deleteById(id);
    }
}
