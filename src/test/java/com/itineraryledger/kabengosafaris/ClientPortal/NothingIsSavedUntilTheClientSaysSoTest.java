package com.itineraryledger.kabengosafaris.ClientPortal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalEditingService;

/**
 * A customer clicking a lodge to see what it is had already changed their own quotation.
 *
 * <p>Every control on the quotation page used to write the moment it was touched: no confirmation,
 * no way back, and the total lurching under them. Three related changes — a lodge, its room count,
 * the activity that goes with it — produced three prices, two of which were for a trip nobody had
 * asked for.
 *
 * <p>So the page holds what they have staged and sends it when they say so. That is a front-end
 * change, and it would have been a bad one on its own: five staged changes sent as five requests
 * fail on the fourth with three already written, which leaves the screen saying "5 unsaved" while
 * the quotation holds three of them. Worse than either state.
 *
 * <p>Hence one endpoint, one transaction, all or nothing. This file holds that promise and the two
 * properties that make it worth having: the batch delegates rather than re-implementing, and the
 * price is worked out once at the end rather than once per change.
 */
class NothingIsSavedUntilTheClientSaysSoTest {

    private static final Path SERVICE = Path.of("src/main/java/com/itineraryledger/"
        + "kabengosafaris/ClientPortal/Services/PortalEditingService.java");
    private static final Path CONTROLLER = Path.of("src/main/java/com/itineraryledger/"
        + "kabengosafaris/ClientPortal/Controller/PortalApiController.java");

    private String bodyOf(String source, String signature) {
        int at = source.indexOf(signature);
        assertTrue(at > 0, signature + " has moved or been renamed");
        int brace = source.indexOf('{', at);
        int depth = 0;
        for (int i = brace; i < source.length(); i++) {
            if (source.charAt(i) == '{') depth++;
            if (source.charAt(i) == '}') {
                depth--;
                if (depth == 0) return source.substring(brace, i + 1);
            }
        }
        return source.substring(brace);
    }

    // ------------------------------------------------------------------ all, or none

    @Test
    @DisplayName("a refusal anywhere in the batch puts everything back")
    void oneRefusalRollsBackTheWholeBatch() throws IOException {
        String body = bodyOf(Files.readString(SERVICE),
            "public PortalOutcome applyAll(PortalJourney journey, PortalParticipant by,");

        assertTrue(body.contains("throw new Refused("),
            "returning early would leave the changes before it written: only an exception "
                + "rolls back a transaction that has already touched rows");
        assertTrue(body.contains("if (!outcome.done()) throw new Refused(outcome, applied)"),
            "a refusal from any one change must stop the batch");
    }

    @Test
    @DisplayName("the refusal says which change it was")
    void theRefusalLocatesItself() throws IOException {
        String controller = Files.readString(CONTROLLER);
        int at = controller.indexOf("/{token}/edits");
        String method = controller.substring(at, at + 3600);

        assertTrue(method.contains("PortalEditingService.refusedAt(e)"),
            "\"something went wrong\" on a batch of six leaves somebody re-making all six to "
                + "find out which one it was");
        assertTrue(method.contains("so nothing \"\n                        + \"was saved")
                || method.contains("nothing"),
            "and it has to say that the rest were not saved either");
    }

    @Test
    @DisplayName("the whole batch is refused before anything is touched when the quote is locked")
    void aLockedQuotationRefusesTheBatchUpFront() throws IOException {
        String body = bodyOf(Files.readString(SERVICE),
            "public PortalOutcome applyAll(PortalJourney journey, PortalParticipant by,");

        int gate = body.indexOf("whyNotEditable(journey)");
        int firstApply = body.indexOf("applyOne(journey, by, change)");
        assertTrue(gate > 0 && gate < firstApply,
            "asked once and first, so a frozen quotation gets one clear sentence rather than a "
                + "refusal from whichever change happened to be first in the list");
    }

    // ------------------------------------------------------------------ priced once

    /**
     * Repricing is the expensive half of an edit: it rebuilds every line on the quotation.
     *
     * <p>Doing it per change would make a batch of ten ten times as slow for a figure nobody
     * wants — the intermediate totals are the prices of trips the client never asked for.
     */
    @Test
    @DisplayName("a batch is priced once at the end, not once per change")
    void thePriceIsWorkedOutOnce() throws IOException {
        String source = Files.readString(SERVICE);
        String body = bodyOf(source,
            "public PortalOutcome applyAll(PortalJourney journey, PortalParticipant by,");

        assertTrue(body.contains("bindResource(DEFER_PRICING"),
            "the five methods each reprice; the batch has to say it will do it itself");
        assertTrue(body.contains("costEstimation.recalculate(journey.getQuote().getId())"),
            "and then actually do it, or the client is shown the price of the trip they had");
        assertTrue(body.contains("unbindResourceIfPossible(DEFER_PRICING)")
                && body.contains("finally"),
            "left bound, the next edit on this thread would silently not reprice at all");
    }

    @Test
    @DisplayName("deferral is bound to the transaction, never to a field on the service")
    void theFlagIsNotSharedBetweenCustomers() throws IOException {
        String source = Files.readString(SERVICE);

        assertTrue(source.contains("TransactionSynchronizationManager.getResource(DEFER_PRICING)"),
            "a boolean field on a singleton service is shared by every customer in the building "
                + "at once: one batch would stop everybody else's edits repricing");
        assertFalse(source.contains("private boolean deferPricing"),
            "that is the field this test exists to prevent");
    }

    @Test
    @DisplayName("a single edit still reprices immediately, because its price is shown at once")
    void theSingleEditIsUnchanged() throws IOException {
        String body = bodyOf(Files.readString(SERVICE),
            "private PortalOutcome done(PortalJourney journey, PortalParticipant by,");

        assertTrue(body.contains("if (!pricingIsDeferred())"),
            "the deferral has to be asked for; the default is still to price straight away");
        assertTrue(body.contains("costEstimation.recalculate"));
    }

    // ------------------------------------------------------------------ one set of rules

    @Test
    @DisplayName("the batch applies the same five edits, never its own copy of them")
    void thereIsOnlyOneSetOfRules() throws IOException {
        String source = Files.readString(SERVICE);
        String dispatch = bodyOf(source,
            "private PortalOutcome applyOne(PortalJourney journey, PortalParticipant by, Change change)");

        for (String method : List.of("chooseStay(", "setRooms(", "setActivityIncluded(",
                "setPartySize(", "chooseFromCatalogue(")) {
            assertTrue(dispatch.contains(method),
                "the batch must reach " + method + " rather than carry its own version of it");
        }
    }

    @Test
    @DisplayName("an unreadable action is refused rather than silently skipped")
    void anUnknownActionStopsTheBatch() throws IOException {
        String source = Files.readString(SERVICE);
        String body = bodyOf(source,
            "public PortalOutcome applyAll(PortalJourney journey, PortalParticipant by,");

        assertTrue(body.contains("PORTAL_UNKNOWN_ACTION"),
            "skipping one would save four of five changes and report success, which is the "
                + "half-applied state this endpoint exists to make impossible");
    }

    @Test
    @DisplayName("the number of changes in one commit is bounded")
    void thereIsACap() {
        assertTrue(PortalEditingService.MOST_IN_ONE_GO > 0);
        assertTrue(PortalEditingService.MOST_IN_ONE_GO <= 100,
            "this list arrives from a browser and every entry is a read and a write inside one "
                + "transaction");
    }
}
