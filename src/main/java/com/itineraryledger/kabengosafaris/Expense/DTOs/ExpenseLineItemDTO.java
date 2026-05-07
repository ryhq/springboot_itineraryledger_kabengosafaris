package com.itineraryledger.kabengosafaris.Expense.DTOs;

import com.itineraryledger.kabengosafaris.Expense.Enums.ExpenseCategory;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseLineItemDTO {

    private String id;
    private String expenseId;
    private String expenseCode;
    private ExpenseCategory category;
    private String categoryDisplayName;
    private String itemName;
    private String description;
    private Integer displayOrder;
    private List<Price> prices;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
