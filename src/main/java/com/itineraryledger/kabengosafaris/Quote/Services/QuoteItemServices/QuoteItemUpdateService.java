package com.itineraryledger.kabengosafaris.Quote.Services.QuoteItemServices;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Quote.DTOs.QuoteItemDTOs.CreateQuoteItemDTO;
import com.itineraryledger.kabengosafaris.Quote.DTOs.QuoteItemDTOs.QuoteItemDTO;
import com.itineraryledger.kabengosafaris.Quote.DTOs.QuoteItemDTOs.UpdateQuoteItemDTO;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import com.itineraryledger.kabengosafaris.Quote.Entity.QuoteItem;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteItemRepository;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteTotalsCalculationService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for updating quote items
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class QuoteItemUpdateService {

    private final QuoteItemRepository quoteItemRepository;
    private final IdObfuscator idObfuscator;
    private final QuoteTotalsCalculationService totalsCalculationService;

    @AuditLogAnnotation(
        action = "UPDATE_QUOTE_ITEM",
        description = "Updating a quote item",
        entityType = "QuoteItem",
        entityIdParamName = "idObfuscated"
    )
    public ResponseEntity<ApiResponse<?>> updateQuoteItem(String idObfuscated, UpdateQuoteItemDTO updateDTO) {
        log.info("Updating quote item with ID: {}", idObfuscated);

        try {
            // Decode ID
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode quote item ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid quote item ID", "INVALID_QUOTE_ITEM_ID")
                );
            }

            // Find quote item
            QuoteItem quoteItem = quoteItemRepository.findById(id).orElse(null);
            if (quoteItem == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Quote item not found", "QUOTE_ITEM_NOT_FOUND")
                );
            }

            // Update fields if provided
            if (updateDTO.getItemType() != null) {
                quoteItem.setItemType(updateDTO.getItemType());
            }
            if (updateDTO.getItemName() != null) {
                quoteItem.setItemName(updateDTO.getItemName());
            }
            if (updateDTO.getDescription() != null) {
                quoteItem.setDescription(updateDTO.getDescription());
            }
            // Note: displayOrder is NOT updated here - use the dedicated reorder service instead
            if (updateDTO.getPrices() != null) {
                // Validate that at least one price is provided
                if (updateDTO.getPrices().isEmpty()) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "At least one price is required. Cannot save item without any prices.", "PRICE_REQUIRED")
                    );
                }

                // Validate for duplicate currencies
                Set<String> currencies = new HashSet<>();
                for (UpdateQuoteItemDTO.PriceInput priceInput : updateDTO.getPrices()) {
                    String currency = priceInput.getCurrency().toUpperCase();
                    if (!currencies.add(currency)) {
                        return ResponseEntity.badRequest().body(
                            ApiResponse.error(400,
                                "Duplicate currency detected: " + currency + ". Please merge prices for the same currency before submitting.",
                                "DUPLICATE_CURRENCY")
                        );
                    }
                }

                // Convert PriceInput objects to Price objects with computed totalPrice
                List<Price> prices = new ArrayList<>();
                if (!updateDTO.getPrices().isEmpty()) {
                    prices = updateDTO.getPrices().stream()
                        .map(priceInput -> {
                            // Compute totalPrice = quantity × unitPrice
                            BigDecimal totalPrice = priceInput.getUnitPrice()
                                .multiply(BigDecimal.valueOf(priceInput.getQuantity()));

                            return Price.builder()
                                .currency(priceInput.getCurrency())
                                .quantity(priceInput.getQuantity())
                                .unitPrice(priceInput.getUnitPrice())
                                .totalPrice(totalPrice)
                                .breakdown(priceInput.getBreakdown())
                                .build();
                        })
                        .collect(Collectors.toList());
                }
                quoteItem.setPrices(prices);
            }
            if (updateDTO.getIsActive() != null) {
                quoteItem.setIsActive(updateDTO.getIsActive());
            }

            // Save updated quote item
            quoteItem = quoteItemRepository.save(quoteItem);

            // Recalculate quote totals after updating item
            totalsCalculationService.recalculateTotals(quoteItem.getQuote().getId());

            // Convert to DTO
            QuoteItemDTO quoteItemDTO = convertToDTO(quoteItem);

            log.info("Quote item updated successfully: {}", quoteItem.getItemName());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Quote item updated successfully", quoteItemDTO)
            );

        } catch (Exception e) {
            log.error("Error updating quote item", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update quote item", "QUOTE_ITEM_UPDATE_FAILED")
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

    /**
     * Merge prices with the same currency by summing their total prices.
     * If multiple price entries exist for the same currency, they are combined into one.
     *
     * Example:
     * Input: [TZS qty=3 unit=10000, USD qty=2 unit=100, TZS qty=1 unit=5000]
     * Output: [TZS qty=1 unit=35000 total=35000, USD qty=1 unit=200 total=200]
     *
     * @param priceInputs List of price inputs from user
     * @return List of merged Price objects
     */
    public List<Price> mergePricesByCurrency(List<CreateQuoteItemDTO.PriceInput> priceInputs) {
        // Use LinkedHashMap to preserve insertion order
        Map<String, PriceMergeData> currencyMap = new LinkedHashMap<>();

        // Group by currency and accumulate totals
        for (CreateQuoteItemDTO.PriceInput priceInput : priceInputs) {
            String currency = priceInput.getCurrency().toUpperCase();
            BigDecimal totalPrice = priceInput.getUnitPrice()
                .multiply(BigDecimal.valueOf(priceInput.getQuantity()));

            currencyMap.compute(currency, (key, existing) -> {
                if (existing == null) {
                    return new PriceMergeData(totalPrice, priceInput.getBreakdown());
                } else {
                    // Merge: sum total prices and combine breakdowns
                    BigDecimal newTotal = existing.totalPrice.add(totalPrice);
                    String combinedBreakdown = combineBreakdowns(existing.breakdown, priceInput.getBreakdown());
                    return new PriceMergeData(newTotal, combinedBreakdown);
                }
            });
        }

        // Convert merged data to Price objects
        return currencyMap.entrySet().stream()
            .map(entry -> Price.builder()
                .currency(entry.getKey())
                .quantity(1)
                .unitPrice(entry.getValue().totalPrice)
                .totalPrice(entry.getValue().totalPrice)
                .breakdown(entry.getValue().breakdown)
                .build())
            .collect(Collectors.toList());
    }

    /**
     * Combine breakdown strings from multiple prices
     */
    private String combineBreakdowns(String breakdown1, String breakdown2) {
        if (breakdown1 == null || breakdown1.trim().isEmpty()) {
            return breakdown2;
        }
        if (breakdown2 == null || breakdown2.trim().isEmpty()) {
            return breakdown1;
        }
        return breakdown1 + "; " + breakdown2;
    }

    /**
     * Helper class to accumulate price data during merge
     */
    private static class PriceMergeData {
        BigDecimal totalPrice;
        String breakdown;

        PriceMergeData(BigDecimal totalPrice, String breakdown) {
            this.totalPrice = totalPrice;
            this.breakdown = breakdown;
        }
    }
}
