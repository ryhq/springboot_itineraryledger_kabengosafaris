package com.itineraryledger.kabengosafaris.CreditNote.Services.CreditNoteServices;

import com.itineraryledger.kabengosafaris.CreditNote.Entity.CreditNote;
import com.itineraryledger.kabengosafaris.CreditNote.Entity.CreditNoteLineItem;
import com.itineraryledger.kabengosafaris.CreditNote.Repository.CreditNoteLineItemRepository;
import com.itineraryledger.kabengosafaris.CreditNote.Repository.CreditNoteRepository;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes per-currency subtotals, taxes, and totals for a credit note on demand
 * from its active line items + taxPercentage.
 *
 * These values are NOT persisted on the CreditNote entity — they are derived
 * at read time so they never drift from the underlying line-item prices.
 */
@Service
@RequiredArgsConstructor
public class CreditNoteTotalsAggregationService {

    private final CreditNoteLineItemRepository creditNoteLineItemRepository;
    private final CreditNoteRepository creditNoteRepository;

    /**
     * Container for the three derived totals lists.
     * One call loads line items once and computes all three.
     */
    public record ComputedTotals(List<Price> subtotals, List<Price> taxes, List<Price> totals) {
        public static ComputedTotals empty() {
            return new ComputedTotals(List.of(), List.of(), List.of());
        }
    }

    public ComputedTotals compute(CreditNote creditNote) {
        if (creditNote == null || creditNote.getId() == null) {
            return ComputedTotals.empty();
        }

        List<CreditNoteLineItem> lineItems = creditNoteLineItemRepository
            .findByCreditNoteIdOrderByDisplayOrderAsc(creditNote.getId());

        Map<String, BigDecimal> subtotalsByCurrency = new HashMap<>();
        for (CreditNoteLineItem item : lineItems) {
            if (!Boolean.TRUE.equals(item.getIsActive()) || item.getPrices() == null) continue;
            for (Price price : item.getPrices()) {
                BigDecimal total = price.getTotalPrice() != null ? price.getTotalPrice() : BigDecimal.ZERO;
                subtotalsByCurrency.merge(price.getCurrency(), total, BigDecimal::add);
            }
        }

        BigDecimal taxPercentage = creditNote.getTaxPercentage();
        boolean hasTax = taxPercentage != null && taxPercentage.compareTo(BigDecimal.ZERO) > 0;

        Map<String, BigDecimal> taxesByCurrency = new HashMap<>();
        Map<String, BigDecimal> totalsByCurrency = new HashMap<>();

        for (Map.Entry<String, BigDecimal> entry : subtotalsByCurrency.entrySet()) {
            String currency = entry.getKey();
            BigDecimal subtotal = entry.getValue().setScale(2, RoundingMode.HALF_UP);

            BigDecimal tax = BigDecimal.ZERO;
            if (hasTax) {
                tax = subtotal.multiply(taxPercentage)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                taxesByCurrency.put(currency, tax);
            }

            totalsByCurrency.put(currency, subtotal.add(tax).setScale(2, RoundingMode.HALF_UP));
        }

        return new ComputedTotals(
            toPriceList(subtotalsByCurrency),
            toPriceList(taxesByCurrency),
            toPriceList(totalsByCurrency)
        );
    }

    /**
     * Sum of current credit-note totals per currency across all active credit notes
     * tied to the given invoice. Inactive credit notes (isActive=false) are skipped.
     * Includes all statuses (DRAFT through CONSUMED) — a DRAFT still reserves its amount.
     */
    public Map<String, BigDecimal> existingCreditsByInvoice(Invoice invoice) {
        Map<String, BigDecimal> totals = new HashMap<>();
        if (invoice == null || invoice.getId() == null) {
            return totals;
        }
        List<CreditNote> creditNotes = creditNoteRepository.findByInvoiceId(invoice.getId());
        for (CreditNote cn : creditNotes) {
            if (!Boolean.TRUE.equals(cn.getIsActive())) continue;
            ComputedTotals computed = compute(cn);
            for (Price p : computed.totals()) {
                if (p.getCurrency() == null || p.getTotalPrice() == null) continue;
                totals.merge(p.getCurrency(), p.getTotalPrice(), BigDecimal::add);
            }
        }
        return totals;
    }

