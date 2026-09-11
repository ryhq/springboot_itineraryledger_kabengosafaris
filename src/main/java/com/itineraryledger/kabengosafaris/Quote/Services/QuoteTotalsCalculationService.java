package com.itineraryledger.kabengosafaris.Quote.Services;

import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.Entity.QuoteItem;
import com.itineraryledger.kabengosafaris.Quote.Enums.QuoteItemTypeScope;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteItemRepository;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteRepository;
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
 * Service for calculating and updating Quote financial totals.
 *
 * This service automatically recalculates:
 * - Subtotals by currency (sum of all active item prices)
 * - Taxes by currency (the lines taxAppliesTo names × taxPercentage)
 * - Discounts by currency (the lines discountAppliesTo names × discountPercentage)
 * - Grand totals by currency (subtotal + taxes - discounts)
 *
 * Should be called whenever:
 * - Quote is created or updated
 * - QuoteItems are added, updated, or deleted
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuoteTotalsCalculationService {

    private final QuoteRepository quoteRepository;
    private final QuoteItemRepository quoteItemRepository;

    /**
     * Recalculate and update all financial totals for a quote
     * Runs asynchronously to avoid blocking the main thread
     *
     * @param quoteId The quote ID to recalculate
     */
    // @Async
    @Transactional
    public void recalculateTotals(Long quoteId) {
        Quote quote = quoteRepository.findById(quoteId)
            .orElseThrow(() -> new RuntimeException("Quote not found with ID: " + quoteId));

        recalculateTotals(quote);
    }

    /**
     * Recalculate and update all financial totals for a quote
     * Runs asynchronously to avoid blocking the main thread
     *
     * @param quote The quote entity to recalculate
     */
    // @Async
    @Transactional
    public void recalculateTotals(Quote quote) {
        log.debug("Recalculating totals for quote: {}", quote.getId());

        // Fetch all active items for this quote
        List<QuoteItem> items = quoteItemRepository.findActiveByQuoteId(quote.getId());

        // Calculate subtotals by currency
        Map<String, BigDecimal> subtotalsByCurrency = calculateSubtotalsByCurrency(items);

        /*
         * Tax is charged on the lines it applies to, not on the whole quote.
         *
         * Park, crater and conservation fees are government charges with no VAT of ours inside
         * them, so a single percentage over the subtotal invents a liability nobody owes. The
         * scope names the categories; null means all of them, which is what every quote written
         * before this said by saying nothing.
         */
        Map<String, BigDecimal> taxableByCurrency = subtotalsByCurrency(
            items, quote.getTaxAppliesTo());

        Map<String, BigDecimal> taxesByCurrency = calculateTaxesByCurrency(
            taxableByCurrency,
            quote.getTaxPercentage()
        );

        /*
         * And the discount comes off the lines it applies to, not off the whole quote.
         *
         * Park, crater and conservation fees are the authority's own charge, gazetted to the
         * dollar and not ours to give away. A discount promised on the parts we sell had to be
         * written up as an odd percentage of everything -- 10% off accommodation, transport and
         * activities became 7.92% of the quote, worked out by hand -- and the next line added
         * made that 7.92 quietly wrong. The scope names the lines; null means all of them.
         */
        Map<String, BigDecimal> discountableByCurrency = subtotalsByCurrency(
            items, quote.getDiscountAppliesTo());

        Map<String, BigDecimal> discountsByCurrency = calculateDiscountsByCurrency(
            discountableByCurrency,
            quote.getDiscountPercentage()
        );

        // Calculate grand totals by currency
        Map<String, BigDecimal> grandTotalsByCurrency = calculateGrandTotalsByCurrency(
            subtotalsByCurrency,
            taxesByCurrency,
            discountsByCurrency
        );

        // Convert maps to Price lists
        quote.setSubtotals(convertToPriceList(subtotalsByCurrency));
        quote.setTaxes(convertToPriceList(taxesByCurrency));
        quote.setDiscounts(convertToPriceList(discountsByCurrency));
        quote.setGrandTotals(convertToPriceList(grandTotalsByCurrency));

        // Save updated quote
        quoteRepository.save(quote);

        log.info("Recalculated totals for quote {}: {} currencies, grand total: {}",
            quote.getId(),
            grandTotalsByCurrency.size(),
            formatTotals(grandTotalsByCurrency)
        );
        if (quote.getTaxAppliesTo() != null && quote.getTaxPercentage() != null) {
            log.info("  tax {}% charged on {} ({} of the subtotal)",
                quote.getTaxPercentage(),
                QuoteItemTypeScope.describe(quote.getTaxAppliesTo()),
                formatTotals(taxableByCurrency));
        }
        if (quote.getDiscountAppliesTo() != null && quote.getDiscountPercentage() != null) {
            log.info("  discount {}% taken off {} ({} of the subtotal)",
                quote.getDiscountPercentage(),
                QuoteItemTypeScope.describe(quote.getDiscountAppliesTo()),
                formatTotals(discountableByCurrency));
        }
    }

    /**
     * Calculate subtotals by currency from all active quote items
     */
    private Map<String, BigDecimal> calculateSubtotalsByCurrency(List<QuoteItem> items) {
        return subtotalsByCurrency(items, null);
    }

    /**
     * The same sum, over the lines a scope names.
     *
     * <p>Null scope means every line, so this is also how the plain subtotal is worked out. One
     * method rather than two, because a taxable base computed differently from the subtotal is how
     * a tax comes to be charged on a figure that appears nowhere on the quote.
     */
    private Map<String, BigDecimal> subtotalsByCurrency(List<QuoteItem> items, String scope) {
        Map<String, BigDecimal> subtotals = new HashMap<>();

        for (QuoteItem item : items) {
            if (!QuoteItemTypeScope.covers(scope, item.getItemType())) {
                continue;
            }
            if (Boolean.TRUE.equals(item.getIsActive()) && item.getPrices() != null) {
                for (Price price : item.getPrices()) {
                    String currency = price.getCurrency();
                    /*
                     * Normalised to cents as it is merged.
                     *
                     * The sum used to carry whatever scale the item prices happened to have, so the
                     * persisted subtotal depended on how the items had been written. Re-summing the
                     * same stored items after a metadata save could produce a figure four cents from
                     * the one a pricing save produced, on a quote nobody had touched.
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
