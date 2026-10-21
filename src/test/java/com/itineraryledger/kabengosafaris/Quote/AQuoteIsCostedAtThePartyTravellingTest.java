package com.itineraryledger.kabengosafaris.Quote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.FullCostEstimationDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.CalculationMode;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.CostEstimationOrchestrator;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.FullItineraryDTO;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices.QuoteCostingDocumentService;

/**
 * A costing document for a quote prices the people who are going.
 *
 * <p>An itinerary is a blueprint and is priced at a reference party: two guests, one vehicle, one
 * room a night. That is the right answer for a product sheet and the wrong one to hand a
 * customer. Six guests in three double rooms across two vehicles is not the blueprint multiplied
 * by three, because the per-vehicle and per-group lines do not scale with heads, so the error is
 * silent and in the client's favour or ours depending on the day.
 *
 * <p>The protection is not a second costing engine. It is that the quote's own day snapshot --
 * the one that already writes its line items -- is projected once and handed to the one engine.
 * These tests pin that arrangement, because the failure they prevent is two implementations
 * drifting until a costing sheet and the quotation stapled to it disagree.
 */
class AQuoteIsCostedAtThePartyTravellingTest {

    private static final Path SERVICE = Path.of(
        "src/main/java/com/itineraryledger/kabengosafaris/Quote/Services/QuoteServices/QuoteCostingDocumentService.java");
    private static final Path PRICE_SHEET = Path.of(
        "src/main/resources/templates/pdf-templates/full_cost_estimation_client_price_sheet.html");
    private static final Path COSTING_SHEET = Path.of(
        "src/main/resources/templates/pdf-templates/full_cost_estimation_default.html");
    private static final Path SCHEMA = Path.of(
        "src/main/resources/schemas/pdf-documents/full-cost-estimation-schema.json");

    @Test
    @DisplayName("the engine can be handed a day tree, so a quote need not have its own")
    void theEngineTakesAPreparedDayTree() throws Exception {
        Method estimateFrom = CostEstimationOrchestrator.class.getMethod(
            "estimateFrom", FullItineraryDTO.class, LocalDate.class, CalculationMode.class);

        assertNotNull(estimateFrom,
            "without an entry point that takes a prepared tree, a quote can only be costed by a "
            + "second engine, and two engines over one set of rates will not agree for long");
    }

    @Test
    @DisplayName("a quote is projected once and priced by the shared engine")
    void oneProjectionAndOneEngine() throws Exception {
        String source = Files.readString(SERVICE);

        assertTrue(source.contains("buildSyntheticDTO"),
            "the costing must reuse the projection that writes the quote's line items. A second "
            + "mapper is how a costing sheet comes to show a different room count from the "
            + "quotation beside it");
        assertTrue(source.contains("orchestrator.estimateFrom"),
            "and it must price through the shared engine rather than its own");

        /*
         * The shape that would undo it: reaching for the aggregators or the rate lookups
         * directly. Those are the engine's internals, and a caller that assembles them has
         * written a second engine whether or not it meant to.
         */
        Pattern ownEngine = Pattern.compile(
            "\\b(PerDayCostAggregator|PerPaxCostAggregator|AccommodationCostCalculator"
            + "|ParkTariffCostCalculator|ActivityCostCalculator|RateLookupService)\\b");
        Matcher matcher = ownEngine.matcher(source);
        List<String> offences = new ArrayList<>();
        while (matcher.find()) offences.add(matcher.group());
        assertTrue(offences.isEmpty(),
            () -> "the quote costing reaches into the engine's internals (" + offences
                + "), which makes it a second engine");
    }

    @Test
    @DisplayName("the document says it is costing a quote, not the product")
    void theSubjectIsTheQuote() throws Exception {
        assertTrue(Files.readString(SERVICE).contains("\"QUOTE\""),
            "subjectKind is the one field that tells a template which of the three subjects it "
            + "is printing");

        for (Path template : List.of(PRICE_SHEET, COSTING_SHEET)) {
            String html = Files.readString(template);
            assertTrue(html.contains("subjectKind == 'QUOTE'"),
                template.getFileName() + " does not recognise a quote, so a client's costing "
                + "would be headed 'Itinerary template' over their own party's figures");
        }
    }

