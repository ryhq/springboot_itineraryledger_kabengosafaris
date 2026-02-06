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
 * Simplified workflow (14 states):
 * Core Journey: DRAFT → PENDING_APPROVAL → APPROVED → CONFIRMED →
 *               PENDING_PAYMENT → FULLY_PAID → IN_PROGRESS → COMPLETED → CLOSED
 *
 * Exception/Special: ON_HOLD, CANCELLED, REFUND_PENDING, REFUND_COMPLETE, DISPUTED
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
    // CORE JOURNEY TRANSITIONS
    // ========================

    /**
     * Submit safari for approval (DRAFT -> PENDING_APPROVAL)
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> submitForApproval(String idObfuscated, SafariStateTransitionDTO dto) {
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

            if (safari.getState() != SafariState.DRAFT) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400,
                                "Can only submit DRAFT safaris for approval. Current state: " + safari.getState().getDisplayName(),
                                "INVALID_STATE_TRANSITION")
                );
            }

            // Validation: must have basic requirements
            if (!canSubmitForApproval(safari)) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400,
                                "Safari must have itinerary, customer, dates, and at least one pax to submit for approval",
                                "VALIDATION_FAILED")
                );
            }

            SafariState previousState = safari.getState();
            safari.changeState(SafariState.PENDING_APPROVAL,
                    dto != null && dto.getReason() != null ? dto.getReason() : "Submitted for approval");
            Safari savedSafari = safariRepository.save(safari);

            logStateChange(savedSafari.getId(), "SUBMIT_FOR_APPROVAL",
                    "Safari submitted for approval",
                    previousState.name(), SafariState.PENDING_APPROVAL.name());

            log.info("Safari {} submitted for approval", savedSafari.getId());

            return ResponseEntity.ok().body(
                    ApiResponse.success(200, "Safari submitted for approval successfully", convertToDTO(savedSafari))
            );

        } catch (Exception e) {
            log.error("Error submitting safari for approval", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to submit safari for approval", "STATE_TRANSITION_FAILED")
            );
        }
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
    // PAYMENT TRANSITIONS (Simplified)
    // ========================

    /**
     * Request payment (CONFIRMED -> PENDING_PAYMENT)
     * Replaces separate requestDeposit method
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> requestPayment(String idObfuscated, SafariStateTransitionDTO dto) {
        return executeTransition(
                idObfuscated,
                SafariState.PENDING_PAYMENT,
                dto != null ? dto.getReason() : "Payment requested",
                Set.of(SafariState.CONFIRMED),
                "REQUEST_PAYMENT"
        );
    }

    /**
     * Record payment received (PENDING_PAYMENT -> FULLY_PAID if complete, or stays in PENDING_PAYMENT)
     * Replaces separate recordDeposit and recordFullPayment methods
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> recordPayment(String idObfuscated, SafariStateTransitionDTO dto) {
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

            if (safari.getState() != SafariState.PENDING_PAYMENT) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400,
                                "Can only record payment for safaris in PENDING_PAYMENT state. Current state: " + safari.getState().getDisplayName(),
                                "INVALID_STATE_TRANSITION")
                );
            }

            SafariState previousState = safari.getState();

            // Determine if payment is complete based on DTO or safari data
            boolean isFullyPaid = dto != null && dto.getIsFullPayment() != null ? dto.getIsFullPayment() : false;

            SafariState targetState = isFullyPaid ? SafariState.FULLY_PAID : SafariState.PENDING_PAYMENT;
            String reason = dto != null && dto.getReason() != null ? dto.getReason() :
                    (isFullyPaid ? "Full payment received" : "Partial payment received");

            safari.changeState(targetState, reason);
            Safari savedSafari = safariRepository.save(safari);

            logStateChange(savedSafari.getId(), "RECORD_PAYMENT", reason,
                    previousState.name(), targetState.name());

            log.info("Payment recorded for safari {}, new state: {}", savedSafari.getId(), targetState);

            return ResponseEntity.ok().body(
                    ApiResponse.success(200,
                            isFullyPaid ? "Full payment recorded successfully" : "Partial payment recorded successfully",
                            convertToDTO(savedSafari))
            );

        } catch (Exception e) {
            log.error("Error recording payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to record payment", "STATE_TRANSITION_FAILED")
            );
        }
    }

    // ========================
    // OPERATIONAL TRANSITIONS
    // ========================

    /**
     * Start safari (FULLY_PAID -> IN_PROGRESS)
     * Note: READY state removed - SafariPhase handles time urgency
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> startSafari(String idObfuscated, SafariStateTransitionDTO dto) {
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

            if (safari.getState() != SafariState.FULLY_PAID) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400,
                                "Can only start safaris in FULLY_PAID state. Current state: " + safari.getState().getDisplayName(),
                                "INVALID_STATE_TRANSITION")
                );
            }

            // Validation: start date should be today or in the past
            if (safari.getStartDate() != null && safari.getStartDate().isAfter(LocalDate.now())) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400,
                                "Cannot start safari before its start date: " + safari.getStartDate(),
                                "VALIDATION_FAILED")
                );
            }

            SafariState previousState = safari.getState();
            safari.changeState(SafariState.IN_PROGRESS,
                    dto != null && dto.getReason() != null ? dto.getReason() : "Safari started");
            Safari savedSafari = safariRepository.save(safari);

            logStateChange(savedSafari.getId(), "START", "Safari started",
                    previousState.name(), SafariState.IN_PROGRESS.name());

            log.info("Safari {} started", savedSafari.getId());

            return ResponseEntity.ok().body(
                    ApiResponse.success(200, "Safari started successfully", convertToDTO(savedSafari))
            );

        } catch (Exception e) {
            log.error("Error starting safari", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to start safari", "STATE_TRANSITION_FAILED")
            );
        }
    }

    /**
     * Complete safari (IN_PROGRESS -> COMPLETED)
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> completeSafari(String idObfuscated, SafariStateTransitionDTO dto) {
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

            if (safari.getState() != SafariState.IN_PROGRESS) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400,
                                "Can only complete safaris in IN_PROGRESS state. Current state: " + safari.getState().getDisplayName(),
                                "INVALID_STATE_TRANSITION")
                );
            }

            // Validation: end date should be today or in the past
            if (safari.getEndDate() != null && safari.getEndDate().isAfter(LocalDate.now())) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400,
                                "Cannot complete safari before its end date: " + safari.getEndDate(),
                                "VALIDATION_FAILED")
                );
            }

            SafariState previousState = safari.getState();
            safari.changeState(SafariState.COMPLETED,
                    dto != null && dto.getReason() != null ? dto.getReason() : "Safari completed successfully");
            Safari savedSafari = safariRepository.save(safari);

            logStateChange(savedSafari.getId(), "COMPLETE", "Safari completed",
                    previousState.name(), SafariState.COMPLETED.name());

            log.info("Safari {} completed", savedSafari.getId());

            return ResponseEntity.ok().body(
                    ApiResponse.success(200, "Safari completed successfully", convertToDTO(savedSafari))
            );

        } catch (Exception e) {
            log.error("Error completing safari", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to complete safari", "STATE_TRANSITION_FAILED")
            );
        }
    }

    /**
     * Close safari (COMPLETED -> CLOSED)
     * Note: PENDING_REVIEW state removed - reviews happen in COMPLETED state
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> closeSafari(String idObfuscated, SafariStateTransitionDTO dto) {
        return executeTransition(
                idObfuscated,
                SafariState.CLOSED,
                dto != null ? dto.getReason() : "Safari closed, all post-trip tasks completed",
                Set.of(SafariState.COMPLETED),
                "CLOSE"
        );
    }

    // ========================
    // HOLD/PAUSE TRANSITIONS (Simplified)
    // ========================

    /**
     * Put safari on hold (multiple states -> ON_HOLD)
     * Replaces separate holdSafari, markPendingDocuments, markPendingAvailability, postponeSafari methods
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> holdSafari(String idObfuscated, SafariStateTransitionDTO dto) {
        if (dto == null || dto.getHoldReason() == null) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Hold reason is required", "REASON_REQUIRED")
            );
        }
        if (dto.getReason() == null || dto.getReason().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Detailed reason is required for hold", "REASON_REQUIRED")
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

            Set<SafariState> allowedStates = Set.of(
                    SafariState.DRAFT, SafariState.PENDING_APPROVAL, SafariState.APPROVED,
                    SafariState.CONFIRMED, SafariState.PENDING_PAYMENT, SafariState.FULLY_PAID
            );

            if (!allowedStates.contains(safari.getState())) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400,
                                "Cannot put safari on hold from state: " + safari.getState().getDisplayName(),
                                "INVALID_STATE_TRANSITION")
                );
            }

            SafariState previousState = safari.getState();

            // Store hold reason and previous state for later resumption
            // Note: These fields should be added to Safari entity
            String fullReason = String.format("[%s] %s", dto.getHoldReason().getDisplayName(), dto.getReason());
            safari.changeState(SafariState.ON_HOLD, fullReason);

            Safari savedSafari = safariRepository.save(safari);

            logStateChange(savedSafari.getId(), "HOLD", fullReason,
                    previousState.name(), SafariState.ON_HOLD.name());

            log.info("Safari {} put on hold (reason: {})", savedSafari.getId(), dto.getHoldReason());

            return ResponseEntity.ok().body(
                    ApiResponse.success(200, "Safari put on hold successfully", convertToDTO(savedSafari))
            );

        } catch (Exception e) {
            log.error("Error putting safari on hold", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to put safari on hold", "STATE_TRANSITION_FAILED")
            );
        }
    }

    /**
     * Release hold (ON_HOLD -> previous state or specified state)
     * Replaces separate release methods
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> releaseHold(String idObfuscated, SafariStateTransitionDTO dto) {
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

            if (safari.getState() != SafariState.ON_HOLD) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400,
                                "Can only release safaris in ON_HOLD state. Current state: " + safari.getState().getDisplayName(),
                                "INVALID_STATE_TRANSITION")
                );
            }

            // Determine target state: specified in DTO or default to CONFIRMED
            SafariState targetState = dto != null && dto.getTargetState() != null
                    ? dto.getTargetState()
                    : SafariState.CONFIRMED;

            String reason = dto != null && dto.getReason() != null ? dto.getReason() : "Hold released";

            SafariState previousState = safari.getState();
            safari.changeState(targetState, reason);
            Safari savedSafari = safariRepository.save(safari);

            logStateChange(savedSafari.getId(), "RELEASE_HOLD", reason,
                    previousState.name(), targetState.name());

            log.info("Hold released for safari {}, returned to {}", savedSafari.getId(), targetState);

            return ResponseEntity.ok().body(
                    ApiResponse.success(200, "Hold released successfully", convertToDTO(savedSafari))
            );

        } catch (Exception e) {
            log.error("Error releasing hold", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to release hold", "STATE_TRANSITION_FAILED")
            );
        }
    }

    // ========================
    // CANCELLATION TRANSITIONS (Simplified)
    // ========================

    /**
     * Cancel safari (multiple states -> CANCELLED)
     * Replaces separate cancelSafari, cancelByClient, cancelByOperator, cancelForceMajeure methods
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> cancelSafari(String idObfuscated, SafariStateTransitionDTO dto) {
        if (dto == null || dto.getCancellationReason() == null) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Cancellation reason is required", "REASON_REQUIRED")
            );
        }
        if (dto.getReason() == null || dto.getReason().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Detailed reason is required for cancellation", "REASON_REQUIRED")
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
                        ApiResponse.error(400,
                                "Safari cannot be cancelled in state: " + safari.getState().getDisplayName(),
                                "NOT_CANCELLABLE")
                );
            }

            SafariState previousState = safari.getState();

            // Store cancellation reason
            String fullReason = String.format("[%s] %s", dto.getCancellationReason().getDisplayName(), dto.getReason());
            safari.changeState(SafariState.CANCELLED, fullReason);

            Safari savedSafari = safariRepository.save(safari);

            logStateChange(savedSafari.getId(), "CANCEL", fullReason,
                    previousState.name(), SafariState.CANCELLED.name());

            log.info("Safari {} cancelled (reason: {})", savedSafari.getId(), dto.getCancellationReason());

            return ResponseEntity.ok().body(
                    ApiResponse.success(200, "Safari cancelled successfully", convertToDTO(savedSafari))
            );

        } catch (Exception e) {
            log.error("Error cancelling safari", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to cancel safari", "STATE_TRANSITION_FAILED")
            );
        }
    }

    // ========================
    // REFUND TRANSITIONS (Simplified)
    // ========================

    /**
     * Initiate refund (CANCELLED -> REFUND_PENDING)
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> initiateRefund(String idObfuscated, SafariStateTransitionDTO dto) {
        return executeTransition(
                idObfuscated,
                SafariState.REFUND_PENDING,
                dto != null ? dto.getReason() : "Refund process initiated",
                Set.of(SafariState.CANCELLED),
                "INITIATE_REFUND"
        );
    }

    /**
     * Record refund (REFUND_PENDING -> REFUND_COMPLETE if full, or stays in REFUND_PENDING if partial)
     * Replaces separate recordPartialRefund and recordFullRefund methods
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> recordRefund(String idObfuscated, SafariStateTransitionDTO dto) {
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

            if (safari.getState() != SafariState.REFUND_PENDING) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400,
                                "Can only record refund for safaris in REFUND_PENDING state. Current state: " + safari.getState().getDisplayName(),
                                "INVALID_STATE_TRANSITION")
                );
            }

            SafariState previousState = safari.getState();

            // Determine if refund is complete based on DTO
            boolean isFinalRefund = dto != null && dto.getIsFinalRefund() != null ? dto.getIsFinalRefund() : true;

            SafariState targetState = isFinalRefund ? SafariState.REFUND_COMPLETE : SafariState.REFUND_PENDING;
            String reason = dto != null && dto.getReason() != null ? dto.getReason() :
                    (isFinalRefund ? "Full refund completed" : "Partial refund issued");

            safari.changeState(targetState, reason);
            Safari savedSafari = safariRepository.save(safari);

            logStateChange(savedSafari.getId(), "RECORD_REFUND", reason,
                    previousState.name(), targetState.name());

            log.info("Refund recorded for safari {}, new state: {}", savedSafari.getId(), targetState);

            return ResponseEntity.ok().body(
                    ApiResponse.success(200,
                            isFinalRefund ? "Full refund recorded successfully" : "Partial refund recorded successfully",
                            convertToDTO(savedSafari))
            );

        } catch (Exception e) {
            log.error("Error recording refund", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to record refund", "STATE_TRANSITION_FAILED")
            );
        }
    }

    // ========================
    // DISPUTE TRANSITIONS (Simplified)
    // ========================

    /**
     * Mark safari as disputed (post-trip/cancellation states -> DISPUTED)
     * Replaces separate markDisputed and investigateDispute methods
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
                Set.of(SafariState.COMPLETED, SafariState.CLOSED, SafariState.CANCELLED,
                        SafariState.REFUND_PENDING, SafariState.REFUND_COMPLETE),
                "MARK_DISPUTED"
        );
    }

    /**
     * Resolve dispute (DISPUTED -> resolution state)
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> resolveDispute(String idObfuscated, SafariStateTransitionDTO dto) {
        if (dto == null || dto.getReason() == null || dto.getReason().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Resolution reason is required", "REASON_REQUIRED")
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

            if (safari.getState() != SafariState.DISPUTED) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400,
                                "Can only resolve disputes for safaris in DISPUTED state. Current state: " + safari.getState().getDisplayName(),
                                "INVALID_STATE_TRANSITION")
                );
            }

            // Determine target state: specified in DTO or default to CLOSED
            SafariState targetState = dto.getTargetState() != null
                    ? dto.getTargetState()
                    : SafariState.CLOSED;

            SafariState previousState = safari.getState();
            safari.changeState(targetState, dto.getReason());
            Safari savedSafari = safariRepository.save(safari);

            logStateChange(savedSafari.getId(), "RESOLVE_DISPUTE", dto.getReason(),
                    previousState.name(), targetState.name());

            log.info("Dispute resolved for safari {}, new state: {}", savedSafari.getId(), targetState);

            return ResponseEntity.ok().body(
                    ApiResponse.success(200, "Dispute resolved successfully", convertToDTO(savedSafari))
            );

        } catch (Exception e) {
            log.error("Error resolving dispute", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to resolve dispute", "STATE_TRANSITION_FAILED")
            );
        }
    }

    // ========================
    // HELPER METHODS
    // ========================

    /**
     * Check if safari can be submitted for approval
     */
    private boolean canSubmitForApproval(Safari safari) {
        return safari.getItinerary() != null
                && safari.getStartDate() != null
                && safari.getEndDate() != null
                && safari.getTotalPaxCount() > 0
                && safari.getDays() != null && !safari.getDays().isEmpty();
    }

    /**
     * Generic state transition executor
     */
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
