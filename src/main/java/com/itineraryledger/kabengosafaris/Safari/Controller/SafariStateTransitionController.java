package com.itineraryledger.kabengosafaris.Safari.Controller;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.DTOs.SafariStateTransitionDTO;
import com.itineraryledger.kabengosafaris.Safari.Services.SafariStateTransitionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * SafariStateTransitionController - REST API endpoints for Safari state transitions
 *
 * Implements the simplified 14-state Safari workflow:
 *
 * Core Journey (9 states):
 *   DRAFT → PENDING_APPROVAL → APPROVED → CONFIRMED →
 *   PENDING_PAYMENT → FULLY_PAID → IN_PROGRESS → COMPLETED → CLOSED
 *
 * Exception/Special (5 states):
 *   ON_HOLD, CANCELLED, REFUND_PENDING, REFUND_COMPLETE, DISPUTED
 *
 * Each endpoint is protected by a specific permission that controls who can
 * perform that state transition. This enables fine-grained access control
 * over the safari booking lifecycle.
 *
 * Base URL: /api/safaris/{id}/state
 */
@RestController
@RequestMapping("/api/safaris/{id}/state")
public class SafariStateTransitionController {

    private final SafariStateTransitionService stateTransitionService;

    @Autowired
    public SafariStateTransitionController(SafariStateTransitionService stateTransitionService) {
        this.stateTransitionService = stateTransitionService;
    }

    // ========================
    // CORE JOURNEY - BOOKING PHASE
    // ========================

    /**
     * Submit safari for approval (DRAFT -> PENDING_APPROVAL)
     *
     * POST /api/safaris/{id}/state/submit-for-approval
     *
     * Validates:
     * - Safari has itinerary assigned
     * - Safari has customer assigned
     * - Safari has dates set
     * - Safari has at least 1 pax
     */
    @PostMapping("/submit-for-approval")
    @PreAuthorize("hasAuthority('PERM_SUBMIT_SAFARI_FOR_APPROVAL')")
    public ResponseEntity<ApiResponse<?>> submitForApproval(
            @PathVariable String id,
            @RequestBody(required = false) SafariStateTransitionDTO dto
    ) {
        return stateTransitionService.submitForApproval(id, dto);
    }

    /**
     * Approve safari booking (PENDING_APPROVAL -> APPROVED)
     *
     * POST /api/safaris/{id}/state/approve
     */
    @PostMapping("/approve")
    @PreAuthorize("hasAuthority('PERM_APPROVE_SAFARI')")
    public ResponseEntity<ApiResponse<?>> approveSafari(
            @PathVariable String id,
            @RequestBody(required = false) SafariStateTransitionDTO dto
    ) {
        return stateTransitionService.approveSafari(id, dto);
    }

    /**
     * Reject safari booking (PENDING_APPROVAL -> DRAFT)
     *
     * POST /api/safaris/{id}/state/reject
     *
     * Request body:
     * - reason: (required) Detailed explanation for rejection
     */
    @PostMapping("/reject")
    @PreAuthorize("hasAuthority('PERM_REJECT_SAFARI')")
    public ResponseEntity<ApiResponse<?>> rejectSafari(
            @PathVariable String id,
            @Valid @RequestBody SafariStateTransitionDTO dto
    ) {
        return stateTransitionService.rejectSafari(id, dto);
    }

    /**
     * Confirm safari with client and suppliers (APPROVED -> CONFIRMED)
     *
     * POST /api/safaris/{id}/state/confirm
     */
    @PostMapping("/confirm")
    @PreAuthorize("hasAuthority('PERM_CONFIRM_SAFARI')")
    public ResponseEntity<ApiResponse<?>> confirmSafari(
            @PathVariable String id,
            @RequestBody(required = false) SafariStateTransitionDTO dto
    ) {
        return stateTransitionService.confirmSafari(id, dto);
    }

    // ========================
    // CORE JOURNEY - PAYMENT PHASE
    // ========================

