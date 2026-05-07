package com.itineraryledger.kabengosafaris.Expense.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Aggregate of expense + line items + payments — single round-trip for UI detail page. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FullExpenseDTO {
    private ExpenseDTO expense;
    private List<ExpenseLineItemDTO> lineItems;
    private List<ExpensePaymentDTO> payments;
}
