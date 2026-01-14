package com.itineraryledger.kabengosafaris.Tariff.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Tariff.Repositories.TariffRepository;
import com.itineraryledger.kabengosafaris.Tariff.Tariff;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * DeleteTariffService - Service for deleting tariffs
 *
 * System tariffs (isSystem = true) cannot be deleted.
 * Tariffs with existing park links (ParkTariff) should be handled carefully.
 */
@Service
@Slf4j
public class DeleteTariffService {

    private final TariffRepository tariffRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public DeleteTariffService(
        TariffRepository tariffRepository,
        IdObfuscator idObfuscator
    ) {
        this.tariffRepository = tariffRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Delete tariffs by list of IDs
     *
     * @param ids List of obfuscated tariff IDs
     * @return ResponseEntity with ApiResponse
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> deleteTariffs(List<String> ids) {
        log.info("Deleting tariffs: {}", ids);

        try {
            // Validate input
            if (ids == null || ids.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "No tariff IDs provided", "NO_IDS_PROVIDED")
                );
            }

            // Decode all IDs and validate
            List<Long> decodedIds = new ArrayList<>();
            List<String> invalidIds = new ArrayList<>();

            for (String id : ids) {
                Long decodedId = idObfuscator.decodeId(id);
                if (decodedId == null) {
                    invalidIds.add(id);
                } else {
                    decodedIds.add(decodedId);
                }
            }

            if (!invalidIds.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid tariff ID(s): " + String.join(", ", invalidIds),
                        "INVALID_TARIFF_IDS"
                    )
                );
            }

            // Check for system tariffs
            List<Tariff> tariffsToDelete = tariffRepository.findAllById(decodedIds);
            List<String> systemTariffNames = new ArrayList<>();

            for (Tariff tariff : tariffsToDelete) {
                if (tariff.isSystemTariff()) {
                    systemTariffNames.add(tariff.getName());
                }
            }

            if (!systemTariffNames.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Cannot delete system tariffs: " + String.join(", ", systemTariffNames),
                        "CANNOT_DELETE_SYSTEM_TARIFFS"
                    )
                );
            }

            // Delete tariffs with audit logging
            int deletedCount = 0;
            DeleteTariffService proxy = (DeleteTariffService) AopContext.currentProxy();

            for (Tariff tariff : tariffsToDelete) {
                proxy.deleteSingleTariff(tariff);
                deletedCount++;
            }

            if (deletedCount == 0) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "No tariffs were deleted (not found)",
                        "NO_TARIFFS_DELETED"
                    )
                );
            }

            log.info("Deleted {} tariffs", deletedCount);

            return ResponseEntity.ok(
                ApiResponse.success(
                    200,
                    deletedCount + " tariff(s) deleted successfully",
                    null
                )
            );

        } catch (Exception e) {
            log.error("Error deleting tariffs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to delete tariffs: " + e.getMessage(),
                    "TARIFF_DELETE_FAILED"
                )
            );
        }
    }

    /**
     * Delete a single tariff with audit logging
     */
    @AuditLogAnnotation(
        action = "DELETE_TARIFF",
        description = "Deleting a tariff",
        entityType = "Tariff"
    )
    public void deleteSingleTariff(Tariff tariff) {
        log.info("Deleting tariff: {}", tariff.getName());
        tariffRepository.delete(tariff);
    }
}
