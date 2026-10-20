package com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceServices;

import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Invoice.Entity.InvoiceShare;
import com.itineraryledger.kabengosafaris.Invoice.Repository.PaymentRepository;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes per-currency amountsPaid and balances for an invoice on demand
 * from the underlying Payment rows + the invoice's grandTotals.
 *
 * These values are NOT persisted on the Invoice entity — they are derived
 * at read time so they never drift from the source of truth (the payments table).
 *
 * Cross-currency payments are accounted for via their baseAmount (= amount × exchangeRate),
 * grouped by the invoice currency they settle (Payment.invoiceCurrency).
 */
@Service
@RequiredArgsConstructor
public class InvoicePaymentAggregationService {

    private final PaymentRepository paymentRepository;

    /**
     * Sum of payments (in base/invoice-currency terms) for each currency present
     * on the invoice's grand totals. Produces one Price entry per grand-total currency,
     * even when no payment exists (totalPrice = 0.00).
     */
    public List<Price> computeAmountsPaid(Invoice invoice) {
        List<Price> result = new ArrayList<>();
        if (invoice == null || invoice.getId() == null || invoice.getGrandTotals() == null) {
            return result;
        }

        for (Price grandTotal : invoice.getGrandTotals()) {
            String currency = grandTotal.getCurrency();
            BigDecimal paid = paymentRepository.sumBaseAmountByInvoiceIdAndInvoiceCurrency(
                invoice.getId(), currency);
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
     * Balance remaining per currency = grandTotal − sum(baseAmounts settling that currency).
     */
    public List<Price> computeBalances(Invoice invoice) {
        List<Price> result = new ArrayList<>();
        if (invoice == null || invoice.getId() == null || invoice.getGrandTotals() == null) {
            return result;
        }

        for (Price grandTotal : invoice.getGrandTotals()) {
            String currency = grandTotal.getCurrency();
            BigDecimal grand = grandTotal.getTotalPrice() != null ? grandTotal.getTotalPrice() : BigDecimal.ZERO;
            BigDecimal paid = paymentRepository.sumBaseAmountByInvoiceIdAndInvoiceCurrency(
                invoice.getId(), currency);
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

    /**
     * What one payer has actually sent against their share.
     *
     * <p>Derived the same way and for the same reason as the invoice's own figure: there is no
     * stored column, so it cannot drift from the payments it describes.
     */
    public BigDecimal computeSharePaid(InvoiceShare share) {
        if (share == null || share.getId() == null || share.getCurrency() == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal paid = paymentRepository.sumBaseAmountByShareIdAndCurrency(
            share.getId(), share.getCurrency());
        return (paid == null ? BigDecimal.ZERO : paid).setScale(2, RoundingMode.HALF_UP);
    }

    /** What this payer still owes. Negative means they have overpaid their share. */
    public BigDecimal computeShareBalance(InvoiceShare share) {
        if (share == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal amount = share.getAmount() != null ? share.getAmount() : BigDecimal.ZERO;
        return amount.subtract(computeSharePaid(share)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Where a share stands, in one word, for a list somebody is chasing payment from.
     *
     * <p>Derived rather than stored, so there is no status column to fall out of step with the
     * money, and no enum constant to widen a native MySQL column for.
     */
    public String computeShareStatus(InvoiceShare share) {
        if (share == null) {
            return "UNPAID";
        }
        BigDecimal paid = computeSharePaid(share);
        if (paid.compareTo(BigDecimal.ZERO) <= 0) {
            return "UNPAID";
        }
        BigDecimal amount = share.getAmount() != null ? share.getAmount() : BigDecimal.ZERO;
        return paid.compareTo(amount) >= 0 ? "PAID" : "PARTIALLY_PAID";
    }

    /**
     * What the shares add up to, per currency, so a caller can say whether they cover the
     * invoice.
     *
     * <p>Shares are allowed not to add up: half a group may be settled before the rest have even
     * been asked. The rule is that the gap is shown, never that it is silently closed or silently
     * ignored.
     */
    public Map<String, BigDecimal> computeAllocatedByCurrency(List<InvoiceShare> shares) {
        Map<String, BigDecimal> allocated = new LinkedHashMap<>();
        if (shares == null) {
            return allocated;
        }
        for (InvoiceShare share : shares) {
            if (share == null || !Boolean.TRUE.equals(share.getIsActive())) {
                continue;
            }
            String currency = share.getCurrency();
            BigDecimal amount = share.getAmount() != null ? share.getAmount() : BigDecimal.ZERO;
            allocated.merge(currency, amount, BigDecimal::add);
        }
        allocated.replaceAll((currency, total) -> total.setScale(2, RoundingMode.HALF_UP));
        return allocated;
    }

    /**
     * The part of the invoice nobody has been made responsible for, per currency.
     *
     * <p>Positive means some of the bill is unallocated; negative means the shares promise more
     * than the invoice asks for. Both are reported rather than corrected, because which share is
     * wrong is a judgement only the person who set them up can make.
     */
    public Map<String, BigDecimal> computeUnallocatedByCurrency(
        Invoice invoice, List<InvoiceShare> shares) {

        Map<String, BigDecimal> unallocated = new LinkedHashMap<>();
        if (invoice == null || invoice.getGrandTotals() == null) {
            return unallocated;
        }
        Map<String, BigDecimal> allocated = computeAllocatedByCurrency(shares);
        for (Price grandTotal : invoice.getGrandTotals()) {
            String currency = grandTotal.getCurrency();
            BigDecimal grand = grandTotal.getTotalPrice() != null
                ? grandTotal.getTotalPrice() : BigDecimal.ZERO;
            BigDecimal covered = allocated.getOrDefault(currency, BigDecimal.ZERO);
            unallocated.put(currency, grand.subtract(covered).setScale(2, RoundingMode.HALF_UP));
        }
        return unallocated;
    }
}
