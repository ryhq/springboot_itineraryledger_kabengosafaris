package com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices;

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
 * Takes a quote out of the working lists, or puts it back.
 *
 * This is the reversible half of delete, and the reason most quotes should never
 * be deleted: a quote is part of the history with a customer even when it came
 * to nothing.
 *
 * <p>Deliberately separate from the status lifecycle. Cancelled is an outcome
 * the customer is part of; inactive is only about whether the office still wants
 * to see the row. The status is untouched here, so a withdrawn quote reactivates
 * exactly where it left off.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class QuoteActivationService {

    private final QuoteRepository quoteRepository;
    private final IdObfuscator idObfuscator;

    @AuditLogAnnotation(action = "SET_QUOTE_ACTIVE", description = "Activating or withdrawing a quote",
        entityType = "Quote", entityIdParamName = "idObfuscated")
    public ResponseEntity<ApiResponse<?>> setActive(String idObfuscated, boolean active) {
        Long id;
        try {
            id = idObfuscator.decodeId(idObfuscated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid quote ID", "INVALID_QUOTE_ID")
            );
        }

        Quote quote = quoteRepository.findById(id).orElse(null);
        if (quote == null) {
            return ResponseEntity.status(404).body(
                ApiResponse.error(404, "Quote not found", "QUOTE_NOT_FOUND")
            );
        }

        if (Boolean.valueOf(active).equals(quote.getIsActive())) {
            // saying so beats a 200 that changed nothing
            return ResponseEntity.ok().body(ApiResponse.success(200,
                active ? "Quote is already active" : "Quote is already withdrawn", null));
        }

        quote.setIsActive(active);
        quoteRepository.save(quote);
        log.info("Quote {} {}", quote.getQuoteCode(), active ? "reactivated" : "deactivated");

        return ResponseEntity.ok().body(ApiResponse.success(200,
            active ? "Quote reactivated successfully" : "Quote deactivated successfully", null));
    }
}
