package com.itineraryledger.kabengosafaris.Quotation.DTOs;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * CreateQuotationPaxDTO - Request DTO for adding pax configuration to a quotation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateQuotationPaxDTO {

    @NotBlank(message = "Nation category ID is required")
    private String nationCategoryId; // Obfuscated ID

    @NotBlank(message = "Age category ID is required")
    private String ageCategoryId; // Obfuscated ID

    @Min(value = 1, message = "Count must be at least 1")
    @Builder.Default
    private Integer count = 1;

    private BigDecimal unitPrice;

    private String notes;
}
