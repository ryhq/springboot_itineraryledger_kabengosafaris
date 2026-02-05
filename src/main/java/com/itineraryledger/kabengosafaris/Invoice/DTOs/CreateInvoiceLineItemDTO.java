package com.itineraryledger.kabengosafaris.Invoice.DTOs;

import java.math.BigDecimal;
import java.util.List;

import com.itineraryledger.kabengosafaris.Invoice.Enums.InvoiceItemType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new InvoiceLineItem
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateInvoiceLineItemDTO {

    @NotNull(message = "Item type is required")
    private InvoiceItemType itemType;

    @NotBlank(message = "Item name is required")
    private String itemName;

    private String description;

    private Integer displayOrder;

    @NotEmpty(message = "At least one price is required")
    @Valid
    private List<PriceInput> prices;

    private Boolean isActive;

    /**
     * Inner class for price input when creating an invoice line item
     * totalPrice will be computed automatically: quantity × unitPrice
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PriceInput {

        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
        private String currency;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;

        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Unit price must be positive")
        private BigDecimal unitPrice;

        @Size(max = 500, message = "Breakdown must not exceed 500 characters")
        private String breakdown;
    }
}
