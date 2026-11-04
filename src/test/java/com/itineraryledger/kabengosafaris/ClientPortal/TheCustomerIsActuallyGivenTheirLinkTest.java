package com.itineraryledger.kabengosafaris.ClientPortal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalJourney;
import com.itineraryledger.kabengosafaris.ClientPortal.Enums.PortalSection;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalAccessService;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalJourneyService;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalLinkService;
import com.itineraryledger.kabengosafaris.Feature.Feature;
import com.itineraryledger.kabengosafaris.Feature.FeatureService;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.Enums.RevisionReason;

/**
 * The link existed, said it had been sent, and had been sent to nobody.
 *
 * <p>Sharing a quotation created the journey and left the address sitting in the panel for a
 * human to copy into a message by hand. Nothing emailed it: not the publish action, and not the
 * quotation email, which carried the PDF and no link at all. Meanwhile the history labelled that
 * first revision <em>"Sent to the customer"</em> — the display name of a revision REASON, not a
 * record of any message — beside a view count of nought.
 *
 * <p>Two readings of one screen, and a consultant would reach the wrong one: the customer is
 * ignoring us, when the truth was that nobody had ever given them the address. That is the same
 * failure the insight work exists to prevent, sitting two tabs away from it.
 *
 * <p>So the quotation email carries the link — the message the customer is already expecting,
 * already carrying the PDF of the same quotation, going to the address the access code will go
 * to. And there is a second route for the quotations that went out before any of this existed.
 */
class TheCustomerIsActuallyGivenTheirLinkTest {

    private static final Path QUOTE_EMAIL = Path.of("src/main/java/com/itineraryledger/"
        + "kabengosafaris/Quote/Services/QuoteServices/QuoteStatusService.java");
    private static final Path TEMPLATE = Path.of("src/main/resources/templates/"
        + "email-templates/send_quote_default.html");

    private FeatureService features;
    private PortalJourneyService journeys;
    private PortalAccessService access;
    private PortalLinkService service;
    private Quote quote;

    @BeforeEach
    void setUp() {
        features = mock(FeatureService.class);
        journeys = mock(PortalJourneyService.class);
        access = mock(PortalAccessService.class);
        service = new PortalLinkService(features, journeys, access);

        quote = new Quote();
        quote.setId(1L);
        quote.setQuoteCode("QT-1047-1026-1");

        when(features.isEnabled(Feature.CLIENT_PORTAL)).thenReturn(true);
        PortalJourney journey = PortalJourney.builder().build();
        journey.setId(7L);
        when(journeys.publishFor(quote)).thenReturn(journey);
        when(access.linkFor(journey)).thenReturn("https://mytrip.example.com/portal/abc");
    }

    // ------------------------------------------------------------------ the label that lied

    @Test
    @DisplayName("sharing is called sharing, because sharing is what it does")
    void theBaselineDoesNotClaimAnythingWasSent() {
        assertEquals("Shared with the customer", RevisionReason.PUBLISHED.getDisplayName());
        assertFalse(RevisionReason.PUBLISHED.getDisplayName().contains("Sent"),
            "'Sent to the customer' beside nought views reads as the customer ignoring us, "
                + "which is the opposite of what had happened");
    }

    // ------------------------------------------------------------------ the first route

    @Test
    @DisplayName("the quotation email carries the link, which is how a customer gets it")
    void theQuoteEmailCarriesIt() throws IOException {
        String source = Files.readString(QUOTE_EMAIL);
        assertTrue(source.contains("variables.put(\"portalUrl\", portalLinks.linkFor(quote))"),
            "without this the link is only ever copied by hand");

        String template = Files.readString(TEMPLATE);
        assertTrue(template.contains("{{#portalUrl}}") && template.contains("{{/portalUrl}}"),
            "an optional block, because the portal can be switched off per company");
        assertTrue(template.contains("{{portalUrl}}"), "and the address itself");
    }

