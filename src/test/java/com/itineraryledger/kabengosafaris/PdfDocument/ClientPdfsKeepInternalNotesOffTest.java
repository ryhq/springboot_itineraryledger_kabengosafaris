package com.itineraryledger.kabengosafaris.PdfDocument;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * That a document we email to a client never prints the notes we keep for ourselves.
 *
 * <p>The safari itinerary template rendered {@code safari.internalNotes} under a heading that said,
 * in as many words, "Internal Notes". On a live booking that field held the guests' own email
 * addresses, their children's ages, which member of staff was handling the file, and the family's
 * AIRLINE BOOKING REFERENCE. An airline reference plus a surname is enough to view, change or
 * cancel a booking on the carrier's own site, so this was not a tidiness problem — it was a way to
 * hand somebody the means to lose their own return flights, attached to a PDF we send out.
 *
 * <p>The mistake is easy to repeat, because printing every field is the obvious thing to do when
 * building a template and the harm is invisible until a real record has real notes in it. So the
 * rule is enforced here rather than remembered: internal fields render only on documents written
 * for our own staff.
 */
class ClientPdfsKeepInternalNotesOffTest {

    private static final Path TEMPLATES = Path.of("src/main/resources/templates/pdf-templates");

    /**
     * The documents written for our own people, where operational notes belong.
     *
     * <p>Kept deliberately short. A template earns a place here by being one a guest never receives,
     * and adding one is a decision worth having to make explicitly.
     */
    private static final Set<String> INTERNAL_DOCUMENTS = Set.of(
        "full_itinerary_driver_roadbook.html"
    );

    /**
     * Fields that exist to hold what the client must not read.
     *
     * <p>Deliberately only the two that are ours. {@code guestFeedback} was in this list for one
     * run and came straight back out: it is the guest's own words about their own stay, so showing
     * it to them leaks nothing, and a test that flags things which are fine is a test somebody
     * eventually switches off.
     */
    private static final List<String> INTERNAL_FIELDS = List.of(
        "internalNotes", "driverNotes"
    );

    @Test
    @DisplayName("no client-facing template prints an internal field")
    void clientTemplatesDoNotPrintInternalFields() throws IOException {
        List<String> offences = new ArrayList<>();

        try (Stream<Path> files = Files.list(TEMPLATES)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".html")).toList()) {
                String name = file.getFileName().toString();
                if (INTERNAL_DOCUMENTS.contains(name)) continue;

                String body = Files.readString(file);
                for (String field : INTERNAL_FIELDS) {
                    for (String line : body.split("\n")) {
                        if (!line.contains(field)) continue;
                        // A comment explaining why the field is absent is the point, not a breach.
                        String trimmed = line.strip();
                        if (trimmed.startsWith("*") || trimmed.startsWith("<!--")
                            || trimmed.startsWith("//") || !line.contains("th:")) continue;
                        offences.add(name + " renders " + field + ":  " + trimmed);
                    }
                }
            }
        }

        assertTrue(offences.isEmpty(),
            "These templates are sent to clients and print notes meant for staff.\n"
                + "One of them once carried a family's airline booking reference to the family's own\n"
                + "inbox, which is enough for anyone holding the PDF to cancel their flights.\n"
                + "Move the content to a driver/operations template instead.\n  "
                + String.join("\n  ", offences));
    }

    @Test
    @DisplayName("the roadbook is still allowed to carry them, or this test is guarding nothing")
    void theInternalDocumentStillExists() {
        for (String name : INTERNAL_DOCUMENTS) {
            assertTrue(Files.exists(TEMPLATES.resolve(name)),
                name + " is listed as the internal document but no longer exists — the exemption "
                    + "list is stale, and a renamed roadbook would silently become client-facing");
        }
    }
}
