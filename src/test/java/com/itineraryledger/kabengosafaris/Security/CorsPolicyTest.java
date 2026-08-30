package com.itineraryledger.kabengosafaris.Security;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * That this API never again answers everybody with credentials.
 *
 * It did: `setAllowedOriginPatterns("*")` together with `setAllowCredentials(true)` means any page on
 * the internet could make a credentialed request here and read the reply — the browser's same-origin
 * rule switched off for every site at once. It was there for the backup cookie-bridge, which does
 * need credentials, and the fix is to name who may use them rather than to allow everyone.
 *
 * A regression would be one word long and would look harmless in a diff, so it is checked here.
 */
class CorsPolicyTest {

    private static final Path CONFIG = Paths.get(
        "src/main/java/com/itineraryledger/kabengosafaris/Configurations/SecurityConfigurations.java");

    @Test
    @DisplayName("no wildcard origin, and credentials are still allowed for the named ones")
    void noWildcardWithCredentials() throws IOException {
        String source = Files.readString(CONFIG);

        /*
         * Scoped to the CREDENTIALED configuration. The public API deliberately answers any origin,
         * with credentials off — so a file-wide search for a wildcard now finds a legitimate one,
         * and the thing that actually matters is the PAIRING.
         */
        String credentialed = source.substring(source.indexOf("CorsConfiguration corsConfiguration"),
            source.indexOf("CorsConfiguration publicConfiguration"));

        assertFalse(credentialed.contains("setAllowedOriginPatterns(List.of(\"*\"))"),
            "a wildcard origin pattern is back in the credentialed configuration");
        assertFalse(credentialed.contains("setAllowedOrigins(List.of(\"*\"))"),
            "a wildcard origin is back in the credentialed configuration");
        assertTrue(source.contains("setAllowedOrigins(allowedOrigins())"),
            "origins should come from configuration, so each deployment names its own callers");
        assertTrue(source.contains("setAllowCredentials(true)"),
            "credentials are still needed — the backup download hands over a short-lived cookie");
    }

    @Test
    @DisplayName("preflight and HEAD are allowed, or a monitor reads a 403 as an outage")
    void preflightAndHeadAllowed() throws IOException {
        String source = Files.readString(CONFIG);
        assertTrue(source.contains("\"OPTIONS\""), "a browser sends OPTIONS before any non-simple request");
        assertTrue(source.contains("\"HEAD\""), "HEAD used to 403, which looks exactly like a broken endpoint");
    }

    @Test
    @DisplayName("the public API answers any origin, and carries no credentials while doing it")
    void thePublicSitesAreStillServed() throws IOException {
        String source = Files.readString(CONFIG);

        /*
         * This is the second half of the same lesson, and it cost a live outage to learn.
         *
         * Locking CORS to the panel's origin was right for the authenticated API and wrong for the
         * public one: both company websites went blank while the API kept answering 200 with every
         * row present, because the browser was throwing the answers away. The comment justifying it
         * said the sites were unaffected since "their API references are <img> sources" — true of the
         * pictures, false of the parks, heroes, accommodations and testimonials they FETCH.
         */
        int publicAt = source.indexOf("CorsConfiguration publicConfiguration");
        assertTrue(publicAt > 0,
            "there must be a separate CORS configuration for /api/public/**, or every public website "
                + "served by this API goes blank");

        String publicBlock = source.substring(publicAt, source.indexOf("cors.configurationSource", publicAt));

        assertTrue(publicBlock.contains("setAllowCredentials(false)"),
            "a wildcard origin is only safe with credentials OFF — with them on it is the exact hole "
                + "the rest of this configuration exists to close");
        assertTrue(publicBlock.contains("\"POST\""),
            "the sites submit testimonials, newsletter sign-ups and booking inquiries");
        assertTrue(source.contains("registerCorsConfiguration(\"/api/public/**\""),
            "the public rule has to be registered, and before the catch-all — the source takes the "
                + "first pattern that matches");
        assertTrue(source.indexOf("registerCorsConfiguration(\"/api/public/**\"")
                < source.indexOf("registerCorsConfiguration(\"/**\""),
            "the specific pattern must be registered first or the catch-all swallows it");
    }
}
