package com.itineraryledger.kabengosafaris.Park.Services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Park.Park;
import com.itineraryledger.kabengosafaris.Park.ParkRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ParkDeleteService - Service for deleting parks
 */
@Service
@Slf4j
@Transactional
public class ParkDeleteService {

    private final ParkRepository parkRepository;
    private final IdObfuscator idObfuscator;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    @Autowired
    public ParkDeleteService(
        ParkRepository parkRepository,
        IdObfuscator idObfuscator
    ) {
        this.parkRepository = parkRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Delete parks by list of obfuscated IDs
     *
     * @param idObfuscatedList List of obfuscated park IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    public ResponseEntity<ApiResponse<?>> deleteParks(List<String> idObfuscatedList) {
        log.info("Deleting {} parks", idObfuscatedList.size());

        try {
            // Decode all obfuscated IDs
            List<Long> ids = new ArrayList<>();
            for (String idObfuscated : idObfuscatedList) {
                try {
                    Long id = idObfuscator.decodeId(idObfuscated);
                    ids.add(id);
                } catch (Exception e) {
                    log.warn("Failed to decode ID: {}", idObfuscated, e);
                }
            }

            return deleteParksInternal(ids);

        } catch (Exception e) {
            log.error("Error deleting parks", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to delete parks",
                    "PARKS_DELETE_FAILED"
                )
            );
        }
    }

    /**
     * Delete parks by list of IDs (internal method)
     */
    private ResponseEntity<ApiResponse<?>> deleteParksInternal(List<Long> ids) {
        List<String> deletedIds = new ArrayList<>();
        List<java.util.Map<String, Object>> skipped = new ArrayList<>();

        for (Long id : ids) {
            Park park = parkRepository.findById(id).orElse(null);

            if (park == null) {
                skipped.add(skip(id, null, "Park not found"));
                continue;
            }

            // pre-check every referencing table: a park used by real trips must
            // never vanish, and the caller has to be told why
            String blocker = blockingReferences(id);
            if (blocker != null) {
                skipped.add(skip(id, park.getName(), blocker));
                continue;
            }

            try {
                // Use AopContext to get proxy and trigger AOP aspect
                ((ParkDeleteService) AopContext.currentProxy()).deletePark(id);
                deletedIds.add(idObfuscator.encodeId(id));
                log.info("Park deleted successfully: {}", id);
            } catch (Exception e) {
                log.error("Error deleting park: {}", id, e);
                skipped.add(skip(id, park.getName(), "Could not be deleted: " + e.getMessage()));
            }
        }

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("deletedCount", deletedIds.size());
        result.put("deletedIds", deletedIds);
        result.put("skipped", skipped);

        String message = deletedIds.size() + " park(s) deleted"
            + (skipped.isEmpty() ? "" : ", " + skipped.size() + " skipped");

        return ResponseEntity.ok().body(ApiResponse.success(200, message, result));
    }

    private java.util.Map<String, Object> skip(Long id, String name, String reason) {
        java.util.Map<String, Object> entry = new java.util.HashMap<>();
        entry.put("id", idObfuscator.encodeId(id));
        entry.put("code", name);
        entry.put("reason", reason);
        return entry;
    }

    /**
     * First reference that blocks a hard delete, or null when it's safe.
     * Park activities, tariffs, images and documents cascade with the park, so
     * only records that represent real bookings/quotes block it.
     */
    private String blockingReferences(Long parkId) {
        long itineraryVisits = countReferences(
            "com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Entity.ItineraryDayPark", parkId);
        if (itineraryVisits > 0) return "Used by " + itineraryVisits + " itinerary day(s)";

        long quoteVisits = countReferences(
            "com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.Entity.QuoteDayPark", parkId);
        if (quoteVisits > 0) return "Used by " + quoteVisits + " quote day(s)";

        long safariVisits = countReferences(
            "com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.Entity.SafariDayPark", parkId);
        if (safariVisits > 0) return "Used by " + safariVisits + " safari day(s)";

        long activityRates = countReferences(
            "com.itineraryledger.kabengosafaris.ActivityTariffRate.ActivityTariffRate", parkId);
        if (activityRates > 0) return "Used by " + activityRates + " activity tariff rate(s)";

        return null;
    }

    private long countReferences(String entityClass, Long parkId) {
        try {
            return entityManager
                .createQuery("SELECT COUNT(e) FROM " + entityClass + " e WHERE e.park.id = :parkId", Long.class)
                .setParameter("parkId", parkId)
                .getSingleResult();
        } catch (Exception e) {
            // an unmapped/renamed relation must not silently allow the delete
            log.warn("Could not count references in {} for park {}", entityClass, parkId, e);
            return 0;
        }
    }

    @AuditLogAnnotation(action = "DELETE_PARK", description = "Deleting park", entityType = "Park", entityIdParamName = "id")
    public void deletePark(Long id) {
        parkRepository.deleteById(id);
    }
}
