package com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceServices;

import com.itineraryledger.kabengosafaris.GlobalEnums.LineCategoryScope;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Invoice.Enums.InvoiceItemType;
import com.itineraryledger.kabengosafaris.Invoice.Entity.InvoiceLineItem;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceLineItemRepository;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceRepository;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for calculating and updating Invoice financial totals.
 *
 * This service automatically recalculates:
 * - Subtotals by currency (sum of all active line item prices)
 * - Taxes by currency (subtotal × taxPercentage)
 * - Discounts by currency (the lines discountAppliesTo names × discountPercentage)
 * - Grand totals by currency (subtotal + taxes - discounts)
 *
 * Balances by currency are NOT persisted — they are derived on demand
 * from grandTotals and payment rows (see InvoicePaymentAggregationService).
 *
 * Should be called whenever:
 * - Invoice is created or updated
 * - InvoiceLineItems are added, updated, or deleted
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceTotalsCalculationService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineItemRepository invoiceLineItemRepository;

    /**
     * Recalculate and update all financial totals for an invoice
     * Runs asynchronously to avoid blocking the main thread
     *
     * @param invoiceId The invoice ID to recalculate
     */
    // @Async
    @Transactional
    public void recalculateTotals(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + invoiceId));

        recalculateTotals(invoice);
    }

    /**
     * Recalculate and update all financial totals for an invoice
     * Runs asynchronously to avoid blocking the main thread
     *
     * @param invoice The invoice entity to recalculate
     */
    // @Async
    @Transactional
    public void recalculateTotals(Invoice invoice) {
        log.debug("Recalculating totals for invoice: {}", invoice.getId());

        // Fetch all active line items for this invoice
        List<InvoiceLineItem> lineItems = invoiceLineItemRepository.findByInvoiceIdAndIsActiveTrueOrderByDisplayOrderAsc(invoice.getId());

        // Calculate subtotals by currency
        Map<String, BigDecimal> subtotalsByCurrency = calculateSubtotalsByCurrency(lineItems);

        /*
         * Tax is charged on the lines it applies to, not on the whole invoice.
         *
         * The scope arrives with the quote. Charging the quoted percentage over everything here
         * would bill the client more than they were quoted, which is the one place the difference
         * is not a discussion but a debt.
         */
        Map<String, BigDecimal> taxableByCurrency = subtotalsByCurrency(
            lineItems, invoice.getTaxAppliesTo());

        Map<String, BigDecimal> taxesByCurrency = calculateTaxesByCurrency(
            taxableByCurrency,
            invoice.getTaxPercentage()
        );

        /*
         * And the discount comes off the lines it applies to, not off the whole invoice.
         *
         * The scope arrives with the quote, like the tax scope beside it. Taking the quoted
         * percentage off everything here would credit the client against park fees we merely
         * collect, and short the company by the difference at the moment the money is banked.
         */
        Map<String, BigDecimal> discountableByCurrency = subtotalsByCurrency(
            lineItems, invoice.getDiscountAppliesTo());

        Map<String, BigDecimal> discountsByCurrency = calculateDiscountsByCurrency(
            discountableByCurrency,
            invoice.getDiscountPercentage()
        );

        // Calculate grand totals by currency
        Map<String, BigDecimal> grandTotalsByCurrency = calculateGrandTotalsByCurrency(
            subtotalsByCurrency,
            taxesByCurrency,
            discountsByCurrency
        );

        // Convert maps to Price lists
        invoice.setSubtotals(convertToPriceList(subtotalsByCurrency));
        invoice.setTaxes(convertToPriceList(taxesByCurrency));
        invoice.setDiscounts(convertToPriceList(discountsByCurrency));
        invoice.setGrandTotals(convertToPriceList(grandTotalsByCurrency));

        // Save updated invoice
        invoiceRepository.save(invoice);

        log.info("Recalculated totals for invoice {}: {} currencies, grand total: {}",
            invoice.getId(),
            grandTotalsByCurrency.size(),
            formatTotals(grandTotalsByCurrency)
        );
        if (invoice.getTaxAppliesTo() != null && invoice.getTaxPercentage() != null) {
            log.info("  tax {}% charged on {} ({} of the subtotal)",
                invoice.getTaxPercentage(),
                LineCategoryScope.describe(invoice.getTaxAppliesTo(), InvoiceItemType.class),
                formatTotals(taxableByCurrency));
        }
        if (invoice.getDiscountAppliesTo() != null && invoice.getDiscountPercentage() != null) {
            log.info("  discount {}% taken off {} ({} of the subtotal)",
                invoice.getDiscountPercentage(),
                LineCategoryScope.describe(invoice.getDiscountAppliesTo(), InvoiceItemType.class),
                formatTotals(discountableByCurrency));
        }
    }

    /**
     * Calculate subtotals by currency from all active line items
     */
    private Map<String, BigDecimal> calculateSubtotalsByCurrency(List<InvoiceLineItem> lineItems) {
        return subtotalsByCurrency(lineItems, null);
    }

    /**
     * The same sum, over the lines a scope names.
     *
     * <p>Null scope means every line, so this is also how the plain subtotal is worked out. One
     * method rather than two, because a taxable base computed differently from the subtotal is how
     * a tax comes to be charged on a figure that appears nowhere on the invoice.
     */
    private Map<String, BigDecimal> subtotalsByCurrency(List<InvoiceLineItem> lineItems, String scope) {
        Map<String, BigDecimal> subtotals = new HashMap<>();

        for (InvoiceLineItem item : lineItems) {
            if (!LineCategoryScope.covers(scope, item.getItemType())) {
                continue;
            }
            if (Boolean.TRUE.equals(item.getIsActive()) && item.getPrices() != null) {
                for (Price price : item.getPrices()) {
                    String currency = price.getCurrency();
                    /*
                     * Normalised to cents as it is merged, as on the quote side. The sum otherwise
                     * carries whatever scale the line items happened to have, so re-summing the
                     * same stored rows can land a few cents from the figure already persisted.
                     */
                    BigDecimal totalPrice = price.getTotalPrice() != null
                        ? price.getTotalPrice().setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

                    subtotals.merge(currency, totalPrice, BigDecimal::add);
                }
            }
        }

        return subtotals;
    }

    /**
     * Calculate taxes by currency based on subtotals and tax percentage
     */
    private Map<String, BigDecimal> calculateTaxesByCurrency(
        Map<String, BigDecimal> subtotals,
        BigDecimal taxPercentage
    ) {
        Map<String, BigDecimal> taxes = new HashMap<>();

        if (taxPercentage == null || taxPercentage.compareTo(BigDecimal.ZERO) <= 0) {
            // No tax to apply
            return taxes;
        }

        for (Map.Entry<String, BigDecimal> entry : subtotals.entrySet()) {
            String currency = entry.getKey();
            BigDecimal subtotal = entry.getValue();

            // Calculate tax: subtotal × (taxPercentage / 100)
            BigDecimal tax = subtotal
                .multiply(taxPercentage)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            taxes.put(currency, tax);
        }

        return taxes;
    }

    /**
     * Calculate discounts by currency based on subtotals and discount percentage
     */
    private Map<String, BigDecimal> calculateDiscountsByCurrency(
        Map<String, BigDecimal> subtotals,
        BigDecimal discountPercentage
    ) {
        Map<String, BigDecimal> discounts = new HashMap<>();

        if (discountPercentage == null || discountPercentage.compareTo(BigDecimal.ZERO) <= 0) {
            // No discount to apply
            return discounts;
        }

        for (Map.Entry<String, BigDecimal> entry : subtotals.entrySet()) {
            String currency = entry.getKey();
            BigDecimal subtotal = entry.getValue();

            // Calculate discount: subtotal × (discountPercentage / 100)
            BigDecimal discount = subtotal
                .multiply(discountPercentage)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            discounts.put(currency, discount);
        }

        return discounts;
    }

    /**
     * Calculate grand totals by currency: subtotal + taxes - discounts
     */
    private Map<String, BigDecimal> calculateGrandTotalsByCurrency(
        Map<String, BigDecimal> subtotals,
        Map<String, BigDecimal> taxes,
        Map<String, BigDecimal> discounts
    ) {
        Map<String, BigDecimal> grandTotals = new HashMap<>();

        for (String currency : subtotals.keySet()) {
            BigDecimal subtotal = subtotals.getOrDefault(currency, BigDecimal.ZERO);
            BigDecimal tax = taxes.getOrDefault(currency, BigDecimal.ZERO);
            BigDecimal discount = discounts.getOrDefault(currency, BigDecimal.ZERO);

            // Grand Total = Subtotal + Tax - Discount
            BigDecimal grandTotal = subtotal
                .add(tax)
                .subtract(discount)
                .setScale(2, RoundingMode.HALF_UP);

            grandTotals.put(currency, grandTotal);
        }

        return grandTotals;
    }

    /**
     * Convert a currency-amount map to a list of Price objects
     */
    private List<Price> convertToPriceList(Map<String, BigDecimal> amountsByCurrency) {
        List<Price> prices = new ArrayList<>();

        for (Map.Entry<String, BigDecimal> entry : amountsByCurrency.entrySet()) {
            Price price = Price.builder()
                .currency(entry.getKey())
                .quantity(1)
                .unitPrice(entry.getValue())
                .totalPrice(entry.getValue())
                .build();

            prices.add(price);
        }

        return prices;
    }

    /**
     * Format totals for logging
     */
    private String formatTotals(Map<String, BigDecimal> totals) {
        if (totals.isEmpty()) {
            return "none";
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, BigDecimal> entry : totals.entrySet()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(entry.getKey())
                .append(" ")
                .append(entry.getValue().toPlainString());
        }
        return sb.toString();
    }
}
