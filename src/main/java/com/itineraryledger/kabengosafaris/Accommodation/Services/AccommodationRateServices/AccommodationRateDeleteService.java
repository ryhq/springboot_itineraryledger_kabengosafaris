package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationRateServices;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRate;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRateRepository;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * AccommodationRateDeleteService - Service for deleting accommodation rates
 *
 * Handles single and bulk delete operations for accommodation rates.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccommodationRateDeleteService {

    private final AccommodationRateRepository rateRepository;
    private final IdObfuscator idObfuscator;

    /**
     * Delete rates by IDs
     *
     * @param idsObfuscated List of obfuscated rate IDs to delete
     * @return Response with number of deleted rates
     */
    @Transactional
    @AuditLogAnnotation(action = "DELETE_ACCOMMODATION_RATES", description = "Deleting accommodation rates", entityType = "AccommodationRate")
    public ResponseEntity<ApiResponse<?>> deleteRates(List<String> idsObfuscated) {
        log.info("Deleting {} accommodation rates", idsObfuscated.size());

        try {
            if (idsObfuscated == null || idsObfuscated.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "No rate IDs provided", "NO_IDS")
                );
            }

            List<Long> ids = new ArrayList<>();
            List<String> invalidIds = new ArrayList<>();

            for (String idObfuscated : idsObfuscated) {
                Long id = idObfuscator.decodeId(idObfuscated);
                if (id == null) {
                    invalidIds.add(idObfuscated);
                } else {
                    ids.add(id);
                }
            }

            if (!invalidIds.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid rate ID(s): " + String.join(", ", invalidIds), "INVALID_IDS")
                );
            }

            List<AccommodationRate> ratesToDelete = rateRepository.findAllById(ids);
            if (ratesToDelete.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "No rates found to delete", "RATES_NOT_FOUND")
                );
            }

            rateRepository.deleteAll(ratesToDelete);
            log.info("Deleted {} accommodation rates", ratesToDelete.size());

            return ResponseEntity.ok(ApiResponse.success(200, ratesToDelete.size() + " rate(s) deleted successfully", null));

        } catch (Exception e) {
            log.error("Error deleting accommodation rates", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete rates: " + e.getMessage(), "DELETE_FAILED")
            );
        }
    }
}
