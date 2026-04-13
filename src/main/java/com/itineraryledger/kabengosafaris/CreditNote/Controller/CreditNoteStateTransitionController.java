package com.itineraryledger.kabengosafaris.CreditNote.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.itineraryledger.kabengosafaris.CreditNote.DTOs.CreditNoteStateTransitionDTO;
import com.itineraryledger.kabengosafaris.CreditNote.Services.CreditNoteStateTransitionService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * CreditNoteStateTransitionController - REST API endpoints for CreditNote state transitions
 *
 * Workflow:
 *   DRAFT -> CONFIRMED -> SENT -> CONSUMED
 *
 * Revert:
 *   CONFIRMED -> DRAFT (allow corrections)
 *
 * Base URL: /api/credit-notes/{id}/state
 */
@RestController
@RequestMapping("/api/credit-notes/{id}/state")
@Slf4j
@RequiredArgsConstructor
public class CreditNoteStateTransitionController {

    private final CreditNoteStateTransitionService stateTransitionService;

    /**
     * Confirm credit note (DRAFT -> CONFIRMED)
     * POST /api/credit-notes/{id}/state/confirm
     */
    @PostMapping("/confirm")
    @PreAuthorize("hasAuthority('PERM_CONFIRM_CREDIT_NOTE')")
    public ResponseEntity<ApiResponse<?>> confirmCreditNote(
            @PathVariable String id,
            @RequestBody(required = false) CreditNoteStateTransitionDTO dto
    ) {
        log.info("POST /api/credit-notes/{}/state/confirm", id);
        return stateTransitionService.confirmCreditNote(id, dto);
    }

    /**
     * Send credit note to customer (CONFIRMED -> SENT)
     * POST /api/credit-notes/{id}/state/send
     */
    @PostMapping("/send")
    @PreAuthorize("hasAuthority('PERM_SEND_CREDIT_NOTE')")
    public ResponseEntity<ApiResponse<?>> sendCreditNote(
            @PathVariable String id,
            @RequestBody(required = false) CreditNoteStateTransitionDTO dto
    ) {
        log.info("POST /api/credit-notes/{}/state/send", id);
        return stateTransitionService.sendCreditNote(id, dto);
    }

    /**
     * Consume credit note (SENT -> CONSUMED)
     * POST /api/credit-notes/{id}/state/consume
     */
    @PostMapping("/consume")
    @PreAuthorize("hasAuthority('PERM_CONSUME_CREDIT_NOTE')")
    public ResponseEntity<ApiResponse<?>> consumeCreditNote(
            @PathVariable String id,
            @RequestBody CreditNoteStateTransitionDTO dto
    ) {
        log.info("POST /api/credit-notes/{}/state/consume", id);
        return stateTransitionService.consumeCreditNote(id, dto);
    }

    /**
     * Revert credit note to draft (CONFIRMED -> DRAFT)
     * POST /api/credit-notes/{id}/state/revert-to-draft
     */
    @PostMapping("/revert-to-draft")
    @PreAuthorize("hasAuthority('PERM_UPDATE_CREDIT_NOTE')")
    public ResponseEntity<ApiResponse<?>> revertToDraft(
            @PathVariable String id,
            @RequestBody(required = false) CreditNoteStateTransitionDTO dto
    ) {
        log.info("POST /api/credit-notes/{}/state/revert-to-draft", id);
        return stateTransitionService.revertToDraft(id, dto);
    }
}
