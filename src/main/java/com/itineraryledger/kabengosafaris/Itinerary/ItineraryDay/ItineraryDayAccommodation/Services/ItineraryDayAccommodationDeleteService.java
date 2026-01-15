package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.Services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.Entity.ItineraryDayAccommodation;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.Repository.ItineraryDayAccommodationRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryDayAccommodationDeleteService - Service for deleting itinerary day accommodations
 */
@Service
@Slf4j
@Transactional
public class ItineraryDayAccommodationDeleteService {

    private final ItineraryDayAccommodationRepository accommodationRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ItineraryDayAccommodationDeleteService(
        ItineraryDayAccommodationRepository accommodationRepository,
        IdObfuscator idObfuscator
    ) {
        this.accommodationRepository = accommodationRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Delete an accommodation
     *
     * @param itineraryIdObfuscated The obfuscated itinerary ID
     * @param dayIdObfuscated The obfuscated day ID
     * @param accommodationIdObfuscated The obfuscated accommodation entry ID
     * @return ResponseEntity with ApiResponse
     */
    public ResponseEntity<ApiResponse<?>> deleteItineraryDayAccommodation(
        String itineraryIdObfuscated,
        String dayIdObfuscated,
        String accommodationIdObfuscated
    ) {
        log.info("Deleting accommodation: {}", accommodationIdObfuscated);

        try {
            // Decode IDs
            Long itineraryId;
            Long dayId;
            Long accommodationEntryId;
            try {
                itineraryId = idObfuscator.decodeId(itineraryIdObfuscated);
                dayId = idObfuscator.decodeId(dayIdObfuscated);
                accommodationEntryId = idObfuscator.decodeId(accommodationIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid ID", "INVALID_ID")
                );
            }

            // Find accommodation
            ItineraryDayAccommodation accommodation = accommodationRepository.findById(accommodationEntryId).orElse(null);
            if (accommodation == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Accommodation not found", "ACCOMMODATION_NOT_FOUND")
                );
            }

            // Verify ownership chain
            if (!accommodation.getItineraryDay().getId().equals(dayId) ||
                !accommodation.getItineraryDay().getItinerary().getId().equals(itineraryId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Accommodation does not belong to this day/itinerary", "OWNERSHIP_MISMATCH")
                );
            }

            // Delete
            ((ItineraryDayAccommodationDeleteService) AopContext.currentProxy()).deleteAccommodation(accommodationEntryId);

            log.info("Accommodation deleted successfully: {}", accommodationEntryId);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Accommodation deleted successfully", null)
            );

        } catch (Exception e) {
            log.error("Error deleting accommodation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete accommodation", "ACCOMMODATION_DELETE_FAILED")
            );
        }
    }

    /**
     * Delete multiple accommodations
     */
    public ResponseEntity<ApiResponse<?>> deleteItineraryDayAccommodations(
        String itineraryIdObfuscated,
        String dayIdObfuscated,
        List<String> accommodationIdObfuscatedList
    ) {
        log.info("Deleting {} accommodations", accommodationIdObfuscatedList.size());

        try {
            Long itineraryId = idObfuscator.decodeId(itineraryIdObfuscated);
            Long dayId = idObfuscator.decodeId(dayIdObfuscated);

            List<Long> accommodationIds = new ArrayList<>();
            for (String idObfuscated : accommodationIdObfuscatedList) {
                try {
                    accommodationIds.add(idObfuscator.decodeId(idObfuscated));
                } catch (Exception e) {
                    log.warn("Failed to decode accommodation ID: {}", idObfuscated);
                }
            }

            int deletedCount = 0;
            for (Long accommodationId : accommodationIds) {
                try {
                    ItineraryDayAccommodation accommodation = accommodationRepository.findById(accommodationId).orElse(null);
                    if (accommodation == null) continue;

                    if (!accommodation.getItineraryDay().getId().equals(dayId) ||
                        !accommodation.getItineraryDay().getItinerary().getId().equals(itineraryId)) {
                        continue;
                    }

                    ((ItineraryDayAccommodationDeleteService) AopContext.currentProxy()).deleteAccommodation(accommodationId);
                    deletedCount++;
                } catch (Exception e) {
                    log.error("Error deleting accommodation: {}", accommodationId, e);
                }
            }

            return ResponseEntity.ok().body(
                ApiResponse.success(200, deletedCount + " accommodation(s) deleted successfully", null)
            );

        } catch (Exception e) {
            log.error("Error deleting accommodations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete accommodations", "ACCOMMODATIONS_DELETE_FAILED")
            );
        }
    }

    @AuditLogAnnotation(action = "DELETE_ITINERARY_DAY_ACCOMMODATION", description = "Deleting accommodation", entityType = "ItineraryDayAccommodation", entityIdParamName = "id")
    public void deleteAccommodation(Long id) {
        accommodationRepository.deleteById(id);
    }
}
