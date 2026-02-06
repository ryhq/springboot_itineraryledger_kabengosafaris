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
import com.itineraryledger.kabengosafaris.Safari.Enums.SafariState;
import com.itineraryledger.kabengosafaris.Safari.Repository.SafariRepository;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * SafariDeleteService - Service for deleting safaris
 *
 * WORKFLOW ENFORCEMENT:
 * - Allows deletion of: DRAFT, PENDING_APPROVAL
 * - Blocks deletion of: All other states (APPROVED, CONFIRMED, PENDING_PAYMENT, FULLY_PAID,
 *   IN_PROGRESS, COMPLETED, CLOSED, ON_HOLD, CANCELLED, REFUND_PENDING, REFUND_COMPLETE, DISPUTED)
 *
 * Prevents accidental deletion of approved, confirmed, or operational safaris.
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
     * WORKFLOW ENFORCEMENT: Only DRAFT and PENDING_APPROVAL safaris can be deleted.
     * Safaris in other states will be skipped with detailed error messages.
     *
     * @param idObfuscatedList List of obfuscated safari IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    public ResponseEntity<ApiResponse<?>> deleteSafaris(List<String> idObfuscatedList) {
        log.info("Attempting to delete {} safaris", idObfuscatedList.size());

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
        int skippedCount = 0;
        List<String> skippedReasons = new ArrayList<>();

        for (Long id : ids) {
            try {
                Safari safari = safariRepository.findById(id).orElse(null);

                if (safari == null) {
                    log.warn("Safari not found: {}", id);
                    skippedCount++;
                    skippedReasons.add(String.format("Safari ID %d not found", id));
                    continue;
                }

                // WORKFLOW ENFORCEMENT: Only allow deletion of DRAFT and PENDING_APPROVAL safaris
                SafariState state = safari.getState();

                if (state == SafariState.APPROVED || state == SafariState.CONFIRMED ||
                    state == SafariState.PENDING_PAYMENT || state == SafariState.FULLY_PAID) {
                    log.warn("Cannot delete safari {} - state is {} (approved/confirmed safaris cannot be deleted)",
                             safari.getCode(), state.getDisplayName());
                    skippedCount++;
                    skippedReasons.add(String.format("Cannot delete safari %s - state is %s (cannot delete approved/confirmed bookings)",
                                                     safari.getCode(), state.getDisplayName()));
                    continue;
                }

                if (state == SafariState.IN_PROGRESS) {
                    log.warn("Cannot delete safari {} - safari is currently running",
                             safari.getCode());
                    skippedCount++;
                    skippedReasons.add(String.format("Cannot delete safari %s - safari is currently IN_PROGRESS",
                                                     safari.getCode()));
                    continue;
                }

                if (state == SafariState.COMPLETED || state == SafariState.CLOSED) {
                    log.warn("Cannot delete safari {} - safari has already ended ({})",
                             safari.getCode(), state.getDisplayName());
                    skippedCount++;
                    skippedReasons.add(String.format("Cannot delete safari %s - safari has ended (%s)",
                                                     safari.getCode(), state.getDisplayName()));
                    continue;
                }

                if (state == SafariState.CANCELLED || state == SafariState.REFUND_PENDING ||
                    state == SafariState.REFUND_COMPLETE) {
                    log.warn("Cannot delete safari {} - safari has been cancelled/refunded ({})",
                             safari.getCode(), state.getDisplayName());
                    skippedCount++;
                    skippedReasons.add(String.format("Cannot delete safari %s - safari has been cancelled/refunded (%s)",
                                                     safari.getCode(), state.getDisplayName()));
                    continue;
                }

                if (state == SafariState.DISPUTED) {
                    log.warn("Cannot delete safari {} - safari is under investigation",
                             safari.getCode());
                    skippedCount++;
                    skippedReasons.add(String.format("Cannot delete safari %s - safari is DISPUTED and under investigation",
                                                     safari.getCode()));
                    continue;
                }

                if (state == SafariState.ON_HOLD) {
                    log.warn("Cannot delete safari {} - safari is ON_HOLD",
                             safari.getCode());
                    skippedCount++;
                    skippedReasons.add(String.format("Cannot delete safari %s - safari is ON_HOLD (release from hold or cancel first)",
                                                     safari.getCode()));
                    continue;
                }

                // Only DRAFT and PENDING_APPROVAL reach here

                // Use AopContext to get proxy and trigger AOP aspect
                ((SafariDeleteService) AopContext.currentProxy()).deleteSafari(id);
                deletedCount++;
                log.info("Safari deleted successfully: {} ({})", safari.getCode(), id);

            } catch (Exception e) {
                log.error("Error deleting safari: {}", id, e);
                skippedCount++;
                skippedReasons.add(String.format("Error deleting safari ID %d: %s", id, e.getMessage()));
            }
        }

        // Build response message
        StringBuilder message = new StringBuilder();
        if (deletedCount > 0) {
            message.append(deletedCount)
                   .append(deletedCount > 1 ? " safaris deleted successfully" : " safari deleted successfully");
        }

        if (skippedCount > 0) {
            if (deletedCount > 0) {
                message.append(". ");
            }
            message.append(skippedCount)
                   .append(skippedCount > 1 ? " safaris skipped" : " safari skipped");
        }

        // Return appropriate response
        if (deletedCount == 0 && skippedCount > 0) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    400,
                    message.toString() + ": " + String.join("; ", skippedReasons),
                    "NO_SAFARIS_DELETED"
                )
            );
        }

        return ResponseEntity.ok().body(
            ApiResponse.success(
                200,
                message.toString(),
                skippedCount > 0 ? skippedReasons : null
            )
        );
    }

    /**
     * Delete a single safari by ID (internal method with audit logging)
     *
     * @param id Safari ID to delete
     */
    @AuditLogAnnotation(
        action = "DELETE_SAFARI",
        description = "Deleting safari",
        entityType = "Safari",
        entityIdParamName = "id"
    )
    public void deleteSafari(Long id) {
        safariRepository.deleteById(id);
    }
}
