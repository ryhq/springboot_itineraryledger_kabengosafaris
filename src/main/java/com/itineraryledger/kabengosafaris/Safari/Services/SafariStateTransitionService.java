package com.itineraryledger.kabengosafaris.Safari.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLog;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.DTOs.SafariDTO;
import com.itineraryledger.kabengosafaris.Safari.DTOs.SafariStateTransitionDTO;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.Enums.SafariState;
import com.itineraryledger.kabengosafaris.Safari.Repository.SafariRepository;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.User.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

/**
 * SafariStateTransitionService - Service for managing Safari state transitions
 *
 * This service handles all state transitions with validation rules ensuring
 * that only valid transitions are allowed and proper audit logging is performed.
 */
@Service
@Slf4j
public class SafariStateTransitionService {

    private final SafariRepository safariRepository;
    private final IdObfuscator idObfuscator;
    private final AuditLogService auditLogService;

    @Autowired
    public SafariStateTransitionService(
            SafariRepository safariRepository,
            IdObfuscator idObfuscator,
            AuditLogService auditLogService
    ) {
        this.safariRepository = safariRepository;
        this.idObfuscator = idObfuscator;
        this.auditLogService = auditLogService;
    }

    // ========================
    // BOOKING STATE TRANSITIONS
    // ========================

