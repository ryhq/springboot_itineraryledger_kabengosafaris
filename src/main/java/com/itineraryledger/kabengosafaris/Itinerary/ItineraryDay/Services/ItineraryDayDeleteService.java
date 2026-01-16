package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Entity.ItineraryDay;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Repository.ItineraryDayRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryDayDeleteService - Service for deleting itinerary days
 *
 * Provides bulk deletion of itinerary days with automatic renumbering.
 * After deletion, remaining days are renumbered to maintain sequential order (1, 2, 3, ...).
 * Uses a two-pass approach to avoid unique constraint violations on (itinerary_id, day_number).
 */
@Service
@Slf4j
@Transactional
public class ItineraryDayDeleteService {

    private final ItineraryDayRepository itineraryDayRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ItineraryDayDeleteService(
        ItineraryDayRepository itineraryDayRepository,
        IdObfuscator idObfuscator
    ) {
        this.itineraryDayRepository = itineraryDayRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Delete itinerary days by list of obfuscated IDs.
     *
     * This method handles both single and bulk deletions:
     * - For single deletion, pass a list with one ID
     * - For bulk deletion, pass a list with multiple IDs
     *
     * After successful deletion, remaining days are automatically renumbered
     * to maintain sequential day numbers (1, 2, 3, ...) and dayTags are updated accordingly.
     *
     * @param itineraryIdObfuscated The obfuscated itinerary ID
     * @param dayIdObfuscatedList List of obfuscated day IDs to delete (can be single or multiple)
     * @return ResponseEntity with ApiResponse containing:
     *         - 200: Success with count of deleted days
     *         - 400: Invalid itinerary ID format
     *         - 500: Internal server error
     */
    public ResponseEntity<ApiResponse<?>> deleteItineraryDays(String itineraryIdObfuscated, List<String> dayIdObfuscatedList) {
        log.info("Deleting {} days from itinerary: {}", dayIdObfuscatedList.size(), itineraryIdObfuscated);

        try {
            // Decode itinerary ID
            Long itineraryId;
            try {
                itineraryId = idObfuscator.decodeId(itineraryIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid itinerary ID", "INVALID_ITINERARY_ID")
                );
            }

            // Decode all day IDs
            List<Long> dayIds = new ArrayList<>();
            for (String idObfuscated : dayIdObfuscatedList) {
                try {
                    Long id = idObfuscator.decodeId(idObfuscated);
                    dayIds.add(id);
                } catch (Exception e) {
                    log.warn("Failed to decode day ID: {}", idObfuscated, e);
                }
            }

            return deleteDaysInternal(itineraryId, dayIds);

        } catch (Exception e) {
            log.error("Error deleting itinerary days", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete itinerary days", "ITINERARY_DAYS_DELETE_FAILED")
            );
        }
    }

    /**
     * Delete days by list of IDs (internal method)
     */
    private ResponseEntity<ApiResponse<?>> deleteDaysInternal(Long itineraryId, List<Long> dayIds) {
        int deletedCount = 0;

        for (Long dayId : dayIds) {
            try {
                ItineraryDay day = itineraryDayRepository.findById(dayId).orElse(null);

                if (day == null) {
                    log.warn("Day not found: {}", dayId);
                    continue;
                }

                // Verify day belongs to the itinerary
                if (!day.getItinerary().getId().equals(itineraryId)) {
                    log.warn("Day {} does not belong to itinerary {}", dayId, itineraryId);
                    continue;
                }

                // Use AopContext to get proxy and trigger AOP aspect
                ((ItineraryDayDeleteService) AopContext.currentProxy()).deleteDay(dayId);
                deletedCount++;
                log.info("Itinerary day deleted: {}", dayId);

            } catch (Exception e) {
                log.error("Error deleting day: {}", dayId, e);
            }
        }

        // Renumber remaining days to maintain sequential order
        if (deletedCount > 0) {
            renumberDaysAfterDeletion(itineraryId);
        }

        return ResponseEntity.ok().body(
            ApiResponse.success(200, deletedCount + " day(s) deleted successfully", null)
        );
    }

    /**
     * Renumber remaining days after deletion to maintain sequential day numbers.
     * Uses two-pass approach to avoid unique constraint violations on (itinerary_id, day_number).
     *
     * @param itineraryId The itinerary ID
     */
    private void renumberDaysAfterDeletion(Long itineraryId) {
        // Fetch remaining days ordered by current day number
        List<ItineraryDay> remainingDays = itineraryDayRepository.findByItineraryIdOrderByDayNumberAsc(itineraryId);

        if (remainingDays.isEmpty()) {
            return;
        }

        // Check if renumbering is needed (gaps in day numbers)
        boolean needsRenumbering = false;
        int expectedDayNumber = 1;
        for (ItineraryDay day : remainingDays) {
            if (!day.getDayNumber().equals(expectedDayNumber)) {
                needsRenumbering = true;
                break;
            }
            expectedDayNumber++;
        }

        if (!needsRenumbering) {
            return;
        }

        log.info("Renumbering {} days for itinerary {}", remainingDays.size(), itineraryId);

        // Pass 1: Set temporary negative day numbers to avoid unique constraint violations
        int tempNumber = -1;
        for (ItineraryDay day : remainingDays) {
            day.setDayNumber(tempNumber--);
        }
        itineraryDayRepository.saveAll(remainingDays);
        itineraryDayRepository.flush();

        // Pass 2: Set final sequential day numbers and update dayTag
        int newDayNumber = 1;
        for (ItineraryDay day : remainingDays) {
            day.setDayNumber(newDayNumber);
            day.setDayTag("Day " + newDayNumber);
            newDayNumber++;
        }
        itineraryDayRepository.saveAll(remainingDays);

        log.info("Days renumbered successfully for itinerary {}", itineraryId);
    }

    @AuditLogAnnotation(action = "DELETE_ITINERARY_DAY", description = "Deleting itinerary day", entityType = "ItineraryDay", entityIdParamName = "id")
    public void deleteDay(Long id) {
        itineraryDayRepository.deleteById(id);
    }
}
