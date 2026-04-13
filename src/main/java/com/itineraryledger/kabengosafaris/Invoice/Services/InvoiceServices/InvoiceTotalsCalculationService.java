package com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceServices;

import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
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
 * - Discounts by currency (subtotal × discountPercentage)
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

        // Calculate taxes by currency (if tax percentage is set)
        Map<String, BigDecimal> taxesByCurrency = calculateTaxesByCurrency(
            subtotalsByCurrency,
            invoice.getTaxPercentage()
        );

        // Calculate discounts by currency (if discount percentage is set)
        Map<String, BigDecimal> discountsByCurrency = calculateDiscountsByCurrency(
            subtotalsByCurrency,
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
    }

    /**
     * Calculate subtotals by currency from all active line items
     */
    private Map<String, BigDecimal> calculateSubtotalsByCurrency(List<InvoiceLineItem> lineItems) {
        Map<String, BigDecimal> subtotals = new HashMap<>();

        for (InvoiceLineItem item : lineItems) {
            if (Boolean.TRUE.equals(item.getIsActive()) && item.getPrices() != null) {
                for (Price price : item.getPrices()) {
                    String currency = price.getCurrency();
                    BigDecimal totalPrice = price.getTotalPrice() != null
                        ? price.getTotalPrice()
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
