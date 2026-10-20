package com.itineraryledger.kabengosafaris.PdfDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itineraryledger.kabengosafaris.Quote.DTOs.FullQuoteDTO;

/**
 * The first thing a client does with a party total is divide it by the number of people going.
 *
 * <p>No quote had ever printed that figure, and no quote template could have: {@code FullQuoteDTO}
 * carried no headcount at all. {@code grep -i "per person"} across all three quote templates
 * returned nothing, which looked like a styling choice and was actually a missing field. The
 * quote's own {@code paxList} held the number the whole time; nothing read it into the model the
 * PDF binds.
 *
 * <p>Two things can go wrong once it is wired, and neither throws. A schema path that no longer
 * resolves renders as nothing, because SpEL answers a missing property with silence, so a client
 * gets a quote with a blank where the per-guest price should be. And a per-guest figure over a
 * mixed party is an average, not a price: a resident child and a non-resident adult are different
 * money, and a reader who multiplies the average by their own headcount lands somewhere we never
 * quoted. So the label has to change when the party does.
 */
class AQuoteSaysWhatEachGuestPaysTest {

    private static final String DOCUMENT = "FULL_QUOTE";
    private static final Path SCHEMA =
        Path.of("src/main/resources/schemas/pdf-documents/full-quote-schema.json");
    private static final List<Path> TEMPLATES = List.of(
        Path.of("src/main/resources/templates/pdf-templates/full_quote_default.html"),
        Path.of("src/main/resources/templates/pdf-templates/full_quote_multicurrency.html"),
        Path.of("src/main/resources/templates/pdf-templates/full_quote_payment_schedule.html"));

    @Test
    @DisplayName("the declared data source is a class that exists")
    void theDataSourceClassLoads() throws Exception {
        String className = PdfDocumentVariables.getDataSourceClass(DOCUMENT);
        assertNotNull(className, DOCUMENT + " declares no data source class");
        assertNotNull(Class.forName(className), className + " does not exist");
    }

    @Test
    @DisplayName("every field the schema promises is readable on the declared model")
    void theSchemaMatchesTheModel() throws Exception {
        Class<?> model = Class.forName(PdfDocumentVariables.getDataSourceClass(DOCUMENT));
        JsonNode schema = new ObjectMapper().readTree(Files.readString(SCHEMA));

        List<String> missing = new ArrayList<>();
        for (JsonNode field : schema) {
            String path = field.path("path").asText();
            if (path.isEmpty()) continue;
            if (resolve(model, path) == null) {
                missing.add(path);
            }
        }

        if (!missing.isEmpty()) {
            fail(model.getSimpleName() + " cannot answer " + missing
                + ", which the schema tells template authors it will");
        }
    }

    @Test
    @DisplayName("every path all three quote templates render resolves on the declared model")
    void theTemplatesMatchTheModel() throws Exception {
        Class<?> model = Class.forName(PdfDocumentVariables.getDataSourceClass(DOCUMENT));
        String root = PdfDocumentVariables.getRootVariableName(DOCUMENT);
        assertEquals("quote", root, "the templates address the model as quote, not " + root);

        Pattern reference = Pattern.compile("\\$\\{" + root + "\\.([A-Za-z][A-Za-z0-9.]*)");
        List<String> unresolved = new ArrayList<>();
        int scanned = 0;

        for (Path template : TEMPLATES) {
            Matcher matcher = reference.matcher(Files.readString(template));
            Set<String> paths = new LinkedHashSet<>();
            while (matcher.find()) {
                paths.add(matcher.group(1));
            }
            assertTrue(paths.size() > 5,
                template.getFileName() + " yielded only " + paths.size() + " paths; the scan is not working");
            scanned += paths.size();
            for (String path : paths) {
                if (resolve(model, path) == null) {
                    unresolved.add(template.getFileName() + " -> " + path);
                }
            }
        }

        assertTrue(scanned > 30, "found only " + scanned + " paths across three templates");
        if (!unresolved.isEmpty()) {
            fail("these read properties " + model.getSimpleName() + " does not have: " + unresolved
                + "; SpEL would print nothing and the PDF would come out blank there");
        }
    }

    @Test
    @DisplayName("the per-guest figure is only called a price when the whole party is one band")
    void aMixedPartyIsLabelledAnAverage() {
        FullQuoteDTO oneBand = new FullQuoteDTO();
        oneBand.setTotalPax(6);
        oneBand.setPerGuestIsExact(true);
        assertEquals("Per guest, based on 6 travelling", oneBand.getPerGuestBasisLabel());

        FullQuoteDTO mixed = new FullQuoteDTO();
        mixed.setTotalPax(6);
        mixed.setPerGuestIsExact(false);
        assertEquals("Average per guest, based on 6 travelling", mixed.getPerGuestBasisLabel(),
            "a non-resident adult and a resident child are not the same money, so a party "
            + "spanning both must not be described as a per-guest price");

        FullQuoteDTO noPax = new FullQuoteDTO();
        assertEquals(null, noPax.getPerGuestBasisLabel(),
            "with no headcount there is nothing to divide by and nothing to say");
    }

    @Test
    @DisplayName("a pax band reads as one phrase even when half of it is missing")
    void aBandLabelsItself() {
        assertEquals("Non-Resident Adult", FullQuoteDTO.PaxBandDTO.builder()
            .nationCategoryName("Non-Resident").ageCategoryName("Adult").count(6).build().getLabel());
        assertEquals("Adult", FullQuoteDTO.PaxBandDTO.builder()
            .ageCategoryName("Adult").count(2).build().getLabel());
        assertEquals("Non-Resident", FullQuoteDTO.PaxBandDTO.builder()
            .nationCategoryName("Non-Resident").count(2).build().getLabel());
        assertEquals(null, FullQuoteDTO.PaxBandDTO.builder().count(2).build().getLabel(),
            "an unnamed band prints nothing rather than a stray space");
    }

    /**
     * Walk a dotted path the way SpEL would, by getter, returning the type it lands on.
     *
     * <p>A list is walked as itself, so {@code grandTotals.isEmpty} resolves against List rather
     * than against whatever the list holds.
     */
    private static Class<?> resolve(Class<?> type, String path) {
        Class<?> current = type;
        for (String segment : path.split("\\.")) {
            current = property(current, segment);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private static Class<?> property(Class<?> type, String segment) {
        String capitalised = Character.toUpperCase(segment.charAt(0)) + segment.substring(1);
        for (String candidate : List.of("get" + capitalised, "is" + capitalised, segment)) {
            try {
                Method method = type.getMethod(candidate);
                return method.getReturnType();
            } catch (NoSuchMethodException ignored) {
                // try the next shape
            }
        }
        return null;
    }
}
