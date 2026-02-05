package com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceLineItemServices;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.aop.framework.AopContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
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
    private final IdObfuscator idObfuscator;
    private final InvoiceTotalsCalculationService totalsCalculationService;

    public ResponseEntity<ApiResponse<?>> deleteInvoiceLineItems(String invoiceId, List<String> itemIds) {
        log.info("Deleting {} invoice line items", itemIds.size());

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

            return deleteInvoiceLineItemsInternal(ids);

        } catch (Exception e) {
            log.error("Error deleting invoice line items", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete invoice line items", "INVOICE_LINE_ITEMS_DELETE_FAILED")
            );
        }
    }

    private ResponseEntity<ApiResponse<?>> deleteInvoiceLineItemsInternal(List<Long> ids) {
        int deletedCount = 0;
        Set<Long> affectedInvoiceIds = new HashSet<>();

        for (Long id : ids) {
            try {
                InvoiceLineItem lineItem = invoiceLineItemRepository.findById(id).orElse(null);

                if (lineItem == null) {
                    log.warn("Invoice line item not found: {}", id);
                    continue;
                }

                // Check if invoice is editable
                if (!lineItem.getInvoice().isEditable()) {
                    log.warn("Cannot delete line item {} - invoice is not editable", id);
                    continue;
                }

                Long invoiceId = lineItem.getInvoice().getId();
                affectedInvoiceIds.add(invoiceId);

                ((InvoiceLineItemDeleteService) AopContext.currentProxy()).deleteInvoiceLineItem(id);
                deletedCount++;
                log.info("Invoice line item deleted successfully: {}", id);

            } catch (Exception e) {
                log.error("Error deleting invoice line item: {}", id, e);
            }
        }

        if (deletedCount > 0) {
            for (Long invoiceId : affectedInvoiceIds) {
                renumberItemsAfterDeletion(invoiceId);
                totalsCalculationService.recalculateTotals(invoiceId);
            }
        }

        return ResponseEntity.ok().body(
            ApiResponse.success(200, deletedCount + " invoice line item(s) deleted successfully", null)
        );
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