    /**
     * Submit safari for approval (DRAFT -> PENDING_APPROVAL)
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> submitForApproval(String idObfuscated, SafariStateTransitionDTO dto) {
        return executeTransition(
                idObfuscated,
                SafariState.PENDING_APPROVAL,
                dto != null ? dto.getReason() : "Submitted for approval",
                Set.of(SafariState.DRAFT),
                "SUBMIT_FOR_APPROVAL"
        );
    }

    /**
     * Approve safari booking (PENDING_APPROVAL -> APPROVED)
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> approveSafari(String idObfuscated, SafariStateTransitionDTO dto) {
        return executeTransition(
                idObfuscated,
                SafariState.APPROVED,
                dto != null ? dto.getReason() : "Safari approved",
                Set.of(SafariState.PENDING_APPROVAL),
                "APPROVE"
        );
    }

    /**
     * Reject safari booking (PENDING_APPROVAL -> DRAFT)
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> rejectSafari(String idObfuscated, SafariStateTransitionDTO dto) {
        if (dto == null || dto.getReason() == null || dto.getReason().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Reason is required for rejection", "REASON_REQUIRED")
            );
        }
        return executeTransition(
                idObfuscated,
                SafariState.DRAFT,
                dto.getReason(),
                Set.of(SafariState.PENDING_APPROVAL),
                "REJECT"
        );
    }

    /**
     * Confirm safari with client and suppliers (APPROVED -> CONFIRMED)
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> confirmSafari(String idObfuscated, SafariStateTransitionDTO dto) {
        return executeTransition(
                idObfuscated,
                SafariState.CONFIRMED,
                dto != null ? dto.getReason() : "Safari confirmed with client and suppliers",
                Set.of(SafariState.APPROVED),
                "CONFIRM"
        );
    }

    // ========================
    // PAYMENT STATE TRANSITIONS
    // ========================

    /**
     * Request deposit payment (CONFIRMED -> PENDING_DEPOSIT)
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> requestDeposit(String idObfuscated, SafariStateTransitionDTO dto) {
        return executeTransition(
                idObfuscated,
                SafariState.PENDING_DEPOSIT,
                dto != null ? dto.getReason() : "Deposit payment requested",
                Set.of(SafariState.CONFIRMED),
                "REQUEST_DEPOSIT"
        );
    }

    /**
     * Record deposit payment received (PENDING_DEPOSIT -> DEPOSIT_PAID)
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> recordDeposit(String idObfuscated, SafariStateTransitionDTO dto) {
        return executeTransition(
                idObfuscated,
                SafariState.DEPOSIT_PAID,
                dto != null ? dto.getReason() : "Deposit payment received",
                Set.of(SafariState.PENDING_DEPOSIT),
                "RECORD_DEPOSIT"
        );
    }

    /**
     * Record full payment received (DEPOSIT_PAID/CONFIRMED -> FULLY_PAID)
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> recordFullPayment(String idObfuscated, SafariStateTransitionDTO dto) {
        return executeTransition(
                idObfuscated,
                SafariState.FULLY_PAID,
                dto != null ? dto.getReason() : "Full payment received",
                Set.of(SafariState.DEPOSIT_PAID, SafariState.CONFIRMED, SafariState.PENDING_DEPOSIT),
                "RECORD_FULL_PAYMENT"
        );
    }

    // ========================
    // OPERATIONAL STATE TRANSITIONS
    // ========================

    /**
     * Mark safari as ready to commence (FULLY_PAID -> READY)
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> markReady(String idObfuscated, SafariStateTransitionDTO dto) {
        return executeTransition(
                idObfuscated,
                SafariState.READY,
                dto != null ? dto.getReason() : "Safari preparations complete, ready to commence",
                Set.of(SafariState.FULLY_PAID),
                "MARK_READY"
        );
    }

    /**
     * Start safari (READY -> IN_PROGRESS)
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> startSafari(String idObfuscated, SafariStateTransitionDTO dto) {
        return executeTransition(
                idObfuscated,
                SafariState.IN_PROGRESS,
                dto != null ? dto.getReason() : "Safari started",
                Set.of(SafariState.READY),
                "START"
        );
    }

    /**
     * Complete safari (IN_PROGRESS -> COMPLETED)
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> completeSafari(String idObfuscated, SafariStateTransitionDTO dto) {
        return executeTransition(
                idObfuscated,
                SafariState.COMPLETED,
                dto != null ? dto.getReason() : "Safari completed successfully",
                Set.of(SafariState.IN_PROGRESS),
                "COMPLETE"
        );
    }

    // ========================
    // POST-SAFARI STATE TRANSITIONS
    // ========================

    /**
     * Request review (COMPLETED -> PENDING_REVIEW)
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> requestReview(String idObfuscated, SafariStateTransitionDTO dto) {
        return executeTransition(
                idObfuscated,
                SafariState.PENDING_REVIEW,
                dto != null ? dto.getReason() : "Post-trip review requested",
                Set.of(SafariState.COMPLETED),
                "REQUEST_REVIEW"
        );
    }

    /**
     * Close safari (COMPLETED/PENDING_REVIEW -> CLOSED)
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> closeSafari(String idObfuscated, SafariStateTransitionDTO dto) {
        return executeTransition(
                idObfuscated,
                SafariState.CLOSED,
                dto != null ? dto.getReason() : "Safari closed, all post-trip tasks completed",
                Set.of(SafariState.COMPLETED, SafariState.PENDING_REVIEW),
                "CLOSE"
        );
    }

    // ========================
    // HOLD/PAUSE STATE TRANSITIONS
    // ========================

    /**
     * Put safari on hold (multiple states -> ON_HOLD)
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> holdSafari(String idObfuscated, SafariStateTransitionDTO dto) {
        if (dto == null || dto.getReason() == null || dto.getReason().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Reason is required for hold", "REASON_REQUIRED")
            );
        }
        return executeTransitionWithPreviousState(
                idObfuscated,
                SafariState.ON_HOLD,
                dto.getReason(),
                Set.of(SafariState.DRAFT, SafariState.PENDING_APPROVAL, SafariState.APPROVED,
                        SafariState.CONFIRMED, SafariState.PENDING_DEPOSIT, SafariState.DEPOSIT_PAID,
                        SafariState.FULLY_PAID, SafariState.READY),
                "HOLD"
        );
    }

    /**
     * Release hold (ON_HOLD -> previous state or CONFIRMED)
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> releaseHold(String idObfuscated, SafariStateTransitionDTO dto) {
        // For simplicity, returning to CONFIRMED state
        // In a full implementation, you might track the previous state
        SafariState targetState = dto != null && dto.getTargetState() != null
                ? dto.getTargetState()
                : SafariState.CONFIRMED;

        return executeTransition(
                idObfuscated,
                targetState,
                dto != null ? dto.getReason() : "Hold released",
                Set.of(SafariState.ON_HOLD),
                "RELEASE_HOLD"
        );
    }

    /**
     * Mark pending documents (multiple states -> PENDING_DOCUMENTS)
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> markPendingDocuments(String idObfuscated, SafariStateTransitionDTO dto) {
        if (dto == null || dto.getReason() == null || dto.getReason().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Reason is required (specify which documents are pending)", "REASON_REQUIRED")
            );
        }
        return executeTransitionWithPreviousState(
                idObfuscated,
                SafariState.PENDING_DOCUMENTS,
                dto.getReason(),
                Set.of(SafariState.DRAFT, SafariState.PENDING_APPROVAL, SafariState.APPROVED,
                        SafariState.CONFIRMED, SafariState.PENDING_DEPOSIT, SafariState.DEPOSIT_PAID),
                "MARK_PENDING_DOCUMENTS"
        );
    }

    /**
     * Mark documents received (PENDING_DOCUMENTS -> previous state or CONFIRMED)
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> markDocumentsReceived(String idObfuscated, SafariStateTransitionDTO dto) {
        SafariState targetState = dto != null && dto.getTargetState() != null
                ? dto.getTargetState()
                : SafariState.CONFIRMED;

        return executeTransition(
                idObfuscated,
                targetState,
                dto != null ? dto.getReason() : "Required documents received",
                Set.of(SafariState.PENDING_DOCUMENTS),
                "MARK_DOCUMENTS_RECEIVED"
        );
    }

    /**
     * Mark pending availability (multiple states -> PENDING_AVAILABILITY)
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> markPendingAvailability(String idObfuscated, SafariStateTransitionDTO dto) {
        if (dto == null || dto.getReason() == null || dto.getReason().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Reason is required (specify what availability is pending)", "REASON_REQUIRED")
            );
        }
        return executeTransitionWithPreviousState(
                idObfuscated,
                SafariState.PENDING_AVAILABILITY,
                dto.getReason(),
                Set.of(SafariState.APPROVED, SafariState.CONFIRMED),
                "MARK_PENDING_AVAILABILITY"
        );
    }

    /**
     * Mark availability confirmed (PENDING_AVAILABILITY -> previous state or CONFIRMED)
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> markAvailabilityConfirmed(String idObfuscated, SafariStateTransitionDTO dto) {
        SafariState targetState = dto != null && dto.getTargetState() != null
                ? dto.getTargetState()
                : SafariState.CONFIRMED;

        return executeTransition(
                idObfuscated,
                targetState,
                dto != null ? dto.getReason() : "Availability confirmed",
                Set.of(SafariState.PENDING_AVAILABILITY),
                "MARK_AVAILABILITY_CONFIRMED"
        );
    }

    // ========================
    // RESCHEDULE STATE TRANSITIONS
    // ========================

    /**
     * Postpone safari (multiple states -> POSTPONED)
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> postponeSafari(String idObfuscated, SafariStateTransitionDTO dto) {
        if (dto == null || dto.getReason() == null || dto.getReason().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Reason is required for postponement", "REASON_REQUIRED")
            );
        }
        return executeTransition(
                idObfuscated,
                SafariState.POSTPONED,
                dto.getReason(),
                Set.of(SafariState.DRAFT, SafariState.PENDING_APPROVAL, SafariState.APPROVED,
                        SafariState.CONFIRMED, SafariState.PENDING_DEPOSIT, SafariState.DEPOSIT_PAID,
                        SafariState.FULLY_PAID, SafariState.READY, SafariState.ON_HOLD),
                "POSTPONE"
        );
    }

    /**
     * Initiate reschedule (multiple states -> RESCHEDULING)
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> initiateReschedule(String idObfuscated, SafariStateTransitionDTO dto) {
        if (dto == null || dto.getReason() == null || dto.getReason().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Reason is required for rescheduling", "REASON_REQUIRED")
            );
        }
        return executeTransition(
                idObfuscated,
                SafariState.RESCHEDULING,
                dto.getReason(),
                Set.of(SafariState.CONFIRMED, SafariState.PENDING_DEPOSIT, SafariState.DEPOSIT_PAID,
                        SafariState.FULLY_PAID, SafariState.READY, SafariState.POSTPONED),
                "INITIATE_RESCHEDULE"
        );
    }

    /**
     * Complete reschedule with new dates (RESCHEDULING -> CONFIRMED)
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> completeReschedule(String idObfuscated, SafariStateTransitionDTO dto) {
        if (dto == null || dto.getNewStartDate() == null) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "New start date is required to complete reschedule", "NEW_DATE_REQUIRED")
            );
        }

        try {
            Long id = decodeId(idObfuscated);
            if (id == null) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid safari ID", "INVALID_SAFARI_ID")
                );
            }

            Safari safari = safariRepository.findById(id).orElse(null);
            if (safari == null) {
                return ResponseEntity.status(404).body(
                        ApiResponse.error(404, "Safari not found", "SAFARI_NOT_FOUND")
                );
            }

            if (safari.getState() != SafariState.RESCHEDULING) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Safari must be in RESCHEDULING state to complete reschedule", "INVALID_STATE")
                );
            }

            // Update dates
            LocalDate newStartDate = dto.getNewStartDate();
            LocalDate newEndDate = newStartDate.plusDays(safari.getTotalDays() - 1);

            safari.setStartDate(newStartDate);
            safari.setEndDate(newEndDate);
            safari.changeState(SafariState.CONFIRMED, dto.getReason() != null ? dto.getReason() : "Reschedule completed with new dates");

            Safari savedSafari = safariRepository.save(safari);

            // Audit log
            logStateChange(savedSafari.getId(), "COMPLETE_RESCHEDULE",
                    "Safari rescheduled to new dates: " + newStartDate + " - " + newEndDate,
                    SafariState.RESCHEDULING.name(), SafariState.CONFIRMED.name());

            log.info("Safari {} rescheduled to {} - {}", savedSafari.getId(), newStartDate, newEndDate);

            return ResponseEntity.ok().body(
                    ApiResponse.success(200, "Safari rescheduled successfully", convertToDTO(savedSafari))
            );

        } catch (Exception e) {
            log.error("Error completing reschedule", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to complete reschedule", "RESCHEDULE_FAILED")
            );
        }
    }

    // ========================
    // CANCELLATION STATE TRANSITIONS
    // ========================

    /**
     * Request cancellation (multiple states -> CANCELLATION_REQUESTED)
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> requestCancellation(String idObfuscated, SafariStateTransitionDTO dto) {
        if (dto == null || dto.getReason() == null || dto.getReason().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Reason is required for cancellation request", "REASON_REQUIRED")
            );
        }

        try {
            Long id = decodeId(idObfuscated);
            if (id == null) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid safari ID", "INVALID_SAFARI_ID")
                );
            }

            Safari safari = safariRepository.findById(id).orElse(null);
            if (safari == null) {
                return ResponseEntity.status(404).body(
                        ApiResponse.error(404, "Safari not found", "SAFARI_NOT_FOUND")
                );
            }

            if (!safari.getState().isCancellable()) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Safari cannot be cancelled in state: " + safari.getState().getDisplayName(), "NOT_CANCELLABLE")
                );
            }

            SafariState previousState = safari.getState();
            safari.changeState(SafariState.CANCELLATION_REQUESTED, dto.getReason());
            Safari savedSafari = safariRepository.save(safari);

            logStateChange(savedSafari.getId(), "REQUEST_CANCELLATION", dto.getReason(),
                    previousState.name(), SafariState.CANCELLATION_REQUESTED.name());
            log.info("Cancellation requested for safari {}", savedSafari.getId());

            return ResponseEntity.ok().body(
                    ApiResponse.success(200, "Cancellation requested successfully", convertToDTO(savedSafari))
            );

        } catch (Exception e) {
            log.error("Error requesting cancellation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to request cancellation", "CANCELLATION_REQUEST_FAILED")
            );
        }
    }

    /**
     * Cancel safari (generic cancellation)
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> cancelSafari(String idObfuscated, SafariStateTransitionDTO dto) {
        if (dto == null || dto.getReason() == null || dto.getReason().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Reason is required for cancellation", "REASON_REQUIRED")
            );
        }
        return executeCancellation(idObfuscated, SafariState.CANCELLED, dto.getReason(), "CANCEL");
    }

    /**
     * Cancel safari by client
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> cancelByClient(String idObfuscated, SafariStateTransitionDTO dto) {
        if (dto == null || dto.getReason() == null || dto.getReason().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Reason is required for cancellation", "REASON_REQUIRED")
            );
        }
        return executeCancellation(idObfuscated, SafariState.CANCELLED_BY_CLIENT, dto.getReason(), "CANCEL_BY_CLIENT");
    }

    /**
     * Cancel safari by operator
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> cancelByOperator(String idObfuscated, SafariStateTransitionDTO dto) {
        if (dto == null || dto.getReason() == null || dto.getReason().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Reason is required for cancellation", "REASON_REQUIRED")
            );
        }
        return executeCancellation(idObfuscated, SafariState.CANCELLED_BY_OPERATOR, dto.getReason(), "CANCEL_BY_OPERATOR");
    }

    /**
     * Cancel safari due to force majeure
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> cancelForceMajeure(String idObfuscated, SafariStateTransitionDTO dto) {
        if (dto == null || dto.getReason() == null || dto.getReason().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Reason is required for force majeure cancellation", "REASON_REQUIRED")
            );
        }
        return executeCancellation(idObfuscated, SafariState.CANCELLED_FORCE_MAJEURE, dto.getReason(), "CANCEL_FORCE_MAJEURE");
    }

    // ========================
    // REFUND STATE TRANSITIONS
    // ========================

    /**
     * Initiate refund (CANCELLED_* -> REFUND_PENDING)
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> initiateRefund(String idObfuscated, SafariStateTransitionDTO dto) {
        return executeTransition(
                idObfuscated,
                SafariState.REFUND_PENDING,
                dto != null ? dto.getReason() : "Refund process initiated",
                Set.of(SafariState.CANCELLED, SafariState.CANCELLED_BY_CLIENT,
                        SafariState.CANCELLED_BY_OPERATOR, SafariState.CANCELLED_FORCE_MAJEURE,
                        SafariState.CANCELLATION_REQUESTED),
                "INITIATE_REFUND"
        );
    }

    /**
     * Record partial refund (REFUND_PENDING -> REFUND_PARTIAL)
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> recordPartialRefund(String idObfuscated, SafariStateTransitionDTO dto) {
        return executeTransition(
                idObfuscated,
                SafariState.REFUND_PARTIAL,
                dto != null ? dto.getReason() : "Partial refund issued",
                Set.of(SafariState.REFUND_PENDING),
                "RECORD_PARTIAL_REFUND"
        );
    }

    /**
     * Record full refund (REFUND_PENDING/REFUND_PARTIAL -> REFUND_COMPLETE)
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> recordFullRefund(String idObfuscated, SafariStateTransitionDTO dto) {
        return executeTransition(
                idObfuscated,
                SafariState.REFUND_COMPLETE,
                dto != null ? dto.getReason() : "Full refund completed",
                Set.of(SafariState.REFUND_PENDING, SafariState.REFUND_PARTIAL),
                "RECORD_FULL_REFUND"
        );
    }

    // ========================
    // DISPUTE STATE TRANSITIONS
    // ========================

    /**
     * Mark safari as disputed (multiple states -> DISPUTED)
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> markDisputed(String idObfuscated, SafariStateTransitionDTO dto) {
        if (dto == null || dto.getReason() == null || dto.getReason().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Reason is required for dispute", "REASON_REQUIRED")
            );
        }
        return executeTransition(
                idObfuscated,
                SafariState.DISPUTED,
                dto.getReason(),
                Set.of(SafariState.COMPLETED, SafariState.PENDING_REVIEW, SafariState.CLOSED,
                        SafariState.CANCELLED, SafariState.CANCELLED_BY_CLIENT,
                        SafariState.CANCELLED_BY_OPERATOR, SafariState.CANCELLED_FORCE_MAJEURE,
                        SafariState.REFUND_PENDING, SafariState.REFUND_PARTIAL, SafariState.REFUND_COMPLETE),
                "MARK_DISPUTED"
        );
    }

    /**
     * Start investigation (DISPUTED -> UNDER_INVESTIGATION)
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> investigateDispute(String idObfuscated, SafariStateTransitionDTO dto) {
        return executeTransition(
                idObfuscated,
                SafariState.UNDER_INVESTIGATION,
                dto != null ? dto.getReason() : "Dispute under investigation",
                Set.of(SafariState.DISPUTED),
                "INVESTIGATE_DISPUTE"
        );
    }

    /**
     * Resolve dispute (DISPUTED/UNDER_INVESTIGATION -> resolution state)
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> resolveDispute(String idObfuscated, SafariStateTransitionDTO dto) {
        if (dto == null || dto.getReason() == null || dto.getReason().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Resolution reason is required", "REASON_REQUIRED")
            );
        }

        SafariState targetState = dto.getTargetState() != null
                ? dto.getTargetState()
                : SafariState.CLOSED;

        return executeTransition(
                idObfuscated,
                targetState,
                dto.getReason(),
                Set.of(SafariState.DISPUTED, SafariState.UNDER_INVESTIGATION),
                "RESOLVE_DISPUTE"
        );
    }

    // ========================
    // HELPER METHODS
    // ========================

    private ResponseEntity<ApiResponse<?>> executeTransition(
            String idObfuscated,
            SafariState targetState,
            String reason,
            Set<SafariState> allowedFromStates,
            String actionName
    ) {
        try {
            Long id = decodeId(idObfuscated);
            if (id == null) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid safari ID", "INVALID_SAFARI_ID")
                );
            }

            Safari safari = safariRepository.findById(id).orElse(null);
            if (safari == null) {
                return ResponseEntity.status(404).body(
                        ApiResponse.error(404, "Safari not found", "SAFARI_NOT_FOUND")
                );
            }

            SafariState currentState = safari.getState();
            if (!allowedFromStates.contains(currentState)) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400,
                                "Cannot transition from " + currentState.getDisplayName() + " to " + targetState.getDisplayName(),
                                "INVALID_STATE_TRANSITION")
                );
            }

            safari.changeState(targetState, reason);
            Safari savedSafari = safariRepository.save(safari);

            logStateChange(savedSafari.getId(), actionName,
                    "State changed from " + currentState + " to " + targetState + ": " + reason,
                    currentState.name(), targetState.name());

            log.info("Safari {} transitioned from {} to {}", savedSafari.getId(), currentState, targetState);

            return ResponseEntity.ok().body(
                    ApiResponse.success(200, "Safari state changed to " + targetState.getDisplayName(), convertToDTO(savedSafari))
            );

        } catch (Exception e) {
            log.error("Error executing state transition", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to change safari state", "STATE_TRANSITION_FAILED")
            );
        }
    }

    private ResponseEntity<ApiResponse<?>> executeTransitionWithPreviousState(
            String idObfuscated,
            SafariState targetState,
            String reason,
            Set<SafariState> allowedFromStates,
            String actionName
    ) {
        // For states that need to track previous state for returning
        // This is a simplified implementation - full implementation would store previous state
        return executeTransition(idObfuscated, targetState, reason, allowedFromStates, actionName);
    }

    private ResponseEntity<ApiResponse<?>> executeCancellation(
            String idObfuscated,
            SafariState cancellationState,
            String reason,
            String actionName
    ) {
        try {
            Long id = decodeId(idObfuscated);
            if (id == null) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid safari ID", "INVALID_SAFARI_ID")
                );
            }

            Safari safari = safariRepository.findById(id).orElse(null);
            if (safari == null) {
                return ResponseEntity.status(404).body(
                        ApiResponse.error(404, "Safari not found", "SAFARI_NOT_FOUND")
                );
            }

            if (!safari.getState().isCancellable() && safari.getState() != SafariState.CANCELLATION_REQUESTED) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400,
                                "Safari cannot be cancelled in state: " + safari.getState().getDisplayName(),
                                "NOT_CANCELLABLE")
                );
            }

            SafariState previousState = safari.getState();
            safari.changeState(cancellationState, reason);
            Safari savedSafari = safariRepository.save(safari);

            logStateChange(savedSafari.getId(), actionName,
                    "Safari cancelled from " + previousState + ": " + reason,
                    previousState.name(), cancellationState.name());

            log.info("Safari {} cancelled ({})", savedSafari.getId(), cancellationState);

            return ResponseEntity.ok().body(
                    ApiResponse.success(200, "Safari cancelled successfully", convertToDTO(savedSafari))
            );

        } catch (Exception e) {
            log.error("Error cancelling safari", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to cancel safari", "CANCELLATION_FAILED")
            );
        }
    }

    /**
     * Log state change to audit log
     */
    private void logStateChange(Long safariId, String action, String description, String oldState, String newState) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Long userId = null;
            String username = "SYSTEM";

