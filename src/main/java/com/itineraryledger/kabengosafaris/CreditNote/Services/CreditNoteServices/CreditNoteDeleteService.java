package com.itineraryledger.kabengosafaris.CreditNote.Services.CreditNoteServices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
     * Only DRAFT credit notes can be deleted; anything further along is SKIPPED and says why.
     *
     * Reports per id — {deletedCount, deletedIds, skipped:[{id, code, reason}]} — because a
     * confirmed credit note is one the customer may already have been told about, and a plain 200
     * that quietly deleted three of five rows is the worst way for that to be discovered.
     *
     * @param idObfuscatedList List of obfuscated credit note IDs
     * @return ResponseEntity with ApiResponse containing the per-id report
     */
    public ResponseEntity<ApiResponse<?>> deleteCreditNotes(List<String> idObfuscatedList) {
        log.info("Attempting to delete {} credit notes", idObfuscatedList.size());

        try {
            List<String> deletedIds = new ArrayList<>();
            List<Map<String, Object>> skipped = new ArrayList<>();

            for (String idObfuscated : idObfuscatedList) {
                Long id;
                try {
                    id = idObfuscator.decodeId(idObfuscated);
                } catch (Exception e) {
                    /* an id that decodes to nothing is still an id the caller asked about */
                    log.warn("Failed to decode ID: {}", idObfuscated, e);
                    skipped.add(skip(idObfuscated, null, "That is not a credit note id"));
                    continue;
                }

                try {
                    CreditNote creditNote = creditNoteRepository.findById(id).orElse(null);

                    if (creditNote == null) {
                        log.warn("Credit note not found: {}", id);
                        skipped.add(skip(idObfuscated, null, "No longer exists"));
                        continue;
                    }

                    // Only allow deletion of DRAFT credit notes
                    if (!creditNote.isDeletable()) {
                        log.warn("Cannot delete credit note {} - status is {} (only DRAFT credit notes can be deleted)",
                                 creditNote.getCreditNoteCode(), creditNote.getStatus().getDisplayName());
                        skipped.add(skip(idObfuscated, creditNote.getCreditNoteCode(),
                            String.format("It is %s. Only a draft can be deleted — this one is on record.",
                                creditNote.getStatus().getDisplayName().toLowerCase())));
                        continue;
                    }

                    // Use AopContext to get proxy and trigger AOP aspect
                    ((CreditNoteDeleteService) AopContext.currentProxy()).deleteCreditNote(id);
                    deletedIds.add(idObfuscated);
                    log.info("Credit note deleted successfully: {} ({})", creditNote.getCreditNoteCode(), id);

                } catch (Exception e) {
                    log.error("Error deleting credit note: {}", id, e);
                    skipped.add(skip(idObfuscated, null, "Could not be deleted: " + e.getMessage()));
                }
            }

            Map<String, Object> report = new HashMap<>();
            report.put("deletedCount", deletedIds.size());
            report.put("deletedIds", deletedIds);
            report.put("skipped", skipped);

            StringBuilder message = new StringBuilder();
            if (!deletedIds.isEmpty()) {
                message.append(deletedIds.size())
                       .append(deletedIds.size() > 1 ? " credit notes deleted" : " credit note deleted");
            }
            if (!skipped.isEmpty()) {
                if (!deletedIds.isEmpty()) message.append(". ");
                message.append(skipped.size())
                       .append(skipped.size() > 1 ? " skipped" : " skipped");
            }

            /* nothing deleted and something asked for: a failure, reported as one */
            if (deletedIds.isEmpty() && !skipped.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        message + ": " + skipped.get(0).get("reason"),
                        "NO_CREDIT_NOTES_DELETED"
                    )
                );
            }

            return ResponseEntity.ok().body(
                ApiResponse.success(200, message.toString(), report)
            );

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

    private Map<String, Object> skip(String id, String code, String reason) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("code", code);
        row.put("reason", reason);
        return row;
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
