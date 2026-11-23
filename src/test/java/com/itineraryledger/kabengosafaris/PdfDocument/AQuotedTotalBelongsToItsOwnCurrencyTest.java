package com.itineraryledger.kabengosafaris.PdfDocument;

import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.FullCostEstimationDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.PaxCategoryCostDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A costing sheet prices one currency per block, and says when a per-head figure is an average.
 *
 * <p>The costing templates print one block per currency, and inside each block a "Quoted" table
 * taken from the quote's own stored totals. That inner table loops over every currency the quote
 * carries, so without a filter it printed the USD quotation inside the TZS block and the TZS
 * quotation inside the USD block, neither labelled. On QT-1011-1126-2 - seven German guests on
 * non-resident rates and one Tanzanian whose park fees are in shillings - that produced four
 * totals where there are two, and the office read it as the engine having quoted the trip twice.
 *
 * <p>The second half is the figure underneath. Dividing a currency total by the whole party is an
 * average whenever the party spans more than one band, and on that quotation it is a bad one: the
 * TZS total is incurred by exactly one of the eight. "Per guest (8 pax)" over it invites the
 * reader to multiply by their own headcount, which is how USD 3,756.89 came to be read as a
 * price. The quotation PDF already distinguishes the two cases; the costing sheets now use the
 * same words.
 */
class AQuotedTotalBelongsToItsOwnCurrencyTest {

    private static final Path TEMPLATES = Path.of("src/main/resources/templates/pdf-templates");

    private static final List<String> COSTING_SHEETS = List.of(
        "full_cost_estimation_default.html",
        "full_cost_estimation_client_price_sheet.html");

    /** The opening tag of the inner loop, however its attributes are wrapped across lines. */
    private static final Pattern QUOTED_LOOP = Pattern.compile(
        "<table[^>]*th:each=\"quoted : \\$\\{costing\\.quoteTotals}\"[^>]*>", Pattern.DOTALL);

    @Test
    @DisplayName("the quoted table inside a currency block is filtered to that currency")
    void aQuotedTableNamesOneCurrency() throws IOException {
        List<String> offenders = new ArrayList<>();

        for (String name : COSTING_SHEETS) {
            String body = Files.readString(TEMPLATES.resolve(name));
            Matcher m = QUOTED_LOOP.matcher(body);
            boolean found = false;
            while (m.find()) {
                found = true;
                if (!m.group().contains("quoted.currency == total.currency")) {
                    offenders.add(name);
                }
            }
            assertTrue(found, name + " no longer has a quoted-totals loop - has it been renamed?");
        }

        assertTrue(offenders.isEmpty(),
            "These loop the quote's totals inside the per-currency block without constraining "
                + "quoted.currency to total.currency, so every currency's quotation prints under "
                + "every currency heading: " + offenders);
    }

    @Test
    @DisplayName("no costing sheet still hard-codes 'Per guest' against the whole party")
    void thePerHeadLabelIsDerived() throws IOException {
        List<String> offenders = new ArrayList<>();

        for (String name : COSTING_SHEETS) {
            String body = Files.readString(TEMPLATES.resolve(name));
            if (body.contains("Per guest (<span th:text=\"${costing.totalPax}\">")) {
                offenders.add(name);
            }
        }

        assertTrue(offenders.isEmpty(),
            "These call the figure a per-guest price whatever the party is made of. Use "
                + "${costing.perGuestBasisLabel}, which says 'Average per guest' for a mixed "
                + "party: " + offenders);
    }

    @Test
    @DisplayName("one band is a price, two bands is an average")
    void theLabelFollowsTheBands() {
        FullCostEstimationDTO oneBand = FullCostEstimationDTO.builder()
            .totalPax(8)
            .paxCostDetails(List.of(band("Non-Resident", 8)))
            .build();
        assertFalse(oneBand.isPerGuestAveraged());
        assertEquals("Per guest, over 8 travelling", oneBand.getPerGuestBasisLabel());

        /* QT-1011-1126-2: seven Germans and one Tanzanian, who do not pay the same park fee. */
        FullCostEstimationDTO mixed = FullCostEstimationDTO.builder()
            .totalPax(8)
            .paxCostDetails(List.of(band("Non-Resident", 7), band("East African", 1)))
            .build();
        assertTrue(mixed.isPerGuestAveraged());
        assertEquals("Average per guest, over 8 travelling", mixed.getPerGuestBasisLabel());
    }

    @Test
    @DisplayName("a lone traveller is not 'travelling', and no headcount means no label")
    void theEdgesRead() {
        FullCostEstimationDTO alone = FullCostEstimationDTO.builder()
            .totalPax(1)
            .paxCostDetails(List.of(band("Non-Resident", 1)))
            .build();
        assertEquals("Per guest, over 1 traveller", alone.getPerGuestBasisLabel());

        /*
         * A costing run before anybody said who is going. The template binds the label straight
         * into a cell, so null has to mean "print nothing" rather than "print the word null".
         */
        assertEquals(null, FullCostEstimationDTO.builder().build().getPerGuestBasisLabel());
        assertEquals(null, FullCostEstimationDTO.builder().totalPax(0).build().getPerGuestBasisLabel());
    }

    private static PaxCategoryCostDTO band(String nation, int count) {
        return PaxCategoryCostDTO.builder()
            .nationCategoryName(nation)
            .ageCategoryName("Adult")
            .paxCount(count)
            .build();
    }
}
