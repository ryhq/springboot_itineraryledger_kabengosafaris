package com.itineraryledger.kabengosafaris.Flight;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A listing has to survive being asked for with none of its optional parameters, because that is how
 * it is asked for most of the time: it is the first page the panel opens and every call the importer
 * makes.
 *
 * <p>Two of these shipped to production on the same day, behind a suite of 346 passing tests, and
 * both were found by calling the API rather than by any test. They are the same mistake twice — a
 * helper that rejects null, handed the null that absence produces:
 *
 * <ol>
 *   <li>{@code SORTABLE.contains(sortBy)} — {@code List.of} forbids nulls, and that extends to
 *       reads, so {@code contains(null)} throws NullPointerException instead of answering false.
 *       Every flight listing answered 500.</li>
 *   <li>{@code idObfuscator.decodeId(airlineId)} inside a filter — it throws "Hash cannot be null or
 *       empty", so the UNFILTERED listing answered 400.</li>
 * </ol>
 *
 * <p>Source-level tests like this one cannot see a runtime failure, which is exactly why neither was
 * caught before release; what they can do is stop the same line being written again.
 */
class AListingSurvivesItsOptionalParametersTest {

    private static final Path SERVICES =
        Path.of("src/main/java/com/itineraryledger/kabengosafaris/Flight/Services");

    /** `SORTABLE.contains(sortBy)` with nothing guarding sortBy against null. */
    private static final Pattern UNGUARDED_CONTAINS = Pattern.compile(
        "(?<!!= null && )\\b([A-Z_]+)\\.contains\\(\\s*(sortBy|sortDirection)\\s*\\)");

    private static final Pattern DECODE = Pattern.compile("idObfuscator\\.decodeId\\(");

    @Test
    @DisplayName("no listing tests an immutable list for a sort parameter that may be null")
    void noUnguardedContains() throws IOException {
        List<String> offences = new ArrayList<>();

        for (Path file : javaFiles()) {
            String source = Files.readString(file);
            Matcher m = UNGUARDED_CONTAINS.matcher(source);
            while (m.find()) {
                int line = 1 + (int) source.substring(0, m.start()).chars().filter(c -> c == '\n').count();
                offences.add(file.getFileName() + ":" + line + "  " + m.group().trim());
            }
        }

        assertTrue(offences.isEmpty(), () ->
            "List.of() throws NullPointerException from contains(null) rather than answering false, "
            + "and the sort parameter is absent on most requests. Guard it:\n\n"
            + "    sortBy != null && SORTABLE.contains(sortBy)\n\n"
            + String.join("\n", offences));
    }

    @Test
    @DisplayName("no filter decodes an optional id without allowing for it being absent")
    void noUnguardedDecodeInAFilter() throws IOException {
        List<String> offences = new ArrayList<>();

        for (Path file : javaFiles()) {
            String source = Files.readString(file);
            for (String body : buildSpecBodies(source)) {
                if (DECODE.matcher(body).find()) {
                    offences.add(file.getFileName()
                        + "  decodeId inside buildSpec — use optionalId(...) instead");
                }
            }
        }

        assertTrue(offences.isEmpty(), () ->
            "decodeId throws \"Hash cannot be null or empty\" on a null, and a filter id is absent on "
            + "most requests, so the unfiltered listing answers 400:\n\n" + String.join("\n", offences));
    }

    @Test
    @DisplayName("the traps themselves, so the reasons survive even if the patterns are rewritten")
    void theHelpersThatRejectNull() {
        List<String> sortable = List.of("name", "code");

        assertThrows(NullPointerException.class, () -> sortable.contains(null),
            "if this ever stops throwing, the first guard is no longer needed and can go");

        /* The shape that is safe, and the one every listing here uses. */
        String sortBy = null;
        assertDoesNotThrow(() -> sortBy != null && sortable.contains(sortBy) ? sortBy : "name");
    }

    private static List<Path> javaFiles() throws IOException {
        try (Stream<Path> files = Files.walk(SERVICES)) {
            return files.filter(p -> p.toString().endsWith(".java")).toList();
        }
    }

    /**
     * The BODY of each buildSpec method, found by brace matching.
     *
     * <p>Scoped deliberately narrowly. A first cut of this read from the first {@code buildSpec(} to
     * the end of the file and flagged six innocent lines: decodeId in create, update and delete,
     * where the id is a required path parameter that cannot be null, and the one inside
     * {@code optionalId} itself. A check that cries wolf is a check somebody switches off, so it
     * matches only where the rule actually applies.
     */
    private static final Pattern BUILD_SPEC_DECLARATION =
        Pattern.compile("private\\s+Specification<[^>]+>\\s+buildSpec\\s*\\(");

    private static List<String> buildSpecBodies(String source) {
        List<String> bodies = new ArrayList<>();
        Matcher declaration = BUILD_SPEC_DECLARATION.matcher(source);
        int from = 0;
        while (true) {
            /*
             * The DECLARATION, not the name. Matching "buildSpec(" alone finds the call site inside
             * getAll first, and brace-matching from there walks the wrong block entirely — which is
             * how this test managed to flag two services that have no decodeId in a filter at all.
             */
            if (!declaration.find(from)) return bodies;
            int at = declaration.start();
            int open = source.indexOf('{', at);
            if (open < 0) return bodies;
            int depth = 0;
            int end = -1;
            for (int i = open; i < source.length(); i++) {
                char c = source.charAt(i);
                if (c == '{') depth++;
                else if (c == '}' && --depth == 0) { end = i; break; }
            }
            if (end < 0) return bodies;
            bodies.add(source.substring(open, end));
            from = end;
        }
    }
}
