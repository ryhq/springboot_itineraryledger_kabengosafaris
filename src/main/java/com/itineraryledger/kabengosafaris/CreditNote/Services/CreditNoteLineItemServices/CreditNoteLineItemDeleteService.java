package com.itineraryledger.kabengosafaris.CreditNote.Services.CreditNoteLineItemServices;

import java.util.ArrayList;
import java.util.List;

import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.CreditNote.Entity.CreditNoteLineItem;
import com.itineraryledger.kabengosafaris.CreditNote.Repository.CreditNoteLineItemRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for deleting credit note line items
 *
 * Only allows deletion from DRAFT credit notes.
 */
@Service
@Slf4j
@Transactional
public class CreditNoteLineItemDeleteService {

    private final CreditNoteLineItemRepository creditNoteLineItemRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public CreditNoteLineItemDeleteService(
        CreditNoteLineItemRepository creditNoteLineItemRepository,
        IdObfuscator idObfuscator
    ) {
        this.creditNoteLineItemRepository = creditNoteLineItemRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Delete credit note line items by list of obfuscated IDs
     *
     * Only line items from DRAFT credit notes can be deleted.
     *
     * @param creditNoteId The obfuscated credit note ID (for context)
     * @param itemIds List of obfuscated line item IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    public ResponseEntity<ApiResponse<?>> deleteCreditNoteLineItems(String creditNoteId, List<String> itemIds) {
        log.info("Deleting {} credit note line items", itemIds.size());

        try {
            List<Long> ids = new ArrayList<>();
            for (String idObfuscated : itemIds) {
                try {
                    Long id = idObfuscator.decodeId(idObfuscated);
                    ids.add(id);
                } catch (Exception e) {
                    log.warn("Failed to decode ID: {}", idObfuscated, e);
                }
            }

            return deleteCreditNoteLineItemsInternal(ids);

        } catch (Exception e) {
            log.error("Error deleting credit note line items", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete credit note line items", "CREDIT_NOTE_LINE_ITEMS_DELETE_FAILED")
            );
        }
    }

    /**
     * Delete credit note line items by list of IDs (internal method)
     */
    private ResponseEntity<ApiResponse<?>> deleteCreditNoteLineItemsInternal(List<Long> ids) {
        int deletedCount = 0;
        int skippedCount = 0;
        List<String> skippedReasons = new ArrayList<>();

        for (Long id : ids) {
            try {
                CreditNoteLineItem lineItem = creditNoteLineItemRepository.findById(id).orElse(null);

                if (lineItem == null) {
                    log.warn("Credit note line item not found: {}", id);
                    skippedCount++;
                    skippedReasons.add(String.format("Credit note line item ID %d not found", id));
                    continue;
                }

                // Check if parent credit note is editable (DRAFT only)
                if (!lineItem.getCreditNote().isEditable()) {
                    log.warn("Cannot delete line item {} - credit note is not editable (status: {})",
                             id, lineItem.getCreditNote().getStatus().getDisplayName());
                    skippedCount++;
                    skippedReasons.add(String.format("Cannot delete line item - credit note %s is in %s state (only DRAFT credit notes allow deletion)",
                                                     lineItem.getCreditNote().getCreditNoteCode(),
                                                     lineItem.getCreditNote().getStatus().getDisplayName()));
                    continue;
                }

                // Use AopContext to get proxy and trigger AOP aspect
                ((CreditNoteLineItemDeleteService) AopContext.currentProxy()).deleteCreditNoteLineItem(id);
                deletedCount++;
                log.info("Credit note line item deleted successfully: {}", id);

            } catch (Exception e) {
                log.error("Error deleting credit note line item: {}", id, e);
                skippedCount++;
                skippedReasons.add(String.format("Error deleting credit note line item ID %d: %s", id, e.getMessage()));
            }
        }

        // Build response message
        StringBuilder message = new StringBuilder();
        if (deletedCount > 0) {
            message.append(deletedCount)
                   .append(deletedCount > 1 ? " credit note line items deleted successfully" : " credit note line item deleted successfully");
        }

        if (skippedCount > 0) {
            if (deletedCount > 0) {
                message.append(". ");
            }
            message.append(skippedCount)
                   .append(skippedCount > 1 ? " items skipped" : " item skipped");
        }

        // Return appropriate response
        if (deletedCount == 0 && skippedCount > 0) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    400,
                    message.toString() + ": " + String.join("; ", skippedReasons),
                    "NO_CREDIT_NOTE_LINE_ITEMS_DELETED"
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
     * Delete a single credit note line item by ID (internal method with audit logging)
     *
     * @param id Credit note line item ID to delete
     */
    @AuditLogAnnotation(
        action = "DELETE_CREDIT_NOTE_LINE_ITEM",
        description = "Deleting credit note line item",
        entityType = "CreditNoteLineItem",
        entityIdParamName = "id"
    )
    public void deleteCreditNoteLineItem(Long id) {
        creditNoteLineItemRepository.deleteById(id);
    }
}
