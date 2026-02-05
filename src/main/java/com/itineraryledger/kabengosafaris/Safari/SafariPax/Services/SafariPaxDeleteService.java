package com.itineraryledger.kabengosafaris.Safari.SafariPax.Services;

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
import com.itineraryledger.kabengosafaris.Safari.SafariPax.Entity.SafariPax;
import com.itineraryledger.kabengosafaris.Safari.SafariPax.Repository.SafariPaxRepository;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * SafariPaxDeleteService - Service for deleting safari pax entries
 */
@Service
@Slf4j
@Transactional
public class SafariPaxDeleteService {

    private final SafariPaxRepository safariPaxRepository;
    private final IdObfuscator idObfuscator;
    private final SafariRepository safariRepository;

    @Autowired
    public SafariPaxDeleteService(
        SafariPaxRepository safariPaxRepository,
        IdObfuscator idObfuscator,
        SafariRepository safariRepository
    ) {
        this.safariPaxRepository = safariPaxRepository;
        this.idObfuscator = idObfuscator;
        this.safariRepository = safariRepository;
    }

    /**
     * Delete pax entries by list of obfuscated IDs
     *
     * @param safariIdObfuscated The obfuscated safari ID
     * @param paxIdObfuscatedList List of obfuscated pax IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    public ResponseEntity<ApiResponse<?>> deleteSafariPax(String safariIdObfuscated, List<String> paxIdObfuscatedList) {
        log.info("Deleting {} pax entries from safari: {}", paxIdObfuscatedList.size(), safariIdObfuscated);

        try {
            // Decode safari ID
            Long safariId;
            try {
                safariId = idObfuscator.decodeId(safariIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid safari ID", "INVALID_SAFARI_ID")
                );
            }

            // Find safari
            Safari safari = safariRepository.findById(safariId).orElse(null);
            if (safari == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Safari not found", "SAFARI_NOT_FOUND")
                );
            }

            // Check if safari is editable
            if (!safari.isEditable()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Safari cannot be edited in state: " + safari.getState().getDisplayName(), "SAFARI_NOT_EDITABLE")
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

            return deletePaxInternal(safariId, paxIds);

        } catch (Exception e) {
            log.error("Error deleting safari pax", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete safari pax", "SAFARI_PAX_DELETE_FAILED")
            );
        }
    }

    /**
     * Delete pax entries by list of IDs (internal method)
     */
    private ResponseEntity<ApiResponse<?>> deletePaxInternal(Long safariId, List<Long> paxIds) {
        int deletedCount = 0;

        for (Long paxId : paxIds) {
            try {
                SafariPax pax = safariPaxRepository.findById(paxId).orElse(null);

                if (pax == null) {
                    log.warn("Pax entry not found: {}", paxId);
                    continue;
                }

                // Verify pax belongs to the safari
                if (!pax.getSafari().getId().equals(safariId)) {
                    log.warn("Pax {} does not belong to safari {}", paxId, safariId);
                    continue;
                }

                // Use AopContext to get proxy and trigger AOP aspect
                ((SafariPaxDeleteService) AopContext.currentProxy()).deletePax(paxId);
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

    @AuditLogAnnotation(action = "DELETE_SAFARI_PAX", description = "Deleting safari pax entry", entityType = "SafariPax", entityIdParamName = "id")
    public void deletePax(Long id) {
        safariPaxRepository.deleteById(id);
    }
}
