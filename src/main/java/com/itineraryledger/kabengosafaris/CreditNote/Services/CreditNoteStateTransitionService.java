package com.itineraryledger.kabengosafaris.CreditNote.Services;

import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.CreditNote.DTOs.CreditNoteDTO;
import com.itineraryledger.kabengosafaris.CreditNote.DTOs.CreditNoteStateTransitionDTO;
import com.itineraryledger.kabengosafaris.CreditNote.Entity.CreditNote;
import com.itineraryledger.kabengosafaris.CreditNote.Enums.CreditNoteStatus;
import com.itineraryledger.kabengosafaris.CreditNote.Repository.CreditNoteRepository;
import com.itineraryledger.kabengosafaris.CreditNote.Services.CreditNoteServices.CreditNoteCreateService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Credit Note State Transition Service - Manages credit note workflow state transitions
 *
 * Workflow:
 *   DRAFT -> CONFIRMED -> SENT -> CONSUMED
 *
 * Revert:
 *   CONFIRMED -> DRAFT (allow corrections)
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class CreditNoteStateTransitionService {

    private final CreditNoteRepository creditNoteRepository;
    private final IdObfuscator idObfuscator;
    private final CreditNoteCreateService creditNoteCreateService;

    // ========================
    // DRAFT -> CONFIRMED
    // ========================

    /**
     * Confirm credit note (DRAFT -> CONFIRMED)
     */
    @AuditLogAnnotation(
        action = "CONFIRM_CREDIT_NOTE",
        description = "Confirming credit note",
        entityType = "CreditNote",
        entityIdParamName = "idObfuscated"
    )
    public ResponseEntity<ApiResponse<?>> confirmCreditNote(String idObfuscated, CreditNoteStateTransitionDTO dto) {
        log.info("Confirming credit note: {}", idObfuscated);

        try {
            CreditNote creditNote = findCreditNote(idObfuscated);
            if (creditNote == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(404, "Credit note not found", "CREDIT_NOTE_NOT_FOUND")
                );
            }

            if (creditNote.getStatus() != CreditNoteStatus.DRAFT) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        String.format("Cannot confirm credit note in state %s. Credit note must be in DRAFT state.",
                            creditNote.getStatus().getDisplayName()),
                        "INVALID_STATE_TRANSITION")
                );
            }

            // Validate the transition is allowed
            if (!creditNote.getStatus().canTransitionTo(CreditNoteStatus.CONFIRMED)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        "State transition from " + creditNote.getStatus().getDisplayName() + " to Confirmed is not allowed",
                        "INVALID_STATE_TRANSITION")
                );
            }

            creditNote.setStatus(CreditNoteStatus.CONFIRMED);

            creditNote = creditNoteRepository.save(creditNote);
            CreditNoteDTO creditNoteDTO = creditNoteCreateService.convertToDTO(creditNote);

            log.info("Credit note {} confirmed successfully", creditNote.getCreditNoteCode());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Credit note confirmed successfully", creditNoteDTO)
            );

        } catch (Exception e) {
            log.error("Error confirming credit note", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(500, "Failed to confirm credit note", "CONFIRM_CREDIT_NOTE_FAILED")
            );
        }
    }

    // ========================
    // CONFIRMED -> SENT
    // ========================

    /**
     * Send credit note to customer (CONFIRMED -> SENT)
     */
    @AuditLogAnnotation(
        action = "SEND_CREDIT_NOTE",
        description = "Sending credit note",
        entityType = "CreditNote",
        entityIdParamName = "idObfuscated"
    )
    public ResponseEntity<ApiResponse<?>> sendCreditNote(String idObfuscated, CreditNoteStateTransitionDTO dto) {
        log.info("Sending credit note: {}", idObfuscated);

        try {
            CreditNote creditNote = findCreditNote(idObfuscated);
            if (creditNote == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(404, "Credit note not found", "CREDIT_NOTE_NOT_FOUND")
                );
            }

            if (creditNote.getStatus() != CreditNoteStatus.CONFIRMED) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        String.format("Cannot send credit note in state %s. Credit note must be in CONFIRMED state.",
                            creditNote.getStatus().getDisplayName()),
                        "INVALID_STATE_TRANSITION")
                );
            }

            // Validate the transition is allowed
            if (!creditNote.getStatus().canTransitionTo(CreditNoteStatus.SENT)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        "State transition from " + creditNote.getStatus().getDisplayName() + " to Sent is not allowed",
                        "INVALID_STATE_TRANSITION")
                );
            }

            creditNote.setStatus(CreditNoteStatus.SENT);
            creditNote.setSentDate(LocalDate.now());

            creditNote = creditNoteRepository.save(creditNote);
            CreditNoteDTO creditNoteDTO = creditNoteCreateService.convertToDTO(creditNote);

            log.info("Credit note {} sent successfully", creditNote.getCreditNoteCode());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Credit note sent successfully", creditNoteDTO)
            );

        } catch (Exception e) {
            log.error("Error sending credit note", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(500, "Failed to send credit note", "SEND_CREDIT_NOTE_FAILED")
            );
        }
    }

    // ========================
    // SENT -> CONSUMED
    // ========================

    /**
     * Consume credit note (SENT -> CONSUMED)
     *
     * Requires consumptionMethod and consumptionNotes from DTO.
     */
    @AuditLogAnnotation(
        action = "CONSUME_CREDIT_NOTE",
        description = "Consuming credit note",
        entityType = "CreditNote",
        entityIdParamName = "idObfuscated"
    )
    public ResponseEntity<ApiResponse<?>> consumeCreditNote(String idObfuscated, CreditNoteStateTransitionDTO dto) {
        log.info("Consuming credit note: {}", idObfuscated);

        try {
            if (dto == null || dto.getConsumptionMethod() == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Consumption method is required when consuming a credit note", "CONSUMPTION_METHOD_REQUIRED")
                );
            }

            if (dto.getConsumptionNotes() == null || dto.getConsumptionNotes().isBlank()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Consumption notes are required when consuming a credit note", "CONSUMPTION_NOTES_REQUIRED")
                );
            }

            CreditNote creditNote = findCreditNote(idObfuscated);
            if (creditNote == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(404, "Credit note not found", "CREDIT_NOTE_NOT_FOUND")
                );
            }

            if (creditNote.getStatus() != CreditNoteStatus.SENT) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        String.format("Cannot consume credit note in state %s. Credit note must be in SENT state.",
                            creditNote.getStatus().getDisplayName()),
                        "INVALID_STATE_TRANSITION")
                );
            }

            // Validate the transition is allowed
            if (!creditNote.getStatus().canTransitionTo(CreditNoteStatus.CONSUMED)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        "State transition from " + creditNote.getStatus().getDisplayName() + " to Consumed is not allowed",
                        "INVALID_STATE_TRANSITION")
                );
            }

            creditNote.setStatus(CreditNoteStatus.CONSUMED);
            creditNote.setConsumedDate(LocalDate.now());
            creditNote.setConsumptionMethod(dto.getConsumptionMethod());
            creditNote.setConsumptionNotes(dto.getConsumptionNotes());

            creditNote = creditNoteRepository.save(creditNote);
            CreditNoteDTO creditNoteDTO = creditNoteCreateService.convertToDTO(creditNote);

            log.info("Credit note {} consumed successfully via {}", creditNote.getCreditNoteCode(), dto.getConsumptionMethod());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Credit note consumed successfully", creditNoteDTO)
            );

        } catch (Exception e) {
            log.error("Error consuming credit note", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(500, "Failed to consume credit note", "CONSUME_CREDIT_NOTE_FAILED")
            );
        }
    }

    // ========================
    // CONFIRMED -> DRAFT (REVERT)
    // ========================

    /**
     * Revert credit note to draft (CONFIRMED -> DRAFT)
     *
     * Allows corrections to be made on a confirmed credit note.
     */
    @AuditLogAnnotation(
        action = "REVERT_CREDIT_NOTE_TO_DRAFT",
        description = "Reverting credit note to draft",
        entityType = "CreditNote",
        entityIdParamName = "idObfuscated"
    )
    public ResponseEntity<ApiResponse<?>> revertToDraft(String idObfuscated, CreditNoteStateTransitionDTO dto) {
        log.info("Reverting credit note to draft: {}", idObfuscated);

        try {
            CreditNote creditNote = findCreditNote(idObfuscated);
            if (creditNote == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(404, "Credit note not found", "CREDIT_NOTE_NOT_FOUND")
                );
            }

            if (creditNote.getStatus() != CreditNoteStatus.CONFIRMED) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        String.format("Cannot revert credit note in state %s. Only CONFIRMED credit notes can be reverted to DRAFT.",
                            creditNote.getStatus().getDisplayName()),
                        "INVALID_STATE_TRANSITION")
                );
            }

            // Validate the transition is allowed
            if (!creditNote.getStatus().canTransitionTo(CreditNoteStatus.DRAFT)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        "State transition from " + creditNote.getStatus().getDisplayName() + " to Draft is not allowed",
                        "INVALID_STATE_TRANSITION")
                );
            }

            creditNote.setStatus(CreditNoteStatus.DRAFT);

            creditNote = creditNoteRepository.save(creditNote);
            CreditNoteDTO creditNoteDTO = creditNoteCreateService.convertToDTO(creditNote);

            log.info("Credit note {} reverted to draft successfully", creditNote.getCreditNoteCode());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Credit note reverted to draft successfully", creditNoteDTO)
            );

        } catch (Exception e) {
            log.error("Error reverting credit note to draft", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(500, "Failed to revert credit note to draft", "REVERT_CREDIT_NOTE_FAILED")
            );
        }
    }

    // ========================
    // HELPER METHODS
    // ========================

    private CreditNote findCreditNote(String idObfuscated) {
        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            return creditNoteRepository.findById(id).orElse(null);
        } catch (Exception e) {
            log.warn("Failed to decode credit note ID: {}", idObfuscated, e);
            return null;
        }
    }
}