    /**
     * Record safari payment (deposit or full payment)
     *
     * POST /api/safaris/{id}/state/record-payment
     *
     * State transitions:
     * - PENDING_PAYMENT -> PENDING_PAYMENT (partial payment/deposit)
     * - PENDING_PAYMENT -> FULLY_PAID (full payment complete)
     *
     * Request body:
     * - isFullPayment: (required) true for full payment, false for partial/deposit
     * - notes: (optional) Payment reference, amount, method details
     */
    @PostMapping("/record-payment")
    @PreAuthorize("hasAuthority('PERM_RECORD_SAFARI_PAYMENT')")
    public ResponseEntity<ApiResponse<?>> recordPayment(
            @PathVariable String id,
            @Valid @RequestBody SafariStateTransitionDTO dto
    ) {
        return stateTransitionService.recordPayment(id, dto);
    }

    // ========================
    // CORE JOURNEY - OPERATIONAL PHASE
    // ========================

    /**
     * Start safari (FULLY_PAID -> IN_PROGRESS)
     *
     * POST /api/safaris/{id}/state/start
     */
    @PostMapping("/start")
    @PreAuthorize("hasAuthority('PERM_START_SAFARI')")
    public ResponseEntity<ApiResponse<?>> startSafari(
            @PathVariable String id,
            @RequestBody(required = false) SafariStateTransitionDTO dto
    ) {
        return stateTransitionService.startSafari(id, dto);
    }

    /**
     * Complete safari (IN_PROGRESS -> COMPLETED)
     *
     * POST /api/safaris/{id}/state/complete
     */
    @PostMapping("/complete")
    @PreAuthorize("hasAuthority('PERM_COMPLETE_SAFARI')")
    public ResponseEntity<ApiResponse<?>> completeSafari(
            @PathVariable String id,
            @RequestBody(required = false) SafariStateTransitionDTO dto
    ) {
        return stateTransitionService.completeSafari(id, dto);
    }

    // ========================
    // CORE JOURNEY - POST-SAFARI PHASE
    // ========================

    /**
     * Close safari after all post-trip tasks are complete (COMPLETED -> CLOSED)
     *
     * POST /api/safaris/{id}/state/close
     */
    @PostMapping("/close")
    @PreAuthorize("hasAuthority('PERM_CLOSE_SAFARI')")
    public ResponseEntity<ApiResponse<?>> closeSafari(
            @PathVariable String id,
            @RequestBody(required = false) SafariStateTransitionDTO dto
    ) {
        return stateTransitionService.closeSafari(id, dto);
    }

    // ========================
    // EXCEPTION STATES - HOLD MANAGEMENT
    // ========================

    /**
     * Put safari on hold (multiple states -> ON_HOLD)
     *
     * POST /api/safaris/{id}/state/hold
     *
     * Request body:
     * - holdReason: (required) Enum value - PENDING_DOCUMENTS, PENDING_AVAILABILITY,
     *               RESCHEDULING, CLIENT_REQUEST, PAYMENT_ISSUE, OPERATIONAL_ISSUE, OTHER
     * - reason: (required) Detailed explanation
     * - notes: (optional) Additional context
     */
    @PostMapping("/hold")
    @PreAuthorize("hasAuthority('PERM_HOLD_SAFARI')")
    public ResponseEntity<ApiResponse<?>> holdSafari(
            @PathVariable String id,
            @Valid @RequestBody SafariStateTransitionDTO dto
    ) {
        return stateTransitionService.holdSafari(id, dto);
    }

    /**
     * Release safari from hold (ON_HOLD -> previous state or target state)
     *
     * POST /api/safaris/{id}/state/release-hold
     *
     * Request body:
     * - targetState: (optional) Specific state to transition to (e.g., CONFIRMED)
     *                If not provided, returns to previous state before hold
     */
    @PostMapping("/release-hold")
    @PreAuthorize("hasAuthority('PERM_RELEASE_SAFARI_HOLD')")
    public ResponseEntity<ApiResponse<?>> releaseHold(
            @PathVariable String id,
            @RequestBody(required = false) SafariStateTransitionDTO dto
    ) {
        return stateTransitionService.releaseHold(id, dto);
    }

