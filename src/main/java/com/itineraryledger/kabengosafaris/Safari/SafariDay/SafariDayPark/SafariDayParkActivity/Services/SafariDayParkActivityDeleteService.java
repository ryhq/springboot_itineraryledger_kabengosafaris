package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.Services;

import java.util.List;

import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.Entity.SafariDayParkActivity;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.Repository.SafariDayParkActivityRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * SafariDayParkActivityDeleteService - Service for deleting park activities within a safari park visit
 *
 * Provides bulk deletion of park activities with automatic renumbering.
 * After deletion, remaining activities are renumbered to maintain sequential sortOrder (1, 2, 3, ...).
 * Uses a two-pass approach to avoid unique constraint violations if any exist on sortOrder.
 */
@Service
@Slf4j
@Transactional
public class SafariDayParkActivityDeleteService {

    private final SafariDayParkActivityRepository parkActivityRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public SafariDayParkActivityDeleteService(
        SafariDayParkActivityRepository parkActivityRepository,
        IdObfuscator idObfuscator
    ) {
        this.parkActivityRepository = parkActivityRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Delete safari park activities by list of obfuscated IDs.
     *
     * This method handles both single and bulk deletions:
     * - For single deletion, pass a list with one ID
     * - For bulk deletion, pass a list with multiple IDs
     *
     * After successful deletion, remaining activities are automatically renumbered
     * to maintain sequential sortOrder (1, 2, 3, ...).
     *
     * @param parkVisitIdObfuscated The obfuscated park visit ID
     * @param activityIdObfuscatedList List of obfuscated activity entry IDs to delete
     * @return ResponseEntity with ApiResponse containing count of deleted activities
     */
    public ResponseEntity<ApiResponse<?>> deleteParkActivities(
        String parkVisitIdObfuscated,
        List<String> activityIdObfuscatedList
    ) {
        log.info("Deleting {} safari park activities", activityIdObfuscatedList.size());

        try {
            Long parkVisitId;
            try {
                parkVisitId = idObfuscator.decodeId(parkVisitIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid park visit ID", "INVALID_ID")
                );
            }

            int deletedCount = 0;
            Safari safari = null;

            for (String idObfuscated : activityIdObfuscatedList) {
                try {
                    Long activityEntryId = idObfuscator.decodeId(idObfuscated);
                    SafariDayParkActivity entry = parkActivityRepository.findById(activityEntryId).orElse(null);

                    if (entry == null || !entry.getSafariDayPark().getId().equals(parkVisitId)) {
                        continue;
                    }

                    // Get safari instance for editable check
                    if (safari == null) {
                        safari = entry.getSafariDayPark().getSafariDay().getSafari();
                    }

                    ((SafariDayParkActivityDeleteService) AopContext.currentProxy()).deleteActivity(activityEntryId);
                    deletedCount++;
                } catch (Exception e) {
                    log.error("Error deleting safari park activity", e);
                }
            }

            // Check safari is editable after collecting safari instance
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
                renumberActivitiesAfterDeletion(parkVisitId);
            }

            return ResponseEntity.ok().body(
                ApiResponse.success(200, deletedCount + " activities deleted", null)
            );

        } catch (Exception e) {
            log.error("Error deleting safari park activities", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete safari park activities", "DELETE_FAILED")
            );
        }
    }

    /**
     * Renumber remaining activities after deletion to maintain sequential sortOrder.
     * Uses two-pass approach to avoid potential unique constraint violations.
     *
     * @param parkVisitId The park visit ID
     */
    private void renumberActivitiesAfterDeletion(Long parkVisitId) {
        // Fetch remaining activities ordered by current sortOrder
        List<SafariDayParkActivity> remainingActivities = parkActivityRepository.findBySafariDayParkIdOrderBySortOrderAsc(parkVisitId);

        if (remainingActivities.isEmpty()) {
            return;
        }

        // Check if renumbering is needed (gaps in sortOrder)
        boolean needsRenumbering = false;
        int expectedSortOrder = 1;
        for (SafariDayParkActivity activity : remainingActivities) {
            if (!activity.getSortOrder().equals(expectedSortOrder)) {
                needsRenumbering = true;
                break;
            }
            expectedSortOrder++;
        }

        if (!needsRenumbering) {
            return;
        }

        log.info("Renumbering {} activities for safari park visit {}", remainingActivities.size(), parkVisitId);

        // Pass 1: Set temporary negative sortOrder to avoid potential unique constraint violations
        int tempOrder = -1;
        for (SafariDayParkActivity activity : remainingActivities) {
            activity.setSortOrder(tempOrder--);
        }
        parkActivityRepository.saveAll(remainingActivities);
        parkActivityRepository.flush();

        // Pass 2: Set final sequential sortOrder
        int newSortOrder = 1;
        for (SafariDayParkActivity activity : remainingActivities) {
            activity.setSortOrder(newSortOrder++);
        }
        parkActivityRepository.saveAll(remainingActivities);

        log.info("Activities renumbered successfully for safari park visit {}", parkVisitId);
    }

    @AuditLogAnnotation(action = "DELETE_SAFARI_PARK_ACTIVITY", description = "Deleting safari park activity", entityType = "SafariDayParkActivity", entityIdParamName = "id")
    public void deleteActivity(Long id) {
        parkActivityRepository.deleteById(id);
    }
}
