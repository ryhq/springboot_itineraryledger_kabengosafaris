package com.itineraryledger.kabengosafaris.Quote.Services.QuoteItemServices;

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
import com.itineraryledger.kabengosafaris.Quote.Entity.QuoteItem;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteItemRepository;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteTotalsCalculationService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for deleting quote items
 *
 * Provides bulk deletion of quote items with automatic renumbering.
 * After deletion, remaining items are renumbered to maintain sequential order (0, 1, 2, ...).
 * Uses a two-pass approach to avoid unique constraint violations on (quote_id, display_order).
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class QuoteItemDeleteService {

    private final QuoteItemRepository quoteItemRepository;
    private final IdObfuscator idObfuscator;
    private final QuoteTotalsCalculationService totalsCalculationService;

    /**
     * Delete quote items by list of obfuscated IDs
     *
     * @param idObfuscatedList List of obfuscated quote item IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    public ResponseEntity<ApiResponse<?>> deleteQuoteItems(List<String> idObfuscatedList) {
        log.info("Deleting {} quote items", idObfuscatedList.size());

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

            return deleteQuoteItemsInternal(ids);

        } catch (Exception e) {
            log.error("Error deleting quote items", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete quote items", "QUOTE_ITEMS_DELETE_FAILED")
            );
        }
    }

    /**
     * Delete quote items by list of IDs (internal method)
     */
    private ResponseEntity<ApiResponse<?>> deleteQuoteItemsInternal(List<Long> ids) {
        int deletedCount = 0;
        Set<Long> affectedQuoteIds = new HashSet<>();

        for (Long id : ids) {
            try {
                QuoteItem quoteItem = quoteItemRepository.findById(id).orElse(null);

                if (quoteItem == null) {
                    log.warn("Quote item not found: {}", id);
                    continue;
                }

                // Track which quotes are affected by deletions for renumbering
                Long quoteId = quoteItem.getQuote().getId();
                affectedQuoteIds.add(quoteId);

                // Use AopContext to get proxy and trigger AOP aspect
                ((QuoteItemDeleteService) AopContext.currentProxy()).deleteQuoteItem(id);
                deletedCount++;
                log.info("Quote item deleted successfully: {}", id);

            } catch (Exception e) {
                log.error("Error deleting quote item: {}", id, e);
            }
        }

        // Renumber remaining items for all affected quotes to maintain sequential order
        if (deletedCount > 0) {
            for (Long quoteId : affectedQuoteIds) {
                renumberItemsAfterDeletion(quoteId);
                // Recalculate quote totals after deleting items
                totalsCalculationService.recalculateTotals(quoteId);
            }
        }

        return ResponseEntity.ok().body(
            ApiResponse.success(200, deletedCount + " quote item(s) deleted successfully", null)
        );
    }

    /**
     * Renumber remaining items after deletion to maintain sequential display order.
     * Uses two-pass approach to avoid unique constraint violations on (quote_id, display_order).
     *
     * @param quoteId The quote ID
     */
    private void renumberItemsAfterDeletion(Long quoteId) {
        // Fetch remaining items ordered by current display order
        List<QuoteItem> remainingItems = quoteItemRepository.findByQuoteIdOrderByDisplayOrder(quoteId);

        if (remainingItems.isEmpty()) {
            return;
        }

        // Check if renumbering is needed (gaps in display order)
        boolean needsRenumbering = false;
        int expectedDisplayOrder = 0;
        for (QuoteItem item : remainingItems) {
            if (!item.getDisplayOrder().equals(expectedDisplayOrder)) {
                needsRenumbering = true;
                break;
            }
            expectedDisplayOrder++;
        }

        if (!needsRenumbering) {
            return;
        }

        log.info("Renumbering {} items for quote {}", remainingItems.size(), quoteId);

        // Pass 1: Set temporary negative display orders to avoid unique constraint violations
        int tempOrder = -1;
        for (QuoteItem item : remainingItems) {
            item.setDisplayOrder(tempOrder--);
        }
        quoteItemRepository.saveAll(remainingItems);
        quoteItemRepository.flush();

        // Pass 2: Set final sequential display orders
        int newDisplayOrder = 0;
        for (QuoteItem item : remainingItems) {
            item.setDisplayOrder(newDisplayOrder);
            newDisplayOrder++;
        }
        quoteItemRepository.saveAll(remainingItems);

        log.info("Items renumbered successfully for quote {}", quoteId);
    }

    @AuditLogAnnotation(
        action = "DELETE_QUOTE_ITEM",
        description = "Deleting quote item",
        entityType = "QuoteItem",
        entityIdParamName = "id"
    )
    public void deleteQuoteItem(Long id) {
        quoteItemRepository.deleteById(id);
    }
}
