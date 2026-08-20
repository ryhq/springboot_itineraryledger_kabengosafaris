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

        assertFalse(source.contains("setAllowedOriginPatterns(List.of(\"*\"))"),
            "a wildcard origin pattern is back, and this configuration allows credentials");
        assertFalse(source.contains("setAllowedOrigins(List.of(\"*\"))"),
            "a wildcard origin is back, and this configuration allows credentials");
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
}
