package com.itineraryledger.kabengosafaris.PdfDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.PdfDocument.Services.PdfTemplateStorageService;

/**
 * Restore original has to find the file the row actually came from.
 *
 * <p>It used to load "&lt;document&gt;_default.html", so it worked for the one default per document
 * and nothing else. Matching on the row's own file name looked like the fix, and was not: a stored
 * name carries a timestamp ("full_itinerary_modern_20260324_143945.html") so that two saves cannot
 * collide on disk, and matching it literally found nothing at all -- which would have hidden the
 * button on every template on every install.
 *
 * <p>These are the real stored names from a live install. Nothing here needs a database, and it is
 * exactly the check that was missing.
 */
class RestoreFindsTheFileARowCameFromTest {

    private static final Path SHIPPED = Paths.get("src/main/resources/templates/pdf-templates");

    private final PdfTemplateStorageService storage = new PdfTemplateStorageService();

    @Test
    @DisplayName("a stored name finds the shipped file it was seeded from, stamp and all")
    void theStampComesOff() {
        String content = storage.loadShippedTemplate("full_itinerary_modern_20260324_143945.html");

        assertNotNull(content, "the Modern itinerary layout is shipped; the timestamp is ours, "
            + "not part of the name");
        assertTrue(content.contains("<html") || content.contains("<!DOCTYPE") || content.contains("<div"),
            "it should be the actual template, not an error page");
    }

    @Test
    @DisplayName("an unstamped name works too, for a row that was never renamed")
    void theBareNameAlsoWorks() {
        assertNotNull(storage.loadShippedTemplate("full_quote_default.html"));
    }

    @Test
    @DisplayName("a template written in the panel has no original, and says so")
    void nothingIsInventedForACustomTemplate() {
        assertNull(storage.loadShippedTemplate("full_itinerary_my_own_layout_20260324_143945.html"));
        assertNull(storage.loadShippedTemplate("something_nobody_ships.html"));
        assertNull(storage.loadShippedTemplate(null));
        assertNull(storage.loadShippedTemplate("   "));
    }

    @Test
    @DisplayName("a file name is a name, never a path")
    void noPathEscapes() {
        assertNull(storage.loadShippedTemplate("../../../etc/passwd"));
        assertNull(storage.loadShippedTemplate("templates/pdf-templates/full_quote_default.html"));
    }

    @Test
    @DisplayName("every layout we ship can be restored by the name a row would store for it")
    void everyShippedLayoutIsReachable() throws IOException {
        List<String> unreachable = new ArrayList<>();

        for (Path file : shipped()) {
            String bare = file.getFileName().toString();
            String stored = bare.replace(".html", "_20260324_142052.html");
            if (storage.loadShippedTemplate(stored) == null) {
                unreachable.add(bare);
            }
        }

        assertTrue(unreachable.isEmpty(), () -> "shipped layouts a row could never restore to:\n"
            + String.join("\n", unreachable));
    }

    @Test
    @DisplayName("the eight templates changed for the cost leak and the discount scope are restorable")
    void theOnesThisWorkTouched() {
        for (String name : List.of(
                "full_itinerary_default", "full_itinerary_modern",
                "full_safari_default", "full_safari_modern",
                "full_quote_default", "full_quote_multicurrency",
                "full_invoice_default", "invoice_simple")) {
            assertNotNull(storage.loadShippedTemplate(name + "_20260324_142052.html"),
                name + " has to be restorable: it is one of the files that stopped printing our "
                    + "cost, and an install still running the old copy still prints it");
        }
    }

    private static List<Path> shipped() throws IOException {
        try (Stream<Path> files = Files.list(SHIPPED)) {
            return files.filter(f -> f.toString().endsWith(".html")).sorted().toList();
        }
    }

    @Test
    @DisplayName("hasShippedTemplate answers the same question as the loader")
    void theFlagAgreesWithTheLoader() {
        assertEquals(storage.loadShippedTemplate("full_safari_default_20260324_142052.html") != null,
            storage.hasShippedTemplate("full_safari_default_20260324_142052.html"));
        assertEquals(false, storage.hasShippedTemplate("nothing_we_ship.html"));
    }
}
