package com.itineraryledger.kabengosafaris.Text;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The build fails if punctuation nobody types gets back into anything we ship.
 *
 * <p>The content read as machine-written, and the tell was never the wording. It was em dashes
 * joining clauses, middle dots separating a footer, curly quotes, ellipsis glyphs and check marks
 * in subject lines. A reader does not think "an em dash"; they think "this was not written by a
 * person", and on a quotation for a safari that is the wrong first impression. One sweep fixed it
 * once. This is what stops the next template, the next seeded blog post and the next email subject
 * from putting it back.
 *
 * <p>Modelled on {@link com.itineraryledger.kabengosafaris.CompanyProfile.CompanyLiteralsInTemplatesTest},
 * which does the same job for a company's own details, and reports in the same shape:
 * {@code file:line  'what'  ->  what to do instead}.
 *
 * <h2>Two deliberate holes</h2>
 *
 * <p><b>Comments are not scanned.</b> Not in Java, not in a template's CSS. A comment is written
 * for whoever reads the source and never reaches a client, so holding it to the same standard
 * would only make this test something people switch off. The Java scan therefore walks string
 * literals with a small state machine rather than grepping lines.
 *
 * <p><b>Box drawing is allowed.</b> {@link Typography#FOLD_INTERNAL} is not checked here, because
 * its characters only ever appear in banners and diagrams, which are comments by another name.
 *
 * <h2>What it will never flag</h2>
 *
 * <p>Letters. Any letter, in any script. Five locale catalogues are built from accented letters,
 * one is entirely Cyrillic, and a lodge called Sueños keeps its name. The check is for punctuation
 * and symbols, never for "not ASCII", and {@link Typography#KEEP} holds the symbols that are
 * content: currency and the copyright sign.
 */
class ShippedTextIsTypedTest {

    /** Where text we ship to somebody lives. */
    private static final List<Path> SCANNED_RESOURCES = List.of(
        Path.of("src/main/resources/templates"),
        Path.of("src/main/resources/schemas"),
        Path.of("src/main/resources/seed"));

    private static final Path JAVA = Path.of("src/main/java");

    /**
     * The three decode tables, which must hold the real characters because decoding is their job,
     * the map itself, and the one parser that deliberately still reads an arrow because people
     * typed arrows for years.
     */
    private static final List<String> EXEMPT = List.of(
        "Text/Typography.java",
        "EmailMessage/Services/HtmlToText.java",
        "PdfDocument/Services/PdfTemplateValidationService.java",
        "Translation/Services/TranslationService.java",
        "Flight/Services/FlightRouteService.java");

    @Test
    @DisplayName("no shipped template, schema or seed file carries punctuation nobody types")
    void resourcesAreTyped() throws IOException {
        List<String> offences = new ArrayList<>();

        for (Path root : SCANNED_RESOURCES) {
            if (!Files.isDirectory(root)) continue;
            try (Stream<Path> files = Files.walk(root)) {
                for (Path file : files.filter(Files::isRegularFile).toList()) {
                    List<String> lines;
                    try {
                        lines = Files.readAllLines(file);
                    } catch (java.io.UncheckedIOException | java.nio.charset.MalformedInputException e) {
                        continue; /* not text */
                    }
                    for (int i = 0; i < lines.size(); i++) {
                        collect(offences, file + ":" + (i + 1), lines.get(i));
                    }
                }
            }
        }

        assertTrue(offences.isEmpty(), () -> describe(offences,
            "These reach a client, a supplier or a visitor."));
    }

    @Test
    @DisplayName("no Java string literal carries it either, comments excepted")
    void literalsAreTyped() throws IOException {
        List<String> offences = new ArrayList<>();

        try (Stream<Path> files = Files.walk(JAVA)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String path = file.toString().replace('\\', '/');
                if (EXEMPT.stream().anyMatch(path::endsWith)) continue;

                String body = Files.readString(file);
                for (Literal literal : literalsOf(body)) {
                    collect(offences, file + ":" + literal.line, literal.text);
                }
            }
        }

        assertTrue(offences.isEmpty(), () -> describe(offences,
            "A string literal is text somebody reads: an email subject, a document line, a "
                + "message the panel shows, a value seeded into the database. Comments are not "
                + "scanned, so a dash in your explanation is fine."));
    }

    @Test
    @DisplayName("nor written as an HTML entity, which a reader cannot tell apart")
    void entitiesAreNotALoophole() throws IOException {
        List<String> offences = new ArrayList<>();

        for (Path root : SCANNED_RESOURCES) {
            if (!Files.isDirectory(root)) continue;
            try (Stream<Path> files = Files.walk(root)) {
                for (Path file : files.filter(Files::isRegularFile).toList()) {
                    List<String> lines;
                    try {
                        lines = Files.readAllLines(file);
                    } catch (java.io.UncheckedIOException
                             | java.nio.charset.MalformedInputException e) {
                        continue;
                    }
                    for (int i = 0; i < lines.size(); i++) {
                        String line = lines.get(i);
                        for (Map.Entry<String, String> entity : Typography.ENTITIES.entrySet()) {
                            if (line.contains(entity.getKey())) {
                                /*
                                 * An empty replacement means the character is one the map
                                 * refuses to substitute automatically, so say that rather than
                                 * telling somebody to replace an arrow with nothing.
                                 */
                                String advice = entity.getValue().isEmpty()
                                    ? "decide: this one needs a sentence, see \"manual\" in "
                                        + "typography.json"
                                    : "'" + entity.getValue() + "'";
                                offences.add(String.format("%s:%d  '%s'  ->  %s",
                                    file, i + 1, entity.getKey(), advice));
                            }
                        }
                    }
                }
            }
        }

        assertTrue(offences.isEmpty(), () -> describe(offences,
            "A rendered invoice showed arrows and em dashes long after every template body read "
                + "clean, because the markup held &rarr; and &mdash; rather than the characters. "
                + "A reader cannot tell the difference, so neither does this."));
    }

    private void collect(List<String> offences, String where, String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Typography.KEEP.containsKey(c)) continue;

            if (Typography.FOLD.containsKey(c)) {
                offences.add(String.format("%s  '%c'  ->  '%s'", where, c, Typography.FOLD.get(c)));
            } else if (Typography.MANUAL.containsKey(c)) {
                offences.add(String.format("%s  '%c'  ->  %s", where, c, Typography.MANUAL.get(c)));
            } else if (c > 127 && !Character.isLetter(c)
                && Character.getType(c) != Character.NON_SPACING_MARK
                && !Typography.FOLD_INTERNAL.containsKey(c)) {
                offences.add(String.format("%s  '%c' (U+%04X)  ->  nobody has decided about this "
                    + "one. Add it to typography.json, in all four repositories.", where, c, (int) c));
            }
        }
    }

    private String describe(List<String> offences, String why) {
        StringBuilder message = new StringBuilder()
            .append(offences.size())
            .append(" character(s) nobody types. ")
            .append(why)
            .append("\n\nThe map is src/main/resources/typography.json.\n\n");
        offences.stream().limit(60).forEach(o -> message.append("  ").append(o).append('\n'));
        if (offences.size() > 60) {
            message.append("  ... and ").append(offences.size() - 60).append(" more\n");
        }
        return message.toString();
    }

    private record Literal(int line, String text) {
    }

    /**
     * Java string and text-block literals, with their line numbers.
     *
     * <p>A state machine rather than a regular expression, because the point is to know what is a
     * comment. {@code "an em dash in a sentence like this"} inside {@code //} is somebody
     * explaining themselves, and flagging it would make this test an annoyance people delete.
     */
    private List<Literal> literalsOf(String src) {
        List<Literal> found = new ArrayList<>();
        int i = 0;
        int line = 1;
        int n = src.length();

        while (i < n) {
            char c = src.charAt(i);
            if (c == '\n') { line++; i++; continue; }

            if (src.startsWith("//", i)) {
                while (i < n && src.charAt(i) != '\n') i++;
                continue;
            }
            if (src.startsWith("/*", i)) {
                int end = src.indexOf("*/", i + 2);
                if (end < 0) break;
                line += (int) src.substring(i, end).chars().filter(ch -> ch == '\n').count();
                i = end + 2;
                continue;
            }
            if (src.startsWith("\"\"\"", i)) {
                int end = src.indexOf("\"\"\"", i + 3);
                if (end < 0) break;
                String text = src.substring(i + 3, end);
                found.add(new Literal(line, text));
                line += (int) text.chars().filter(ch -> ch == '\n').count();
                i = end + 3;
                continue;
            }
            if (c == '"' || c == '\'') {
                char quote = c;
                int start = ++i;
                while (i < n && src.charAt(i) != quote && src.charAt(i) != '\n') {
                    if (src.charAt(i) == '\\') i++;
                    i++;
                }
                found.add(new Literal(line, src.substring(start, Math.min(i, n))));
                if (i < n && src.charAt(i) == '\n') line++;
                i++;
                continue;
            }
            i++;
        }
        return found;
    }
}