    // ========================
    // EXCEPTION STATES - CANCELLATION
    // ========================

    /**
     * Cancel safari (multiple states -> CANCELLED)
     *
     * POST /api/safaris/{id}/state/cancel
     *
     * Request body:
     * - cancellationReason: (required) Enum value - BY_CLIENT, BY_OPERATOR, FORCE_MAJEURE,
     *                       PAYMENT_FAILURE, NO_AVAILABILITY, OTHER
     * - reason: (required) Detailed explanation
     * - notes: (optional) Additional context
     */
    @PostMapping("/cancel")
    @PreAuthorize("hasAuthority('PERM_CANCEL_SAFARI')")
    public ResponseEntity<ApiResponse<?>> cancelSafari(
            @PathVariable String id,
            @Valid @RequestBody SafariStateTransitionDTO dto
    ) {
        return stateTransitionService.cancelSafari(id, dto);
    }

    // ========================
    // EXCEPTION STATES - REFUND MANAGEMENT
    // ========================

    /**
     * Initiate refund process (CANCELLED -> REFUND_PENDING)
     *
     * POST /api/safaris/{id}/state/initiate-refund
     */
    @PostMapping("/initiate-refund")
    @PreAuthorize("hasAuthority('PERM_INITIATE_SAFARI_REFUND')")
    public ResponseEntity<ApiResponse<?>> initiateRefund(
            @PathVariable String id,
            @RequestBody(required = false) SafariStateTransitionDTO dto
    ) {
        return stateTransitionService.initiateRefund(id, dto);
    }

    /**
     * Record safari refund (partial or final)
     *
     * POST /api/safaris/{id}/state/record-refund
     *
     * State transitions:
     * - REFUND_PENDING -> REFUND_PENDING (partial refund)
     * - REFUND_PENDING -> REFUND_COMPLETE (final refund)
     *
     * Request body:
     * - isFinalRefund: (optional) true for final refund, false for partial. Defaults to true.
     * - notes: (optional) Refund reference, amount, method details
     */
    @PostMapping("/record-refund")
    @PreAuthorize("hasAuthority('PERM_RECORD_SAFARI_REFUND')")
    public ResponseEntity<ApiResponse<?>> recordRefund(
            @PathVariable String id,
            @RequestBody(required = false) SafariStateTransitionDTO dto
    ) {
        return stateTransitionService.recordRefund(id, dto);
    }

    // ========================
    // EXCEPTION STATES - DISPUTE RESOLUTION
    // ========================

    /**
     * Mark safari as disputed (multiple states -> DISPUTED)
     *
     * POST /api/safaris/{id}/state/mark-disputed
     *
     * Request body:
     * - reason: (required) Description of the dispute
     */
    @PostMapping("/mark-disputed")
    @PreAuthorize("hasAuthority('PERM_MARK_SAFARI_DISPUTED')")
    public ResponseEntity<ApiResponse<?>> markDisputed(
            @PathVariable String id,
            @Valid @RequestBody SafariStateTransitionDTO dto
    ) {
        return stateTransitionService.markDisputed(id, dto);
    }

    /**
     * Resolve dispute (DISPUTED -> resolution state)
     *
     * POST /api/safaris/{id}/state/resolve-dispute
     *
     * Request body:
     * - targetState: (required) Resolution state - CLOSED, REFUND_PENDING, or REFUND_COMPLETE
     * - reason: (required) Resolution explanation
     */
    @PostMapping("/resolve-dispute")
    @PreAuthorize("hasAuthority('PERM_RESOLVE_SAFARI_DISPUTE')")
    public ResponseEntity<ApiResponse<?>> resolveDispute(
            @PathVariable String id,
            @Valid @RequestBody SafariStateTransitionDTO dto
    ) {
        return stateTransitionService.resolveDispute(id, dto);
    }
}
