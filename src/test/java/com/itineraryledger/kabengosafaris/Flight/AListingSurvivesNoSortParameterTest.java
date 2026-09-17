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
 * A listing must survive being asked for without a sort, which is how it is asked for most of the time.
 *
 * <p>{@code List.of(...)} forbids nulls, and that extends to {@code contains(null)}: it throws
 * NullPointerException rather than answering false. So
 *
 * <pre>String sortField = SORTABLE.contains(sortBy) ? sortBy : "name";</pre>
 *
 * reads as a harmless default and is a 500 on every request that does not name a sort field — which
 * is every request the panel makes when it first opens a page, and every call the importer makes.
 *
 * <p>Found in production, not in a test: all four flight listings answered "An unexpected error
 * occurred" the moment they were called, minutes after a green deploy. The whole suite passed,
 * because nothing exercised the endpoints with the parameter absent.
 *
 * <p>This reads the source rather than standing up the services, so it costs nothing and still fails
 * the build for the next person who writes the same line.
 */
class AListingSurvivesNoSortParameterTest {

    private static final Path SERVICES =
        Path.of("src/main/java/com/itineraryledger/kabengosafaris/Flight/Services");

    /** `SORTABLE.contains(sortBy)` with nothing guarding sortBy against null. */
    private static final Pattern UNGUARDED = Pattern.compile(
        "(?<!!= null && )\\b([A-Z_]+)\\.contains\\(\\s*(sortBy|sortDirection)\\s*\\)");

    @Test
    @DisplayName("no listing tests an immutable list for a parameter that may be null")
    void noUnguardedContains() throws IOException {
        List<String> offences = new ArrayList<>();

        try (Stream<Path> files = Files.walk(SERVICES)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                Matcher m = UNGUARDED.matcher(source);
                while (m.find()) {
                    int line = 1 + (int) source.substring(0, m.start()).chars().filter(c -> c == '\n').count();
                    offences.add(file.getFileName() + ":" + line + "  " + m.group().trim());
                }
            }
        }

        assertTrue(offences.isEmpty(), () ->
            "List.of() throws NullPointerException from contains(null) rather than answering false, "
            + "and the sort parameter is absent on most requests. Guard it:\n\n"
            + "    sortBy != null && SORTABLE.contains(sortBy)\n\n"
            + String.join("\n", offences));
    }

    @Test
    @DisplayName("the trap itself, so the reason survives even if the regex is rewritten")
    void immutableListsRejectNull() {
        List<String> sortable = List.of("name", "code");

        assertThrows(NullPointerException.class, () -> sortable.contains(null),
            "if this ever stops throwing, the guard above is no longer needed and this test should go");

        /* The shape that is safe, and the one every listing here uses. */
        String sortBy = null;
        assertDoesNotThrow(() -> {
            String unused = sortBy != null && sortable.contains(sortBy) ? sortBy : "name";
            return unused;
        });
    }
}
