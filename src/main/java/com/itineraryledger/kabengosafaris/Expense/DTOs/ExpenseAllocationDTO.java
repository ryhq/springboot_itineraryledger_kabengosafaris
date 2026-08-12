package com.itineraryledger.kabengosafaris.Expense.DTOs;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Expense.Enums.ExpenseSubjectType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One thing a bill covers, and enough about the bill to act on it.
 *
 * The day tree reads these to say "this stay is on 2 bills" and to offer them,
 * so the vendor, the status and the code travel with the link — otherwise every
 * chip on the page would need its own lookup.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExpenseAllocationDTO {

    private String id;

    private ExpenseSubjectType subjectType;
    private String subjectTypeDisplayName;
    /** obfuscated id of the day-object this covers */
    private String subjectId;
    private String safariDayId;
    private Integer dayNumber;
    private LocalDate dayDate;
    private String subjectName;

    private BigDecimal share;
    private String shareCurrency;
    private String note;

    /* the bill, so a chip can be read and followed without a second request */
    private String expenseId;
    private String expenseCode;
    private String expenseTitle;
    private String expenseStatus;
    private String expenseStatusDisplayName;
    private String vendorId;
    private String vendorName;
    private LocalDate dueDate;
    private Boolean isOverdue;

    /** What the bill is for in total, per currency. Never summed across them. */
    private List<com.itineraryledger.kabengosafaris.Quote.Embeddables.Price> grandTotals;
}
