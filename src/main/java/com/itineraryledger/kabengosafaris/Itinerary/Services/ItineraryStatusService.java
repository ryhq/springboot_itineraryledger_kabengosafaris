package com.itineraryledger.kabengosafaris.Itinerary.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary.ItineraryStatus;
import com.itineraryledger.kabengosafaris.Itinerary.Repository.ItineraryRepository;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.ItineraryDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryStatusService - Service for managing itinerary status transitions
 */
@Service
@Slf4j
@Transactional
public class ItineraryStatusService {

    private final ItineraryRepository itineraryRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ItineraryStatusService(
        ItineraryRepository itineraryRepository,
        IdObfuscator idObfuscator
    ) {
        this.itineraryRepository = itineraryRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Evaluate and update itinerary status based on completeness
     * Called after adding/removing days, pax, etc.
     *
     * @param idObfuscated The obfuscated itinerary ID
     * @return ResponseEntity with ApiResponse containing the updated itinerary
     */
    public ResponseEntity<ApiResponse<?>> evaluateStatus(String idObfuscated) {
        log.info("Evaluating status for itinerary: {}", idObfuscated);

        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            Itinerary itinerary = itineraryRepository.findById(id).orElse(null);

            if (itinerary == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Itinerary not found", "ITINERARY_NOT_FOUND")
                );
            }

            // Only evaluate if currently DRAFT
            if (itinerary.getStatus() == ItineraryStatus.DRAFT) {
                if (itinerary.canPublish()) {
                    itinerary.setStatus(ItineraryStatus.COMPLETE);
                    itinerary = itineraryRepository.save(itinerary);
                    log.info("Itinerary {} status changed to COMPLETE", itinerary.getCode());
                }
            }
            // If COMPLETE but no longer meets requirements, revert to DRAFT
            else if (itinerary.getStatus() == ItineraryStatus.COMPLETE) {
                if (!itinerary.canPublish()) {
                    itinerary.setStatus(ItineraryStatus.DRAFT);
                    itinerary = itineraryRepository.save(itinerary);
                    log.info("Itinerary {} status reverted to DRAFT", itinerary.getCode());
                }
            }

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Status evaluated", convertToDTO(itinerary))
            );

        } catch (Exception e) {
            log.error("Error evaluating itinerary status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to evaluate status", "STATUS_EVAL_FAILED")
            );
        }
    }

    /**
     * Publish an itinerary (mark as available for booking, creating itinerary safari from it)
     * Only allowed if status is COMPLETE
     *
     * @param idObfuscated The obfuscated itinerary ID
     * @return ResponseEntity with ApiResponse containing the updated itinerary
     */
    @AuditLogAnnotation(action = "PUBLISH_ITINERARY", description = "Publishing itinerary", entityType = "Itinerary", entityIdParamName = "idObfuscated")
    public ResponseEntity<ApiResponse<?>> publishItinerary(String idObfuscated) {
        log.info("Publishing itinerary: {}", idObfuscated);

        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            Itinerary itinerary = itineraryRepository.findById(id).orElse(null);

            if (itinerary == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Itinerary not found", "ITINERARY_NOT_FOUND")
                );
            }

            // Validate current status
            if (itinerary.getStatus() == ItineraryStatus.ARCHIVED) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Cannot publish an archived itinerary", "INVALID_STATUS_TRANSITION")
                );
            }

            if (itinerary.getStatus() == ItineraryStatus.PUBLISHED) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Itinerary is already published", "ALREADY_PUBLISHED")
                );
            }

            // Validate completeness
            if (!itinerary.canPublish()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Itinerary does not meet publishing requirements. Ensure it has all days defined and at least one passenger category.",
                        "INCOMPLETE_ITINERARY"
                    )
                );
            }

            // Update status
            itinerary.setStatus(ItineraryStatus.PUBLISHED);
            itinerary = itineraryRepository.save(itinerary);

            log.info("Itinerary published successfully: {}", itinerary.getCode());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Itinerary published successfully", convertToDTO(itinerary))
            );

        } catch (Exception e) {
            log.error("Error publishing itinerary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to publish itinerary", "PUBLISH_FAILED")
            );
        }
    }

    /**
     * Unpublish an itinerary (revert to COMPLETE status)
     *
     * @param idObfuscated The obfuscated itinerary ID
     * @return ResponseEntity with ApiResponse containing the updated itinerary
     */
    @AuditLogAnnotation(action = "UNPUBLISH_ITINERARY", description = "Unpublishing itinerary", entityType = "Itinerary", entityIdParamName = "idObfuscated")
    public ResponseEntity<ApiResponse<?>> unpublishItinerary(String idObfuscated) {
        log.info("Unpublishing itinerary: {}", idObfuscated);

        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            Itinerary itinerary = itineraryRepository.findById(id).orElse(null);

            if (itinerary == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Itinerary not found", "ITINERARY_NOT_FOUND")
                );
            }

            if (itinerary.getStatus() != ItineraryStatus.PUBLISHED) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Itinerary is not published", "NOT_PUBLISHED")
                );
            }

            // Revert to COMPLETE (assuming it still meets requirements)
            itinerary.setStatus(itinerary.canPublish() ? ItineraryStatus.COMPLETE : ItineraryStatus.DRAFT);
            itinerary = itineraryRepository.save(itinerary);

            log.info("Itinerary unpublished: {} -> {}", itinerary.getCode(), itinerary.getStatus());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Itinerary unpublished successfully", convertToDTO(itinerary))
            );

        } catch (Exception e) {
            log.error("Error unpublishing itinerary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to unpublish itinerary", "UNPUBLISH_FAILED")
            );
        }
    }

    /**
     * Archive an itinerary
     *
     * @param idObfuscated The obfuscated itinerary ID
     * @return ResponseEntity with ApiResponse containing the updated itinerary
     */
    @AuditLogAnnotation(action = "ARCHIVE_ITINERARY", description = "Archiving itinerary", entityType = "Itinerary", entityIdParamName = "idObfuscated")
    public ResponseEntity<ApiResponse<?>> archiveItinerary(String idObfuscated) {
        log.info("Archiving itinerary: {}", idObfuscated);

        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            Itinerary itinerary = itineraryRepository.findById(id).orElse(null);

            if (itinerary == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Itinerary not found", "ITINERARY_NOT_FOUND")
                );
            }

            if (itinerary.getStatus() == ItineraryStatus.ARCHIVED) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Itinerary is already archived", "ALREADY_ARCHIVED")
                );
            }

            itinerary.setStatus(ItineraryStatus.ARCHIVED);
            itinerary.setIsActive(false);
            itinerary = itineraryRepository.save(itinerary);

            log.info("Itinerary archived: {}", itinerary.getCode());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Itinerary archived successfully", convertToDTO(itinerary))
            );

        } catch (Exception e) {
            log.error("Error archiving itinerary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to archive itinerary", "ARCHIVE_FAILED")
            );
        }
    }

    /**
     * Unarchive an itinerary (restore to DRAFT status)
     *
     * @param idObfuscated The obfuscated itinerary ID
     * @return ResponseEntity with ApiResponse containing the updated itinerary
     */
    @AuditLogAnnotation(action = "UNARCHIVE_ITINERARY", description = "Unarchiving itinerary", entityType = "Itinerary", entityIdParamName = "idObfuscated")
    public ResponseEntity<ApiResponse<?>> unarchiveItinerary(String idObfuscated) {
        log.info("Unarchiving itinerary: {}", idObfuscated);

        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            Itinerary itinerary = itineraryRepository.findById(id).orElse(null);

            if (itinerary == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Itinerary not found", "ITINERARY_NOT_FOUND")
                );
            }

            if (itinerary.getStatus() != ItineraryStatus.ARCHIVED) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Itinerary is not archived", "NOT_ARCHIVED")
                );
            }

            // Restore to appropriate status
            itinerary.setStatus(itinerary.canPublish() ? ItineraryStatus.COMPLETE : ItineraryStatus.DRAFT);
            itinerary.setIsActive(true);
            itinerary = itineraryRepository.save(itinerary);

            log.info("Itinerary unarchived: {} -> {}", itinerary.getCode(), itinerary.getStatus());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Itinerary unarchived successfully", convertToDTO(itinerary))
            );

        } catch (Exception e) {
            log.error("Error unarchiving itinerary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to unarchive itinerary", "UNARCHIVE_FAILED")
            );
        }
    }

    /**
     * Convert Itinerary entity to ItineraryDTO
     */
    private ItineraryDTO convertToDTO(Itinerary itinerary) {
        ItineraryDTO dto = new ItineraryDTO();
        dto.setId(idObfuscator.encodeId(itinerary.getId()));
        dto.setName(itinerary.getName());
        dto.setCode(itinerary.getCode());
        dto.setStatus(itinerary.getStatus());
        dto.setStatusDisplayName(itinerary.getStatus().getDisplayName());
        dto.setTripType(itinerary.getTripType());
        dto.setTripTypeDisplayName(itinerary.getTripType() != null ? itinerary.getTripType().getDisplayName() : null);
        dto.setTripTypeDescription(itinerary.getTripType() != null ? itinerary.getTripType().getDescription() : null);
        dto.setBudgetCategory(itinerary.getBudgetCategory());
        dto.setBudgetCategoryDisplayName(itinerary.getBudgetCategory() != null ? itinerary.getBudgetCategory().getDisplayName() : null);
        dto.setBudgetCategoryDescription(itinerary.getBudgetCategory() != null ? itinerary.getBudgetCategory().getDescription() : null);
        dto.setBudgetCategoryTier(itinerary.getBudgetCategory() != null ? itinerary.getBudgetCategory().getTier() : null);
        dto.setTotalDays(itinerary.getTotalDays());
        dto.setTotalNights(itinerary.getTotalNights());
        dto.setIsDayTrip(itinerary.getTotalDays() == 1 && itinerary.getTotalNights() == 0);
        dto.setCarCount(itinerary.getCarCount());
        dto.setDescription(itinerary.getDescription());
        dto.setHighlights(itinerary.getHighlights());
        dto.setStartLocation(itinerary.getStartLocation());
        dto.setEndLocation(itinerary.getEndLocation());
        dto.setIsActive(itinerary.getIsActive());
        dto.setTotalPaxCount(itinerary.getTotalPaxCount());
        dto.setTotalDaysCount(itinerary.getDays() != null ? itinerary.getDays().size() : 0);
        dto.setCreatedAt(itinerary.getCreatedAt());
        dto.setUpdatedAt(itinerary.getUpdatedAt());
        return dto;
    }
}
