package com.itineraryledger.kabengosafaris.ClientPortal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Two controllers answering one path do not fight it out at runtime. The application refuses to
 * start.
 *
 * <p>The portal has two front ends on purpose: server-rendered pages, which work with no
 * JavaScript and no second hostname, and a JSON surface for the React pages built from the
 * panel's source. They share their rules, their view model and their access checks, and they
 * necessarily share paths, because the rate limiter matches a path prefix and both have to be
 * counted.
 *
 * <p>Spring will take two handlers for one path only when they differ in what they consume, and
 * that distinction is easy to drop in a hurry. Dropping it is not a bug that shows up in a test
 * run or a code review: it is an {@code IllegalStateException} at boot, on both companies at
 * once, several minutes after a deploy reported success.
 *
 * <p><strong>Nothing else here would catch it.</strong> The one test that starts the Spring
 * context is disabled for want of a real database, so there is no other moment between writing
 * the mapping and the application failing to come up.
 */
class TwoHandlersCannotClaimOnePathTest {

    private static final Path CONTROLLERS =
        Path.of("src/main/java/com/itineraryledger/kabengosafaris/ClientPortal/Controller");

    /** method + path -> the files that claim it, and what each says it consumes. */
    private record Claim(String file, String consumes) {}

    @Test
    @DisplayName("no two portal handlers claim one path without saying what each consumes")
    void everySharedPathIsDistinguished() throws IOException {
        Map<String, List<Claim>> claims = new LinkedHashMap<>();

        Pattern classMapping = Pattern.compile("@RequestMapping\\(\"([^\"]+)\"\\)");
        Pattern methodMapping = Pattern.compile(
            "@(Get|Post|Put|Patch|Delete)Mapping\\(\\s*(?:value\\s*=\\s*)?\"([^\"]*)\"([^)]*)\\)");

        for (Path file : controllers()) {
            String source = Files.readString(file);

            Matcher base = classMapping.matcher(source);
            String prefix = base.find() ? base.group(1) : "";

            Matcher mapping = methodMapping.matcher(source);
            while (mapping.find()) {
                String key = mapping.group(1).toUpperCase() + " " + prefix + mapping.group(2);
                String rest = mapping.group(3);
                String consumes = rest.contains("consumes")
                    ? rest.replaceAll("(?s).*consumes\\s*=\\s*", "").trim()
                    : "";
                claims.computeIfAbsent(key, k -> new ArrayList<>())
                    .add(new Claim(file.getFileName().toString(), consumes));
            }
        }

        assertFalse(claims.isEmpty(), "found no portal mappings — this guard would check nothing");

        List<String> clashes = new ArrayList<>();
        for (Map.Entry<String, List<Claim>> entry : claims.entrySet()) {
            List<Claim> holders = entry.getValue();
            if (holders.size() < 2) continue;

            boolean allSaySomething = holders.stream().allMatch(c -> !c.consumes().isBlank());
            long distinct = holders.stream().map(Claim::consumes).distinct().count();

            if (!allSaySomething || distinct != holders.size()) {
                clashes.add(entry.getKey() + " is claimed by "
                    + holders.stream().map(c -> c.file()
                        + (c.consumes().isBlank() ? " (consumes anything)" : " (" + c.consumes() + ")"))
                        .toList());
            }
        }

        if (!clashes.isEmpty()) {
            fail("""
                Two handlers claim the same path and do not say what each one consumes.

                Spring does not pick one. It refuses to start, on every company at once, minutes
                after the deploy has reported success, and the only clue is an
                IllegalStateException in a log nobody is reading yet.

                %s""".formatted(String.join("\n                ", clashes)));
        }
    }

    @Test
    @DisplayName("the two front ends share their access rules rather than each having their own")
    void bothFrontEndsAskTheSameQuestion() throws IOException {
        /*
         * The reason two front ends are tolerable at all. They may differ in how a page is
         * drawn; they must not differ in who is allowed to see it, because the second copy of a
         * security rule is the one that is wrong.
         *
         * The admin controller is excluded, and the distinction is the point rather than an
         * exception: it serves STAFF holding a token and a PERM_ authority, and it has no client
         * session to resolve. A file that served a customer and authorised them some other way
         * is exactly what this is looking for.
         */
        for (Path file : controllers()) {
            String name = file.getFileName().toString();
            if (name.contains("Admin")) {
                String adminSource = Files.readString(file);
                assertTrue(adminSource.contains("@PreAuthorize"),
                    name + " serves staff, so it is authorised by permission. If it has stopped "
                    + "being staff-only it must resolve a client session like the others");
                continue;
            }

            String source = Files.readString(file);
            if (!source.contains("PortalAccessService")) continue;

            assertTrue(source.contains("access.resolve("),
                name + " must resolve access through PortalAccessService rather than deciding "
                + "for itself who may see what");
        }
    }

    private List<Path> controllers() throws IOException {
        try (Stream<Path> files = Files.walk(CONTROLLERS)) {
            return files.filter(f -> f.toString().endsWith("Controller.java")).toList();
        }
    }
}
