package com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceServices;

import java.util.ArrayList;
import java.util.List;

import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Invoice.Enums.InvoiceStatus;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * InvoiceDeleteService - Service for deleting invoices
 *
 * Only allows deletion of DRAFT invoices to prevent accidental deletion
 * of sent or paid invoices.
 */
@Service
@Slf4j
@Transactional
public class InvoiceDeleteService {

    private final InvoiceRepository invoiceRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public InvoiceDeleteService(
        InvoiceRepository invoiceRepository,
        IdObfuscator idObfuscator
    ) {
        this.invoiceRepository = invoiceRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Delete invoices by list of obfuscated IDs
     *
     * Only DRAFT invoices can be deleted. Invoices in other states will be skipped.
     *
     * @param idObfuscatedList List of obfuscated invoice IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    public ResponseEntity<ApiResponse<?>> deleteInvoices(List<String> idObfuscatedList) {
        log.info("Attempting to delete {} invoices", idObfuscatedList.size());

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

            return deleteInvoicesInternal(ids);

        } catch (Exception e) {
            log.error("Error deleting invoices", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to delete invoices",
                    "INVOICES_DELETE_FAILED"
                )
            );
        }
    }

    /**
     * Delete invoices by list of IDs (internal method)
     */
    private ResponseEntity<ApiResponse<?>> deleteInvoicesInternal(List<Long> ids) {
        int deletedCount = 0;
        int skippedCount = 0;
        List<String> skippedReasons = new ArrayList<>();

        for (Long id : ids) {
            try {
                Invoice invoice = invoiceRepository.findById(id).orElse(null);

                if (invoice == null) {
                    log.warn("Invoice not found: {}", id);
                    skippedCount++;
                    skippedReasons.add(String.format("Invoice ID %d not found", id));
                    continue;
                }

                // Only allow deletion of DRAFT invoices
                if (invoice.getStatus() != InvoiceStatus.DRAFT) {
                    log.warn("Cannot delete invoice {} - status is {} (only DRAFT invoices can be deleted)",
                             invoice.getInvoiceCode(), invoice.getStatus().getDisplayName());
                    skippedCount++;
                    skippedReasons.add(String.format("Invoice %s cannot be deleted - status is %s (only DRAFT invoices can be deleted)",
                                                     invoice.getInvoiceCode(), invoice.getStatus().getDisplayName()));
                    continue;
                }

                // Use AopContext to get proxy and trigger AOP aspect
                ((InvoiceDeleteService) AopContext.currentProxy()).deleteInvoice(id);
                deletedCount++;
                log.info("Invoice deleted successfully: {} ({})", invoice.getInvoiceCode(), id);

            } catch (Exception e) {
                log.error("Error deleting invoice: {}", id, e);
                skippedCount++;
                skippedReasons.add(String.format("Error deleting invoice ID %d: %s", id, e.getMessage()));
            }
        }

        // Build response message
        StringBuilder message = new StringBuilder();
        if (deletedCount > 0) {
            message.append(deletedCount)
                   .append(deletedCount > 1 ? " invoices deleted successfully" : " invoice deleted successfully");
        }

        if (skippedCount > 0) {
            if (deletedCount > 0) {
                message.append(". ");
            }
            message.append(skippedCount)
                   .append(skippedCount > 1 ? " invoices skipped" : " invoice skipped");
        }

        // Return appropriate response
        if (deletedCount == 0 && skippedCount > 0) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    400,
                    message.toString() + ": " + String.join("; ", skippedReasons),
                    "NO_INVOICES_DELETED"
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
     * Delete a single invoice by ID (internal method with audit logging)
     *
     * @param id Invoice ID to delete
     */
    @AuditLogAnnotation(
        action = "DELETE_INVOICE",
        description = "Deleting invoice",
        entityType = "Invoice",
        entityIdParamName = "id"
    )
    public void deleteInvoice(Long id) {
        invoiceRepository.deleteById(id);
    }
}