    @Test
    @DisplayName("what was quoted is printed beside what it costs, and declared in the schema")
    void theQuotedFiguresReachTheTemplate() throws Exception {
        /*
         * The cost tables are the trip. The quote then applies tax, a discount, an uplift. Print
         * only the first and a client holds two documents with two totals and no way to tell
         * which to pay; print it from a fresh calculation and the two drift apart the first time
         * somebody edits a percentage.
         */
        String source = Files.readString(SERVICE);
        assertTrue(source.contains("quote.getGrandTotals()"),
            "the quoted total must be read from the quote's own stored totals, never recomputed");

        String schema = Files.readString(SCHEMA);
        List<String> undeclared = new ArrayList<>();
        Pattern quotedPath = Pattern.compile("\\bquoted\\.([A-Za-z]+)\\b");
        for (Path template : List.of(PRICE_SHEET, COSTING_SHEET)) {
            Matcher matcher = quotedPath.matcher(Files.readString(template));
            while (matcher.find()) {
                String field = matcher.group(1);
                if (!schema.contains("\"path\": \"" + field + "\"")) {
                    undeclared.add(template.getFileName() + " → quoted." + field);
                }
            }
        }
        if (!undeclared.isEmpty()) {
            fail("these template paths are not in the schema, so they render blank with no error: "
                + undeclared);
        }
        assertTrue(schema.contains("\"path\": \"quoteTotals\""),
            "quoteTotals itself must be declared, or the whole block is invisible");
    }

    @Test
    @DisplayName("the quoted figure divides by the party, and currencies are never mixed")
    void perGuestDividesByTheParty() throws Exception {
        /*
         * The one piece of arithmetic this class adds. Everything else it does is plumbing, and
         * plumbing is pinned by the tests above; this is a number a client reads.
         */
        Quote quote = Quote.builder()
            .quoteCode("QT-TEST-1")
            .taxPercentage(new BigDecimal("18.00"))
            .subtotals(List.of(price("USD", "17156.72"), price("TZS", "500000.00")))
            .taxes(List.of(price("USD", "1156.68")))
            .discounts(List.of(price("USD", "0.00")))
            .grandTotals(List.of(price("USD", "18313.40"), price("TZS", "500000.00")))
            .build();

        Method quotedFigures = QuoteCostingDocumentService.class
            .getDeclaredMethod("quotedFigures", Quote.class, int.class);
        quotedFigures.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<FullCostEstimationDTO.QuoteTotalsDTO> rows =
            (List<FullCostEstimationDTO.QuoteTotalsDTO>) quotedFigures.invoke(
                new QuoteCostingDocumentService(null, null, null, null, null), quote, 6);

        assertEquals(2, rows.size(), "one row per currency, and neither folded into the other");

        FullCostEstimationDTO.QuoteTotalsDTO usd = rows.stream()
            .filter(r -> "USD".equals(r.getCurrency())).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("18313.40").compareTo(usd.getGrandTotal()));
        assertEquals(0, new BigDecimal("1156.68").compareTo(usd.getTaxAmount()));
        assertEquals(0, new BigDecimal("3052.23").compareTo(usd.getPerGuest()),
            "six travelling, so the per-guest figure is the quoted total over six");

        FullCostEstimationDTO.QuoteTotalsDTO tzs = rows.stream()
            .filter(r -> "TZS".equals(r.getCurrency())).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("500000.00").compareTo(tzs.getGrandTotal()));
        assertEquals(null, tzs.getTaxAmount(),
            "no tax was recorded in TZS, and inventing a zero would read as a tax-free currency");

        /* nobody travelling cannot be divided by, and a crash on a document is not an answer */
        @SuppressWarnings("unchecked")
        List<FullCostEstimationDTO.QuoteTotalsDTO> noParty =
            (List<FullCostEstimationDTO.QuoteTotalsDTO>) quotedFigures.invoke(
                new QuoteCostingDocumentService(null, null, null, null, null), quote, 0);
        assertFalse(noParty.isEmpty(), "the totals still print when the party is not recorded");
        assertEquals(null, noParty.get(0).getPerGuest(),
            "with nobody recorded there is no per-guest figure, and a zero would be a lie");
    }

    private static Price price(String currency, String amount) {
        return Price.builder()
            .currency(currency)
            .quantity(1)
            .unitPrice(new BigDecimal(amount))
            .totalPrice(new BigDecimal(amount))
            .build();
    }
}
