package com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices;

import com.itineraryledger.kabengosafaris.Itinerary.Services.ItineraryCostEstimationService;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.Enums.QuoteItemType;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteItemRepository;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteRepository;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteTotalsCalculationService;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Two markups that look alike and are not.
 *
 * <p>An agent commission is a share of the sale, so it rides on every line. A margin uplift is a
 * provision against a supplier invoicing beyond contract, and suppliers here means lodges: a park
 * gate does not send a surprise invoice. Before this, both were one percentage over everything, so
 * a provision taken out for accommodation was also charged on the crater fee.
 *
 * <p>The multiplier is asserted rather than read because it multiplies every price the client is
 * shown.
 */
class UpliftAppliesOnlyWhereScopedTest {

    private QuoteCostEstimationService service;

    @BeforeEach
    void setUp() {
        service = new QuoteCostEstimationService(
            mock(QuoteRepository.class),
            mock(QuoteItemRepository.class),
            mock(ItineraryCostEstimationService.class),
            mock(QuoteTotalsCalculationService.class),
            mock(IdObfuscator.class)
        );
    }

    @Test
    @DisplayName("neither markup set leaves the cost price untouched")
    void noMarkupIsExactlyOne() {
        Quote quote = Quote.builder().id(1L).build();

        assertEquals(BigDecimal.ONE, service.computeMarkupMultiplier(quote, QuoteItemType.ACCOMMODATION));
        assertEquals(BigDecimal.ONE, service.computeMarkupMultiplier(quote, QuoteItemType.PARK_FEE));
    }

    @Test
    @DisplayName("an unscoped uplift still rides on everything, as it always did")
    void unscopedUpliftIsTheOldBehaviour() {
        Quote quote = Quote.builder()
            .id(1L)
            .marginUpliftPercentage(new BigDecimal("10"))
            .build();

        assertEquals(0, new BigDecimal("1.100000")
            .compareTo(service.computeMarkupMultiplier(quote, QuoteItemType.ACCOMMODATION)));
        assertEquals(0, new BigDecimal("1.100000")
            .compareTo(service.computeMarkupMultiplier(quote, QuoteItemType.PARK_FEE)),
            "a quote saved before this scoping existed must price identically today");
    }

    @Test
    @DisplayName("scoped to accommodation, the park fee is billed at cost")
    void upliftStaysOnTheBeds() {
        Quote quote = Quote.builder()
            .id(1L)
            .marginUpliftPercentage(new BigDecimal("10"))
            .marginUpliftAppliesTo("ACCOMMODATION")
            .build();

        assertEquals(0, new BigDecimal("1.100000")
            .compareTo(service.computeMarkupMultiplier(quote, QuoteItemType.ACCOMMODATION)));
        assertEquals(0, BigDecimal.ONE
            .compareTo(service.computeMarkupMultiplier(quote, QuoteItemType.PARK_FEE)),
            "the gate charges what it charges; there is no supplier risk to provision for");
        assertEquals(0, BigDecimal.ONE
            .compareTo(service.computeMarkupMultiplier(quote, QuoteItemType.ACTIVITY)));
    }

    @Test
    @DisplayName("the commission ignores the scope, because it is a share of the whole sale")
    void commissionIsNeverScoped() {
        Quote quote = Quote.builder()
            .id(1L)
            .agentCommissionPercentage(new BigDecimal("15"))
            .marginUpliftPercentage(new BigDecimal("10"))
            .marginUpliftAppliesTo("ACCOMMODATION")
            .build();

        assertEquals(0, new BigDecimal("1.250000")
            .compareTo(service.computeMarkupMultiplier(quote, QuoteItemType.ACCOMMODATION)),
            "15 + 10 on the beds");
        assertEquals(0, new BigDecimal("1.150000")
            .compareTo(service.computeMarkupMultiplier(quote, QuoteItemType.PARK_FEE)),
            "15 alone on the park fee: the agent is still owed their share of it");
    }
}
