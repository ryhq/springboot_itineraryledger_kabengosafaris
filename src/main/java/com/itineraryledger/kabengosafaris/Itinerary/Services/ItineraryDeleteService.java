package com.itineraryledger.kabengosafaris.Itinerary.Services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.Repository.ItineraryRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryDeleteService - Service for deleting itineraries
 */
@Service
@Slf4j
@Transactional
public class ItineraryDeleteService {

    private final ItineraryRepository itineraryRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ItineraryDeleteService(
        ItineraryRepository itineraryRepository,
        IdObfuscator idObfuscator
    ) {
        this.itineraryRepository = itineraryRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Delete itineraries by list of obfuscated IDs
     *
     * @param idObfuscatedList List of obfuscated itinerary IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    public ResponseEntity<ApiResponse<?>> deleteItineraries(List<String> idObfuscatedList) {
        log.info("Deleting {} itineraries", idObfuscatedList.size());

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

            return deleteItinerariesInternal(ids);

        } catch (Exception e) {
            log.error("Error deleting itineraries", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to delete itineraries",
                    "ITINERARIES_DELETE_FAILED"
                )
            );
        }
    }

    /**
     * Delete itineraries by list of IDs (internal method)
     */
    private ResponseEntity<ApiResponse<?>> deleteItinerariesInternal(List<Long> ids) {
        int deletedCount = 0;

        for (Long id : ids) {
            try {
                Itinerary itinerary = itineraryRepository.findById(id).orElse(null);

                if (itinerary == null) {
                    log.warn("Itinerary not found: {}", id);
                    continue;
                }

                // Use AopContext to get proxy and trigger AOP aspect
                ((ItineraryDeleteService) AopContext.currentProxy()).deleteItinerary(id);
                deletedCount++;
                log.info("Itinerary deleted successfully: {}", id);

            } catch (Exception e) {
                log.error("Error deleting itinerary: {}", id, e);
            }
        }

        String message =  deletedCount > 1 ? " itineraries deleted successfully" : " itinerary deleted successfully" ;

        return ResponseEntity.ok().body(
            ApiResponse.success(
                200,
                deletedCount + message,
                null
            )
        );
    }

    @AuditLogAnnotation(action = "DELETE_ITINERARY", description = "Deleting itinerary", entityType = "Itinerary", entityIdParamName = "id")
    public void deleteItinerary(Long id) {
        itineraryRepository.deleteById(id);
    }
}
