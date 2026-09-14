package com.itineraryledger.kabengosafaris.Response;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A delete that refused has to say which rows it refused, and why.
 *
 * A partially paid bill was deleted from the panel and the toast said "1 Bill(s) deleted". The
 * row was still there. The service had refused it correctly -- only draft bills may be deleted --
 * counted the refusal, put it in a message string and answered 200 with a null body. The panel
 * has nothing structured to read in that case, so it reports every id it asked about as deleted
 * (crud.ts). Both halves were behaving reasonably; together they lied to the user.
 *
 * The house contract (CLAUDE.md) is explicit: deletes return
 * {@code {deletedCount, deletedIds, skipped:[{id, code, reason}]}} and never a 200 that silently
 * deleted nothing.
 *
 * So: any delete service that can decline a row must return that shape. A service that always
 * deletes what it is given has nothing to report and is not the subject here.
 */
class ARefusedDeleteSaysSoTest {

    private static final Path JAVA = Paths.get("src/main/java");

    /** Signs that a service can decline a row rather than delete it. */
    private static final Pattern CAN_REFUSE = Pattern.compile(
        "Refusing to delete|cannot be deleted|not deletable|isDeletable\\(\\)|blocked\\+\\+|skipped\\+\\+");

    /** The report the panel can actually read. */
    private static final Pattern REPORTS = Pattern.compile("deletedCount");

    /**
     * Services that decline rows but report it another way, and are not worth rewriting today.
     * Anything added here needs a reason, because the default answer is to return the report.
     */
    private static final Set<String> ALLOWED = new TreeSet<>(Set.of());

    @Test
    @DisplayName("a delete service that can refuse a row returns a report naming it")
    void everyRefusalIsReported() throws IOException {
        List<String> offences = new ArrayList<>();

        try (Stream<Path> files = Files.walk(JAVA)) {
            for (Path file : files.filter(f -> f.toString().endsWith("DeleteService.java")).sorted().toList()) {
                String name = file.getFileName().toString();
                if (ALLOWED.contains(name)) continue;

                String source = Files.readString(file);
                if (!CAN_REFUSE.matcher(source).find()) continue;      // it never declines anything
                if (REPORTS.matcher(source).find()) continue;          // it reports properly

                offences.add(name);
            }
        }

        assertTrue(offences.isEmpty(), () -> String.format("""
            %d delete service(s) can refuse a row but answer without a report:

            %s

            Return {deletedCount, deletedIds, skipped:[{id, code, reason}]} in the response data.
            A message string is not enough: the panel reads the report, and with no report it
            assumes everything it asked for was deleted -- which is how a bill that was correctly
            refused appeared to have been deleted, under a toast saying so.
            """, offences.size(), String.join("\n", offences)));
    }
}
