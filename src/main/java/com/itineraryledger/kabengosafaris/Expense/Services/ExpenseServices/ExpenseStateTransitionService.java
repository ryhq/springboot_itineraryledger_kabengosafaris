package com.itineraryledger.kabengosafaris.Expense.Services.ExpenseServices;

import com.itineraryledger.kabengosafaris.Expense.Entity.Expense;
import com.itineraryledger.kabengosafaris.Expense.Enums.ExpenseStatus;
import com.itineraryledger.kabengosafaris.Expense.Repository.ExpenseRepository;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Drives the auto-status transitions on payment write/delete:
 *   sum(balances) ≤ 0  → PAID
 *   any payment present → PARTIALLY_PAID (if not already PAID)
 *   no payments         → RECORDED (if previously PARTIALLY_PAID)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseStateTransitionService {

    private final ExpenseRepository expenseRepository;
    private final ExpensePaymentAggregationService aggregationService;

    @Transactional
    public void recomputeStatus(Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId).orElse(null);
        if (expense == null) return;
        if (expense.getStatus() == ExpenseStatus.CANCELLED) return;

        List<Price> balances = aggregationService.computeBalances(expense);
        BigDecimal totalBalance = BigDecimal.ZERO;
        for (Price p : balances) {
            if (p.getTotalPrice() != null) totalBalance = totalBalance.add(p.getTotalPrice());
        }

        List<Price> paid = aggregationService.computeAmountsPaid(expense);
        BigDecimal totalPaid = BigDecimal.ZERO;
        for (Price p : paid) {
            if (p.getTotalPrice() != null) totalPaid = totalPaid.add(p.getTotalPrice());
        }

        ExpenseStatus old = expense.getStatus();
        ExpenseStatus next = old;

        if (totalPaid.compareTo(BigDecimal.ZERO) > 0 && totalBalance.compareTo(BigDecimal.ZERO) <= 0) {
            next = ExpenseStatus.PAID;
        } else if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
            next = ExpenseStatus.PARTIALLY_PAID;
        } else if (old == ExpenseStatus.PARTIALLY_PAID || old == ExpenseStatus.PAID) {
            next = ExpenseStatus.RECORDED;
        }

        if (next != old) {
            log.info("Expense {} auto-transition {} -> {}", expense.getExpenseCode(), old, next);
            expense.setStatus(next);
            expenseRepository.save(expense);
        }
    }
}
