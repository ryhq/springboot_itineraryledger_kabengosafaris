package com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceLineItemServices;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.aop.framework.AopContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.CreditNote.Repository.CreditNoteLineItemRepository;
import com.itineraryledger.kabengosafaris.Invoice.Entity.InvoiceLineItem;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceLineItemRepository;
import com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceServices.InvoiceTotalsCalculationService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class InvoiceLineItemDeleteService {

    private final InvoiceLineItemRepository invoiceLineItemRepository;
    private final CreditNoteLineItemRepository creditNoteLineItemRepository;
    private final IdObfuscator idObfuscator;
    private final InvoiceTotalsCalculationService totalsCalculationService;

    public ResponseEntity<ApiResponse<?>> deleteInvoiceLineItems(String invoiceId, List<String> itemIds) {
        log.info("Deleting {} invoice line items", itemIds.size());

        try {
            List<Long> ids = new ArrayList<>();
            for (String idObfuscated : itemIds) {
                try {
                    ids.add(idObfuscator.decodeId(idObfuscated));
                } catch (Exception e) {
                    log.warn("Failed to decode ID: {}", idObfuscated, e);
                }
            }

            if (ids.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "No valid line item IDs provided", "INVALID_IDS"));
            }

            // Pre-check: any line item referenced by a credit note will fail the
            // FK constraint at delete time. Surface that as a clean 409 instead
            // of letting MySQL throw a generic constraint violation that turns
            // into "Server error" for the user.
            List<Long> blocked = creditNoteLineItemRepository.findReferencedInvoiceLineItemIds(ids);
            if (!blocked.isEmpty()) {
                String blockedCodes = blocked.stream()
                    .map(idObfuscator::encodeId)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
                String message = blocked.size() == 1
                    ? "This line item is referenced by a credit note and cannot be deleted. " +
                      "Update or delete the credit note first."
                    : blocked.size() + " of these line items are referenced by credit notes " +
                      "and cannot be deleted. Update or delete the credit notes first.";
                log.warn("Blocked delete: line items referenced by credit notes: {}", blockedCodes);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    ApiResponse.error(409, message, "INVOICE_LINE_ITEM_HAS_CREDIT_NOTE"));
            }

            return deleteInvoiceLineItemsInternal(ids);

        } catch (DataIntegrityViolationException e) {
            // Catch any FK violation we didn't pre-check (defensive) and turn
            // it into a meaningful error rather than the generic 500.
            log.error("Integrity violation deleting invoice line items", e);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiResponse.error(409,
                    "These line items are referenced elsewhere and cannot be deleted yet.",
                    "INVOICE_LINE_ITEM_REFERENCED"));
        } catch (Exception e) {
            log.error("Error deleting invoice line items", e);
            String detail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500,
                    "Failed to delete invoice line items: " + detail,
                    "INVOICE_LINE_ITEMS_DELETE_FAILED"));
        }
    }

    private ResponseEntity<ApiResponse<?>> deleteInvoiceLineItemsInternal(List<Long> ids) {
        int deletedCount = 0;
        Set<Long> affectedInvoiceIds = new HashSet<>();

        // Note: do NOT swallow per-item exceptions. If anything throws inside
        // the proxy-wrapped delete (FK violation, audit aspect failure, etc.)
        // Spring marks the whole transaction rollback-only — silently catching
        // here just produces a confusing UnexpectedRollbackException at commit.
        // Let the exception propagate so the outer try-catch can return a
        // meaningful error and the whole batch rolls back atomically.
        for (Long id : ids) {
            InvoiceLineItem lineItem = invoiceLineItemRepository.findById(id).orElse(null);

            if (lineItem == null) {
                log.warn("Invoice line item not found, skipping: {}", id);
                continue;
            }

            if (!lineItem.getInvoice().isEditable()) {
                log.warn("Invoice not editable, skipping line item delete: {}", id);
                continue;
            }

            affectedInvoiceIds.add(lineItem.getInvoice().getId());

            ((InvoiceLineItemDeleteService) AopContext.currentProxy()).deleteInvoiceLineItem(id);
            deletedCount++;
            log.info("Invoice line item deleted: {}", id);
        }

        if (deletedCount > 0) {
            for (Long invoiceId : affectedInvoiceIds) {
                renumberItemsAfterDeletion(invoiceId);
                totalsCalculationService.recalculateTotals(invoiceId);
            }
        }

        return ResponseEntity.ok().body(
            ApiResponse.success(200,
                deletedCount + " invoice line item(s) deleted successfully", null));
    }

    private void renumberItemsAfterDeletion(Long invoiceId) {
        List<InvoiceLineItem> items = invoiceLineItemRepository.findByInvoiceIdOrderByDisplayOrderAsc(invoiceId);

        for (InvoiceLineItem item : items) {
            item.setDisplayOrder(item.getDisplayOrder() + 10000);
        }
        invoiceLineItemRepository.saveAll(items);
        invoiceLineItemRepository.flush();

        for (int i = 0; i < items.size(); i++) {
            items.get(i).setDisplayOrder(i);
        }
        invoiceLineItemRepository.saveAll(items);
    }

    @AuditLogAnnotation(
        action = "DELETE_INVOICE_LINE_ITEM",
        description = "Deleting invoice line item",
        entityType = "InvoiceLineItem",
        entityIdParamName = "id"
    )
    public void deleteInvoiceLineItem(Long id) {
        invoiceLineItemRepository.deleteById(id);
    }
}
