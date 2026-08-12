package com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceServices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    /**
     * Deletes what it can and reports every id it did not, one by one.
     *
     * The house contract: {deletedCount, deletedIds, skipped:[{id, code, reason}]}
     * and a 200 even when nothing went — a caller that asked about six invoices
     * needs to know which four survived and why, not a sentence with the reasons
     * glued into it.
     */
    private ResponseEntity<ApiResponse<?>> deleteInvoicesInternal(List<Long> ids) {
        List<String> deletedIds = new ArrayList<>();
        List<Map<String, Object>> skipped = new ArrayList<>();

        for (Long id : ids) {
            String obfuscated = idObfuscator.encodeId(id);
            try {
                Invoice invoice = invoiceRepository.findById(id).orElse(null);

                if (invoice == null) {
                    skipped.add(skip(obfuscated, null, "No longer exists"));
                    continue;
                }

                /*
                 * Only a draft. Once an invoice has been sent, somebody outside
                 * this office is holding a copy of it, and deleting our half
                 * leaves a demand with no record behind it. Cancel instead.
                 */
                if (invoice.getStatus() != InvoiceStatus.DRAFT) {
                    skipped.add(skip(obfuscated, invoice.getInvoiceCode(),
                        "It is " + invoice.getStatus().getDisplayName().toLowerCase()
                            + ". Only a draft can be deleted — cancel it instead."));
                    continue;
                }

                // Use AopContext to get proxy and trigger AOP aspect
                ((InvoiceDeleteService) AopContext.currentProxy()).deleteInvoice(id);
                deletedIds.add(obfuscated);
                log.info("Invoice deleted successfully: {} ({})", invoice.getInvoiceCode(), id);

            } catch (Exception e) {
                log.error("Error deleting invoice: {}", id, e);
                skipped.add(skip(obfuscated, null, "Could not be deleted: " + e.getMessage()));
            }
        }

        Map<String, Object> report = new HashMap<>();
        report.put("deletedCount", deletedIds.size());
        report.put("deletedIds", deletedIds);
        report.put("skipped", skipped);

        String message = deletedIds.size() + (deletedIds.size() == 1 ? " invoice deleted" : " invoices deleted")
            + (skipped.isEmpty() ? "" : ", " + skipped.size() + " skipped");

        return ResponseEntity.ok().body(ApiResponse.success(200, message, report));
    }

    private Map<String, Object> skip(String id, String code, String reason) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("id", id);
        entry.put("code", code);
        entry.put("reason", reason);
        return entry;
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
