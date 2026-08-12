package com.itineraryledger.kabengosafaris.Expense.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.itineraryledger.kabengosafaris.Expense.Entity.ExpenseAllocation;
import com.itineraryledger.kabengosafaris.Expense.Enums.ExpenseSubjectType;

public interface ExpenseAllocationRepository extends JpaRepository<ExpenseAllocation, Long> {

    List<ExpenseAllocation> findByExpenseIdOrderByDayNumberAsc(Long expenseId);

    /**
     * Every allocation on one safari, with its bill.
     *
     * The day tree needs to mark what is already billed, and it needs the answer
     * for the whole trip in ONE request — a fourteen-day itinerary has dozens of
     * billable things and asking per row would be dozens of round trips.
     */
    @Query("""
        select a from ExpenseAllocation a
        join fetch a.expense e
        left join fetch e.vendor v
        where a.safari.id = :safariId
        order by a.dayNumber asc, a.id asc
        """)
    List<ExpenseAllocation> findBySafariWithExpense(@Param("safariId") Long safariId);

    boolean existsByExpenseIdAndSubjectTypeAndSubjectId(
        Long expenseId, ExpenseSubjectType subjectType, Long subjectId);

    long countByExpenseId(Long expenseId);

    void deleteByExpenseId(Long expenseId);
}
