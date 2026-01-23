package com.itineraryledger.kabengosafaris.Safari.Services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.Repository.SafariRepository;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * SafariDeleteService - Service for deleting safaris
 */
@Service
@Slf4j
@Transactional
public class SafariDeleteService {

    private final SafariRepository safariRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public SafariDeleteService(
        SafariRepository safariRepository,
        IdObfuscator idObfuscator
    ) {
        this.safariRepository = safariRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Delete safaris by list of obfuscated IDs
     *
     * @param idObfuscatedList List of obfuscated safari IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    public ResponseEntity<ApiResponse<?>> deleteSafaris(List<String> idObfuscatedList) {
        log.info("Deleting {} safaris", idObfuscatedList.size());

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

            return deleteSafarisInternal(ids);

        } catch (Exception e) {
            log.error("Error deleting safaris", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to delete safaris",
                    "SAFARIS_DELETE_FAILED"
                )
            );
        }
    }

    /**
     * Delete safaris by list of IDs (internal method)
     */
    private ResponseEntity<ApiResponse<?>> deleteSafarisInternal(List<Long> ids) {
        int deletedCount = 0;

        for (Long id : ids) {
            try {
                Safari safari = safariRepository.findById(id).orElse(null);

                if (safari == null) {
                    log.warn("Safari not found: {}", id);
                    continue;
                }

                // Use AopContext to get proxy and trigger AOP aspect
                ((SafariDeleteService) AopContext.currentProxy()).deleteSafari(id);
                deletedCount++;
                log.info("Safari deleted successfully: {}", id);

            } catch (Exception e) {
                log.error("Error deleting safari: {}", id, e);
            }
        }

        String message = deletedCount > 1 ? " safaris deleted successfully" : " safari deleted successfully";

        return ResponseEntity.ok().body(
            ApiResponse.success(
                200,
                deletedCount + message,
                null
            )
        );
    }

    @AuditLogAnnotation(action = "DELETE_SAFARI", description = "Deleting safari", entityType = "Safari", entityIdParamName = "id")
    public void deleteSafari(Long id) {
        safariRepository.deleteById(id);
    }
}
