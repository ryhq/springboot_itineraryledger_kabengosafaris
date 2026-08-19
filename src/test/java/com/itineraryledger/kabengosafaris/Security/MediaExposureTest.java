package com.itineraryledger.kabengosafaris.Security;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Which files anybody may fetch, and which need a token.
 *
 * This exists because every media endpoint was public. That is correct for a park photograph and
 * wrong for a customer document — the module that holds passports and visas — and the difference had
 * never been written down anywhere a build could check. A URL is not a credential: links are
 * forwarded, logged by proxies, kept in browser history, and read off shared screens.
 *
 * Two lists below. A media module must appear in exactly one of them, so adding one is a decision
 * somebody makes on purpose rather than a default nobody notices.
 */
class MediaExposureTest {

    private static final Path JAVA = Paths.get("src/main/java/com/itineraryledger/kabengosafaris");
    private static final Path SECURITY = JAVA.resolve("Configurations/SecurityConfigurations.java");

    /**
     * Published material. The website renders these with a plain {@code <img>}, which carries no
     * bearer token, so a URL is the only credential — which is what "published" means.
     */
    private static final Set<String> PUBLIC_MEDIA = Set.of(
        "accommodation-images",
        "park-images",
        "activity-images",
        "park-activity-images",
        "itinerary-images",
        "hero-images",
        "blog-images",
        "testimony-images");

    /**
     * Everything else. Client paperwork, supplier contracts, quotes, invoices, receipts — read by the
     * panel through the API with its token, and by nobody else.
     */
    private static final Set<String> PRIVATE_MEDIA = Set.of(
        "customer-documents",
        "quote-documents",
        "invoice-documents",
        "safari-documents",
        "itinerary-documents",
        "expense-documents",
        "accommodation-documents",
        "park-documents",
        "activity-documents",
        "park-activity-documents");

    @Test
    @DisplayName("every module that serves files is classified as published or private")
    void everyMediaModuleIsClassified() throws IOException {
        Set<String> unclassified = new TreeSet<>();

        for (String module : mediaModules()) {
            if (!PUBLIC_MEDIA.contains(module) && !PRIVATE_MEDIA.contains(module)) unclassified.add(module);
        }

        assertTrue(unclassified.isEmpty(), () -> """
            %d media module(s) serve files but are not classified: %s

            Add each to PUBLIC_MEDIA (published material the website renders in an <img>) or to
            PRIVATE_MEDIA (anything a client or a supplier would not want handed to a stranger), and
            make SecurityConfigurations agree. A module nobody classified defaults to whatever the
            filter chain happens to do, which is how customer documents ended up public.
            """.formatted(unclassified.size(), unclassified));
    }

    @Test
    @DisplayName("no private file endpoint is exempt from authentication")
    void noPrivateMediaIsPermitAll() throws IOException {
        String security = Files.readString(SECURITY);
        List<String> exposed = new ArrayList<>();

        for (String module : PRIVATE_MEDIA) {
            /* only the permitAll lines matter; the module may be mentioned in a comment */
            for (String line : security.split("\n")) {
                if (line.contains("permitAll") && line.contains("/api/" + module + "/")) {
                    exposed.add(module + "  ->  " + line.trim());
                }
            }
        }

        assertTrue(exposed.isEmpty(), () -> """
            %d private media path(s) are exempt from authentication:

            %s

            These hold client paperwork. A link is not a credential — it is forwarded, logged and
            left in history. The panel reads them through the API with its token, so no exemption is
            needed; if something genuinely cannot send a header, issue a short-lived signed link
            rather than opening the whole module.
            """.formatted(exposed.size(), String.join("\n", exposed)));
    }

    @Test
    @DisplayName("every published module is actually reachable without a token")
    void everyPublicMediaIsPermitAll() throws IOException {
        String security = Files.readString(SECURITY);
        List<String> missing = new ArrayList<>();

        for (String module : new TreeSet<>(PUBLIC_MEDIA)) {
            boolean permitted = security.lines()
                .anyMatch(l -> l.contains("permitAll") && l.contains("/api/" + module + "/*/file"));
            if (!permitted) missing.add(module);
        }

        assertTrue(missing.isEmpty(), () -> """
            %s is listed as published but needs a token, so the website will show broken images:
            add it to the permitAll block in SecurityConfigurations, or move it to PRIVATE_MEDIA.
            """.formatted(missing));
    }

    // ------------------------------------------------------------------ discovery

    /**
     * Every module whose controller serves a file, read from the source.
     *
     * The alternative — asking Spring for its mappings — needs a running context and a database,
     * which is exactly what CI does not have. A regex over the controllers finds the same endpoints
     * and runs in a second.
     */
    private Set<String> mediaModules() throws IOException {
        Pattern mapping = Pattern.compile("@RequestMapping\\(\"/api/([a-z-]+)\"\\)");
        Set<String> modules = new TreeSet<>();

        try (Stream<Path> files = Files.walk(JAVA)) {
            for (Path file : files.filter(f -> f.toString().endsWith("Controller.java")).toList()) {
                String source = Files.readString(file);
                boolean servesFiles = source.contains("/file\")") || source.contains("/file/{fileName}");
                if (!servesFiles) continue;

                Matcher m = mapping.matcher(source);
                while (m.find()) {
                    String module = m.group(1);
                    /* templates and signatures are files too, but they are configuration, not media */
                    if (module.endsWith("-images") || module.endsWith("-documents")) modules.add(module);
                }
            }
        }

        assertFalse(modules.isEmpty(), "found no media controllers — this guard would be checking nothing");
        return modules;
    }
}
