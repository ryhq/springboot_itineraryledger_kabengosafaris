package com.itineraryledger.kabengosafaris.CreditNote.DTOs;

import java.math.BigDecimal;
import java.util.List;

import com.itineraryledger.kabengosafaris.CreditNote.Enums.CreditNoteItemType;

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
 * DTO for creating a new CreditNoteLineItem
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCreditNoteLineItemDTO {

    @NotNull(message = "Item type is required")
    private CreditNoteItemType itemType;

    @NotBlank(message = "Item name is required")
    private String itemName;

    private String description;

    /**
     * Optional obfuscated ID of the InvoiceLineItem being credited.
     * When provided, the referenced line item must belong to the credit note's invoice.
     * Omit for ad-hoc credits that don't correspond to a specific billed item.
     */
    private String invoiceLineItemId;

    /**
     * When true, bypasses the over-credit guard that prevents total credits
     * across all credit notes for an invoice from exceeding the invoice's
     * grand total. Default false — reject if adding this line would push
     * total credits past what's left on the invoice.
     */
    private Boolean force;

    @NotEmpty(message = "At least one price is required")
    @Valid
    private List<PriceInput> prices;

    private Boolean isActive;

    /**
     * Inner class for price input when creating a credit note line item.
     * totalPrice will be computed automatically: quantity x unitPrice
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
    }
}
