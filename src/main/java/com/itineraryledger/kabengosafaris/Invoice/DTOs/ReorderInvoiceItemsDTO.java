package com.itineraryledger.kabengosafaris.Invoice.DTOs;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for reordering invoice line items
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReorderInvoiceItemsDTO {

    @NotNull(message = "Item order list cannot be null")
    @NotEmpty(message = "Item order list cannot be empty")
    private List<ItemOrder> itemOrders;

    /**
     * Nested class representing the order of a single item
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemOrder {
        @NotNull(message = "Item ID is required")
        private String itemId;

        @NotNull(message = "Display order is required")
        private Integer displayOrder;
    }
}
