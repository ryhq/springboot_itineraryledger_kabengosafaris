package com.itineraryledger.kabengosafaris.Itinerary.ItineraryPax.Services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryPax.Entity.ItineraryPax;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryPax.Repository.ItineraryPaxRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryPaxDeleteService - Service for deleting itinerary pax entries
 */
@Service
@Slf4j
@Transactional
public class ItineraryPaxDeleteService {

    private final ItineraryPaxRepository itineraryPaxRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ItineraryPaxDeleteService(
        ItineraryPaxRepository itineraryPaxRepository,
        IdObfuscator idObfuscator
    ) {
        this.itineraryPaxRepository = itineraryPaxRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Delete pax entries by list of obfuscated IDs
     *
     * @param itineraryIdObfuscated The obfuscated itinerary ID
     * @param paxIdObfuscatedList List of obfuscated pax IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    public ResponseEntity<ApiResponse<?>> deleteItineraryPax(String itineraryIdObfuscated, List<String> paxIdObfuscatedList) {
        log.info("Deleting {} pax entries from itinerary: {}", paxIdObfuscatedList.size(), itineraryIdObfuscated);

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

            // Decode all pax IDs
            List<Long> paxIds = new ArrayList<>();
            for (String idObfuscated : paxIdObfuscatedList) {
                try {
                    Long id = idObfuscator.decodeId(idObfuscated);
                    paxIds.add(id);
                } catch (Exception e) {
                    log.warn("Failed to decode pax ID: {}", idObfuscated, e);
                }
            }

            return deletePaxInternal(itineraryId, paxIds);

        } catch (Exception e) {
            log.error("Error deleting itinerary pax", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete itinerary pax", "ITINERARY_PAX_DELETE_FAILED")
            );
        }
    }

    /**
     * Delete pax entries by list of IDs (internal method)
     */
    private ResponseEntity<ApiResponse<?>> deletePaxInternal(Long itineraryId, List<Long> paxIds) {
        int deletedCount = 0;

        for (Long paxId : paxIds) {
            try {
                ItineraryPax pax = itineraryPaxRepository.findById(paxId).orElse(null);

                if (pax == null) {
                    log.warn("Pax entry not found: {}", paxId);
                    continue;
                }

                // Verify pax belongs to the itinerary
                if (!pax.getItinerary().getId().equals(itineraryId)) {
                    log.warn("Pax {} does not belong to itinerary {}", paxId, itineraryId);
                    continue;
                }

                // Use AopContext to get proxy and trigger AOP aspect
                ((ItineraryPaxDeleteService) AopContext.currentProxy()).deletePax(paxId);
                deletedCount++;
                log.info("Pax entry deleted: {}", paxId);

            } catch (Exception e) {
                log.error("Error deleting pax: {}", paxId, e);
            }
        }

        return ResponseEntity.ok().body(
            ApiResponse.success(200, deletedCount + " pax entry(ies) deleted successfully", null)
        );
    }

    @AuditLogAnnotation(action = "DELETE_ITINERARY_PAX", description = "Deleting itinerary pax entry", entityType = "ItineraryPax", entityIdParamName = "id")
    public void deletePax(Long id) {
        itineraryPaxRepository.deleteById(id);
    }
}
