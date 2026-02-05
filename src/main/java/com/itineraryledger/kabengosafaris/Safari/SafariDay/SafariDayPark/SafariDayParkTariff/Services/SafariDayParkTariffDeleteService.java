package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkTariff.Services;

import java.util.List;

import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkTariff.Entity.SafariDayParkTariff;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkTariff.Repository.SafariDayParkTariffRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * SafariDayParkTariffDeleteService - Service for deleting park tariffs within a safari park visit
 *
 * Provides bulk deletion of park tariffs with Safari state validation.
 */
@Service
@Slf4j
@Transactional
public class SafariDayParkTariffDeleteService {

    private final SafariDayParkTariffRepository parkTariffRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public SafariDayParkTariffDeleteService(
        SafariDayParkTariffRepository parkTariffRepository,
        IdObfuscator idObfuscator
    ) {
        this.parkTariffRepository = parkTariffRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Delete safari park tariffs by list of obfuscated IDs.
     *
     * This method handles both single and bulk deletions:
     * - For single deletion, pass a list with one ID
     * - For bulk deletion, pass a list with multiple IDs
     *
     * @param parkVisitIdObfuscated The obfuscated park visit ID
     * @param tariffIdObfuscatedList List of obfuscated tariff entry IDs to delete
     * @return ResponseEntity with ApiResponse containing count of deleted tariffs
     */
    public ResponseEntity<ApiResponse<?>> deleteParkTariffs(
        String parkVisitIdObfuscated,
        List<String> tariffIdObfuscatedList
    ) {
        log.info("Deleting {} safari park tariffs", tariffIdObfuscatedList.size());

        try {
            Long parkVisitId;
            try {
                parkVisitId = idObfuscator.decodeId(parkVisitIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid park visit ID", "INVALID_ID")
                );
            }

            Safari safari = null;
            int deletedCount = 0;

            for (String idObfuscated : tariffIdObfuscatedList) {
                try {
                    Long tariffEntryId = idObfuscator.decodeId(idObfuscated);
                    SafariDayParkTariff entry = parkTariffRepository.findById(tariffEntryId).orElse(null);

                    if (entry == null || !entry.getSafariDayPark().getId().equals(parkVisitId)) {
                        continue;
                    }

                    // Capture safari instance for validation
                    if (safari == null) {
                        safari = entry.getSafariDayPark().getSafariDay().getSafari();
                    }

                    ((SafariDayParkTariffDeleteService) AopContext.currentProxy()).deleteTariff(tariffEntryId);
                    deletedCount++;
                } catch (Exception e) {
                    log.error("Error deleting safari park tariff", e);
                }
            }

            // Check if safari is editable (after collecting safari instance)
            if (safari != null && !safari.isEditable()) {
                log.warn("Safari is not editable: {} (state: {})", safari.getCode(), safari.getState());
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Safari cannot be edited in state: " + safari.getState().getDisplayName(),
                        "SAFARI_NOT_EDITABLE"
                    )
                );
            }

            return ResponseEntity.ok().body(
                ApiResponse.success(200, deletedCount + " tariffs deleted", null)
            );

        } catch (Exception e) {
            log.error("Error deleting safari park tariffs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete safari park tariffs", "DELETE_FAILED")
            );
        }
    }

    @AuditLogAnnotation(action = "DELETE_SAFARI_PARK_TARIFF", description = "Deleting safari park tariff", entityType = "SafariDayParkTariff", entityIdParamName = "id")
    public void deleteTariff(Long id) {
        parkTariffRepository.deleteById(id);
    }
}
