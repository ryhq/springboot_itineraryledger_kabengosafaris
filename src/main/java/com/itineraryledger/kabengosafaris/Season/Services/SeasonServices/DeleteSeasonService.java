package com.itineraryledger.kabengosafaris.Season.Services.SeasonServices;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Season.Repositories.SeasonRepository;
import com.itineraryledger.kabengosafaris.Season.Season;
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
 * DeleteSeasonService - Service for deleting seasons
 *
 * IMPORTANT PROTECTION RULE:
 * System seasons cannot be deleted (created by initializer).
 * Global seasons can be deleted.
 * Protected seasons can only be updated, never deleted.
 */
@Service
@Slf4j
@Transactional
public class DeleteSeasonService {

    private final SeasonRepository seasonRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public DeleteSeasonService(
        SeasonRepository seasonRepository,
        IdObfuscator idObfuscator
    ) {
        this.seasonRepository = seasonRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Delete seasons by list of IDs
     * System seasons cannot be deleted - only user-created and global seasons can be deleted
     *
     * @param idObfuscatedList List of obfuscated season IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    public ResponseEntity<ApiResponse<?>> deleteSeasons(List<String> idObfuscatedList) {
        log.info("Deleting {} seasons", idObfuscatedList.size());

        try {
            if (idObfuscatedList == null || idObfuscatedList.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "No season IDs provided",
                        "NO_IDS_PROVIDED"
                    )
                );
            }

            List<Long> seasonIds = new ArrayList<>();
            List<String> notFoundIds = new ArrayList<>();
            int deletedCount = 0;

            // Decode all IDs first
            for (String idObfuscated : idObfuscatedList) {
                try {
                    Long seasonId = idObfuscator.decodeId(idObfuscated);
                    seasonIds.add(seasonId);
                } catch (Exception e) {
                    log.warn("Failed to decode season ID: {}", idObfuscated, e);
                    notFoundIds.add(idObfuscated);
                }
            }

            // First, validate that no season in the list is system
            List<String> systemSeasonIds = new ArrayList<>();

            for (Long seasonId : seasonIds) {
                Season season = seasonRepository.findById(seasonId).orElse(null);
                if (season != null && season.getIsSystem() != null && season.getIsSystem()) {
                    systemSeasonIds.add(idObfuscator.encodeId(seasonId));
                }
            }

            // If any system seasons found, reject entire operation
            if (!systemSeasonIds.isEmpty()) {
                log.warn("Cannot delete: {} season(s) in the list are system season(s)", systemSeasonIds.size());

                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Cannot delete any seasons: " + systemSeasonIds.size() + " season(s) in the list are system seasons created by initializer",
                        "CANNOT_DELETE_SYSTEM_SEASONS"
                    )
                );
            }

            // Delete each season
            for (Long seasonId : seasonIds) {
                Season season = seasonRepository.findById(seasonId).orElse(null);
                if (season != null) {
                    // Use proxy to ensure audit logging works
                    DeleteSeasonService proxy = (DeleteSeasonService) AopContext.currentProxy();
                    proxy.deleteSingleSeason(seasonId);
                    deletedCount++;
                } else {
                    notFoundIds.add(idObfuscator.encodeId(seasonId));
                }
            }

            // Prepare response
            if (deletedCount > 0) {
                return ResponseEntity.ok().body(
                    ApiResponse.success(
                        200,
                        deletedCount + " season(s) deleted successfully",
                        null
                    )
                );
            } else {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "No seasons were deleted. " + notFoundIds.size() + " season(s) not found",
                        "NO_SEASONS_DELETED"
                    )
                );
            }

        } catch (Exception e) {
            log.error("Error deleting seasons", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to delete seasons",
                    "SEASON_DELETE_FAILED"
                )
            );
        }
    }

    /**
     * Delete a single season by ID (with audit logging)
     */
    @AuditLogAnnotation(
        action = "DELETE_SEASON",
        description = "Deleting season",
        entityType = "Season",
        entityIdParamName = "id"
    )
    public void deleteSingleSeason(Long id) {
        log.info("Deleting season with ID: {}", id);
        seasonRepository.deleteById(id);
    }
}
