package com.itineraryledger.kabengosafaris.Quotation.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * QuotationPaxDTO - Response DTO for quotation pax data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuotationPaxDTO {

    private String id; // Obfuscated ID
    private String quotationId;

    // Nation Category
    private String nationCategoryId;
    private String nationCategoryName;

    // Age Category
    private String ageCategoryId;
    private String ageCategoryName;
    private Integer ageCategoryMinAge;
    private Integer ageCategoryMaxAge;

    // Count and Pricing
    private Integer count;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;

    // Display
    private String displayName;

    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
