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
        int skippedCount = 0;
        List<String> errors = new ArrayList<>();

        for (Long id : ids) {
            try {
                Quote quote = quoteRepository.findById(id).orElse(null);

                if (quote == null) {
                    log.warn("Quote not found: {}", id);
                    skippedCount++;
                    errors.add("Quote with ID " + id + " not found");
                    continue;
                }

                // WORKFLOW ENFORCEMENT: Validate status before deletion
                switch (quote.getStatus()) {
                    case SENT:
                        log.warn("Cannot delete SENT quote: {} ({})", quote.getQuoteCode(), id);
                        skippedCount++;
                        errors.add(String.format("Cannot delete SENT quote '%s'. Cancel it first or create a new version.",
                            quote.getTitle()));
                        continue;

                    case ACCEPTED:
                        log.warn("Cannot delete ACCEPTED quote: {} ({})", quote.getQuoteCode(), id);
                        skippedCount++;
                        errors.add(String.format("Cannot delete ACCEPTED quote '%s'", quote.getTitle()));
                        continue;

                    case CONVERTED:
                        log.warn("Cannot delete CONVERTED quote: {} ({})", quote.getQuoteCode(), id);
                        skippedCount++;
                        errors.add(String.format("Cannot delete CONVERTED quote '%s' - it has been converted to a booking",
                            quote.getTitle()));
                        continue;

                    default:
                        // DRAFT, READY, REJECTED, EXPIRED, CANCELLED can be deleted
                        break;
                }

                // Use AopContext to get proxy and trigger AOP aspect
                ((QuoteDeleteService) AopContext.currentProxy()).deleteQuote(id);
                deletedCount++;
                log.info("Quote deleted successfully: {} (status: {})", quote.getQuoteCode(), quote.getStatus());

            } catch (Exception e) {
                log.error("Error deleting quote: {}", id, e);
                skippedCount++;
                errors.add("Error deleting quote: " + e.getMessage());
            }
        }

        String message = deletedCount > 1 ? " quotes deleted successfully" : " quote deleted successfully";

        if (skippedCount > 0) {
            message += ", " + skippedCount + " skipped";
        }

        // Return error if nothing was deleted
        if (deletedCount == 0 && !errors.isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    400,
                    String.join("; ", errors),
                    "DELETION_BLOCKED"
                )
            );
        }

        return ResponseEntity.ok().body(
            ApiResponse.success(
                200,
                deletedCount + message,
                errors.isEmpty() ? null : errors
            )
        );
    }

    @AuditLogAnnotation(
        action = "DELETE_QUOTE",
        description = "Deleting quote",
        entityType = "Quote",
        entityIdParamName = "id"
    )
    public void deleteQuote(Long id) {
        // Unlink any sibling version pointers that reference this quote.
        // Without this, the self-FK from quotes.previous_version_id /
        // quotes.next_version_id blocks the delete with a constraint
        // violation. JPA cascade can't help here — those are inverse
        // ManyToOne references, not owned children.
        quoteRepository.clearPreviousVersionRefs(id);
        quoteRepository.clearNextVersionRefs(id);
        quoteRepository.flush();

        quoteRepository.deleteById(id);
    }
}
