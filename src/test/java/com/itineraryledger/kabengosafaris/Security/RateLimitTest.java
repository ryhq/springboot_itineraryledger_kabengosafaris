package com.itineraryledger.kabengosafaris.Security;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.Security.RateLimit.RateLimiter;

/**
 * The ceiling on the endpoints a stranger can reach.
 *
 * Registration, password reset and activation-resend each send an email to an address the caller
 * chose, and the websites' forms notify somebody by mail. Unlimited, they are a way to have this
 * installation deliver unwanted mail to anybody — from this company's address, against its own
 * sending reputation.
 */
class RateLimitTest {

    private static final Path FILTER = Path.of("src/main/java/com/itineraryledger/"
        + "kabengosafaris/Security/RateLimit/RateLimitFilter.java");

    @Test
    @DisplayName("a caller is let through up to the limit, then told how long to wait")
    void countsAndRefuses() {
        RateLimiter limiter = new RateLimiter();

        for (int attempt = 1; attempt <= 5; attempt++) {
            assertEquals(0, limiter.check("ip:test", 5, Duration.ofHours(1)),
                "attempt " + attempt + " is within the limit and should pass");
        }

        long waitFor = limiter.check("ip:test", 5, Duration.ofHours(1));
        assertTrue(waitFor > 0, "the sixth attempt should be refused");
        assertTrue(waitFor <= 3600, "the wait cannot exceed the window: " + waitFor);
    }

    @Test
    @DisplayName("callers are counted separately")
    void oneCallerDoesNotExhaustAnother() {
        RateLimiter limiter = new RateLimiter();
        for (int i = 0; i < 5; i++) limiter.check("ip:noisy", 5, Duration.ofHours(1));

        assertTrue(limiter.check("ip:noisy", 5, Duration.ofHours(1)) > 0, "the noisy one is done");
        assertEquals(0, limiter.check("ip:quiet", 5, Duration.ofHours(1)),
            "somebody else's flood must not lock out a real visitor");
    }

    @Test
    @DisplayName("the window forgives, so a limit is not a ban")
    void theWindowExpires() {
        RateLimiter limiter = new RateLimiter();
        for (int i = 0; i < 3; i++) limiter.check("ip:brief", 3, Duration.ofMillis(1));
        assertTrue(limiter.check("ip:brief", 3, Duration.ofMillis(1)) >= 0);

        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        assertEquals(0, limiter.check("ip:brief", 3, Duration.ofMillis(1)),
            "a new window starts clean — somebody who waited should not still be refused");
    }

    @Test
    @DisplayName("the caller's address is read from the END of X-Forwarded-For")
    void theForwardedHeaderIsNotTakenOnTrust() throws IOException {
        String source = Files.readString(FILTER);

        /*
         * The header is written by whoever is in front AND by anybody who feels like sending one.
         * Reading the first entry means a caller can invent a fresh identity per request, every key
         * is new, no limit is ever reached, and the filter looks like it works.
         */
        assertTrue(source.contains("hops.length - proxyHops"),
            "the address must be counted back from the end of X-Forwarded-For, not taken from the "
                + "front where the caller controls it");
        assertFalse(source.contains("hops[0]"),
            "hops[0] is the caller's own claim");
    }

    @Test
    @DisplayName("mail-sending endpoints are limited by recipient too, not only by caller")
    void theVictimIsAKeyAsWell() throws IOException {
        String source = Files.readString(FILTER);

        /*
         * Limiting by IP stops one machine hammering the API. It does nothing about a hundred
         * machines asking this API to mail the SAME person, which is what a mail-bombing actually
         * looks like and is trivial to arrange.
         */
        assertTrue(source.contains("\"email:\""),
            "the address being written to has to be a key of its own");

        for (String path : new String[] { "/api/auth/forgot-password",
            "/api/auth/resend-account-activation" }) {
            int at = source.indexOf(path);
            assertTrue(at > 0, path + " must be limited — it sends mail to an address the caller picks");
            /* to the end of the rule, not to the first bracket, which is inside Duration.ofHours(1) */
            String rule = source.substring(at, source.indexOf("\"),", at) + "\"),".length());
            assertTrue(rule.contains(", true,"),
                path + " should be limited by recipient as well: " + rule);
        }

        /*
         * Registration is the exception, and its per-recipient limit lives elsewhere: the email
         * arrives in a JSON body, and a filter that reads the body consumes the stream the
         * controller needs. If that check disappears, the only guard left is per-IP — and the
         * attack worth stopping is many machines mailing one person.
         */
        String handler = Files.readString(Path.of("src/main/java/com/itineraryledger/"
            + "kabengosafaris/User/Handlers/RegistrationHandler/RegistrationHandler.java"));
        assertTrue(handler.contains("register-email:"),
            "registration must limit activation emails per recipient somewhere, and the filter "
                + "cannot read a JSON body to do it");
    }

    @Test
    @DisplayName("the authenticated API is left alone")
    void signedInWorkIsNotThrottled() throws IOException {
        String source = Files.readString(FILTER);

        /*
         * Somebody signed in and holding a permission is a known person doing their job. Throttling
         * them turns a colleague importing three bundles into a support question.
         */
        for (String path : new String[] { "/api/data-transfer", "/api/customers", "/api/quotes",
            "/api/accommodations", "/api/company" }) {
            assertFalse(source.contains("\"" + path + "\""),
                path + " is behind a permission and should not be rate limited");
        }
    }
}