    @Test
    @DisplayName("the variable is always supplied, or a customer reads {{portalUrl}}")
    void anEmptyLinkIsStillAVariable() {
        when(features.isEnabled(Feature.CLIENT_PORTAL)).thenReturn(false);

        assertEquals("", service.linkFor(quote),
            "this renderer has no inverted sections, so a variable left out of the map prints "
                + "as itself in the customer's own quotation");
        verify(journeys, never()).publishFor(any());
    }

    @Test
    @DisplayName("a quotation with nothing to share still sends")
    void nothingToShareIsNotAnError() {
        when(journeys.publishFor(quote)).thenReturn(null);

        assertEquals("", service.linkFor(quote));
    }

    /**
     * The rule the whole service exists to obey.
     *
     * <p>An operator discovering that the portal is misconfigured by having a customer's
     * quotation fail to send would be far worse than the link being missing from it.
     */
    @Test
    @DisplayName("nothing about the portal may stop a quotation being emailed")
    void thePortalNeverBlocksTheQuotation() {
        when(journeys.publishFor(quote)).thenThrow(new IllegalStateException("portal is down"));

        assertEquals("", service.linkFor(quote), "swallowed, so the quotation still goes out");

        assertEquals("", service.linkFor(null), "and a missing quote is not an exception either");
    }

    @Test
    @DisplayName("sharing twice keeps the address the customer may already hold")
    void theLinkIsStable() {
        assertEquals(service.linkFor(quote), service.linkFor(quote),
            "two links to one trip is how somebody ends up reading last month's price");
    }

    // ------------------------------------------------------------------ the second route

    @Test
    @DisplayName("the link can be emailed on its own, for a quotation sent before any of this")
    void thereIsASecondRoute() throws IOException {
        String controller = Files.readString(Path.of("src/main/java/com/itineraryledger/"
            + "kabengosafaris/ClientPortal/Controller/PortalAdminController.java"));

        assertTrue(controller.contains("/journeys/{id}/send-link"));
        assertTrue(controller.contains("notifier.tripLink(journey"));
    }

    @Test
    @DisplayName("it goes to the address on the booking, never to one typed in the panel")
    void theRecipientIsNotAFreeTextField() throws IOException {
        String notifier = Files.readString(Path.of("src/main/java/com/itineraryledger/"
            + "kabengosafaris/ClientPortal/Services/PortalNotifier.java"));
        int method = notifier.indexOf("public boolean tripLink(");
        String body = notifier.substring(method, notifier.indexOf("\n    }", method));

        assertTrue(body.contains("journey.lead()"),
            "a recipient box would make this a way of mailing anybody a link to a customer's "
                + "trip, and any other address could not get past the trip page anyway");
        assertFalse(body.contains("recipient ="),
            "the address comes from the booking, not from the request");
    }

    @Test
    @DisplayName("sending the link is recorded as ours, not as the customer showing interest")
    void emailingTheLinkIsNotTheirActivity() {
        assertEquals(PortalSection.Kind.OURS, PortalSection.LINK_EMAILED.getKind());
        assertFalse(PortalSection.LINK_EMAILED.isTheirs(),
            "we pressed the button; counting it as their activity is the exact mistake the "
                + "insight panel was built to avoid");
    }

    @Test
    @DisplayName("the one-off email carries a link and no figure")
    void theLinkEmailCarriesNothingPrivate() throws IOException {
        String template = Files.readString(Path.of("src/main/resources/templates/"
            + "email-templates/portal_trip_link_default.html"));

        for (String forbidden : List.of("{{grandTotal", "{{accessCode", "{{balance", "{{total")) {
            assertFalse(template.contains(forbidden),
                "they prove their own address on the page: " + forbidden);
        }
        assertTrue(template.contains("{{portalUrl}}"));
        assertTrue(template.contains("{{#message}}"),
            "the typed line is optional and must not print its own braces when blank");
    }
}
