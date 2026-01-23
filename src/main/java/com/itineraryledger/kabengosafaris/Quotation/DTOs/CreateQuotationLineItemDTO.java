package com.itineraryledger.kabengosafaris.Quotation.DTOs;

import com.itineraryledger.kabengosafaris.Quotation.Enums.LineItemType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * CreateQuotationLineItemDTO - Request DTO for adding a line item to a quotation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateQuotationLineItemDTO {

    /**
     * Day number this item belongs to (null or 0 for overall items)
     */
    private Integer dayNumber;

    private Integer sortOrder;

    @NotNull(message = "Item type is required")
    private LineItemType itemType;

    @NotBlank(message = "Item name is required")
    @Size(max = 500, message = "Item name must not exceed 500 characters")
    private String itemName;

    private String description;

    /**
     * Reference to source entity (obfuscated ID)
     */
    private String referenceId;

    /**
     * Entity type name (e.g., "Accommodation", "Park", "Activity")
     */
    private String referenceType;

    @Min(value = 1, message = "Quantity must be at least 1")
    @Builder.Default
    private Integer quantity = 1;

    private String unitOfMeasure;

    @NotNull(message = "Unit price is required")
    private BigDecimal unitPrice;

    private String currency;

    @Builder.Default
    private Boolean taxable = true;

    @Builder.Default
    private Boolean isIncluded = true;

    @Builder.Default
    private Boolean isOptional = false;

    private String notes;
}
