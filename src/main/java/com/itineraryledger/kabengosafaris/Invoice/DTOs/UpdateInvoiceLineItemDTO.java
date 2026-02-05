package com.itineraryledger.kabengosafaris.Invoice.DTOs;

import java.math.BigDecimal;
import java.util.List;

import com.itineraryledger.kabengosafaris.Invoice.Enums.InvoiceItemType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating an existing InvoiceLineItem
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateInvoiceLineItemDTO {

    private InvoiceItemType itemType;
    private String itemName;
    private String description;

    @Valid
    private List<PriceInput> prices;

    private Boolean isActive;

    /**
     * Inner DTO for price input when updating invoice line items.
     * Users only specify currency, quantity, unitPrice, and breakdown.
     * The totalPrice is computed automatically before save.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PriceInput {

        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter code (e.g., USD, TZS)")
        private String currency;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;

        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Unit price cannot be negative")
        private BigDecimal unitPrice;

        @Size(max = 500, message = "Breakdown cannot exceed 500 characters")
        private String breakdown;

        // Note: totalPrice is NOT included - it will be computed as quantity × unitPrice
    }
}
