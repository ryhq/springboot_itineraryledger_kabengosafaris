package com.itineraryledger.kabengosafaris.Quote.Services.QuoteItemServices;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Quote.DTOs.QuoteItemDTOs.QuoteItemDTO;
import com.itineraryledger.kabengosafaris.Quote.DTOs.QuoteItemDTOs.ReorderQuoteItemsDTO;
import com.itineraryledger.kabengosafaris.Quote.DTOs.QuoteItemDTOs.ReorderQuoteItemsDTO.QuoteItemOrderItem;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.Entity.QuoteItem;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteItemRepository;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * QuoteItemReorderService - Service for reordering quote items
 *
 * Handles drag-and-drop reordering from the UI with comprehensive validations:
 * - Validates all item IDs exist and belong to the quote
 * - Validates no duplicate item IDs
 * - Validates all items are included (no missing items)
 * - Validates expected display orders if provided
 * - Updates display order for all items
 */
@Service
@Slf4j
@Transactional
public class QuoteItemReorderService {

    private final QuoteRepository quoteRepository;
    private final QuoteItemRepository quoteItemRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public QuoteItemReorderService(
        QuoteRepository quoteRepository,
        QuoteItemRepository quoteItemRepository,
        IdObfuscator idObfuscator
    ) {
        this.quoteRepository = quoteRepository;
        this.quoteItemRepository = quoteItemRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Reorder quote items based on the new order provided
     *
     * @param quoteIdObfuscated The obfuscated quote ID
     * @param reorderDTO The reorder data containing the new item order
     * @return ResponseEntity with ApiResponse containing the reordered items
     */
    @AuditLogAnnotation(action = "REORDER_QUOTE_ITEMS", description = "Reordering quote items", entityType = "QuoteItem")
    public ResponseEntity<ApiResponse<?>> reorderQuoteItems(
        String quoteIdObfuscated,
        ReorderQuoteItemsDTO reorderDTO
    ) {
        log.info("Reordering items for quote: {}", quoteIdObfuscated);

        try {
            // ========================
            // DECODE QUOTE ID
            // ========================
            Long quoteId;
            try {
                quoteId = idObfuscator.decodeId(quoteIdObfuscated);
            } catch (Exception e) {
                log.warn("Invalid quote ID format: {}", quoteIdObfuscated);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid quote ID format", "INVALID_QUOTE_ID")
                );
            }

            // ========================
            // FIND QUOTE
            // ========================
            Quote quote = quoteRepository.findById(quoteId).orElse(null);
            if (quote == null) {
                log.warn("Quote not found: {}", quoteId);
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Quote not found", "QUOTE_NOT_FOUND")
                );
            }

            // ========================
            // FETCH EXISTING ITEMS
            // ========================
            List<QuoteItem> existingItems = quoteItemRepository.findByQuoteIdOrderByDisplayOrder(quoteId);

            if (existingItems.isEmpty()) {
                log.warn("No items found for quote: {}", quoteId);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Quote has no items to reorder", "NO_ITEMS_TO_REORDER")
                );
            }

            // ========================
            // VALIDATION: Check item order list size matches existing items
            // ========================
            List<QuoteItemOrderItem> itemOrder = reorderDTO.getItemOrder();

