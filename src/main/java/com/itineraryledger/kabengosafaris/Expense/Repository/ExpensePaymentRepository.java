package com.itineraryledger.kabengosafaris.Expense.Repository;

import com.itineraryledger.kabengosafaris.Expense.Entity.ExpensePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpensePaymentRepository extends JpaRepository<ExpensePayment, Long>, JpaSpecificationExecutor<ExpensePayment> {

    List<ExpensePayment> findByExpenseIdOrderByPaymentDateDesc(Long expenseId);
    long countByExpenseId(Long expenseId);

    List<ExpensePayment> findByPaymentDateBetween(LocalDate from, LocalDate to);
    long countByPaymentDateBetween(LocalDate from, LocalDate to);
    List<ExpensePayment> findTop20ByOrderByPaymentDateDescIdDesc();

    /**
     * Sum the base (expense-currency-equivalent) amounts of all payments that
     * settle a given expense currency. Mirrors the cross-currency-aware
     * aggregator on PaymentRepository.
     */
    @Query("SELECT COALESCE(SUM(COALESCE(p.baseAmount, p.amount)), 0) " +
           "FROM ExpensePayment p " +
           "WHERE p.expense.id = :expenseId " +
           "  AND COALESCE(p.expenseCurrency, p.currency) = :expenseCurrency")
    BigDecimal sumBaseAmountByExpenseIdAndExpenseCurrency(
            @Param("expenseId") Long expenseId,
            @Param("expenseCurrency") String expenseCurrency);
}
