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
        int deletedCount = 0;

        for (Long id : ids) {
            try {
                Park park = parkRepository.findById(id).orElse(null);

                if (park == null) {
                    log.warn("Park not found: {}", id);
                    continue;
                }

                // Use AopContext to get proxy and trigger AOP aspect
                ((ParkDeleteService) AopContext.currentProxy()).deletePark(id);
                deletedCount++;
                log.info("Park deleted successfully: {}", id);

            } catch (Exception e) {
                log.error("Error deleting park: {}", id, e);
            }
        }

        return ResponseEntity.ok().body(
            ApiResponse.success(
                200,
                deletedCount + " park(s) deleted successfully",
                null
            )
        );
    }

    @AuditLogAnnotation(action = "DELETE_PARK", description = "Deleting park", entityType = "Park", entityIdParamName = "id")
    public void deletePark(Long id) {
        parkRepository.deleteById(id);
    }
}
