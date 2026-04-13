package com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceServices;

import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Invoice.Repository.PaymentRepository;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Computes per-currency amountsPaid and balances for an invoice on demand
 * from the underlying Payment rows + the invoice's grandTotals.
 *
 * These values are NOT persisted on the Invoice entity — they are derived
 * at read time so they never drift from the source of truth (the payments table).
 */
@Service
@RequiredArgsConstructor
public class InvoicePaymentAggregationService {

    private final PaymentRepository paymentRepository;

    /**
     * Sum of payments in each currency present on the invoice's grand totals.
     * Produces one Price entry per grand-total currency, even when no payment
     * exists for that currency (in which case totalPrice = 0.00).
     */
    public List<Price> computeAmountsPaid(Invoice invoice) {
        List<Price> result = new ArrayList<>();
        if (invoice == null || invoice.getId() == null || invoice.getGrandTotals() == null) {
            return result;
        }

        for (Price grandTotal : invoice.getGrandTotals()) {
            String currency = grandTotal.getCurrency();
            BigDecimal paid = paymentRepository.sumAmountByInvoiceIdAndCurrency(invoice.getId(), currency);
            if (paid == null) {
                paid = BigDecimal.ZERO;
            }
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

    /**
     * Balance remaining per currency = grandTotal − sum(payments in that currency).
     */
    public List<Price> computeBalances(Invoice invoice) {
        List<Price> result = new ArrayList<>();
        if (invoice == null || invoice.getId() == null || invoice.getGrandTotals() == null) {
            return result;
        }

        for (Price grandTotal : invoice.getGrandTotals()) {
            String currency = grandTotal.getCurrency();
            BigDecimal grand = grandTotal.getTotalPrice() != null ? grandTotal.getTotalPrice() : BigDecimal.ZERO;
            BigDecimal paid = paymentRepository.sumAmountByInvoiceIdAndCurrency(invoice.getId(), currency);
            if (paid == null) {
                paid = BigDecimal.ZERO;
            }

            BigDecimal balance = grand.subtract(paid).setScale(2, RoundingMode.HALF_UP);

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
