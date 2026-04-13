package com.itineraryledger.kabengosafaris.CreditNote.Services.CreditNoteServices;

import java.util.ArrayList;
import java.util.List;

import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.CreditNote.Entity.CreditNote;
import com.itineraryledger.kabengosafaris.CreditNote.Repository.CreditNoteRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * CreditNoteDeleteService - Service for deleting credit notes
 *
 * Only allows deletion of DRAFT credit notes to prevent accidental deletion
 * of confirmed or sent credit notes.
 */
@Service
@Slf4j
@Transactional
public class CreditNoteDeleteService {

    private final CreditNoteRepository creditNoteRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public CreditNoteDeleteService(
        CreditNoteRepository creditNoteRepository,
        IdObfuscator idObfuscator
    ) {
        this.creditNoteRepository = creditNoteRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Delete credit notes by list of obfuscated IDs
     *
     * Only DRAFT credit notes can be deleted. Credit notes in other states will be skipped.
     *
     * @param idObfuscatedList List of obfuscated credit note IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    public ResponseEntity<ApiResponse<?>> deleteCreditNotes(List<String> idObfuscatedList) {
        log.info("Attempting to delete {} credit notes", idObfuscatedList.size());

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

            return deleteCreditNotesInternal(ids);

        } catch (Exception e) {
            log.error("Error deleting credit notes", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to delete credit notes",
                    "CREDIT_NOTES_DELETE_FAILED"
                )
            );
        }
    }

    /**
     * Delete credit notes by list of IDs (internal method)
     */
    private ResponseEntity<ApiResponse<?>> deleteCreditNotesInternal(List<Long> ids) {
        int deletedCount = 0;
        int skippedCount = 0;
        List<String> skippedReasons = new ArrayList<>();

        for (Long id : ids) {
            try {
                CreditNote creditNote = creditNoteRepository.findById(id).orElse(null);

                if (creditNote == null) {
                    log.warn("Credit note not found: {}", id);
                    skippedCount++;
                    skippedReasons.add(String.format("Credit note ID %d not found", id));
                    continue;
                }

                // Only allow deletion of DRAFT credit notes
                if (!creditNote.isDeletable()) {
                    log.warn("Cannot delete credit note {} - status is {} (only DRAFT credit notes can be deleted)",
                             creditNote.getCreditNoteCode(), creditNote.getStatus().getDisplayName());
                    skippedCount++;
                    skippedReasons.add(String.format("Credit note %s cannot be deleted - status is %s (only DRAFT credit notes can be deleted)",
                                                     creditNote.getCreditNoteCode(), creditNote.getStatus().getDisplayName()));
                    continue;
                }

                // Use AopContext to get proxy and trigger AOP aspect
                ((CreditNoteDeleteService) AopContext.currentProxy()).deleteCreditNote(id);
                deletedCount++;
                log.info("Credit note deleted successfully: {} ({})", creditNote.getCreditNoteCode(), id);

            } catch (Exception e) {
                log.error("Error deleting credit note: {}", id, e);
                skippedCount++;
                skippedReasons.add(String.format("Error deleting credit note ID %d: %s", id, e.getMessage()));
            }
        }

        // Build response message
        StringBuilder message = new StringBuilder();
        if (deletedCount > 0) {
            message.append(deletedCount)
                   .append(deletedCount > 1 ? " credit notes deleted successfully" : " credit note deleted successfully");
        }

        if (skippedCount > 0) {
            if (deletedCount > 0) {
                message.append(". ");
            }
            message.append(skippedCount)
                   .append(skippedCount > 1 ? " credit notes skipped" : " credit note skipped");
        }

        // Return appropriate response
        if (deletedCount == 0 && skippedCount > 0) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    400,
                    message.toString() + ": " + String.join("; ", skippedReasons),
                    "NO_CREDIT_NOTES_DELETED"
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
     * Delete a single credit note by ID (internal method with audit logging)
     *
     * @param id Credit note ID to delete
     */
    @AuditLogAnnotation(
        action = "DELETE_CREDIT_NOTE",
        description = "Deleting credit note",
        entityType = "CreditNote",
        entityIdParamName = "id"
    )
    public void deleteCreditNote(Long id) {
        creditNoteRepository.deleteById(id);
    }
}
