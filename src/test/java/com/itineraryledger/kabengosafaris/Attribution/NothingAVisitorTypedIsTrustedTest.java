package com.itineraryledger.kabengosafaris.Attribution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Every value on an arrival came out of a URL a stranger could edit, and is read back in the
 * office panel and in the notification mail.
 *
 * <p>The panel escapes what it renders and the mail templates escape what they interpolate, but
 * that is a promise held in a dozen places by people who will not know this column exists. The
 * cheaper guarantee is that the dangerous shapes never reach the database at all, and that is
 * what these pin.
 */
class NothingAVisitorTypedIsTrustedTest {

    private final AttributionService service = new AttributionService();

    @Test
    @DisplayName("angle brackets and control characters do not reach the column")
    void markupIsStrippedOnTheWayIn() {
        AttributionRequest request = new AttributionRequest();
        request.setSource("<script>alert(1)</script>");
        request.setCampaign("summer \u0007 promo");

        Attribution stored = service.from(request);

        assertNotNull(stored);
        assertFalse(stored.getSource().contains("<"), "an angle bracket survived into storage");
        assertFalse(stored.getSource().contains(">"), "an angle bracket survived into storage");
        assertEquals("summer promo", stored.getCampaign());
    }

    @Test
    @DisplayName("a URL that is not http(s) is dropped, not stored")
    void onlyRealUrlsAreKept() {
        /* Stored, it would eventually be rendered as an href somewhere, once. */
        assertNull(AttributionService.cleanUrl("javascript:alert(1)", 500));
        assertNull(AttributionService.cleanUrl("data:text/html;base64,PHNjcmlwdD4=", 500));

        /* A protocol-relative path navigates off-site from any href it reaches. */
        assertNull(AttributionService.cleanUrl("//evil.example/path", 500));

        assertEquals("/de/safaris/ITI-5D4N-1050",
            AttributionService.cleanUrl("/de/safaris/ITI-5D4N-1050", 500));
        assertEquals("https://chatgpt.com/",
            AttributionService.cleanUrl("https://chatgpt.com/", 500));
    }

    @Test
    @DisplayName("an over-long value is cut to its column rather than failing the whole form")
    void longValuesAreCappedNotRejected() {
        AttributionRequest request = new AttributionRequest();
        request.setSource("s".repeat(5_000));
        request.setCampaign("c".repeat(5_000));

        Attribution stored = service.from(request);

        assertNotNull(stored, "a lead must never be lost because a tag was too long");
        assertEquals(120, stored.getSource().length());
        assertEquals(180, stored.getCampaign().length());
    }

    @Test
    @DisplayName("a touch count out of any sane range is dropped rather than believed")
    void nonsenseCountsAreDropped() {
        assertNull(AttributionService.saneCount(0));
        assertNull(AttributionService.saneCount(-4));
        assertNull(AttributionService.saneCount(99_999_999));
        assertEquals(7, AttributionService.saneCount(7));
    }

    @Test
    @DisplayName("what introduced them is never rewritten by a later campaign")
    void theFirstTouchSurvivesTheSecond() {
        /*
         * Somebody found us through an answer engine in July and came back through a paid
         * ad in September. The ad closed them and takes the last touch. If it also took the
         * first, the channel that actually found the customer would read as worthless, and
         * that bias is the whole reason two touches are kept.
         */
        Attribution existing = new Attribution();
        existing.setFirstChannel(AcquisitionChannel.AI_ASSISTANT);
        existing.setFirstSource("chatgpt.com");
        existing.setFirstSeenAt(LocalDateTime.of(2026, 7, 1, 9, 0));
        existing.setTouchCount(3);

        AttributionRequest returning = new AttributionRequest();
        returning.setSource("google");
        returning.setMedium("cpc");
        returning.setCampaign("serengeti-september");
        returning.setTouchCount(1);

        Attribution merged = service.merge(existing, service.from(returning));

        assertEquals(AcquisitionChannel.PAID_SEARCH, merged.getChannel(), "the ad closed them");
        assertEquals(AcquisitionChannel.AI_ASSISTANT, merged.getFirstChannel(), "the answer engine found them");
        assertEquals("chatgpt.com", merged.getFirstSource());
        assertEquals(LocalDateTime.of(2026, 7, 1, 9, 0), merged.getFirstSeenAt());
        assertEquals(3, merged.getTouchCount(), "a returning visitor cannot have fewer visits than before");
    }

    @Test
    @DisplayName("when only one visit was ever seen, both touches describe it")
    void aSingleVisitFillsBothTouches() {
        AttributionRequest request = new AttributionRequest();
        request.setSource("chatgpt.com");

        Attribution stored = service.from(request);

        assertEquals(AcquisitionChannel.AI_ASSISTANT, stored.getChannel());
        assertEquals(AcquisitionChannel.AI_ASSISTANT, stored.getFirstChannel());
        assertEquals("chatgpt.com", stored.getFirstSource());

        AttributionDTO dto = service.toDto(stored);
        assertNotNull(dto);
        assertFalse(dto.isMultiTouch(), "one visit cannot be two different channels");
        assertTrue(dto.getChannelDisplayName() != null && !dto.getChannelDisplayName().isBlank());
    }
}
