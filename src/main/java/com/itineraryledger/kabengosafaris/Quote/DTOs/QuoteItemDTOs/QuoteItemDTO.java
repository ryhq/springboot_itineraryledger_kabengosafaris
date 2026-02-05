package com.itineraryledger.kabengosafaris.Quote.DTOs.QuoteItemDTOs;

import java.time.LocalDateTime;
import java.util.List;

import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import com.itineraryledger.kabengosafaris.Quote.Enums.QuoteItemType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for QuoteItem responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuoteItemDTO {

    private String id;
    private String quoteId;
    private String quoteCode;

    private QuoteItemType itemType;
    private String itemTypeDisplayName;

    private String itemName;
    private String description;
    private Integer displayOrder;

    private List<Price> prices;

    private Boolean isActive;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
