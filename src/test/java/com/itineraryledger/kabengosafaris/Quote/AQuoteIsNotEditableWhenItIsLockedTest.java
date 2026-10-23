package com.itineraryledger.kabengosafaris.Quote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalJourney;
import com.itineraryledger.kabengosafaris.ClientPortal.Enums.ProposalState;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalJourneyRepository;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.Enums.QuoteStatus;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices.QuoteEditPolicy;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices.QuoteEditPolicy.Actor;

/**
 * A quotation that has been decided, replaced or frozen does not change under anybody.
 *
 * <p>This was two rules before it was one. {@code QuoteUpdateService} refused to edit an
 * ACCEPTED or CONVERTED quote while the four day-tree services refused nothing, so the header of
 * a sold safari was frozen and its prices were not: changing the lodge on day four silently
 * re-priced a quotation the customer had already agreed to. The scan at the bottom is the part
 * that keeps it fixed — a fifth service, or a new method on one of the four, fails this test
 * until it asks the policy too.
 */
class AQuoteIsNotEditableWhenItIsLockedTest {

    private static final Path DAY_TREE =
        Path.of("src/main/java/com/itineraryledger/kabengosafaris/Quote/QuoteDay");

    private QuoteEditPolicy policyWith(PortalJourney journey) {
        PortalJourneyRepository journeys = mock(PortalJourneyRepository.class);
        when(journeys.findByQuoteId(org.mockito.ArgumentMatchers.anyLong()))
            .thenReturn(Optional.ofNullable(journey));
        return new QuoteEditPolicy(journeys);
    }

    private Quote quote(QuoteStatus status) {
        Quote quote = new Quote();
        quote.setId(1L);
        quote.setStatus(status);
        quote.setIsActive(true);
        return quote;
    }

    // ------------------------------------------------------------------ status

    @Test
    @DisplayName("a converted quotation cannot be re-priced, which used to be possible")
    void convertedIsClosed() {
        QuoteEditPolicy.Decision decision =
            policyWith(null).canEditDayTree(quote(QuoteStatus.CONVERTED), Actor.STAFF);

        assertFalse(decision.allowed(), "a safari has been sold against this price");
        assertEquals("QUOTE_DECIDED", decision.code());
    }

    @Test
    @DisplayName("every decided status is closed, not only the one that prompted this")
    void everyDecidedStatusIsClosed() {
        for (QuoteStatus status : List.of(QuoteStatus.ACCEPTED, QuoteStatus.REJECTED,
                QuoteStatus.EXPIRED, QuoteStatus.CANCELLED, QuoteStatus.CONVERTED)) {
            assertFalse(policyWith(null).canEditDayTree(quote(status), Actor.STAFF).allowed(),
                status + " records a decision and must not move under it");
        }
    }

    @Test
    @DisplayName("a draft is open, or the panel could not do its job")
    void draftIsOpen() {
        assertTrue(policyWith(null).canEditDayTree(quote(QuoteStatus.DRAFT), Actor.STAFF).allowed());
    }

    @Test
    @DisplayName("a superseded quotation sends you to the version that replaced it")
    void supersededIsClosed() {
        Quote quote = quote(QuoteStatus.DRAFT);
        quote.setNextVersion(quote(QuoteStatus.DRAFT));

        QuoteEditPolicy.Decision decision = policyWith(null).canEditDayTree(quote, Actor.STAFF);

        assertFalse(decision.allowed(), "editing both leaves nobody able to say which was sent");
        assertEquals("QUOTE_SUPERSEDED", decision.code());
    }

    // ------------------------------------------------------------------ the portal

    @Test
    @DisplayName("frozen stops staff too, because freezing is what you do when something is wrong")
    void frozenStopsEveryone() {
        PortalJourney journey = PortalJourney.builder()
            .locked(true).proposalState(ProposalState.OPEN_TO_CLIENT).build();

        for (Actor actor : Actor.values()) {
            QuoteEditPolicy.Decision decision =
                policyWith(journey).canEditDayTree(quote(QuoteStatus.DRAFT), actor);
            assertFalse(decision.allowed(), actor + " must not edit a frozen quotation");
            assertEquals("QUOTE_FROZEN", decision.code());
        }
    }

    @Test
    @DisplayName("once we are reviewing it the client is refused and we are not")
    void withOperatorStopsOnlyTheClient() {
        PortalJourney journey = PortalJourney.builder()
            .locked(false).proposalState(ProposalState.WITH_OPERATOR).build();

        assertTrue(policyWith(journey).canEditDayTree(quote(QuoteStatus.DRAFT), Actor.STAFF).allowed(),
            "taking the pen is the point of taking the pen");

        QuoteEditPolicy.Decision client =
            policyWith(journey).canEditDayTree(quote(QuoteStatus.DRAFT), Actor.CLIENT);
        assertFalse(client.allowed(), "their edit would overwrite a review in progress");
        assertEquals("WITH_OPERATOR", client.code());
    }

    @Test
    @DisplayName("submitting does not lock them out: they keep the pen until somebody takes it")
    void submittedStillLetsTheClientEdit() {
        PortalJourney journey = PortalJourney.builder()
            .locked(false).proposalState(ProposalState.SUBMITTED).build();

        assertTrue(policyWith(journey).canEditDayTree(quote(QuoteStatus.DRAFT), Actor.CLIENT).allowed(),
            "a deadlock is the failure this state machine exists to avoid");
    }

    // ------------------------------------------------------------------ the scan

    /**
     * Every method that writes to the day tree asks the policy first.
     *
     * <p>A list of the four services would rot the moment somebody adds a fifth. Reading the
     * package is the only check that still holds then, and the failure message names the method
     * so the fix is obvious without opening the test.
     */
    @Test
    @DisplayName("no day-tree service can write without consulting the policy")
    void everyMutationConsultsThePolicy() throws IOException {
        Pattern mutator = Pattern.compile(
            "\\n    public ResponseEntity<ApiResponse<\\?>> (create|update|delete|reorder)\\(");

        List<String> offenders = new ArrayList<>();

        try (Stream<Path> files = Files.walk(DAY_TREE)) {
            for (Path file : files.filter(f -> f.toString().endsWith("Service.java")).toList()) {
                String source = Files.readString(file);
                Matcher m = mutator.matcher(source);
                while (m.find()) {
                    String body = bodyAt(source, m.end());
                    if (!body.contains("editPolicy.")) {
                        offenders.add(file.getFileName() + "#" + m.group(1));
                    }
                }
            }
        }

        assertTrue(offenders.isEmpty(),
            "these write to a quotation's prices without asking whether it may change: " + offenders);
    }

    /** The source of one method, by brace matching from its signature. */
    private String bodyAt(String source, int from) {
        int open = source.indexOf('{', from);
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') depth++;
            else if (c == '}' && --depth == 0) return source.substring(open, i);
        }
        return source.substring(open);
    }
}
