package com.itineraryledger.kabengosafaris.Season.Services.SeasonPeriodServices;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Season.Repositories.SeasonPeriodRepository;
import com.itineraryledger.kabengosafaris.Season.SeasonPeriod;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
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
 * DeleteSeasonPeriodService - Service for deleting season periods
 *
 * IMPORTANT PROTECTION RULE:
 * System season periods cannot be deleted (created by initializer with system seasons).
 * Global season periods can be deleted.
 * Protected periods can only be updated, never deleted.
 */
@Service
@Slf4j
@Transactional
public class DeleteSeasonPeriodService {

    private final SeasonPeriodRepository seasonPeriodRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public DeleteSeasonPeriodService(
        SeasonPeriodRepository seasonPeriodRepository,
        IdObfuscator idObfuscator
    ) {
        this.seasonPeriodRepository = seasonPeriodRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Delete season periods by list of IDs
     * Performs hard delete using repository.deleteById()
     *
     * @param idObfuscatedList List of obfuscated season period IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    public ResponseEntity<ApiResponse<?>> deleteSeasonPeriods(List<String> idObfuscatedList) {
        log.info("Deleting {} season periods", idObfuscatedList.size());

        try {
            if (idObfuscatedList == null || idObfuscatedList.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "No season period IDs provided",
                        "NO_IDS_PROVIDED"
                    )
                );
            }

            List<Long> periodIds = new ArrayList<>();
            List<String> notFoundIds = new ArrayList<>();
            int deletedCount = 0;

            // Decode all IDs first
            for (String idObfuscated : idObfuscatedList) {
                try {
                    Long periodId = idObfuscator.decodeId(idObfuscated);
                    periodIds.add(periodId);
                } catch (Exception e) {
                    log.warn("Failed to decode season period ID: {}", idObfuscated, e);
                    notFoundIds.add(idObfuscated);
                }
            }

            // First, validate that no season period in the list is system
            List<String> systemPeriodIds = new ArrayList<>();

            for (Long periodId : periodIds) {
                SeasonPeriod seasonPeriod = seasonPeriodRepository.findById(periodId).orElse(null);
                if (seasonPeriod != null && seasonPeriod.getIsSystem() != null && seasonPeriod.getIsSystem()) {
                    systemPeriodIds.add(idObfuscator.encodeId(periodId));
                }
            }

            // If any system periods found, reject entire operation
            if (!systemPeriodIds.isEmpty()) {
                log.warn("Cannot delete: {} season period(s) in the list are system season period(s)", systemPeriodIds.size());

                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Cannot delete any season periods: " + systemPeriodIds.size() + " season period(s) in the list are system season periods created by initializer",
                        "CANNOT_DELETE_SYSTEM_SEASON_PERIODS"
                    )
                );
            }

            // Delete each season period
            for (Long periodId : periodIds) {
                SeasonPeriod seasonPeriod = seasonPeriodRepository.findById(periodId).orElse(null);
                if (seasonPeriod != null) {
                    // Use proxy to ensure audit logging works
                    DeleteSeasonPeriodService proxy = (DeleteSeasonPeriodService) AopContext.currentProxy();
                    proxy.deleteSingleSeasonPeriod(periodId);
                    deletedCount++;
                } else {
                    notFoundIds.add(idObfuscator.encodeId(periodId));
                }
            }

            // Prepare response
            if (deletedCount > 0) {
                return ResponseEntity.ok().body(
                    ApiResponse.success(
                        200,
                        deletedCount + " season period(s) deleted successfully",
                        null
                    )
                );
            } else {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "No season periods were deleted. " + notFoundIds.size() + " season period(s) not found",
                        "NO_SEASON_PERIODS_DELETED"
                    )
                );
            }

        } catch (Exception e) {
            log.error("Error deleting season periods", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to delete season periods",
                    "SEASON_PERIOD_DELETE_FAILED"
                )
            );
        }
    }

    /**
     * Delete a single season period by ID (with audit logging)
     */
    @AuditLogAnnotation(
        action = "DELETE_SEASON_PERIOD",
        description = "Deleting season period",
        entityType = "SeasonPeriod",
        entityIdParamName = "id"
    )
    public void deleteSingleSeasonPeriod(Long id) {
        log.info("Deleting season period with ID: {}", id);
        seasonPeriodRepository.deleteById(id);
    }
}
