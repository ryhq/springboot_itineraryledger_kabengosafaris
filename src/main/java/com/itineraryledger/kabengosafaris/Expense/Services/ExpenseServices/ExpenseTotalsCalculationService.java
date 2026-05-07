package com.itineraryledger.kabengosafaris.Expense.Services.ExpenseServices;

import com.itineraryledger.kabengosafaris.Expense.Entity.Expense;
import com.itineraryledger.kabengosafaris.Expense.Entity.ExpenseLineItem;
import com.itineraryledger.kabengosafaris.Expense.Repository.ExpenseLineItemRepository;
import com.itineraryledger.kabengosafaris.Expense.Repository.ExpenseRepository;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mirrors {@code InvoiceTotalsCalculationService} minus the discount math:
 *   subtotal       = sum of active line-item totalPrices per currency
 *   tax            = subtotal × taxPercentage
 *   grandTotal     = subtotal + tax
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseTotalsCalculationService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseLineItemRepository expenseLineItemRepository;

    @Transactional
    public void recalculateTotals(Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
            .orElseThrow(() -> new RuntimeException("Expense not found: " + expenseId));
        recalculateTotals(expense);
    }

    @Transactional
    public void recalculateTotals(Expense expense) {
        log.debug("Recalculating totals for expense {}", expense.getId());

        List<ExpenseLineItem> items = expenseLineItemRepository
            .findByExpenseIdAndIsActiveTrueOrderByDisplayOrderAsc(expense.getId());

        Map<String, BigDecimal> subtotals = subtotalsByCurrency(items);
        Map<String, BigDecimal> taxes = taxesByCurrency(subtotals, expense.getTaxPercentage());
        Map<String, BigDecimal> grandTotals = grandTotalsByCurrency(subtotals, taxes);

        expense.setSubtotals(toPriceList(subtotals));
        expense.setTaxes(toPriceList(taxes));
        expense.setGrandTotals(toPriceList(grandTotals));

        expenseRepository.save(expense);
    }

    private Map<String, BigDecimal> subtotalsByCurrency(List<ExpenseLineItem> items) {
        Map<String, BigDecimal> totals = new HashMap<>();
        for (ExpenseLineItem item : items) {
            if (!Boolean.TRUE.equals(item.getIsActive()) || item.getPrices() == null) continue;
            for (Price p : item.getPrices()) {
                if (p.getCurrency() == null) continue;
                BigDecimal v = p.getTotalPrice() != null ? p.getTotalPrice() : BigDecimal.ZERO;
                totals.merge(p.getCurrency(), v, BigDecimal::add);
            }
        }
        return totals;
    }

    private Map<String, BigDecimal> taxesByCurrency(Map<String, BigDecimal> subtotals, BigDecimal taxPct) {
        Map<String, BigDecimal> taxes = new HashMap<>();
        if (taxPct == null || taxPct.compareTo(BigDecimal.ZERO) <= 0) return taxes;
        for (Map.Entry<String, BigDecimal> e : subtotals.entrySet()) {
            BigDecimal tax = e.getValue()
                .multiply(taxPct)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            taxes.put(e.getKey(), tax);
        }
        return taxes;
    }

    private Map<String, BigDecimal> grandTotalsByCurrency(Map<String, BigDecimal> subtotals,
                                                          Map<String, BigDecimal> taxes) {
        Map<String, BigDecimal> result = new HashMap<>();
        for (String currency : subtotals.keySet()) {
            BigDecimal s = subtotals.getOrDefault(currency, BigDecimal.ZERO);
            BigDecimal t = taxes.getOrDefault(currency, BigDecimal.ZERO);
            result.put(currency, s.add(t).setScale(2, RoundingMode.HALF_UP));
        }
        return result;
    }

    private List<Price> toPriceList(Map<String, BigDecimal> map) {
        List<Price> list = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : map.entrySet()) {
            list.add(Price.builder()
                .currency(e.getKey())
                .quantity(1)
                .unitPrice(e.getValue())
                .totalPrice(e.getValue())
                .build());
        }
        return list;
    }
}
