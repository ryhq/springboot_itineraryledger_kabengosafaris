package com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices;

import java.util.ArrayList;
import java.util.List;

import org.springframework.aop.framework.AopContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for deleting quotes
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class QuoteDeleteService {

    private final QuoteRepository quoteRepository;
    private final IdObfuscator idObfuscator;

    /**
     * Delete quotes by list of obfuscated IDs
     *
     * @param idObfuscatedList List of obfuscated quote IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    public ResponseEntity<ApiResponse<?>> deleteQuotes(List<String> idObfuscatedList) {
        log.info("Deleting {} quotes", idObfuscatedList.size());

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

            return deleteQuotesInternal(ids);

        } catch (Exception e) {
            log.error("Error deleting quotes", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete quotes", "QUOTES_DELETE_FAILED")
            );
        }
    }

    /**
     * Delete quotes by list of IDs (internal method)
     */
    private ResponseEntity<ApiResponse<?>> deleteQuotesInternal(List<Long> ids) {
        int deletedCount = 0;

        for (Long id : ids) {
            try {
                Quote quote = quoteRepository.findById(id).orElse(null);

                if (quote == null) {
                    log.warn("Quote not found: {}", id);
                    continue;
                }

                // Use AopContext to get proxy and trigger AOP aspect
                ((QuoteDeleteService) AopContext.currentProxy()).deleteQuote(id);
                deletedCount++;
                log.info("Quote deleted successfully: {}", id);

            } catch (Exception e) {
                log.error("Error deleting quote: {}", id, e);
            }
        }

        return ResponseEntity.ok().body(
            ApiResponse.success(200, deletedCount + " quote(s) deleted successfully", null)
        );
    }

    @AuditLogAnnotation(
        action = "DELETE_QUOTE",
        description = "Deleting quote",
        entityType = "Quote",
        entityIdParamName = "id"
    )
    public void deleteQuote(Long id) {
        quoteRepository.deleteById(id);
    }
}
