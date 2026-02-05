package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.Services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.Entity.SafariDayPark;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.Repository.SafariDayParkRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * SafariDayParkDeleteService - Service for deleting safari day park visits
 *
 * Provides bulk deletion of park visits with automatic renumbering.
 * After deletion, remaining park visits are renumbered to maintain sequential sortOrder (1, 2, 3, ...).
 * Uses a two-pass approach to avoid unique constraint violations if any exist on sortOrder.
 */
@Service
@Slf4j
@Transactional
public class SafariDayParkDeleteService {

    private final SafariDayParkRepository safariDayParkRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public SafariDayParkDeleteService(
        SafariDayParkRepository safariDayParkRepository,
        IdObfuscator idObfuscator
    ) {
        this.safariDayParkRepository = safariDayParkRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Delete park visits by list of obfuscated IDs.
     *
     * This method handles both single and bulk deletions:
     * - For single deletion, pass a list with one ID
     * - For bulk deletion, pass a list with multiple IDs
     *
     * @param safariIdObfuscated The obfuscated safari ID
     * @param dayIdObfuscated The obfuscated day ID
     * @param parkVisitIdObfuscatedList List of obfuscated park visit IDs to delete
     * @return ResponseEntity with ApiResponse containing count of deleted park visits
     */
    public ResponseEntity<ApiResponse<?>> deleteSafariDayParks(
        String safariIdObfuscated,
        String dayIdObfuscated,
        List<String> parkVisitIdObfuscatedList
    ) {
        log.info("Deleting {} park visits", parkVisitIdObfuscatedList.size());

        try {
            Long safariId = idObfuscator.decodeId(safariIdObfuscated);
            Long dayId = idObfuscator.decodeId(dayIdObfuscated);

            List<Long> parkVisitIds = new ArrayList<>();
            for (String idObfuscated : parkVisitIdObfuscatedList) {
                try {
                    parkVisitIds.add(idObfuscator.decodeId(idObfuscated));
                } catch (Exception e) {
                    log.warn("Failed to decode park visit ID: {}", idObfuscated);
                }
            }

            int deletedCount = 0;
            for (Long parkVisitId : parkVisitIds) {
                try {
                    SafariDayPark parkVisit = safariDayParkRepository.findById(parkVisitId).orElse(null);
                    if (parkVisit == null) continue;

                    if (!parkVisit.getSafariDay().getId().equals(dayId) ||
                        !parkVisit.getSafariDay().getSafari().getId().equals(safariId)) {
                        continue;
                    }

                    ((SafariDayParkDeleteService) AopContext.currentProxy()).deleteParkVisit(parkVisitId);
                    deletedCount++;
                } catch (Exception e) {
                    log.error("Error deleting park visit: {}", parkVisitId, e);
                }
            }

            // Renumber remaining park visits to maintain sequential order
            if (deletedCount > 0) {
                renumberParkVisitsAfterDeletion(dayId);
            }

            return ResponseEntity.ok().body(
                ApiResponse.success(200, deletedCount + " park visit(s) deleted successfully", null)
            );

        } catch (Exception e) {
            log.error("Error deleting park visits", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete park visits", "PARK_VISITS_DELETE_FAILED")
            );
        }
    }

    /**
     * Renumber remaining park visits after deletion to maintain sequential sortOrder.
     * Uses two-pass approach to avoid potential unique constraint violations.
     *
     * @param dayId The safari day ID
     */
    private void renumberParkVisitsAfterDeletion(Long dayId) {
        // Fetch remaining park visits ordered by current sortOrder
        List<SafariDayPark> remainingParkVisits = safariDayParkRepository.findBySafariDayIdOrderBySortOrderAsc(dayId);

        if (remainingParkVisits.isEmpty()) {
            return;
        }

        // Check if renumbering is needed (gaps in sortOrder)
        boolean needsRenumbering = false;
        int expectedSortOrder = 1;
        for (SafariDayPark parkVisit : remainingParkVisits) {
            if (!parkVisit.getSortOrder().equals(expectedSortOrder)) {
                needsRenumbering = true;
                break;
            }
            expectedSortOrder++;
        }

        if (!needsRenumbering) {
            return;
        }

        log.info("Renumbering {} park visits for day {}", remainingParkVisits.size(), dayId);

        // Pass 1: Set temporary negative sortOrder to avoid potential unique constraint violations
        int tempOrder = -1;
        for (SafariDayPark parkVisit : remainingParkVisits) {
            parkVisit.setSortOrder(tempOrder--);
        }
        safariDayParkRepository.saveAll(remainingParkVisits);
        safariDayParkRepository.flush();

        // Pass 2: Set final sequential sortOrder
        int newSortOrder = 1;
        for (SafariDayPark parkVisit : remainingParkVisits) {
            parkVisit.setSortOrder(newSortOrder++);
        }
        safariDayParkRepository.saveAll(remainingParkVisits);

        log.info("Park visits renumbered successfully for day {}", dayId);
    }

    @AuditLogAnnotation(action = "DELETE_SAFARI_DAY_PARK", description = "Deleting park visit", entityType = "SafariDayPark", entityIdParamName = "id")
    public void deleteParkVisit(Long id) {
        safariDayParkRepository.deleteById(id);
    }
}
