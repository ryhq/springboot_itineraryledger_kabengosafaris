package com.itineraryledger.kabengosafaris.Quotation.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Quotation.Enums.LineItemType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * QuotationLineItemDTO - Response DTO for quotation line item data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuotationLineItemDTO {

    private String id; // Obfuscated ID
    private String quotationId;

    // Organization
    private Integer dayNumber;
    private Integer sortOrder;

    // Item Details
    private LineItemType itemType;
    private String itemTypeDisplayName;
    private String itemTypeDescription;
    private String itemName;
    private String description;

    // Reference
    private String referenceId;
    private String referenceType;

    // Pricing
    private Integer quantity;
    private String unitOfMeasure;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String currency;

    // Flags
    private Boolean taxable;
    private Boolean isIncluded;
    private Boolean isOptional;

    // Display
    private String displayLine;

    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
