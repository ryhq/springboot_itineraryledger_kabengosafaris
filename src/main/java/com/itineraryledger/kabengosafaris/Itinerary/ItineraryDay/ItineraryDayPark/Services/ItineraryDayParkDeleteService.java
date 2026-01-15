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
     * Delete a park visit
     *
     * @param itineraryIdObfuscated The obfuscated itinerary ID
     * @param dayIdObfuscated The obfuscated day ID
     * @param parkVisitIdObfuscated The obfuscated park visit ID
     * @return ResponseEntity with ApiResponse
     */
    public ResponseEntity<ApiResponse<?>> deleteItineraryDayPark(
        String itineraryIdObfuscated,
        String dayIdObfuscated,
        String parkVisitIdObfuscated
    ) {
        log.info("Deleting park visit: {}", parkVisitIdObfuscated);

        try {
            // Decode IDs
            Long itineraryId;
            Long dayId;
            Long parkVisitId;
            try {
                itineraryId = idObfuscator.decodeId(itineraryIdObfuscated);
                dayId = idObfuscator.decodeId(dayIdObfuscated);
                parkVisitId = idObfuscator.decodeId(parkVisitIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid ID", "INVALID_ID")
                );
            }

            // Find park visit
            ItineraryDayPark parkVisit = itineraryDayParkRepository.findById(parkVisitId).orElse(null);
            if (parkVisit == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Park visit not found", "PARK_VISIT_NOT_FOUND")
                );
            }

            // Verify ownership chain
            if (!parkVisit.getItineraryDay().getId().equals(dayId) ||
                !parkVisit.getItineraryDay().getItinerary().getId().equals(itineraryId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Park visit does not belong to this day/itinerary", "OWNERSHIP_MISMATCH")
                );
            }

            // Delete
            ((ItineraryDayParkDeleteService) AopContext.currentProxy()).deleteParkVisit(parkVisitId);

            log.info("Park visit deleted successfully: {}", parkVisitId);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Park visit deleted successfully", null)
            );

        } catch (Exception e) {
            log.error("Error deleting park visit", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete park visit", "PARK_VISIT_DELETE_FAILED")
            );
        }
    }

    /**
     * Delete multiple park visits
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

    @AuditLogAnnotation(action = "DELETE_ITINERARY_DAY_PARK", description = "Deleting park visit", entityType = "ItineraryDayPark", entityIdParamName = "id")
    public void deleteParkVisit(Long id) {
        itineraryDayParkRepository.deleteById(id);
    }
}
