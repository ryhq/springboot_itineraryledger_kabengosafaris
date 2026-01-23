package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Entity.ItineraryDayPark;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Repository.ItineraryDayParkRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryDayParkDeleteService - Service for deleting itinerary day park visits
 *
 * Provides bulk deletion of park visits with automatic renumbering.
 * After deletion, remaining park visits are renumbered to maintain sequential sortOrder (1, 2, 3, ...).
 * Uses a two-pass approach to avoid unique constraint violations if any exist on sortOrder.
 */
@Service
@Slf4j
@Transactional
public class ItineraryDayParkDeleteService {

    private final ItineraryDayParkRepository itineraryDayParkRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ItineraryDayParkDeleteService(
        ItineraryDayParkRepository itineraryDayParkRepository,
        IdObfuscator idObfuscator
    ) {
        this.itineraryDayParkRepository = itineraryDayParkRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Delete park visits by list of obfuscated IDs.
     *
     * This method handles both single and bulk deletions:
     * - For single deletion, pass a list with one ID
     * - For bulk deletion, pass a list with multiple IDs
     *
     * @param itineraryIdObfuscated The obfuscated itinerary ID
     * @param dayIdObfuscated The obfuscated day ID
     * @param parkVisitIdObfuscatedList List of obfuscated park visit IDs to delete
     * @return ResponseEntity with ApiResponse containing count of deleted park visits
     */
    public ResponseEntity<ApiResponse<?>> deleteItineraryDayParks(
        String itineraryIdObfuscated,
        String dayIdObfuscated,
        List<String> parkVisitIdObfuscatedList
    ) {
        log.info("Deleting {} park visits", parkVisitIdObfuscatedList.size());

        try {
            Long itineraryId = idObfuscator.decodeId(itineraryIdObfuscated);
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
                    ItineraryDayPark parkVisit = itineraryDayParkRepository.findById(parkVisitId).orElse(null);
                    if (parkVisit == null) continue;

                    if (!parkVisit.getItineraryDay().getId().equals(dayId) ||
                        !parkVisit.getItineraryDay().getItinerary().getId().equals(itineraryId)) {
                        continue;
                    }

                    ((ItineraryDayParkDeleteService) AopContext.currentProxy()).deleteParkVisit(parkVisitId);
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
     * @param dayId The itinerary day ID
     */
    private void renumberParkVisitsAfterDeletion(Long dayId) {
        // Fetch remaining park visits ordered by current sortOrder
        List<ItineraryDayPark> remainingParkVisits = itineraryDayParkRepository.findByItineraryDayIdOrderBySortOrderAsc(dayId);

        if (remainingParkVisits.isEmpty()) {
            return;
        }

        // Check if renumbering is needed (gaps in sortOrder)
        boolean needsRenumbering = false;
        int expectedSortOrder = 1;
        for (ItineraryDayPark parkVisit : remainingParkVisits) {
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
        for (ItineraryDayPark parkVisit : remainingParkVisits) {
            parkVisit.setSortOrder(tempOrder--);
        }
        itineraryDayParkRepository.saveAll(remainingParkVisits);
        itineraryDayParkRepository.flush();

        // Pass 2: Set final sequential sortOrder
        int newSortOrder = 1;
        for (ItineraryDayPark parkVisit : remainingParkVisits) {
            parkVisit.setSortOrder(newSortOrder++);
        }
        itineraryDayParkRepository.saveAll(remainingParkVisits);

        log.info("Park visits renumbered successfully for day {}", dayId);
    }

    @AuditLogAnnotation(action = "DELETE_ITINERARY_DAY_PARK", description = "Deleting park visit", entityType = "ItineraryDayPark", entityIdParamName = "id")
    public void deleteParkVisit(Long id) {
        itineraryDayParkRepository.deleteById(id);
    }
}
