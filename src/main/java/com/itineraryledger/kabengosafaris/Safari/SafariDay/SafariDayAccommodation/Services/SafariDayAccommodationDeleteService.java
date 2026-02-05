package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.Services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.Entity.SafariDayAccommodation;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.Repository.SafariDayAccommodationRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * SafariDayAccommodationDeleteService - Service for deleting safari day accommodations
 */
@Service
@Slf4j
@Transactional
public class SafariDayAccommodationDeleteService {

    private final SafariDayAccommodationRepository accommodationRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public SafariDayAccommodationDeleteService(
        SafariDayAccommodationRepository accommodationRepository,
        IdObfuscator idObfuscator
    ) {
        this.accommodationRepository = accommodationRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Delete an accommodation
     *
     * @param safariIdObfuscated The obfuscated safari ID
     * @param dayIdObfuscated The obfuscated day ID
     * @param accommodationIdObfuscated The obfuscated accommodation entry ID
     * @return ResponseEntity with ApiResponse
     */
    public ResponseEntity<ApiResponse<?>> deleteSafariDayAccommodation(
        String safariIdObfuscated,
        String dayIdObfuscated,
        String accommodationIdObfuscated
    ) {
        log.info("Deleting accommodation: {}", accommodationIdObfuscated);

        try {
            // Decode IDs
            Long safariId;
            Long dayId;
            Long accommodationEntryId;
            try {
                safariId = idObfuscator.decodeId(safariIdObfuscated);
                dayId = idObfuscator.decodeId(dayIdObfuscated);
                accommodationEntryId = idObfuscator.decodeId(accommodationIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid ID", "INVALID_ID")
                );
            }

            // Find accommodation
            SafariDayAccommodation accommodation = accommodationRepository.findById(accommodationEntryId).orElse(null);
            if (accommodation == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Accommodation not found", "ACCOMMODATION_NOT_FOUND")
                );
            }

            // Verify ownership chain
            if (!accommodation.getSafariDay().getId().equals(dayId) ||
                !accommodation.getSafariDay().getSafari().getId().equals(safariId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Accommodation does not belong to this day/safari", "OWNERSHIP_MISMATCH")
                );
            }

            // Check if safari is editable
            Safari safari = accommodation.getSafariDay().getSafari();
            if (!safari.isEditable()) {
                log.warn("Safari is not editable: {} (state: {})", safari.getCode(), safari.getState());
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Safari cannot be edited in state: " + safari.getState().getDisplayName(),
                        "SAFARI_NOT_EDITABLE"
                    )
                );
            }

            // Delete
            ((SafariDayAccommodationDeleteService) AopContext.currentProxy()).deleteAccommodation(accommodationEntryId);

            log.info("Accommodation deleted successfully: {}", accommodationEntryId);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Accommodation deleted successfully", null)
            );

        } catch (Exception e) {
            log.error("Error deleting accommodation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete accommodation", "ACCOMMODATION_DELETE_FAILED")
            );
        }
    }

    /**
     * Delete multiple accommodations
     */
    public ResponseEntity<ApiResponse<?>> deleteSafariDayAccommodations(
        String safariIdObfuscated,
        String dayIdObfuscated,
        List<String> accommodationIdObfuscatedList
    ) {
        log.info("Deleting {} accommodations", accommodationIdObfuscatedList.size());

        try {
            Long safariId = idObfuscator.decodeId(safariIdObfuscated);
            Long dayId = idObfuscator.decodeId(dayIdObfuscated);

            List<Long> accommodationIds = new ArrayList<>();
            for (String idObfuscated : accommodationIdObfuscatedList) {
                try {
                    accommodationIds.add(idObfuscator.decodeId(idObfuscated));
                } catch (Exception e) {
                    log.warn("Failed to decode accommodation ID: {}", idObfuscated);
                }
            }

            int deletedCount = 0;
            Safari safari = null;

            for (Long accommodationId : accommodationIds) {
                try {
                    SafariDayAccommodation accommodation = accommodationRepository.findById(accommodationId).orElse(null);
                    if (accommodation == null) continue;

                    if (!accommodation.getSafariDay().getId().equals(dayId) ||
                        !accommodation.getSafariDay().getSafari().getId().equals(safariId)) {
                        continue;
                    }

                    // Get safari instance for editable check
                    if (safari == null) {
                        safari = accommodation.getSafariDay().getSafari();
                    }

                    ((SafariDayAccommodationDeleteService) AopContext.currentProxy()).deleteAccommodation(accommodationId);
                    deletedCount++;
                } catch (Exception e) {
                    log.error("Error deleting accommodation: {}", accommodationId, e);
                }
            }

            // Check safari is editable after collecting safari instance
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
                ApiResponse.success(200, deletedCount + " accommodation(s) deleted successfully", null)
            );

        } catch (Exception e) {
            log.error("Error deleting accommodations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete accommodations", "ACCOMMODATIONS_DELETE_FAILED")
            );
        }
    }

    @AuditLogAnnotation(action = "DELETE_SAFARI_DAY_ACCOMMODATION", description = "Deleting accommodation", entityType = "SafariDayAccommodation", entityIdParamName = "id")
    public void deleteAccommodation(Long id) {
        accommodationRepository.deleteById(id);
    }
}
