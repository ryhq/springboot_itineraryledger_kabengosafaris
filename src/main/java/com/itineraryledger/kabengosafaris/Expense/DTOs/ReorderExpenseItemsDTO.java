package com.itineraryledger.kabengosafaris.Expense.DTOs;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReorderExpenseItemsDTO {

    /** Item ids (obfuscated) in the desired order — index becomes displayOrder. */
    @NotEmpty(message = "Order list cannot be empty")
    private List<String> itemIds;
}