            if (auth != null && auth.getPrincipal() instanceof User) {
                User user = (User) auth.getPrincipal();
                userId = user.getId();
                username = user.getUsername();
            }

            AuditLog auditLog = AuditLog.builder()
                    .userId(userId != null ? userId : 0L)
                    .username(username)
                    .action("SAFARI_STATE_" + action)
                    .entityType("Safari")
                    .entityId(safariId)
                    .description(description)
                    .oldValues("{\"state\": \"" + oldState + "\"}")
                    .newValues("{\"state\": \"" + newState + "\"}")
                    .status("SUCCESS")
                    .build();

            auditLogService.logAction(auditLog);
        } catch (Exception e) {
            log.warn("Failed to log state change audit: {}", e.getMessage());
        }
    }

    private Long decodeId(String idObfuscated) {
        try {
            return idObfuscator.decodeId(idObfuscated);
        } catch (Exception e) {
            log.warn("Failed to decode safari ID: {}", idObfuscated, e);
            return null;
        }
    }

    private SafariDTO convertToDTO(Safari safari) {
        SafariDTO dto = new SafariDTO();
        dto.setId(idObfuscator.encodeId(safari.getId()));
        dto.setName(safari.getName());
        dto.setCode(safari.getCode());
        dto.setSlug(safari.getSlug());

        if (safari.getItinerary() != null) {
            dto.setItineraryId(idObfuscator.encodeId(safari.getItinerary().getId()));
            dto.setItineraryName(safari.getItinerary().getName());
            dto.setItineraryCode(safari.getItinerary().getCode());
        }

        // State information (booking/operational)
        dto.setState(safari.getState());
        dto.setStateDisplayName(safari.getState().getDisplayName());
        dto.setStateDescription(safari.getState().getDescription());
        dto.setStateReason(safari.getStateReason());
        dto.setStateChangedAt(safari.getStateChangedAt());

        // Phase information (time-based)
        var phase = safari.getCurrentPhase();
        dto.setPhase(phase);
        dto.setPhaseDisplayName(phase.getDisplayName());
        dto.setPhaseDescription(phase.getDescription());
        dto.setPhaseUrgencyLevel(phase.getUrgencyLevel());
        dto.setPhaseColorCode(phase.getColorCode());

        dto.setStartDate(safari.getStartDate());
        dto.setEndDate(safari.getEndDate());

        dto.setTotalDays(safari.getTotalDays());
        dto.setTotalNights(safari.getTotalNights());
        dto.setCarCount(safari.getCarCount());

        dto.setDescription(safari.getDescription());
        dto.setHighlights(safari.getHighlights());
        dto.setStartLocation(safari.getStartLocation());
        dto.setEndLocation(safari.getEndLocation());

        dto.setSpecialRequests(safari.getSpecialRequests());
        dto.setDietaryRequirements(safari.getDietaryRequirements());
        dto.setEmergencyContact(safari.getEmergencyContact());

        dto.setIsActive(safari.getIsActive());
        dto.setIsEditable(safari.isEditable());
        dto.setIsCancellable(safari.isCancellable());
        dto.setHasStarted(safari.hasStarted());
        dto.setHasEnded(safari.hasEnded());
        dto.setIsInProgress(safari.isInProgress());
        dto.setIsUrgentPhase(safari.isUrgentPhase());

        // Time calculations
        dto.setDaysUntilStart(safari.getDaysUntilStart());
        dto.setDaysSinceEnd(safari.getDaysSinceEnd());
        dto.setCurrentDayNumber(safari.getCurrentDayNumber());

        dto.setTotalPaxCount(safari.getTotalPaxCount());
        dto.setTotalDaysCount(safari.getDays() != null ? safari.getDays().size() : 0);

        dto.setCreatedAt(safari.getCreatedAt());
        dto.setUpdatedAt(safari.getUpdatedAt());

        return dto;
    }
}
