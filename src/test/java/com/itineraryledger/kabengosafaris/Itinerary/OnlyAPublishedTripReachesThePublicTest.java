package com.itineraryledger.kabengosafaris.Itinerary;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;

/**
 * Publishing is a person deciding this company will sell a trip. Until then it is not for sale, and
 * the public website must not show it.
 *
 * <p>Written from a live leak. Every public service filtered itineraries on {@code isActive} alone
 * — the list, the search, the navigation, the homepage, the related-trip strips and the
 * resolve-by-identifier path — so three DRAFT itineraries were readable, searchable and priced on
 * the public site. {@code ItinerarySpecification.isActiveAndPublished()} already existed, was
 * documented as "typically for customer-facing queries", and had not one caller.
 *
 * <p>The resolver was the worst of them, because it resolves by CODE and a code is guessable:
 * {@code ITI-6D6N-1070} is a day count and a serial. Fixing the lists alone would have looked
 * fixed and left the door open.
 */
class OnlyAPublishedTripReachesThePublicTest {

    private static final Path PUBLIC_SERVICES =
        Paths.get("src/main/java/com/itineraryledger/kabengosafaris/Public");

    /** `itinerary.getIsActive()`, `itin.getIsActive()`, `Itinerary::getIsActive` — and nothing else. */
    private static final Pattern ITINERARY_ACTIVE = Pattern.compile(
        "\\b(itinerary|itin|itineraries)\\w*\\.getIsActive\\(\\)|Itinerary::getIsActive",
        Pattern.CASE_INSENSITIVE);

    @Test
    @DisplayName("only a published, active itinerary counts as publicly visible")
    void theRuleItself() {
        assertTrue(visible(Itinerary.ItineraryStatus.PUBLISHED, true));

        assertFalse(visible(Itinerary.ItineraryStatus.DRAFT, true), "a draft is being built");
        assertFalse(visible(Itinerary.ItineraryStatus.COMPLETE, true),
            "complete means ready to publish — nobody has said publish it");
        assertFalse(visible(Itinerary.ItineraryStatus.ARCHIVED, true),
            "archived was deliberately withdrawn");
        assertFalse(visible(Itinerary.ItineraryStatus.PUBLISHED, false),
            "deactivated outranks published");
        assertFalse(visible(null, true), "no status is not a decision to sell it");
    }

    @Test
    @DisplayName("no public service filters itineraries on isActive alone")
    void noPublicServiceSettlesForActive() throws IOException {
        List<String> offences = new ArrayList<>();

        try (Stream<Path> files = Files.walk(PUBLIC_SERVICES)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).sorted().toList()) {
                String source = Files.readString(file);
                String name = file.getFileName().toString();

                if (source.contains("ItinerarySpecification.isActive(true)")) {
                    offences.add(name + ": ItinerarySpecification.isActive(true) — use "
                        + "isActiveAndPublished(), or a draft is on the website");
                }
                /*
                 * The in-memory twin of the same mistake, which is how the related-trip strips
                 * leaked even after a list endpoint was fixed.
                 *
                 * Matched on the RECEIVER, not on the file. The first version of this asked
                 * whether a file mentioned Itinerary anywhere and called getIsActive() on
                 * anything, and flagged three services filtering parks and accommodations — which
                 * is correct for those, since a park has no published state. A check that cries
                 * wolf three times out of four gets the whole suite ignored.
                 */
                Matcher m = ITINERARY_ACTIVE.matcher(source);
                while (m.find()) {
                    offences.add(name + ": " + m.group().trim() + " — an itinerary needs "
                        + "isPubliclyVisible(), because active is not published");
                }
            }
        }

        assertTrue(offences.isEmpty(), () -> String.format("""
            %d public read path(s) would show an unpublished trip:

              %s

            Publishing is a person deciding this company will sell the trip. A DRAFT is priced,
            described and half-finished, and anything that reaches the website is quotable by a
            stranger.
            """, offences.size(), String.join("\n  ", offences)));
    }

    @Test
    @DisplayName("the identifier resolver refuses an unpublished trip, however it is named")
    void theResolverIsGuardedOnBothPaths() throws IOException {
        /*
         * Both branches, deliberately. The id branch takes an obfuscated id that rotates on every
         * restart; the code branch takes ITI-6D6N-1070, which anybody can type. Guarding one and
         * not the other leaves the guessable one open.
         */
        String source = Files.readString(
            PUBLIC_SERVICES.resolve("Services/PublicEntityResolver.java"));

        int guards = source.split("isPubliclyVisible", -1).length - 1;
        assertTrue(guards >= 2, () ->
            "PublicEntityResolver guards " + guards + " of its two itinerary lookups. Both the "
            + "by-id and the by-code branch must filter, or a guessable code still reaches a "
            + "trip nobody published.");
    }

    private boolean visible(Itinerary.ItineraryStatus status, boolean active) {
        Itinerary itinerary = new Itinerary();
        itinerary.setStatus(status);
        itinerary.setIsActive(active);
        return itinerary.isPubliclyVisible();
    }
}
