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
