package com.itineraryledger.kabengosafaris.Quote.DTOs.QuoteItemDTOs;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ReorderQuoteItemsDTO - Data Transfer Object for reordering quote items
 *
 * Used when the UI performs drag-and-drop reordering of quote items.
 * The order list should contain ALL quote item IDs in the new desired order.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReorderQuoteItemsDTO {

    /**
     * List of quote item reorder items representing the new order.
     * The position in the list determines the new displayOrder (0-indexed).
     */
    @NotNull(message = "Quote item order list is required")
    @NotEmpty(message = "Quote item order list cannot be empty")
    @Valid
    private List<QuoteItemOrderItem> itemOrder;

    /**
     * Inner class representing a single quote item's position in the new order
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuoteItemOrderItem {

        @NotNull(message = "Quote item ID is required")
        private String itemId; // Obfuscated quote item ID

        /**
         * Optional: The expected new display order (for validation/confirmation)
         * If provided, it will be validated against the position in the list
         */
        private Integer expectedDisplayOrder;
    }
}
