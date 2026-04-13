package com.itineraryledger.kabengosafaris.CreditNote.DTOs;

import java.time.LocalDateTime;
import java.util.List;

import com.itineraryledger.kabengosafaris.CreditNote.Enums.CreditNoteItemType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for CreditNoteLineItem responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditNoteLineItemDTO {

    private String id;
    private String creditNoteId;

    /**
     * Obfuscated ID of the linked InvoiceLineItem, or null if this is an ad-hoc credit.
     */
    private String invoiceLineItemId;

    private CreditNoteItemType itemType;
    private String itemTypeDisplayName;

    private String itemName;
    private String description;
    private Integer displayOrder;
    private List<CreditNoteDTO.PriceDTO> prices;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
