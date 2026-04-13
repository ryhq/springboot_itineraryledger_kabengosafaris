package com.itineraryledger.kabengosafaris.CreditNote.DTOs;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for cloning InvoiceLineItems into CreditNoteLineItems.
 *
 * Each provided invoiceLineItemId must belong to the credit note's invoice.
 * The cloned CreditNoteLineItem copies itemName, description, prices, and
 * sets invoiceLineItem FK so the credit traces back to the billed item.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CloneInvoiceLineItemsDTO {

    @NotEmpty(message = "At least one invoice line item ID is required")
    private List<String> invoiceLineItemIds;

    /**
     * When true, bypasses the over-credit guard. Default false.
     */
    private Boolean force;
}
