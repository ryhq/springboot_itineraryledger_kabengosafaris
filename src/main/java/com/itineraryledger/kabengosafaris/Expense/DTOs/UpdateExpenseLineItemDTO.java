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
public class UpdateExpenseLineItemDTO {

    private ExpenseCategory category;
    private String itemName;
    private String description;

    @Valid
    private List<PriceInput> prices;

    private Boolean isActive;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PriceInput {
        @Size(min = 3, max = 3)
        private String currency;
        @Min(value = 1)
        private Integer quantity;
        @DecimalMin(value = "0.0", inclusive = true)
        private BigDecimal unitPrice;
        @Size(max = 500)
        private String breakdown;
    }
}
