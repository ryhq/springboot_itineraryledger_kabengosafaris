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
     * Delete itinerary days by list of obfuscated IDs
     *
     * @param itineraryIdObfuscated The obfuscated itinerary ID
     * @param dayIdObfuscatedList List of obfuscated day IDs
     * @return ResponseEntity with ApiResponse containing success or error
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
     * Delete a single itinerary day
     *
     * @param itineraryIdObfuscated The obfuscated itinerary ID
     * @param dayIdObfuscated The obfuscated day ID
     * @return ResponseEntity with ApiResponse containing success or error
     */
    public ResponseEntity<ApiResponse<?>> deleteItineraryDay(String itineraryIdObfuscated, String dayIdObfuscated) {
        log.info("Deleting day {} from itinerary: {}", dayIdObfuscated, itineraryIdObfuscated);

        try {
            // Decode IDs
            Long itineraryId;
            Long dayId;
            try {
                itineraryId = idObfuscator.decodeId(itineraryIdObfuscated);
                dayId = idObfuscator.decodeId(dayIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid ID", "INVALID_ID")
                );
            }

            // Find day
            ItineraryDay day = itineraryDayRepository.findById(dayId).orElse(null);
            if (day == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Itinerary day not found", "ITINERARY_DAY_NOT_FOUND")
                );
            }

            // Verify day belongs to the itinerary
            if (!day.getItinerary().getId().equals(itineraryId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Day does not belong to this itinerary", "DAY_ITINERARY_MISMATCH")
                );
            }

            // Delete day
            ((ItineraryDayDeleteService) AopContext.currentProxy()).deleteDay(dayId);

            log.info("Itinerary day deleted successfully: {}", dayId);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Itinerary day deleted successfully", null)
            );

        } catch (Exception e) {
            log.error("Error deleting itinerary day", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete itinerary day", "ITINERARY_DAY_DELETE_FAILED")
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

        return ResponseEntity.ok().body(
            ApiResponse.success(200, deletedCount + " day(s) deleted successfully", null)
        );
    }

    @AuditLogAnnotation(action = "DELETE_ITINERARY_DAY", description = "Deleting itinerary day", entityType = "ItineraryDay", entityIdParamName = "id")
    public void deleteDay(Long id) {
        itineraryDayRepository.deleteById(id);
    }
}
