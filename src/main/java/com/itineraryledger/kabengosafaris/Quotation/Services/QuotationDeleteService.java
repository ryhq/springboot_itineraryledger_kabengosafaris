package com.itineraryledger.kabengosafaris.Quotation.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Quotation.Entity.Quotation;
import com.itineraryledger.kabengosafaris.Quotation.Enums.QuotationStatus;
import com.itineraryledger.kabengosafaris.Quotation.Repository.QuotationRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * QuotationDeleteService - Service for deleting quotations
 */
@Service
@Slf4j
@Transactional
public class QuotationDeleteService {

    private final QuotationRepository quotationRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public QuotationDeleteService(
        QuotationRepository quotationRepository,
        IdObfuscator idObfuscator
    ) {
        this.quotationRepository = quotationRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Delete quotations by list of obfuscated IDs
     */
    public ResponseEntity<ApiResponse<?>> deleteQuotations(List<String> idObfuscatedList) {
        log.info("Deleting {} quotations", idObfuscatedList.size());

        try {
            List<Long> ids = new ArrayList<>();
            for (String idObfuscated : idObfuscatedList) {
                try {
                    Long id = idObfuscator.decodeId(idObfuscated);
                    ids.add(id);
                } catch (Exception e) {
                    log.warn("Failed to decode ID: {}", idObfuscated, e);
                }
            }

            return deleteQuotationsInternal(ids);

        } catch (Exception e) {
            log.error("Error deleting quotations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete quotations", "QUOTATIONS_DELETE_FAILED")
            );
        }
    }

    /**
     * Delete quotations by list of IDs (internal)
     */
    private ResponseEntity<ApiResponse<?>> deleteQuotationsInternal(List<Long> ids) {
        int deletedCount = 0;
        List<String> skippedCodes = new ArrayList<>();

        for (Long id : ids) {
            try {
                Quotation quotation = quotationRepository.findById(id).orElse(null);

                if (quotation == null) {
                    log.warn("Quotation not found: {}", id);
                    continue;
                }

                // Only DRAFT and CANCELLED quotations can be deleted
                if (quotation.getStatus() != QuotationStatus.DRAFT &&
                    quotation.getStatus() != QuotationStatus.CANCELLED) {
                    log.warn("Cannot delete quotation in status {}: {}", quotation.getStatus(), quotation.getCode());
                    skippedCodes.add(quotation.getCode());
                    continue;
                }

                // Use AopContext to get proxy and trigger AOP aspect
                ((QuotationDeleteService) AopContext.currentProxy()).deleteQuotation(id);
                deletedCount++;
                log.info("Quotation deleted successfully: {}", id);

            } catch (Exception e) {
                log.error("Error deleting quotation: {}", id, e);
            }
        }

        String message = deletedCount + " quotation(s) deleted successfully";
        if (!skippedCodes.isEmpty()) {
            message += ". Skipped (not in draft/cancelled status): " + String.join(", ", skippedCodes);
        }

        return ResponseEntity.ok().body(
            ApiResponse.success(200, message, null)
        );
    }

    @AuditLogAnnotation(
        action = "DELETE_QUOTATION",
        description = "Deleting quotation",
        entityType = "Quotation",
        entityIdParamName = "id"
    )
    public void deleteQuotation(Long id) {
        quotationRepository.deleteById(id);
    }
}
