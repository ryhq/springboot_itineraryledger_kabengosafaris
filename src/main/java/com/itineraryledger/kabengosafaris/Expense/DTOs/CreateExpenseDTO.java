package com.itineraryledger.kabengosafaris.Expense.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateExpenseDTO {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotBlank(message = "Vendor is required")
    private String vendorId;             // obfuscated

    /** Optional. Null = operational expense (rent, insurance, etc). */
    private String safariId;             // obfuscated, nullable

    @NotNull(message = "Expense date is required")
    private LocalDate expenseDate;

    private LocalDate dueDate;

    private String referenceNumber;
    private BigDecimal taxPercentage;

    private String internalNotes;
    private Boolean isActive;
}
