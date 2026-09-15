package com.itineraryledger.kabengosafaris.PdfDocument;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.Inclusion.DTOs.InclusionLineDTO;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.FullInvoiceDTO;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.FullItineraryDTO;
import com.itineraryledger.kabengosafaris.Quote.DTOs.FullQuoteDTO;
import com.itineraryledger.kabengosafaris.Safari.DTOs.FullSafariDTO;

/**
 * The templates iterate {@code ${quote.inclusionsList}}, and SpEL resolves that by calling a getter.
 *
 * <p>Which makes the getter's exact name and return type load-bearing in a way the compiler cannot
 * see: rename it, return an array, or let it answer null, and nine templates stop printing with no
 * error anywhere — the PDF simply comes out without the section, exactly as it did before any of
 * this was built.
 *
 * <p>{@code full_safari_modern.html:718} is the standing proof that this happens: it renders
 * {@code safari.itinerary.highlightsList} against a DTO that has no such getter, and has done for
 * as long as anybody has looked.
 */
class ADocumentCanIterateWhatItCoversTest {

    private static final List<Class<?>> DOCUMENTS =
        List.of(FullItineraryDTO.class, FullQuoteDTO.class, FullSafariDTO.class, FullInvoiceDTO.class);

    @Test
    @DisplayName("every document DTO answers inclusionsList and exclusionsList with a List")
    void theGettersExistOnEveryDocument() throws Exception {
        for (Class<?> type : DOCUMENTS) {
            for (String name : List.of("getInclusionsList", "getExclusionsList")) {
                Method method = type.getMethod(name);
                assertTrue(List.class.isAssignableFrom(method.getReturnType()),
                    type.getSimpleName() + "." + name + " must return a List for th:each to iterate it");
            }
        }
    }

    @Test
    @DisplayName("a document with nothing recorded answers an empty list, never null")
    void nothingRecordedIsEmptyNotNull() throws Exception {
        for (Class<?> type : DOCUMENTS) {
            Object dto = type.getDeclaredConstructor().newInstance();
            for (String name : List.of("getInclusionsList", "getExclusionsList")) {
                Object value = type.getMethod(name).invoke(dto);
                assertNotNull(value, type.getSimpleName() + "." + name
                    + " returned null; #lists.isEmpty(null) is true so the block would hide, but a "
                    + "template that iterates without the guard would throw mid-render");
                assertTrue(((List<?>) value).isEmpty());
            }
        }
    }

    @Test
    @DisplayName("the two lists are what the document states, each side separately")
    void eachSideIsWhatWasStated() {
        FullQuoteDTO quote = new FullQuoteDTO();
        quote.setInclusions(List.of(
            InclusionLineDTO.builder().label("All park, conservation & crater-service fees")
                .isIncluded(true).build(),
            InclusionLineDTO.builder().label("International flights & visas")
                .isIncluded(false).build(),
            InclusionLineDTO.builder().label("Professional multilingual safari guide")
                .isIncluded(true).build()));

        assertIterableEquals(
            List.of("All park, conservation & crater-service fees",
                    "Professional multilingual safari guide"),
            quote.getInclusionsList(),
            "order must be the order the rows came in — it is the print order");
        assertIterableEquals(
            List.of("International flights & visas"),
            quote.getExclusionsList());
    }

    @Test
    @DisplayName("a line with no side stated prints under neither")
    void anUnstatedSidePrintsNowhere() {
        /*
         * isIncluded is NOT NULL in the database, so this should not arise — but a DTO is also
         * built by hand in previews and tests, and a null defaulting to "included" would put a
         * sentence on the wrong half of a customer's document.
         */
        FullInvoiceDTO invoice = new FullInvoiceDTO();
        invoice.setInclusions(List.of(
            InclusionLineDTO.builder().label("Ambiguous").isIncluded(null).build()));

        assertTrue(invoice.getInclusionsList().isEmpty(),
            "a line that does not say it is included must not be printed as included");
        assertIterableEquals(List.of("Ambiguous"), invoice.getExclusionsList());
    }

    @Test
    @DisplayName("a blank line is dropped rather than printed as an empty bullet")
    void blankLinesAreDropped() {
        FullSafariDTO safari = new FullSafariDTO();
        safari.setInclusions(List.of(
            InclusionLineDTO.builder().label("  ").isIncluded(true).build(),
            InclusionLineDTO.builder().label(null).isIncluded(true).build(),
            InclusionLineDTO.builder().label("  Drinking water on game drives  ").isIncluded(true).build()));

        assertIterableEquals(List.of("Drinking water on game drives"), safari.getInclusionsList());
    }
}
