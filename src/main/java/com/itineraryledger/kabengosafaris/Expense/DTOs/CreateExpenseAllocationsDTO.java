package com.itineraryledger.kabengosafaris.Expense.DTOs;

import java.math.BigDecimal;
import java.util.List;

import com.itineraryledger.kabengosafaris.Expense.Enums.ExpenseSubjectType;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Attaching one bill to the things it pays for.
 *
 * A list rather than a single subject because that is the ordinary case: the
 * invoice from Outpost Lodge arrives once and covers both nights, so both are
 * said at once and nobody has to remember to come back for the second.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateExpenseAllocationsDTO {

    @NotEmpty(message = "At least one thing to cover is required")
    private List<Subject> subjects;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Subject {

        @NotNull(message = "Subject type is required")
        private ExpenseSubjectType subjectType;

        /** obfuscated id of the day-object */
        @NotNull(message = "Subject id is required")
        private String subjectId;

        private String safariDayId;
        private Integer dayNumber;
        private String dayDate;
        private String subjectName;

        /** optional: what of the bill belongs to this day */
        private BigDecimal share;
        private String shareCurrency;
        private String note;
    }
}
