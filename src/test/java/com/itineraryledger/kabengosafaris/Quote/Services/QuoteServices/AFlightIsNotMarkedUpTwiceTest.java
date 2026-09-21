package com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.Enums.QuoteItemType;
import com.itineraryledger.kabengosafaris.Quote.Enums.QuoteItemTypeScope;

/**
 * A flight already carries its own markup, so the quote's category uplift must not reach it as well.
 *
 * <p>The trap is the default, not the code. {@code QuoteItemTypeScope.covers(null, …)} answers
 * <strong>true</strong> — an uplift somebody set without narrowing it is meant to apply to the whole
 * quote, which is right for the three categories it was written for. Nearly every quote leaves
 * {@code marginUpliftAppliesTo} empty.
 *
 * <p>So the moment FLIGHT joined the enum, every one of those quotes would have started adding the
 * category uplift ON TOP of the airline markup already baked into the fare — a silent price rise on
 * live quotes, with nothing edited and nobody told. Exactly the inversion behind the inclusion-claim
 * defect: a helper whose "unset means everything" default is right for its original cases and
 * exactly wrong for the one added later.
 *
 * <p>For flights the uplift is therefore opt-IN. These tests hold both halves of that.
 *
 * <p>Lives in the service's own package because computeMarkupMultiplier is package-private, and
 * widening production visibility so a test can reach it is the wrong way round.
 */
class AFlightIsNotMarkedUpTwiceTest {

    private final QuoteCostEstimationService service = new QuoteCostEstimationService(
        null, null, null, null, null);

    private Quote quoteWithUplift(String scope) {
        Quote quote = new Quote();
        quote.setMarginUpliftPercentage(new BigDecimal("15"));
        quote.setMarginUpliftAppliesTo(scope);
        return quote;
    }

    @Test
    @DisplayName("a blank scope lifts everything EXCEPT a flight")
    void blankScopeDoesNotReachAFlight() {
        Quote quote = quoteWithUplift(null);

        /* The existing behaviour, unchanged: blank means the whole quote. */
        assertEquals(0, service.computeMarkupMultiplier(quote, QuoteItemType.ACCOMMODATION)
            .compareTo(new BigDecimal("1.15")), "a blank scope must still lift accommodation");
        assertEquals(0, service.computeMarkupMultiplier(quote, QuoteItemType.PARK_FEE)
            .compareTo(new BigDecimal("1.15")));
        assertEquals(0, service.computeMarkupMultiplier(quote, QuoteItemType.ACTIVITY)
            .compareTo(new BigDecimal("1.15")));

        /* The exception, and the whole point of this class. */
        assertEquals(0, service.computeMarkupMultiplier(quote, QuoteItemType.FLIGHT)
            .compareTo(BigDecimal.ONE),
            "a flight already carries the airline's markup; a blank scope must not add 15% on top "
            + "of it on every quote that has ever been written");
    }

    @Test
    @DisplayName("naming FLIGHT explicitly does lift it")
    void anExplicitScopeReachesAFlight() {
        Quote quote = quoteWithUplift("FLIGHT");
        assertEquals(0, service.computeMarkupMultiplier(quote, QuoteItemType.FLIGHT)
            .compareTo(new BigDecimal("1.15")),
            "opt-in has to actually work, or the guard is just a way of ignoring the setting");
    }

    @Test
    @DisplayName("an empty string is as blank as a null")
    void emptyScopeIsBlank() {
        assertEquals(0, service.computeMarkupMultiplier(quoteWithUplift("   "), QuoteItemType.FLIGHT)
            .compareTo(BigDecimal.ONE));
        assertEquals(0, service.computeMarkupMultiplier(quoteWithUplift(""), QuoteItemType.FLIGHT)
            .compareTo(BigDecimal.ONE));
    }

    @Test
    @DisplayName("a scope naming other categories does not sweep a flight up with them")
    void aNarrowedScopeStaysNarrow() {
        Quote quote = quoteWithUplift("ACCOMMODATION,ACTIVITY");
        assertEquals(0, service.computeMarkupMultiplier(quote, QuoteItemType.ACCOMMODATION)
            .compareTo(new BigDecimal("1.15")));
        assertEquals(0, service.computeMarkupMultiplier(quote, QuoteItemType.FLIGHT)
            .compareTo(BigDecimal.ONE));
    }

    @Test
    @DisplayName("the agent commission is a share of the whole sale, flights included")
    void commissionStillApplies() {
        Quote quote = new Quote();
        quote.setAgentCommissionPercentage(new BigDecimal("10"));
        /*
         * Unscoped on purpose and left that way: a referral fee is a cut of what the client paid,
         * and the client paid for the seat too. Only the margin UPLIFT is opt-in for flights.
         */
        assertEquals(0, service.computeMarkupMultiplier(quote, QuoteItemType.FLIGHT)
            .compareTo(new BigDecimal("1.10")));
    }

    @Test
    @DisplayName("the shared helper still answers true for null, which is why the guard is needed")
    void theSharedDefaultIsUnchanged() {
        assertTrue(QuoteItemTypeScope.covers(null, QuoteItemType.FLIGHT),
            "if this ever answers false, the guard above is redundant and should go — but until "
            + "then it is the only thing standing between a blank scope and a double markup");
    }
}
