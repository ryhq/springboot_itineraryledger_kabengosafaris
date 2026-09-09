package com.itineraryledger.kabengosafaris.Quote;

import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.Entity.QuoteItem;
import com.itineraryledger.kabengosafaris.Quote.Enums.QuoteItemType;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteItemRepository;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteRepository;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteTotalsCalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The money, not the parser.
 *
 * <p>The figures below are a real quote: the VENOT family's nine day trip, USD 13,177.74 across
 * beds 7,120.00, park and crater fees 3,412.24, and activities 2,645.50.
 *
 * <p>Charging 18% over all of it takes 2,372.00. Park fees are a government charge collected at the
 * gate; there is no tax of ours inside them to pass on, so most of that 1,090.40 difference is a
 * liability nobody owes, charged to a client who cannot see it in the line items.
 */
class TaxIsChargedOnlyOnTheCategoriesItNamesTest {

    private static final BigDecimal BEDS = new BigDecimal("7120.00");
    private static final BigDecimal PARK_FEES = new BigDecimal("3412.24");
    private static final BigDecimal ACTIVITIES = new BigDecimal("2645.50");
    private static final BigDecimal SUBTOTAL = new BigDecimal("13177.74");

    private QuoteRepository quotes;
    private QuoteItemRepository quoteItems;
    private QuoteTotalsCalculationService service;

    @BeforeEach
    void setUp() {
        quotes = mock(QuoteRepository.class);
        quoteItems = mock(QuoteItemRepository.class);
        service = new QuoteTotalsCalculationService(quotes, quoteItems);

        when(quoteItems.findActiveByQuoteId(anyLong())).thenReturn(List.of(
            line(QuoteItemType.ACCOMMODATION, BEDS),
            line(QuoteItemType.PARK_FEE, PARK_FEES),
            line(QuoteItemType.ACTIVITY, ACTIVITIES)
        ));
    }

    @Test
    @DisplayName("with no scope the quote totals exactly as it did before this existed")
    void nullScopeIsTheOldBehaviour() {
        Quote quote = quote(new BigDecimal("18"), null, null);

        service.recalculateTotals(quote);

        assertEquals(SUBTOTAL, usd(quote.getSubtotals()));
        assertEquals(new BigDecimal("2371.99"), usd(quote.getTaxes()),
            "18% of the whole 13,177.74 -- what every quote written before this got");
        assertEquals(new BigDecimal("15549.73"), usd(quote.getGrandTotals()));
    }

    @Test
    @DisplayName("scoped to accommodation, the tax is 18% of the beds and nothing else")
    void scopedToBeds() {
        Quote quote = quote(new BigDecimal("18"), "ACCOMMODATION", null);

        service.recalculateTotals(quote);

        assertEquals(SUBTOTAL, usd(quote.getSubtotals()),
            "the subtotal is still every line: scoping the tax must not hide a charge");
        assertEquals(new BigDecimal("1281.60"), usd(quote.getTaxes()),
            "18% of 7,120.00");
        assertEquals(new BigDecimal("14459.34"), usd(quote.getGrandTotals()));
    }

    @Test
    @DisplayName("the client keeps 1,090.39 that the blanket rate would have taken")
    void whatTheClientKeeps() {
        Quote blanket = quote(new BigDecimal("18"), null, null);
        Quote scoped = quote(new BigDecimal("18"), "ACCOMMODATION", null);

        service.recalculateTotals(blanket);
        service.recalculateTotals(scoped);

        assertEquals(new BigDecimal("1090.39"),
            usd(blanket.getGrandTotals()).subtract(usd(scoped.getGrandTotals())),
            "18% of the park fees and activities, which is the fee we stop inventing");
    }

    @Test
    @DisplayName("the discount stays over the whole quote, because it is a gesture, not a tax")
    void discountIsNotScoped() {
        Quote quote = quote(new BigDecimal("18"), "ACCOMMODATION", new BigDecimal("10"));

        service.recalculateTotals(quote);

        assertEquals(new BigDecimal("1317.77"), usd(quote.getDiscounts()),
            "10% of the full 13,177.74 -- a discount the client can see is a discount off the trip");
        assertEquals(new BigDecimal("13141.57"), usd(quote.getGrandTotals()));
    }

    @Test
    @DisplayName("a scope naming a category the quote has none of charges no tax at all")
    void aScopeWithNothingUnderIt() {
        Quote quote = quote(new BigDecimal("18"), "TRANSPORT", null);

        service.recalculateTotals(quote);

        assertTrue(quote.getTaxes().isEmpty() || usd(quote.getTaxes()).signum() == 0,
            "there is no transport on this quote, so there is nothing to tax");
        assertEquals(SUBTOTAL, usd(quote.getGrandTotals()));
    }

    @Test
    @DisplayName("zero tax is zero however it is scoped")
    void noTaxPercentage() {
        Quote quote = quote(null, "ACCOMMODATION", null);

        service.recalculateTotals(quote);

        assertTrue(quote.getTaxes().isEmpty());
        assertEquals(SUBTOTAL, usd(quote.getGrandTotals()));
    }

    private Quote quote(BigDecimal taxPercentage, String taxAppliesTo, BigDecimal discount) {
        return Quote.builder()
            .id(1L)
            .taxPercentage(taxPercentage)
            .taxAppliesTo(taxAppliesTo)
            .discountPercentage(discount)
            .build();
    }

    private QuoteItem line(QuoteItemType type, BigDecimal total) {
        return QuoteItem.builder()
            .itemType(type)
            .isActive(true)
            .prices(List.of(Price.builder()
                .currency("USD")
                .quantity(1)
                .unitPrice(total)
                .totalPrice(total)
                .build()))
            .build();
    }

    private BigDecimal usd(List<Price> prices) {
        return prices.stream()
            .filter(p -> "USD".equals(p.getCurrency()))
            .map(Price::getTotalPrice)
            .findFirst()
            .orElse(BigDecimal.ZERO);
    }
}
