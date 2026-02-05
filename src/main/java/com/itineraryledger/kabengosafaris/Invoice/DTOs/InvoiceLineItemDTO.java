package com.itineraryledger.kabengosafaris.Invoice.DTOs;

import java.time.LocalDateTime;
import java.util.List;

import com.itineraryledger.kabengosafaris.Invoice.Enums.InvoiceItemType;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for InvoiceLineItem responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceLineItemDTO {

    private String id;
    private String invoiceId;
    private String invoiceCode;

    private InvoiceItemType itemType;
    private String itemTypeDisplayName;

    private String itemName;
    private String description;
    private Integer displayOrder;
    private List<Price> prices;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
