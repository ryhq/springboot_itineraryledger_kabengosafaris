package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.Services;

import java.util.List;

import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.Entity.ItineraryDayParkTariff;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.Repository.ItineraryDayParkTariffRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryDayParkTariffDeleteService - Service for deleting park tariffs within a park visit
 *
 * Provides bulk deletion of park tariffs.
 */
@Service
@Slf4j
@Transactional
public class ItineraryDayParkTariffDeleteService {

    private final ItineraryDayParkTariffRepository parkTariffRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ItineraryDayParkTariffDeleteService(
        ItineraryDayParkTariffRepository parkTariffRepository,
        IdObfuscator idObfuscator
    ) {
        this.parkTariffRepository = parkTariffRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Delete park tariffs by list of obfuscated IDs.
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
        log.info("Deleting {} park tariffs", tariffIdObfuscatedList.size());

        try {
            Long parkVisitId;
            try {
                parkVisitId = idObfuscator.decodeId(parkVisitIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid park visit ID", "INVALID_ID")
                );
            }

            int deletedCount = 0;
            for (String idObfuscated : tariffIdObfuscatedList) {
                try {
                    Long tariffEntryId = idObfuscator.decodeId(idObfuscated);
                    ItineraryDayParkTariff entry = parkTariffRepository.findById(tariffEntryId).orElse(null);

                    if (entry == null || !entry.getItineraryDayPark().getId().equals(parkVisitId)) {
                        continue;
                    }

                    ((ItineraryDayParkTariffDeleteService) AopContext.currentProxy()).deleteTariff(tariffEntryId);
                    deletedCount++;
                } catch (Exception e) {
                    log.error("Error deleting park tariff", e);
                }
            }

            return ResponseEntity.ok().body(
                ApiResponse.success(200, deletedCount + " tariffs deleted", null)
            );

        } catch (Exception e) {
            log.error("Error deleting park tariffs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete park tariffs", "DELETE_FAILED")
            );
        }
    }

    @AuditLogAnnotation(action = "DELETE_PARK_TARIFF", description = "Deleting park tariff", entityType = "ItineraryDayParkTariff", entityIdParamName = "id")
    public void deleteTariff(Long id) {
        parkTariffRepository.deleteById(id);
    }
}
