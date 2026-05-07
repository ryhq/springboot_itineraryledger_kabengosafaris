package com.itineraryledger.kabengosafaris.Expense.DTOs;

import com.itineraryledger.kabengosafaris.Expense.Enums.ExpenseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/** All fields optional; null = leave unchanged. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateExpenseDTO {

    private String title;
    private String description;

    /** Re-link to a different vendor (rare). Pass obfuscated id. */
    private String vendorId;

    /**
     * Pass an obfuscated id to attach a safari, or the empty string ""
     * to detach (set safari to null). null in this DTO = leave unchanged.
     */
    private String safariId;

    private LocalDate expenseDate;
    private LocalDate dueDate;
    private String referenceNumber;
    private BigDecimal taxPercentage;
    private ExpenseStatus status;        // workflow transitions
    private String internalNotes;
    private Boolean isActive;
}
