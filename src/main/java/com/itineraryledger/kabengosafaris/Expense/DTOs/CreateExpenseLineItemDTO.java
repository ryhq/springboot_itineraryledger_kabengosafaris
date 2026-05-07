package com.itineraryledger.kabengosafaris.Expense.DTOs;

import com.itineraryledger.kabengosafaris.Expense.Enums.ExpenseCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateExpenseLineItemDTO {

    @NotNull(message = "Category is required")
    private ExpenseCategory category;

    @NotBlank(message = "Item name is required")
    private String itemName;

    private String description;

    @NotEmpty(message = "At least one price is required")
    @Valid
    private List<PriceInput> prices;

    private Boolean isActive;

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

        @Size(max = 500)
        private String breakdown;
    }
}
