package com.itineraryledger.kabengosafaris.Expense.DTOs;

import com.itineraryledger.kabengosafaris.Expense.Enums.ExpenseStatus;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseDTO {

    private String id;
    private String expenseCode;
    private String title;
    private String description;

    // Vendor
    private String vendorId;
    private String vendorName;
    private String vendorType;

    // Safari (nullable for operational expenses)
    private String safariId;
    private String safariCode;
    private String safariName;

    // Multi-currency totals
    private List<Price> subtotals;
    private List<Price> taxes;
    private List<Price> grandTotals;
    private List<Price> amountsPaid;     // derived from ExpensePaymentAggregationService
    private List<Price> balances;        // derived

    private BigDecimal taxPercentage;

    private LocalDate expenseDate;
    private LocalDate dueDate;
    private String referenceNumber;

    private ExpenseStatus status;
    private String statusDisplayName;

    private String internalNotes;
    private Boolean isActive;
    private Boolean isOverdue;
    private Long lineItemCount;

    // Audit
    private String createdById;
    private String createdByName;
    private String updatedById;
    private String updatedByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
