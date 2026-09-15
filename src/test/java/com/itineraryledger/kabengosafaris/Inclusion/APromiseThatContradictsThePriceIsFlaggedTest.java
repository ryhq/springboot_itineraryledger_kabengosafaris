package com.itineraryledger.kabengosafaris.Inclusion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.Inclusion.DTOs.InclusionWarningDTO;
import com.itineraryledger.kabengosafaris.Inclusion.Services.InclusionAccuracyService;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.Entity.QuoteItem;
import com.itineraryledger.kabengosafaris.Quote.Enums.QuoteItemType;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.Entity.QuoteDay;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.Entity.QuoteDayPark;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkTariff.Entity.QuoteDayParkTariff;
import com.itineraryledger.kabengosafaris.Quote.QuoteInclusion.Entity.QuoteInclusion;

/**
 * When a quote promises something its own prices contradict, somebody should hear about it before
 * the customer does.
 *
 * <p>The third test here is the important one, and the reason this class exists at all.
 * {@link com.itineraryledger.kabengosafaris.GlobalEnums.LineCategoryScope} is shared with
 * {@code taxAppliesTo} and {@code discountAppliesTo}, where an unset scope means "the whole quote"
 * — so {@code covers(null, anything)} answers true. Eight of the fourteen standard lines claim
 * nothing a machine can verify, and read the shared way, "Drinking water on game drives" would
 * accuse every quote ever written of contradicting itself about park fees, insurance and visas at
 * once. A guard that fires on everything is a guard somebody switches off.
 */
class APromiseThatContradictsThePriceIsFlaggedTest {

    private final InclusionAccuracyService accuracy = new InclusionAccuracyService();

    @Test
    @DisplayName("promising park fees while pricing them outside the total is reported")
    void aPromiseTheQuoteChargesForSeparately() {
        Quote quote = new Quote();
        quote.addInclusion(included("All park, conservation & crater-service fees", "PARK_FEE"));
        quote.getDays().add(dayWithParkFee(3, false));

        List<InclusionWarningDTO> warnings = accuracy.check(quote);

        assertEquals(1, warnings.size());
        assertEquals("CONTRADICTED", warnings.get(0).getSeverity());
        assertEquals("All park, conservation & crater-service fees", warnings.get(0).getInclusionLabel());
        assertEquals(1, warnings.get(0).getTotalOffending());
        assertEquals(3, warnings.get(0).getOffendingLines().get(0).getDayNumber(),
            "the day has to be named, or nobody can go and look at it");
    }

    @Test
    @DisplayName("the same promise with the fees actually in the price says nothing")
    void noWarningWhenThePriceAgrees() {
        Quote quote = new Quote();
        quote.addInclusion(included("All park, conservation & crater-service fees", "PARK_FEE"));
        quote.getDays().add(dayWithParkFee(3, true));

        assertTrue(accuracy.check(quote).isEmpty());
    }

    @Test
    @DisplayName("a line that claims nothing is never checked, however the quote is priced")
    void aLineWithNoClaimIsNeverChecked() {
        /*
         * The trap. This quote prices its park fees outside the total AND carries a line with no
         * claim. If the null scope were read the shared way — as every category — the water line
         * would be reported as contradicting the park fees, on this quote and on every other.
         */
        Quote quote = new Quote();
        quote.addInclusion(included("Drinking water on game drives", null));
        quote.addInclusion(included("Government taxes & levies", "   "));
        quote.getDays().add(dayWithParkFee(2, false));

        assertTrue(accuracy.check(quote).isEmpty(),
            "a sentence that names no line category must produce no warning, ever");
    }

    @Test
    @DisplayName("saying a thing is not included and then charging for it is reported")
    void chargingForWhatWasExcluded() {
        /* The direction that actually costs money, rather than merely looking untidy. */
        Quote quote = new Quote();
        quote.addInclusion(excluded("Travel & medical insurance", "INSURANCE"));
        quote.getItems().add(itemOfType(QuoteItemType.INSURANCE));

        List<InclusionWarningDTO> warnings = accuracy.check(quote);

        assertEquals(1, warnings.size());
        assertEquals("CONTRADICTED", warnings.get(0).getSeverity());
        assertTrue(warnings.get(0).getMessage().contains("not included"));
    }

    @Test
    @DisplayName("promising something the quote has none of is reported, more quietly")
    void promisingSomethingThatIsNotThere() {
        Quote quote = new Quote();
        quote.addInclusion(included("Airport transfers on arrival & departure", "TRANSPORT"));

        List<InclusionWarningDTO> warnings = accuracy.check(quote);

        assertEquals(1, warnings.size());
        assertEquals("UNSUPPORTED", warnings.get(0).getSeverity(),
            "not the same as a contradiction — it may simply not belong on this trip");
    }

    @Test
    @DisplayName("a quote that says nothing at all about what it covers is reported once")
    void sayingNothingIsItsOwnWarning() {
        /*
         * The most likely real defect after this ships, and it costs an isEmpty() to catch: this is
         * the state every quote produced before the chain existed is already in, including the one
         * sent to a real customer in September priced at 5,629 USD.
         */
        Quote quote = new Quote();

        List<InclusionWarningDTO> warnings = accuracy.check(quote);

        assertEquals(1, warnings.size());
        assertEquals("MISSING", warnings.get(0).getSeverity());
    }

    /* ------------------------------------------------------------------ helpers */

    private QuoteInclusion included(String label, String claim) {
        return QuoteInclusion.builder().label(label).claimAppliesTo(claim).isIncluded(true).build();
    }

    private QuoteInclusion excluded(String label, String claim) {
        return QuoteInclusion.builder().label(label).claimAppliesTo(claim).isIncluded(false).build();
    }

    private QuoteDay dayWithParkFee(int dayNumber, boolean includedInPrice) {
        QuoteDay day = new QuoteDay();
        day.setDayNumber(dayNumber);
        QuoteDayPark park = new QuoteDayPark();
        QuoteDayParkTariff tariff = new QuoteDayParkTariff();
        tariff.setIsIncludedInPrice(includedInPrice);
        park.getParkTariffs().add(tariff);
        day.getParks().add(park);
        return day;
    }

    private QuoteItem itemOfType(QuoteItemType type) {
        QuoteItem item = new QuoteItem();
        item.setItemType(type);
        item.setItemName(type.name());
        return item;
    }
}
