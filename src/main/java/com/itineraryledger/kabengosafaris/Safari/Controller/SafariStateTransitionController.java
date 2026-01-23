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
    // BOOKING STATE TRANSITIONS
    // ========================

    /**
     * Submit safari for approval (DRAFT -> PENDING_APPROVAL)
     *
     * POST /api/safaris/{id}/state/submit-for-approval
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
    // PAYMENT STATE TRANSITIONS
    // ========================

    /**
     * Request deposit payment (CONFIRMED -> PENDING_DEPOSIT)
     *
     * POST /api/safaris/{id}/state/request-deposit
     */
    @PostMapping("/request-deposit")
    @PreAuthorize("hasAuthority('PERM_REQUEST_SAFARI_DEPOSIT')")
    public ResponseEntity<ApiResponse<?>> requestDeposit(
            @PathVariable String id,
            @RequestBody(required = false) SafariStateTransitionDTO dto
    ) {
        return stateTransitionService.requestDeposit(id, dto);
    }

    /**
     * Record deposit payment received (PENDING_DEPOSIT -> DEPOSIT_PAID)
     *
     * POST /api/safaris/{id}/state/record-deposit
     */
    @PostMapping("/record-deposit")
    @PreAuthorize("hasAuthority('PERM_RECORD_SAFARI_DEPOSIT')")
    public ResponseEntity<ApiResponse<?>> recordDeposit(
            @PathVariable String id,
            @RequestBody(required = false) SafariStateTransitionDTO dto
    ) {
        return stateTransitionService.recordDeposit(id, dto);
    }

    /**
     * Record full payment received (DEPOSIT_PAID/CONFIRMED -> FULLY_PAID)
     *
     * POST /api/safaris/{id}/state/record-full-payment
     */
    @PostMapping("/record-full-payment")
    @PreAuthorize("hasAuthority('PERM_RECORD_SAFARI_FULL_PAYMENT')")
    public ResponseEntity<ApiResponse<?>> recordFullPayment(
            @PathVariable String id,
            @RequestBody(required = false) SafariStateTransitionDTO dto
    ) {
        return stateTransitionService.recordFullPayment(id, dto);
    }

    // ========================
    // OPERATIONAL STATE TRANSITIONS
    // ========================

    /**
     * Mark safari as ready to commence (FULLY_PAID -> READY)
     *
     * POST /api/safaris/{id}/state/mark-ready
     */
    @PostMapping("/mark-ready")
    @PreAuthorize("hasAuthority('PERM_MARK_SAFARI_READY')")
    public ResponseEntity<ApiResponse<?>> markReady(
            @PathVariable String id,
            @RequestBody(required = false) SafariStateTransitionDTO dto
    ) {
        return stateTransitionService.markReady(id, dto);
    }

    /**
     * Start safari (READY -> IN_PROGRESS)
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
    // POST-SAFARI STATE TRANSITIONS
    // ========================

    /**
     * Request review (COMPLETED -> PENDING_REVIEW)
     *
     * POST /api/safaris/{id}/state/request-review
     */
    @PostMapping("/request-review")
    @PreAuthorize("hasAuthority('PERM_REQUEST_SAFARI_REVIEW')")
    public ResponseEntity<ApiResponse<?>> requestReview(
            @PathVariable String id,
            @RequestBody(required = false) SafariStateTransitionDTO dto
    ) {
        return stateTransitionService.requestReview(id, dto);
    }

    /**
     * Close safari (COMPLETED/PENDING_REVIEW -> CLOSED)
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
    // HOLD/PAUSE STATE TRANSITIONS
    // ========================

    /**
     * Put safari on hold (multiple states -> ON_HOLD)
     *
     * POST /api/safaris/{id}/state/hold
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
     * Release hold (ON_HOLD -> previous state or CONFIRMED)
     *
     * POST /api/safaris/{id}/state/release-hold
     */
    @PostMapping("/release-hold")
    @PreAuthorize("hasAuthority('PERM_RELEASE_SAFARI_HOLD')")
    public ResponseEntity<ApiResponse<?>> releaseHold(
            @PathVariable String id,
            @RequestBody(required = false) SafariStateTransitionDTO dto
    ) {
        return stateTransitionService.releaseHold(id, dto);
    }

    /**
     * Mark pending documents (multiple states -> PENDING_DOCUMENTS)
     *
     * POST /api/safaris/{id}/state/pending-documents
     */
    @PostMapping("/pending-documents")
    @PreAuthorize("hasAuthority('PERM_MARK_SAFARI_PENDING_DOCUMENTS')")
    public ResponseEntity<ApiResponse<?>> markPendingDocuments(
            @PathVariable String id,
            @Valid @RequestBody SafariStateTransitionDTO dto
    ) {
        return stateTransitionService.markPendingDocuments(id, dto);
    }

    /**
     * Mark documents received (PENDING_DOCUMENTS -> previous state or CONFIRMED)
     *
     * POST /api/safaris/{id}/state/documents-received
     */
    @PostMapping("/documents-received")
    @PreAuthorize("hasAuthority('PERM_MARK_SAFARI_DOCUMENTS_RECEIVED')")
    public ResponseEntity<ApiResponse<?>> markDocumentsReceived(
            @PathVariable String id,
            @RequestBody(required = false) SafariStateTransitionDTO dto
    ) {
        return stateTransitionService.markDocumentsReceived(id, dto);
    }

    /**
     * Mark pending availability (multiple states -> PENDING_AVAILABILITY)
     *
     * POST /api/safaris/{id}/state/pending-availability
     */
    @PostMapping("/pending-availability")
    @PreAuthorize("hasAuthority('PERM_MARK_SAFARI_PENDING_AVAILABILITY')")
    public ResponseEntity<ApiResponse<?>> markPendingAvailability(
            @PathVariable String id,
            @Valid @RequestBody SafariStateTransitionDTO dto
    ) {
        return stateTransitionService.markPendingAvailability(id, dto);
    }

    /**
     * Mark availability confirmed (PENDING_AVAILABILITY -> previous state or CONFIRMED)
     *
     * POST /api/safaris/{id}/state/availability-confirmed
     */
    @PostMapping("/availability-confirmed")
    @PreAuthorize("hasAuthority('PERM_MARK_SAFARI_AVAILABILITY_CONFIRMED')")
    public ResponseEntity<ApiResponse<?>> markAvailabilityConfirmed(
            @PathVariable String id,
            @RequestBody(required = false) SafariStateTransitionDTO dto
    ) {
        return stateTransitionService.markAvailabilityConfirmed(id, dto);
    }

    // ========================
    // RESCHEDULE STATE TRANSITIONS
    // ========================

    /**
     * Postpone safari (multiple states -> POSTPONED)
     *
     * POST /api/safaris/{id}/state/postpone
     */
    @PostMapping("/postpone")
    @PreAuthorize("hasAuthority('PERM_POSTPONE_SAFARI')")
    public ResponseEntity<ApiResponse<?>> postponeSafari(
            @PathVariable String id,
            @Valid @RequestBody SafariStateTransitionDTO dto
    ) {
        return stateTransitionService.postponeSafari(id, dto);
    }

    /**
     * Initiate reschedule (multiple states -> RESCHEDULING)
     *
     * POST /api/safaris/{id}/state/initiate-reschedule
     */
    @PostMapping("/initiate-reschedule")
    @PreAuthorize("hasAuthority('PERM_INITIATE_SAFARI_RESCHEDULE')")
    public ResponseEntity<ApiResponse<?>> initiateReschedule(
            @PathVariable String id,
            @Valid @RequestBody SafariStateTransitionDTO dto
    ) {
        return stateTransitionService.initiateReschedule(id, dto);
    }

    /**
     * Complete reschedule with new dates (RESCHEDULING -> CONFIRMED)
     *
     * POST /api/safaris/{id}/state/complete-reschedule
     */
    @PostMapping("/complete-reschedule")
    @PreAuthorize("hasAuthority('PERM_COMPLETE_SAFARI_RESCHEDULE')")
    public ResponseEntity<ApiResponse<?>> completeReschedule(
            @PathVariable String id,
            @Valid @RequestBody SafariStateTransitionDTO dto
    ) {
        return stateTransitionService.completeReschedule(id, dto);
    }

    // ========================
    // CANCELLATION STATE TRANSITIONS
    // ========================

    /**
     * Request cancellation (multiple states -> CANCELLATION_REQUESTED)
     *
     * POST /api/safaris/{id}/state/request-cancellation
     */
    @PostMapping("/request-cancellation")
    @PreAuthorize("hasAuthority('PERM_REQUEST_SAFARI_CANCELLATION')")
    public ResponseEntity<ApiResponse<?>> requestCancellation(
            @PathVariable String id,
            @Valid @RequestBody SafariStateTransitionDTO dto
    ) {
        return stateTransitionService.requestCancellation(id, dto);
    }

    /**
     * Cancel safari (generic cancellation)
     *
     * POST /api/safaris/{id}/state/cancel
     */
    @PostMapping("/cancel")
    @PreAuthorize("hasAuthority('PERM_CANCEL_SAFARI')")
    public ResponseEntity<ApiResponse<?>> cancelSafari(
            @PathVariable String id,
            @Valid @RequestBody SafariStateTransitionDTO dto
    ) {
        return stateTransitionService.cancelSafari(id, dto);
    }

    /**
     * Cancel safari by client (-> CANCELLED_BY_CLIENT)
     *
     * POST /api/safaris/{id}/state/cancel-by-client
     */
    @PostMapping("/cancel-by-client")
    @PreAuthorize("hasAuthority('PERM_CANCEL_SAFARI_BY_CLIENT')")
    public ResponseEntity<ApiResponse<?>> cancelByClient(
            @PathVariable String id,
            @Valid @RequestBody SafariStateTransitionDTO dto
    ) {
        return stateTransitionService.cancelByClient(id, dto);
    }

    /**
     * Cancel safari by operator (-> CANCELLED_BY_OPERATOR)
     *
     * POST /api/safaris/{id}/state/cancel-by-operator
     */
    @PostMapping("/cancel-by-operator")
    @PreAuthorize("hasAuthority('PERM_CANCEL_SAFARI_BY_OPERATOR')")
    public ResponseEntity<ApiResponse<?>> cancelByOperator(
            @PathVariable String id,
            @Valid @RequestBody SafariStateTransitionDTO dto
    ) {
        return stateTransitionService.cancelByOperator(id, dto);
    }

    /**
     * Cancel safari due to force majeure (-> CANCELLED_FORCE_MAJEURE)
     *
     * POST /api/safaris/{id}/state/cancel-force-majeure
     */
    @PostMapping("/cancel-force-majeure")
    @PreAuthorize("hasAuthority('PERM_CANCEL_SAFARI_FORCE_MAJEURE')")
    public ResponseEntity<ApiResponse<?>> cancelForceMajeure(
            @PathVariable String id,
            @Valid @RequestBody SafariStateTransitionDTO dto
    ) {
        return stateTransitionService.cancelForceMajeure(id, dto);
    }

    // ========================
    // REFUND STATE TRANSITIONS
    // ========================

    /**
     * Initiate refund (CANCELLED_* -> REFUND_PENDING)
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
     * Record partial refund (REFUND_PENDING -> REFUND_PARTIAL)
     *
     * POST /api/safaris/{id}/state/record-partial-refund
     */
    @PostMapping("/record-partial-refund")
    @PreAuthorize("hasAuthority('PERM_RECORD_SAFARI_PARTIAL_REFUND')")
    public ResponseEntity<ApiResponse<?>> recordPartialRefund(
            @PathVariable String id,
            @RequestBody(required = false) SafariStateTransitionDTO dto
    ) {
        return stateTransitionService.recordPartialRefund(id, dto);
    }

    /**
     * Record full refund (REFUND_PENDING/REFUND_PARTIAL -> REFUND_COMPLETE)
     *
     * POST /api/safaris/{id}/state/record-full-refund
     */
    @PostMapping("/record-full-refund")
    @PreAuthorize("hasAuthority('PERM_RECORD_SAFARI_FULL_REFUND')")
    public ResponseEntity<ApiResponse<?>> recordFullRefund(
            @PathVariable String id,
            @RequestBody(required = false) SafariStateTransitionDTO dto
    ) {
        return stateTransitionService.recordFullRefund(id, dto);
    }

    // ========================
    // DISPUTE STATE TRANSITIONS
    // ========================

    /**
     * Mark safari as disputed (multiple states -> DISPUTED)
     *
     * POST /api/safaris/{id}/state/mark-disputed
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
     * Start investigation (DISPUTED -> UNDER_INVESTIGATION)
     *
     * POST /api/safaris/{id}/state/investigate-dispute
     */
    @PostMapping("/investigate-dispute")
    @PreAuthorize("hasAuthority('PERM_INVESTIGATE_SAFARI_DISPUTE')")
    public ResponseEntity<ApiResponse<?>> investigateDispute(
            @PathVariable String id,
            @RequestBody(required = false) SafariStateTransitionDTO dto
    ) {
        return stateTransitionService.investigateDispute(id, dto);
    }

    /**
     * Resolve dispute (DISPUTED/UNDER_INVESTIGATION -> resolution state)
     *
     * POST /api/safaris/{id}/state/resolve-dispute
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
