package com.itineraryledger.kabengosafaris.Expense.Services.ExpenseServices;

import com.itineraryledger.kabengosafaris.Expense.Entity.Expense;
import com.itineraryledger.kabengosafaris.Expense.Repository.ExpensePaymentRepository;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Mirrors {@code InvoicePaymentAggregationService}: computes per-currency
 * amountsPaid and balances for an expense on demand from ExpensePayment rows
 * + the expense's grandTotals. Never persisted to avoid drift.
 */
@Service
@RequiredArgsConstructor
public class ExpensePaymentAggregationService {

    private final ExpensePaymentRepository expensePaymentRepository;

    public List<Price> computeAmountsPaid(Expense expense) {
        List<Price> result = new ArrayList<>();
        if (expense == null || expense.getId() == null || expense.getGrandTotals() == null) return result;

        for (Price grand : expense.getGrandTotals()) {
            String currency = grand.getCurrency();
            BigDecimal paid = expensePaymentRepository
                .sumBaseAmountByExpenseIdAndExpenseCurrency(expense.getId(), currency);
            if (paid == null) paid = BigDecimal.ZERO;
            paid = paid.setScale(2, RoundingMode.HALF_UP);

            result.add(Price.builder()
                .currency(currency)
                .quantity(1)
                .unitPrice(paid)
                .totalPrice(paid)
                .build());
        }
        return result;
    }

    public List<Price> computeBalances(Expense expense) {
        List<Price> result = new ArrayList<>();
        if (expense == null || expense.getId() == null || expense.getGrandTotals() == null) return result;

        for (Price grand : expense.getGrandTotals()) {
            String currency = grand.getCurrency();
            BigDecimal grandAmt = grand.getTotalPrice() != null ? grand.getTotalPrice() : BigDecimal.ZERO;
            BigDecimal paid = expensePaymentRepository
                .sumBaseAmountByExpenseIdAndExpenseCurrency(expense.getId(), currency);
            if (paid == null) paid = BigDecimal.ZERO;

            BigDecimal balance = grandAmt.subtract(paid).setScale(2, RoundingMode.HALF_UP);

            result.add(Price.builder()
                .currency(currency)
                .quantity(1)
                .unitPrice(balance)
                .totalPrice(balance)
                .build());
        }
        return result;
    }
}
