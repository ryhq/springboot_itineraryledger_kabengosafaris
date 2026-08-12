package com.itineraryledger.kabengosafaris.Expense.Entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.itineraryledger.kabengosafaris.Expense.Enums.ExpenseSubjectType;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * What one bill is paying for, on which day.
 *
 * Billing is many-to-many in both directions and always was: one invoice from
 * Outpost Lodge covers nights on days 1 and 2; one TANAPA reservation covers
 * Tarangire on day 3 and the Serengeti on day 8; and a single night can attract a
 * second bill later when the lodge charges for something it forgot. A field on
 * either side could say none of that, so the relation gets its own record.
 *
 * <p><b>Why the subject is a soft reference.</b> The day tree is edited for as
 * long as the trip runs — a lodge is swapped, a park visit is removed — but what
 * a bill paid for is a historical fact that must not change when it does. So the
 * id is kept without a foreign key and the label, the day number and the date are
 * snapshotted beside it. A removed stay leaves an allocation that still reads
 * "Outpost Lodge, day 1", which is the truth about the money even once the plan
 * has moved on.
 */
@Entity
@Table(
    name = "expense_allocations",
    indexes = {
        @Index(name = "idx_ea_expense", columnList = "expense_id"),
        @Index(name = "idx_ea_safari", columnList = "safari_id"),
        // the day tree asks "what on this safari is billed" in one query
        @Index(name = "idx_ea_subject", columnList = "subject_type,subject_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    /**
     * The trip this covers, kept alongside so the day tree can ask for every
     * allocation on a safari without walking its bills.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "safari_id")
    private Safari safari;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false, length = 32)
    private ExpenseSubjectType subjectType;

    /** Raw id of the day-object. Soft on purpose — see the class note. */
    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    /** The safari day it sits on, for grouping the coverage by day. */
    @Column(name = "safari_day_id")
    private Long safariDayId;

    private Integer dayNumber;

    private LocalDate dayDate;

    /** "Outpost Lodge", "Tarangire National Park", "4x4 Game Drive Safari". */
    @Column(length = 300)
    private String subjectName;

    /**
     * What of this bill belongs to this day, when somebody has bothered to say.
     *
     * A lodge invoice covering two nights is usually settled as one figure, and
     * splitting it is guesswork nobody needs. But when a share IS known — two of
     * five park permits on this day — recording it beats recomputing it later.
     */
    @Column(precision = 19, scale = 2)
    private BigDecimal share;

    @Column(length = 3)
    private String shareCurrency;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