            if (itemOrder.size() != existingItems.size()) {
                log.warn("Item order count mismatch. Expected: {}, Received: {}", existingItems.size(), itemOrder.size());
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Item order list must contain exactly " + existingItems.size() + " items. Received: " + itemOrder.size(),
                        "ITEM_COUNT_MISMATCH"
                    )
                );
            }

            // ========================
            // DECODE ALL ITEM IDs AND VALIDATE FORMAT
            // ========================
            Map<Long, QuoteItemOrderItem> decodedItemIds = new LinkedHashMap<>();
            List<String> invalidIds = new ArrayList<>();
            List<String> duplicateIds = new ArrayList<>();

            for (QuoteItemOrderItem item : itemOrder) {
                if (item.getItemId() == null || item.getItemId().isBlank()) {
                    invalidIds.add("null/empty");
                    continue;
                }

                try {
                    Long decodedId = idObfuscator.decodeId(item.getItemId());

                    // Check for duplicates
                    if (decodedItemIds.containsKey(decodedId)) {
                        duplicateIds.add(item.getItemId());
                    } else {
                        decodedItemIds.put(decodedId, item);
                    }
                } catch (Exception e) {
                    invalidIds.add(item.getItemId());
                }
            }

            if (!invalidIds.isEmpty()) {
                log.warn("Invalid item ID formats: {}", invalidIds);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid item ID format(s): " + String.join(", ", invalidIds),
                        "INVALID_ITEM_ID_FORMAT"
                    )
                );
            }

            if (!duplicateIds.isEmpty()) {
                log.warn("Duplicate item IDs in reorder list: {}", duplicateIds);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Duplicate item ID(s) in reorder list: " + String.join(", ", duplicateIds),
                        "DUPLICATE_ITEM_IDS"
                    )
                );
            }

            // ========================
            // VALIDATION: All items belong to this quote
            // ========================
            Set<Long> existingItemIds = existingItems.stream()
                .map(QuoteItem::getId)
                .collect(Collectors.toSet());

            Set<Long> providedItemIds = decodedItemIds.keySet();

            // Check for items that don't belong to this quote
            Set<Long> foreignItems = new HashSet<>(providedItemIds);
            foreignItems.removeAll(existingItemIds);

            if (!foreignItems.isEmpty()) {
                List<String> foreignItemObfuscated = foreignItems.stream()
                    .map(id -> idObfuscator.encodeId(id))
                    .collect(Collectors.toList());
                log.warn("Item IDs not belonging to quote: {}", foreignItemObfuscated);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Item ID(s) do not belong to this quote: " + String.join(", ", foreignItemObfuscated),
                        "ITEM_QUOTE_MISMATCH"
                    )
                );
            }

            // Check for missing items
            Set<Long> missingItems = new HashSet<>(existingItemIds);
            missingItems.removeAll(providedItemIds);

            if (!missingItems.isEmpty()) {
                List<String> missingItemObfuscated = missingItems.stream()
                    .map(id -> idObfuscator.encodeId(id))
                    .collect(Collectors.toList());
                log.warn("Missing item IDs in reorder list: {}", missingItemObfuscated);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Missing item ID(s) in reorder list: " + String.join(", ", missingItemObfuscated),
                        "MISSING_ITEM_IDS"
                    )
                );
            }

            // ========================
            // VALIDATION: Expected display orders (if provided)
            // ========================
            /*
             * `expectedDisplayOrder` is what the CALLER believes the item's order
             * is right now — an optimistic-concurrency check, so a reorder built
             * from a stale list is refused instead of silently reshuffling
             * somebody else's arrangement.
             *
             * It used to be compared against the item's index in the submitted
             * list, which is a tautology: it could only fail if the caller
             * miscounted the list it had just built. Every honest client failed
             * it, because a caller numbering positions from 1 was told
             * "expected 1, but position is 0".
             */
            List<String> expectedOrderMismatches = new ArrayList<>();
            Map<Long, QuoteItem> byId = existingItems.stream()
                .collect(Collectors.toMap(QuoteItem::getId, item -> item));

            for (QuoteItemOrderItem item : itemOrder) {
                if (item.getExpectedDisplayOrder() == null) continue;
                Long rawId;
                try {
                    rawId = idObfuscator.decodeId(item.getItemId());
                } catch (Exception e) {
                    continue; // the id checks below report this properly
                }
                QuoteItem existing = byId.get(rawId);
                if (existing == null) continue;
                Integer stored = existing.getDisplayOrder();
                if (stored != null && !item.getExpectedDisplayOrder().equals(stored)) {
                    expectedOrderMismatches.add(
                        String.format("Item %s: caller expected it at %d, but it is at %d — the list has moved on",
                            item.getItemId(), item.getExpectedDisplayOrder(), stored)
                    );
                }
            }

            if (!expectedOrderMismatches.isEmpty()) {
                log.warn("Expected display order mismatches: {}", expectedOrderMismatches);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Expected display order mismatches: " + String.join("; ", expectedOrderMismatches),
                        "EXPECTED_ORDER_MISMATCH"
                    )
                );
            }

            // ========================
            // CREATE ITEM LOOKUP MAP
            // ========================
            Map<Long, QuoteItem> itemLookup = existingItems.stream()
                .collect(Collectors.toMap(QuoteItem::getId, item -> item));

            // ========================
            // CHECK IF REORDER IS ACTUALLY NEEDED
            // ========================
            boolean orderChanged = false;
            // 1-based, matching what create and the estimator write
            int newDisplayOrder = 1;

            for (Long itemId : decodedItemIds.keySet()) {
                QuoteItem item = itemLookup.get(itemId);
                if (!item.getDisplayOrder().equals(newDisplayOrder)) {
                    orderChanged = true;
                    break;
                }
                newDisplayOrder++;
            }

            if (!orderChanged) {
                log.info("Item order unchanged for quote: {}", quote.getQuoteCode());
                List<QuoteItemDTO> resultDTOs = existingItems.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

                return ResponseEntity.ok().body(
                    ApiResponse.success(200, "Item order unchanged", resultDTOs)
                );
            }

            // ========================
            // PERFORM REORDER
            // ========================
            // Use a two-pass approach to avoid unique constraint violations:
            // 1. First, set all display orders to negative (temporary)
            // 2. Then, set to the final positive values

            log.info("Performing reorder for {} items", existingItems.size());

            // Pass 1: Set temporary negative display orders
            int tempOrder = -1;
            for (QuoteItem item : existingItems) {
                item.setDisplayOrder(tempOrder--);
            }
            quoteItemRepository.saveAll(existingItems);
            quoteItemRepository.flush(); // Ensure changes are persisted

            // Pass 2: Set final display orders based on new order
            List<QuoteItem> reorderedItems = new ArrayList<>();
            newDisplayOrder = 1;

            for (Long itemId : decodedItemIds.keySet()) {
                QuoteItem item = itemLookup.get(itemId);
                item.setDisplayOrder(newDisplayOrder);
                reorderedItems.add(item);
                newDisplayOrder++;
            }

            quoteItemRepository.saveAll(reorderedItems);

            log.info("Reorder completed successfully for quote: {}", quote.getQuoteCode());

            // ========================
            // CONVERT TO DTOs AND RETURN
            // ========================
            List<QuoteItemDTO> resultDTOs = reorderedItems.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Quote items reordered successfully", resultDTOs)
            );

        } catch (Exception e) {
            log.error("Error reordering quote items", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to reorder quote items", "QUOTE_ITEMS_REORDER_FAILED")
            );
        }
    }

    /**
     * Convert QuoteItem entity to QuoteItemDTO
     */
    private QuoteItemDTO convertToDTO(QuoteItem quoteItem) {
        return QuoteItemDTO.builder()
            .id(idObfuscator.encodeId(quoteItem.getId()))
            .quoteId(idObfuscator.encodeId(quoteItem.getQuote().getId()))
            .quoteCode(quoteItem.getQuote().getQuoteCode())
            .itemType(quoteItem.getItemType())
            .itemName(quoteItem.getItemName())
            .description(quoteItem.getDescription())
            .displayOrder(quoteItem.getDisplayOrder())
            .prices(quoteItem.getPrices())
            .isActive(quoteItem.getIsActive())
            .createdAt(quoteItem.getCreatedAt())
            .updatedAt(quoteItem.getUpdatedAt())
            .build();
    }
}