    /**
     * Over-credit check result. When {@code overCredit} is true, {@code message}
     * explains which currency over-credited by how much.
     */
    public record OverCreditCheck(boolean overCredit, String currency, BigDecimal available,
                                  BigDecimal requested, String message) {
        public static OverCreditCheck ok() {
            return new OverCreditCheck(false, null, null, null, null);
        }
    }

    /**
     * Would adding {@code additionalContribution} (per-currency increments, tax-inclusive)
     * to the existing credits against {@code invoice} exceed the invoice's grandTotal
     * for any currency?
     *
     * The caller is responsible for computing {@code additionalContribution} — for a
     * new line item that's {@code subtotal + (subtotal × taxPct / 100)}.
     */
    public OverCreditCheck checkOverCredit(Invoice invoice, Map<String, BigDecimal> additionalContribution) {
        if (invoice == null || invoice.getGrandTotals() == null || invoice.getGrandTotals().isEmpty()) {
            return OverCreditCheck.ok();
        }
        if (additionalContribution == null || additionalContribution.isEmpty()) {
            return OverCreditCheck.ok();
        }

        Map<String, BigDecimal> grandTotalsByCurrency = new HashMap<>();
        for (Price p : invoice.getGrandTotals()) {
            if (p.getCurrency() == null) continue;
            grandTotalsByCurrency.put(p.getCurrency(), p.getTotalPrice() != null ? p.getTotalPrice() : BigDecimal.ZERO);
        }

        Map<String, BigDecimal> existing = existingCreditsByInvoice(invoice);

        for (Map.Entry<String, BigDecimal> entry : additionalContribution.entrySet()) {
            String currency = entry.getKey();
            BigDecimal addition = entry.getValue() != null ? entry.getValue() : BigDecimal.ZERO;
            if (addition.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal grandTotal = grandTotalsByCurrency.getOrDefault(currency, BigDecimal.ZERO);
            BigDecimal alreadyCredited = existing.getOrDefault(currency, BigDecimal.ZERO);
            BigDecimal available = grandTotal.subtract(alreadyCredited).setScale(2, RoundingMode.HALF_UP);

            if (addition.compareTo(available) > 0) {
                String msg = String.format(
                    "Over-credit: adding %s %s would exceed the remaining creditable amount of %s %s on invoice %s " +
                    "(grand total %s %s, already credited %s %s). Set force=true to override.",
                    addition.setScale(2, RoundingMode.HALF_UP), currency,
                    available, currency,
                    invoice.getInvoiceCode(),
                    grandTotal.setScale(2, RoundingMode.HALF_UP), currency,
                    alreadyCredited.setScale(2, RoundingMode.HALF_UP), currency
                );
                return new OverCreditCheck(true, currency, available, addition, msg);
            }
        }
        return OverCreditCheck.ok();
    }

    /**
     * Compute the tax-inclusive per-currency contribution for a hypothetical new line item
     * being added to a credit note. Uses the credit note's taxPercentage.
     */
    public Map<String, BigDecimal> contributionForNewLineItem(CreditNote creditNote, List<Price> prices) {
        Map<String, BigDecimal> result = new HashMap<>();
        if (prices == null || prices.isEmpty()) return result;

        BigDecimal taxPct = creditNote != null ? creditNote.getTaxPercentage() : null;
        boolean hasTax = taxPct != null && taxPct.compareTo(BigDecimal.ZERO) > 0;

        for (Price price : prices) {
            if (price.getCurrency() == null || price.getTotalPrice() == null) continue;
            BigDecimal subtotal = price.getTotalPrice();
            BigDecimal contribution = subtotal;
            if (hasTax) {
                BigDecimal tax = subtotal.multiply(taxPct)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                contribution = subtotal.add(tax);
            }
            result.merge(price.getCurrency(), contribution, BigDecimal::add);
        }
        return result;
    }

    private List<Price> toPriceList(Map<String, BigDecimal> amountsByCurrency) {
        List<Price> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : amountsByCurrency.entrySet()) {
            BigDecimal amount = entry.getValue().setScale(2, RoundingMode.HALF_UP);
            result.add(Price.builder()
                .currency(entry.getKey())
                .quantity(1)
                .unitPrice(amount)
                .totalPrice(amount)
                .build());
        }
        return result;
    }
}
