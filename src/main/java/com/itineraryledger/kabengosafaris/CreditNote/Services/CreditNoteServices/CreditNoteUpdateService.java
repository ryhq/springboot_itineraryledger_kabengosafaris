package com.itineraryledger.kabengosafaris.CreditNote.Services.CreditNoteServices;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.CreditNote.DTOs.CreditNoteDTO;
import com.itineraryledger.kabengosafaris.CreditNote.DTOs.UpdateCreditNoteDTO;
import com.itineraryledger.kabengosafaris.CreditNote.Entity.CreditNote;
import com.itineraryledger.kabengosafaris.CreditNote.Enums.CreditNoteStatus;
import com.itineraryledger.kabengosafaris.CreditNote.Repository.CreditNoteRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.User.User;
import com.itineraryledger.kabengosafaris.User.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for updating credit notes (metadata only)
 *
 * NOTE: Invoice and Customer relationships cannot be updated after creation.
 * Only DRAFT credit notes can be edited.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class CreditNoteUpdateService {

    private final CreditNoteRepository creditNoteRepository;
    private final UserRepository userRepository;
    private final IdObfuscator idObfuscator;
    private final CreditNoteCreateService creditNoteCreateService;

    @AuditLogAnnotation(
        action = "UPDATE_CREDIT_NOTE",
        description = "Updating a credit note",
        entityType = "CreditNote",
        entityIdParamName = "idObfuscated"
    )
    public ResponseEntity<ApiResponse<?>> updateCreditNote(String idObfuscated, UpdateCreditNoteDTO updateDTO) {
        log.info("Updating credit note with ID: {}", idObfuscated);

        try {
            // Decode ID
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode credit note ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid credit note ID", "INVALID_CREDIT_NOTE_ID")
                );
            }

            // Find credit note
            CreditNote creditNote = creditNoteRepository.findById(id).orElse(null);
            if (creditNote == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Credit note not found", "CREDIT_NOTE_NOT_FOUND")
                );
            }

            CreditNoteStatus currentStatus = creditNote.getStatus();

            // ========================
            // WORKFLOW ENFORCEMENT
            // ========================

            // Only DRAFT credit notes can be edited
            if (!creditNote.isEditable()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        String.format("Cannot edit %s credit note. Only DRAFT credit notes can be modified. " +
                            "Use the revert-to-draft endpoint if the credit note is in CONFIRMED state.",
                            currentStatus.getDisplayName()),
                        "EDIT_BLOCKED")
                );
            }

            // ========================
            // UPDATE ALLOWED FIELDS
            // ========================

            // Update title
            if (updateDTO.getTitle() != null) {
                creditNote.setTitle(updateDTO.getTitle());
            }

            // Update description
            if (updateDTO.getDescription() != null) {
                creditNote.setDescription(updateDTO.getDescription());
            }

            // Update reason
            if (updateDTO.getReason() != null) {
                creditNote.setReason(updateDTO.getReason());
            }

            // Update tax percentage
            if (updateDTO.getTaxPercentage() != null) {
                creditNote.setTaxPercentage(updateDTO.getTaxPercentage());
            }

            // Update issue date
            if (updateDTO.getIssueDate() != null) {
                creditNote.setIssueDate(updateDTO.getIssueDate());
            }

            // Update notes
            if (updateDTO.getInternalNotes() != null) {
                creditNote.setInternalNotes(updateDTO.getInternalNotes());
            }
            if (updateDTO.getCustomerNotes() != null) {
                creditNote.setCustomerNotes(updateDTO.getCustomerNotes());
            }

            // Update isActive
            if (updateDTO.getIsActive() != null) {
                creditNote.setIsActive(updateDTO.getIsActive());
            }

            // Set updated by current user
            User currentUser = getCurrentUser();
            if (currentUser != null) {
                creditNote.setUpdatedBy(currentUser);
            }

            // Save updated credit note
            creditNote = creditNoteRepository.save(creditNote);

            // Convert to DTO
            CreditNoteDTO creditNoteDTO = creditNoteCreateService.convertToDTO(creditNote);

            log.info("Credit note updated successfully: {}", creditNote.getCreditNoteCode());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Credit note updated successfully", creditNoteDTO)
            );

        } catch (Exception e) {
            log.error("Error updating credit note", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update credit note", "CREDIT_NOTE_UPDATE_FAILED")
            );
        }
    }

    /**
     * Get the current authenticated user
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
            !authentication.getPrincipal().equals("anonymousUser")) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof User) {
                User user = (User) principal;
                // Fetch from repository to ensure it's a managed entity
                return userRepository.findById(user.getId()).orElse(null);
            }
        }
        return null;
    }
}
