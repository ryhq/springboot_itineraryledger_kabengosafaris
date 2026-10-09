package com.itineraryledger.kabengosafaris.PdfDocument;

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

/**
 * A credit note that cannot be printed is a credit note the customer never sees.
 *
 * <p>The FULL_CREDIT_NOTE document type shipped complete on every side but one. The type was
 * seeded and enabled, {@code full-credit-note-schema.json} described twenty-five fields, and
 * {@code full_credit_note_default.html} rendered all of them. Nothing ever reached it: the
 * dispatcher in PdfGenerationService had a case for the other five document types and none for
 * this one, so every request fell to the default branch and came back
 * {@code "PDF document type not found: FULL_CREDIT_NOTE"}.
 *
 * <p>Underneath that was a second fault which the wiring had been hiding. The type was registered
 * against {@code CreditNoteDTO}, the flat list DTO, which carries {@code customerId} and
 * {@code invoiceCode} and no customer, invoice or line items at all. Had the dispatcher been
 * wired to it, the template would have rendered a credit note that could not say who it was for
 * or what it credited, and nothing would have thrown: SpEL answers a missing property with
 * nothing, and a PDF comes out with blanks where the money should be.
 *
 * <p>So the test is not "does a PDF appear". It is that the model the type declares can actually
 * answer every question the template and the schema ask of it.
 */
class ACreditNoteCanBePrintedTest {

    private static final String DOCUMENT = "FULL_CREDIT_NOTE";
    private static final Path TEMPLATE =
        Path.of("src/main/resources/templates/pdf-templates/full_credit_note_default.html");
    private static final Path SCHEMA =
        Path.of("src/main/resources/schemas/pdf-documents/full-credit-note-schema.json");

    @Test
    @DisplayName("the declared data source is a class that exists")
    void theDataSourceClassLoads() throws Exception {
        String className = PdfDocumentVariables.getDataSourceClass(DOCUMENT);
        assertNotNull(className, "FULL_CREDIT_NOTE declares no data source class");
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
    @DisplayName("every path the template renders resolves on the declared model")
    void theTemplateMatchesTheModel() throws Exception {
        Class<?> model = Class.forName(PdfDocumentVariables.getDataSourceClass(DOCUMENT));
        String root = PdfDocumentVariables.getRootVariableName(DOCUMENT);
        assertTrue("creditNote".equals(root), "the template addresses the model as creditNote, not " + root);

        String html = Files.readString(TEMPLATE);
        Matcher matcher = Pattern.compile("\\$\\{" + root + "\\.([A-Za-z][A-Za-z0-9.]*)").matcher(html);

        Set<String> paths = new LinkedHashSet<>();
        while (matcher.find()) {
            paths.add(matcher.group(1));
        }
        assertTrue(paths.size() > 10, "found only " + paths.size() + " paths; the scan is not working");

        List<String> unresolved = new ArrayList<>();
        for (String path : paths) {
            if (resolve(model, path) == null) {
                unresolved.add(path);
            }
        }

        if (!unresolved.isEmpty()) {
            fail("the template reads " + unresolved + " on " + model.getSimpleName()
                + ", which has no such property; SpEL would print nothing and the PDF would come out blank there");
        }
    }

    /**
     * Walk a dotted path the way SpEL would, by getter, returning the type it lands on.
     *
     * <p>A list is walked as itself, so {@code lineItems.isEmpty} resolves against List rather
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
