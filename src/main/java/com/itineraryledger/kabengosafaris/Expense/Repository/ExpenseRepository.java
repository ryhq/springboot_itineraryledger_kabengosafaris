package com.itineraryledger.kabengosafaris.Expense.Repository;

import com.itineraryledger.kabengosafaris.Expense.Entity.Expense;
import com.itineraryledger.kabengosafaris.Expense.Enums.ExpenseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

    Optional<Expense> findByExpenseCode(String expenseCode);
    boolean existsByExpenseCode(String expenseCode);
    boolean existsByExpenseCodeAndIdNot(String expenseCode, Long id);

    long countByStatus(ExpenseStatus status);
    long countByIsActiveTrue();
    long countByVendorId(Long vendorId);
    long countBySafariId(Long safariId);

    List<Expense> findByStatus(ExpenseStatus status);
    List<Expense> findByStatusIn(List<ExpenseStatus> statuses);
    List<Expense> findBySafariId(Long safariId);
    List<Expense> findByVendorId(Long vendorId);

    @Query("SELECT e FROM Expense e WHERE e.safari.id = :safariId " +
           "AND e.status IN ('DRAFT', 'RECORDED', 'PARTIALLY_PAID')")
    List<Expense> findUnpaidBySafariId(@Param("safariId") Long safariId);

    List<Expense> findByExpenseDateBetween(LocalDate from, LocalDate to);

    /**
     * Bills whose due date falls on one of a set of days, in a state that still owes money.
     *
     * Written as a query because the reminder asks for several specific DAYS rather than a range —
     * 7, 3 and 0 days out are three dates, not a window, and fetching the window would mean
     * reminding about every bill in between on every single one of those days.
     */
    @Query("""
        SELECT e FROM Expense e
        WHERE e.isActive = true
          AND e.dueDate IN :dueDates
          AND e.status IN :statuses
        ORDER BY e.dueDate ASC
        """)
    List<Expense> findDueOn(@Param("dueDates") java.util.Collection<java.time.LocalDate> dueDates,
                            @Param("statuses") java.util.Collection<com.itineraryledger.kabengosafaris.Expense.Enums.ExpenseStatus> statuses);
    long countByExpenseDateBetween(LocalDate from, LocalDate to);

    // Navigation queries (mirror InvoiceRepository)
    @Query("SELECT e.id FROM Expense e WHERE e.id > :currentId ORDER BY e.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT e.id FROM Expense e WHERE e.id < :currentId ORDER BY e.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT e.id FROM Expense e ORDER BY e.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT e.id FROM Expense e ORDER BY e.id DESC LIMIT 1")
    Optional<Long> findLastId();
}
