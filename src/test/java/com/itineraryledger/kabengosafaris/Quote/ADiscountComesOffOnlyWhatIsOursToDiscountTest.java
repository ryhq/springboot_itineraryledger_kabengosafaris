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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A discount is ours to give on what we sell, and park fees are not that.
 *
 * <p>The figures are a real quote: beds 7,704.00, transport 1,320.00, activities 2,000.00 and park,
 * crater and conservation fees 2,902.64 -- 13,926.64 in all. The customer was promised 10% off
 * everything except the government charges, which is 1,102.40.
 *
 * <p>Without a scope that had to be written as a percentage of the whole quote instead: 7.92%,
 * worked out by hand, with the real rule typed into the discount reason so the odd figure could be
 * explained at all. Add one line to the quote and 7.92 is quietly the wrong number, and nothing
 * says so. That is what this is for.
 */
class ADiscountComesOffOnlyWhatIsOursToDiscountTest {

    private static final BigDecimal BEDS = new BigDecimal("7704.00");
    private static final BigDecimal TRANSPORT = new BigDecimal("1320.00");
    private static final BigDecimal ACTIVITIES = new BigDecimal("2000.00");
    private static final BigDecimal PARK_FEES = new BigDecimal("2902.64");
    private static final BigDecimal SUBTOTAL = new BigDecimal("13926.64");

    /** What we sell. Everything else on this quote is collected at a government gate. */
    private static final String OURS = "ACCOMMODATION,ACTIVITY,TRANSPORT";

    private QuoteTotalsCalculationService service;

    @BeforeEach
    void setUp() {
        QuoteRepository quotes = mock(QuoteRepository.class);
        QuoteItemRepository quoteItems = mock(QuoteItemRepository.class);
        service = new QuoteTotalsCalculationService(quotes, quoteItems);

        when(quoteItems.findActiveByQuoteId(anyLong())).thenReturn(List.of(
            line(QuoteItemType.ACCOMMODATION, BEDS),
            line(QuoteItemType.TRANSPORT, TRANSPORT),
            line(QuoteItemType.ACTIVITY, ACTIVITIES),
            line(QuoteItemType.PARK_FEE, PARK_FEES)
        ));
    }

    @Test
    @DisplayName("with no scope the discount comes off everything, exactly as it did before")
    void nullScopeIsTheOldBehaviour() {
        Quote quote = quote(new BigDecimal("10"), null);

        service.recalculateTotals(quote);

        assertEquals(new BigDecimal("1392.66"), usd(quote.getDiscounts()),
            "10% of the whole 13,926.64 -- what every quote written before this got");
        assertEquals(new BigDecimal("12533.98"), usd(quote.getGrandTotals()));
    }

    @Test
    @DisplayName("scoped to what we sell, 10% takes 1,102.40 and leaves the park fees whole")
    void scopedToWhatWeSell() {
        Quote quote = quote(new BigDecimal("10"), OURS);

        service.recalculateTotals(quote);

        assertEquals(new BigDecimal("1102.40"), usd(quote.getDiscounts()),
            "10% of 7,704.00 + 1,320.00 + 2,000.00");
        assertEquals(SUBTOTAL, usd(quote.getSubtotals()),
            "the subtotal is still every line: scoping the discount must not hide a charge");
        assertEquals(new BigDecimal("12824.24"), usd(quote.getGrandTotals()));
    }

    @Test
    @DisplayName("the company keeps the 290.26 the blanket discount would have given away")
    void whatTheCompanyKeeps() {
        Quote blanket = quote(new BigDecimal("10"), null);
        Quote scoped = quote(new BigDecimal("10"), OURS);

        service.recalculateTotals(blanket);
        service.recalculateTotals(scoped);

        assertEquals(new BigDecimal("290.26"),
            usd(scoped.getGrandTotals()).subtract(usd(blanket.getGrandTotals())),
            "10% of the park fees, which we collect for the authority and cannot discount");
    }

    @Test
    @DisplayName("the hand-computed 7.92% of everything was never the promised figure")
    void theFudgeWasWrong() {
        Quote fudged = quote(new BigDecimal("7.92"), null);
        Quote scoped = quote(new BigDecimal("10"), OURS);

        service.recalculateTotals(fudged);
        service.recalculateTotals(scoped);

        assertNotEquals(usd(scoped.getDiscounts()), usd(fudged.getDiscounts()),
            "close is what made it survive review; it is still not 10% of anything the client "
                + "can point at");
        assertEquals(new BigDecimal("1102.99"), usd(fudged.getDiscounts()));
    }

    @Test
    @DisplayName("a scope naming a category the quote has none of discounts nothing")
    void aScopeWithNothingUnderIt() {
        Quote quote = quote(new BigDecimal("10"), "VISA");

        service.recalculateTotals(quote);

        assertTrue(quote.getDiscounts().isEmpty() || usd(quote.getDiscounts()).signum() == 0,
            "there is no visa line on this quote, so there is nothing to discount");
        assertEquals(SUBTOTAL, usd(quote.getGrandTotals()));
    }

    @Test
    @DisplayName("the tax scope and the discount scope do not have to be the same lines")
    void theTwoScopesAreIndependent() {
        Quote quote = Quote.builder()
            .id(1L)
            .taxPercentage(new BigDecimal("18"))
            .taxAppliesTo("ACCOMMODATION")
            .discountPercentage(new BigDecimal("10"))
            .discountAppliesTo(OURS)
            .build();

        service.recalculateTotals(quote);

        assertEquals(new BigDecimal("1386.72"), usd(quote.getTaxes()), "18% of the beds");
        assertEquals(new BigDecimal("1102.40"), usd(quote.getDiscounts()), "10% of what we sell");
        assertEquals(new BigDecimal("14210.96"), usd(quote.getGrandTotals()),
            "13,926.64 + 1,386.72 - 1,102.40");
    }

    @Test
    @DisplayName("a scope naming every category means every category, and stores as no scope")
    void namingEverythingIsTheSameAsNamingNothing() {
        Quote all = quote(new BigDecimal("10"),
            "ACCOMMODATION,PARK_FEE,ACTIVITY,TRANSPORT,GUIDE,MEALS,EQUIPMENT,INSURANCE,VISA,OTHER");
        Quote none = quote(new BigDecimal("10"), null);

        service.recalculateTotals(all);
        service.recalculateTotals(none);

        assertEquals(usd(none.getDiscounts()), usd(all.getDiscounts()));
    }

    private Quote quote(BigDecimal discountPercentage, String discountAppliesTo) {
        return Quote.builder()
            .id(1L)
            .discountPercentage(discountPercentage)
            .discountAppliesTo(discountAppliesTo)
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
