package com.itineraryledger.kabengosafaris.Safari.Services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Expense.Entity.Expense;
import com.itineraryledger.kabengosafaris.Expense.Entity.ExpenseLineItem;
import com.itineraryledger.kabengosafaris.Expense.Enums.ExpenseCategory;
import com.itineraryledger.kabengosafaris.Expense.Enums.ExpenseStatus;
import com.itineraryledger.kabengosafaris.Expense.Repository.ExpenseRepository;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import com.itineraryledger.kabengosafaris.Safari.DTOs.SafariBillingDTO;
import com.itineraryledger.kabengosafaris.Safari.DTOs.SafariCostingDTO;
import com.itineraryledger.kabengosafaris.Safari.DTOs.SafariCostingDTO.CategoryTotal;
import com.itineraryledger.kabengosafaris.Safari.DTOs.SafariCostingDTO.MarginLine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * What a safari cost, asked of its expenses — the counterpart to
 * {@link SafariBillingService}, which asks its invoices what it earned.
 *
 * Together they answer the only question an operator asks after a trip: did it
 * make money. Neither figure is stored on the safari, so neither can drift from
 * the bills and invoices it came from.
 *
 * <p>The margin is deliberately conservative about currencies. Revenue in USD and
 * costs in TZS are two facts; calling their difference a profit would require an
 * exchange rate this service has no business inventing. A currency appears in the
 * margin only when both sides of it exist.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SafariCostingService {

    private final ExpenseRepository expenseRepository;
    /*
     * Payments are held per payment row with their own currency and exchange
     * rate, so what a bill has actually settled is a computation rather than a
     * column. The expense module already owns that arithmetic.
     */
    private final com.itineraryledger.kabengosafaris.Expense.Services.ExpenseServices
        .ExpensePaymentAggregationService paymentAggregation;

    @Transactional(readOnly = true)
    public SafariCostingDTO forSafari(Long safariId, SafariBillingDTO billing) {
        List<Expense> expenses = expenseRepository.findBySafariId(safariId).stream()
            .filter(e -> e.getStatus() != ExpenseStatus.CANCELLED)
            .toList();

        if (expenses.isEmpty()) {
            return SafariCostingDTO.builder()
                .expenseCount(0)
                .unpaidExpenseCount(0)
                .supplierStatus("NOTHING_RECORDED")
                .supplierStatusDisplayName("Nothing recorded")
                .expensed(new ArrayList<>())
                .paidOut(new ArrayList<>())
                .owedToSuppliers(new ArrayList<>())
                .byCategory(new ArrayList<>())
                .margin(marginLines(billing, new LinkedHashMap<>()))
                .build();
        }

        Map<String, BigDecimal> expensed = new LinkedHashMap<>();
        Map<String, BigDecimal> paidOut = new LinkedHashMap<>();
        Map<ExpenseCategory, Map<String, BigDecimal>> perCategory = new LinkedHashMap<>();
        int unpaid = 0;

        for (Expense expense : expenses) {
            for (Price total : nz(expense.getGrandTotals())) {
                add(expensed, total.getCurrency(), total.getTotalPrice());
            }
            for (Price paid : nz(paymentAggregation.computeAmountsPaid(expense))) {
                add(paidOut, paid.getCurrency(), paid.getTotalPrice());
            }
            if (expense.getStatus() != null && expense.getStatus().isUnpaid()) unpaid++;

            /*
             * A bill's categories come from its line items, and its total is the
             * sum of them plus tax — so the breakdown is built from the lines
             * while the headline figure stays the bill's own stored total. The
             * two can differ by the tax, and the breakdown says what was bought
             * rather than what was owed.
             */
            for (ExpenseLineItem item : nzItems(expense.getLineItems())) {
                ExpenseCategory category = item.getCategory() != null
                    ? item.getCategory() : ExpenseCategory.OTHER;
                Map<String, BigDecimal> bucket =
                    perCategory.computeIfAbsent(category, key -> new LinkedHashMap<>());
                for (Price price : nz(item.getPrices())) {
                    add(bucket, price.getCurrency(), price.getTotalPrice());
                }
            }
        }

        List<Price> owed = new ArrayList<>();
        boolean anyOwing = false;
        for (Map.Entry<String, BigDecimal> entry : expensed.entrySet()) {
            BigDecimal settled = paidOut.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            BigDecimal balance = entry.getValue().subtract(settled);
            if (balance.signum() > 0) anyOwing = true;
            owed.add(price(entry.getKey(), balance));
        }

        List<CategoryTotal> byCategory = perCategory.entrySet().stream()
            .map(entry -> CategoryTotal.builder()
                .category(entry.getKey().name())
                .categoryDisplayName(entry.getKey().getDisplayName())
                .expensed(toPrices(entry.getValue()))
                .build())
            // biggest spend first: it is the one worth explaining
            .sorted(Comparator.comparing(SafariCostingService::largest).reversed())
            .toList();

        return SafariCostingDTO.builder()
            .expenseCount(expenses.size())
            .unpaidExpenseCount(unpaid)
            .supplierStatus(anyOwing ? "OWING" : "SETTLED")
            .supplierStatusDisplayName(anyOwing ? "Owing to suppliers" : "Suppliers settled")
            .expensed(toPrices(expensed))
            .paidOut(toPrices(paidOut))
            .owedToSuppliers(owed)
            .byCategory(new ArrayList<>(byCategory))
            .margin(marginLines(billing, expensed))
            .build();
    }

    /** Revenue against cost, only where one currency holds both. */
    private List<MarginLine> marginLines(SafariBillingDTO billing, Map<String, BigDecimal> expensed) {
        List<MarginLine> out = new ArrayList<>();
        if (billing == null || billing.getInvoiced() == null) return out;

        for (Price invoiced : billing.getInvoiced()) {
            String currency = invoiced.getCurrency();
            BigDecimal revenue = invoiced.getTotalPrice() != null
                ? invoiced.getTotalPrice() : BigDecimal.ZERO;
            BigDecimal cost = expensed.get(currency);
            if (cost == null) continue;

            BigDecimal margin = revenue.subtract(cost);
            BigDecimal percent = revenue.signum() > 0
                ? margin.multiply(BigDecimal.valueOf(100)).divide(revenue, 1, RoundingMode.HALF_UP)
                : null;

            out.add(MarginLine.builder()
                .currency(currency)
                .revenue(revenue)
                .cost(cost)
                .margin(margin)
                .marginPercent(percent)
                .build());
        }
        return out;
    }

    private static BigDecimal largest(CategoryTotal total) {
        return nz(total.getExpensed()).stream()
            .map(p -> p.getTotalPrice() != null ? p.getTotalPrice() : BigDecimal.ZERO)
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);
    }

    private static List<Price> nz(List<Price> prices) {
        return prices != null ? prices : List.of();
    }

    private static List<ExpenseLineItem> nzItems(List<ExpenseLineItem> items) {
        return items != null ? items : List.of();
    }

    private void add(Map<String, BigDecimal> into, String currency, BigDecimal amount) {
        if (currency == null) return;
        into.merge(currency, amount != null ? amount : BigDecimal.ZERO, BigDecimal::add);
    }

    private List<Price> toPrices(Map<String, BigDecimal> amounts) {
        List<Price> out = new ArrayList<>();
        amounts.forEach((currency, amount) -> out.add(price(currency, amount)));
        return out;
    }

    private Price price(String currency, BigDecimal amount) {
        return Price.builder()
            .currency(currency)
            .quantity(1)
            .unitPrice(amount)
            .totalPrice(amount)
            .build();
    }
}
