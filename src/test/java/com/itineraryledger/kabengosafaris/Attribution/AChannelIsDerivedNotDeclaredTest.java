package com.itineraryledger.kabengosafaris.Attribution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The channel is worked out here or it is worthless.
 *
 * <p>Everything this service is handed arrived in a query string, which anybody can write. A
 * channel the browser could name would be a number nobody could budget against, so the browser
 * sends the tags and the server decides what they mean. These are the readings that decide where
 * money goes, so each is pinned rather than left to whoever edits the brand lists next.
 */
class AChannelIsDerivedNotDeclaredTest {

    private final AttributionService service = new AttributionService();

    @Test
    @DisplayName("an answer engine is its own channel, not search")
    void answerEnginesAreTheirOwnChannel() {
        /*
         * The lead that started this: a German customer arrived on a safari page with
         * ?utm_source=chatgpt.com and the panel recorded it, like everything else, as
         * the literal "WEBSITE".
         */
        assertEquals(AcquisitionChannel.AI_ASSISTANT,
            service.resolveChannel("chatgpt.com", null, null, null));
        assertEquals(AcquisitionChannel.AI_ASSISTANT,
            service.resolveChannel("perplexity.ai", null, null, null));

        /*
         * The one that would quietly go wrong: Google's assistants live underneath the
         * search brand, so a host-suffix reading would file them as organic search and
         * the answer-engine number would stay near zero while the traffic grew.
         */
        assertEquals(AcquisitionChannel.AI_ASSISTANT,
            service.resolveChannel("gemini.google.com", null, null, null));
        assertNotEquals(AcquisitionChannel.ORGANIC_SEARCH,
            service.resolveChannel("bard.google.com", null, null, null));
    }

    @Test
    @DisplayName("a click identifier that only exists on paid traffic outranks the medium")
    void paidClickIdsWinOverTags() {
        /* A campaign mistagged as organic still cost money; gclid proves it. */
        assertEquals(AcquisitionChannel.PAID_SEARCH,
            service.resolveChannel("google", "organic", "gclid", null));
        assertEquals(AcquisitionChannel.PAID_SEARCH,
            service.resolveChannel(null, null, "msclkid", null));
        assertEquals(AcquisitionChannel.PAID_DISPLAY,
            service.resolveChannel(null, null, "dclid", null));
    }

    @Test
    @DisplayName("fbclid is not proof that anybody was paid")
    void facebookClickIdDoesNotInventSpend() {
        /*
         * Facebook and Instagram stamp fbclid on every outbound link, an organic post as
         * readily as an ad. Reading it as paid would credit a budget with conversions it
         * never bought, which is worse than not measuring at all.
         */
        assertEquals(AcquisitionChannel.ORGANIC_SOCIAL,
            service.resolveChannel("facebook.com", null, "fbclid", null));
        assertTrue(!service.resolveChannel("facebook.com", null, "fbclid", null).isPaid());

        /* Tagged as paid social, it is paid social — because somebody said so, not because of the id. */
        assertEquals(AcquisitionChannel.PAID_SOCIAL,
            service.resolveChannel("facebook", "paid_social", "fbclid", null));
    }

    @Test
    @DisplayName("an untagged visit is read from the referrer, and no referrer means direct")
    void untaggedVisitsFallBackHonestly() {
        assertEquals(AcquisitionChannel.ORGANIC_SEARCH,
            service.resolveChannel(null, null, null, "https://www.google.de/search?q=serengeti"));
        assertEquals(AcquisitionChannel.AI_ASSISTANT,
            service.resolveChannel(null, null, null, "https://chatgpt.com/"));
        assertEquals(AcquisitionChannel.REFERRAL,
            service.resolveChannel(null, null, null, "https://safaribookings.example/listing"));
        assertEquals(AcquisitionChannel.DIRECT,
            service.resolveChannel(null, null, null, null));
    }

    @Test
    @DisplayName("a tagged source we do not recognise is OTHER, never DIRECT")
    void anUnknownTagIsNotDirect() {
        /*
         * Direct means nobody sent them. A campaign we have not heard of is the opposite
         * of that, and folding it into DIRECT would hide exactly the new platform a test
         * budget was spent to evaluate.
         */
        assertEquals(AcquisitionChannel.OTHER,
            service.resolveChannel("some-new-network", null, null, null));
    }

    @Test
    @DisplayName("a form with no tags stores nothing rather than claiming DIRECT")
    void nothingCapturedStaysNothing() {
        /*
         * DIRECT is a statement: nobody sent them. A payload with nothing in it is the
         * absence of a statement — a browser with storage blocked, a caller that omits
         * the field — and stamping it DIRECT would quietly fold every unmeasured lead
         * into a channel it never came from.
         */
        assertNull(service.from(null));
        assertNull(service.from(new AttributionRequest()));
    }

    @Test
    @DisplayName("a visitor who really did type the address is DIRECT, not blank")
    void aRealDirectVisitIsStillRecorded() {
        /* The page always records the page they landed on, so a real arrival is never empty. */
        AttributionRequest typedTheAddress = new AttributionRequest();
        typedTheAddress.setLandingPage("/de/safaris");

        Attribution stored = service.from(typedTheAddress);

        assertEquals(AcquisitionChannel.DIRECT, stored.getChannel());
    }
}
