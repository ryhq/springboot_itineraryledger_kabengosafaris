package com.itineraryledger.kabengosafaris.Expense.Specifications;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.itineraryledger.kabengosafaris.Expense.Enums.ExpenseCategory;
import com.itineraryledger.kabengosafaris.Expense.Enums.ExpenseStatus;

import lombok.Data;

/**
 * Everything a caller can narrow the expense list by, in one object.
 *
 * The rows, the stat cards and the record walk are all built from this, so a card
 * cannot report a figure the table would contradict, and prev/next cannot wander
 * out of the set on screen. Spring binds it from the query string with
 * {@code @ModelAttribute}, so every parameter the old signature took is still
 * spelled the same on the wire.
 */
@Data
public class ExpenseFilter {

    /** Free text across the code, the title, the reference and the vendor's name. */
    private String keyword;

    private String expenseCode;
    private String title;
    private String referenceNumber;

    private ExpenseStatus status;
    private List<ExpenseStatus> statuses;

    /**
     * What the money was spent ON.
     *
     * Category lives on the line items, not the bill, because one invoice from a
     * lodge can carry a room charge and a park fee. Filtering by it therefore
     * asks "does this bill have any line of that kind".
     */
    private List<ExpenseCategory> categories;

    /** Obfuscated ids, as the list page sends them. */
    private String vendorId;
    private String safariId;
    private String createdById;

    /** Running costs rather than a trip's: rent, salaries, a service. */
    private Boolean operationalOnly;

    private Boolean isActive;
    private Boolean isOverdue;

    /**
     * Actionable states, the ones the office chases.
     *
     * unpaid — money still owed to somebody. overdue — past the date they asked
     * for it. dueSoon — falls due inside a week. unsettled covers both of the
     * first two, which is the weekly "who do we owe" question.
     */
    private List<String> qualities;

    private LocalDate expenseDateAfter;
    private LocalDate expenseDateBefore;
    private LocalDate dueDateAfter;
    private LocalDate dueDateBefore;

    /** The statuses asked for, however they were spelled. */
    public List<ExpenseStatus> allStatuses() {
        List<ExpenseStatus> out = new ArrayList<>();
        if (statuses != null) statuses.stream().filter(Objects::nonNull).forEach(out::add);
        if (status != null && !out.contains(status)) out.add(status);
        return out;
    }

    public boolean wants(String quality) {
        return qualities != null && qualities.contains(quality);
    }
}
