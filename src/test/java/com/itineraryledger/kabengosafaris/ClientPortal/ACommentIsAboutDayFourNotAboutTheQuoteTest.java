package com.itineraryledger.kabengosafaris.ClientPortal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation;
import com.itineraryledger.kabengosafaris.ClientPortal.DTOs.PortalComment;
import com.itineraryledger.kabengosafaris.ClientPortal.DTOs.PortalOutcome;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalJourney;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalParticipant;
import com.itineraryledger.kabengosafaris.ClientPortal.Enums.JourneyStatus;
import com.itineraryledger.kabengosafaris.ClientPortal.Enums.ProposalState;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalActivityService;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalCommentService;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.Entity.QuoteProposalComment;
import com.itineraryledger.kabengosafaris.Quote.Enums.CommentAnchor;
import com.itineraryledger.kabengosafaris.Quote.Enums.RevisionAuthor;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.Entity.QuoteDay;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayAccommodation.Entity.QuoteDayAccommodation;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteProposalCommentRepository;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices.QuoteRevisionService;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

/**
 * A comment is about day four, and what we say among ourselves stays among ourselves.
 *
 * <p>Two things are being protected and they are unequal in consequence.
 *
 * <p>The first is the feature: an anchor. Without one, a client who thinks the day-three lodge
 * is too far from the gate can only decline the whole quotation or write an email, and both
 * throw away which part they meant. That is three more messages and a week.
 *
 * <p>The second is the one that would end a relationship. The same thread carries the office
 * talking to each other — what we think somebody can afford, whether a request is worth
 * humouring — and a read path that forgets one flag publishes all of it to the person it is
 * about. There is no recovering from that, so it is checked in the query, and checked again
 * here, and a source scan below makes sure the portal never reaches for the other method.
 */
class ACommentIsAboutDayFourNotAboutTheQuoteTest {

    private QuoteProposalCommentRepository repository;
    private PortalCommentService service;
    private Quote quote;

    @BeforeEach
    void setUp() {
        repository = mock(QuoteProposalCommentRepository.class);
        QuoteRevisionService revisions = mock(QuoteRevisionService.class);
        when(revisions.latest(anyLong())).thenReturn(Optional.empty());

        IdObfuscator obfuscator = mock(IdObfuscator.class);
        when(obfuscator.encodeId(anyLong())).thenReturn("abc123");

        service = new PortalCommentService(repository, revisions,
            mock(PortalActivityService.class), obfuscator);

        quote = new Quote();
        quote.setId(1L);
        quote.setDays(new ArrayList<>(List.of(day(3, "Serengeti", 50L, "Kudu Lodge"))));
    }

    private QuoteDay day(int number, String title, Long stayId, String lodgeName) {
        Accommodation lodge = new Accommodation();
        lodge.setName(lodgeName);

        QuoteDayAccommodation stay = new QuoteDayAccommodation();
        stay.setId(stayId);
        stay.setAccommodation(lodge);

        QuoteDay day = new QuoteDay();
        day.setId(100L + number);
        day.setDayNumber(number);
        day.setTitle(title);
        day.setAccommodations(new ArrayList<>(List.of(stay)));
        stay.setQuoteDay(day);
        return day;
    }

    private PortalJourney journey() {
        PortalJourney journey = PortalJourney.builder()
            .quote(quote).status(JourneyStatus.OPEN)
            .proposalState(ProposalState.OPEN_TO_CLIENT).locked(false).build();
        journey.setId(7L);
        return journey;
    }

    private PortalParticipant lead() {
        return PortalParticipant.builder().email("sarah@example.com").build();
    }

    private QuoteProposalComment saved() {
        ArgumentCaptor<QuoteProposalComment> captor =
            ArgumentCaptor.forClass(QuoteProposalComment.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    // ------------------------------------------------------------------ privacy

    @Test
    @DisplayName("what the office says among itself never reaches the person it is about")
    void anInternalNoteIsNotPublished() {
        service.forClient(quote);

        verify(repository).findVisibleToClient(1L);
        verify(repository, never()).findByQuoteIdOrderByIdAsc(anyLong());
    }

    @Test
    @DisplayName("a client's own comment is always visible, because a secret needs somebody to keep it from")
    void aClientCannotWhisper() {
        service.addFromClient(journey(), lead(), "DAY", 3, null, "Is this near the gate?");

        assertTrue(saved().getVisibleToClient(),
            "there is nowhere for a client to keep a secret from the only other party");
        assertEquals(RevisionAuthor.CLIENT, saved().getAuthorKind());
    }

    @Test
    @DisplayName("the office's comment carries the visibility the office chose")
    void staffChooseTheirOwnVisibility() {
        service.addFromStaff(quote, journey(), "DAY", 3, null,
            "They will not pay this; try the mid-range option", false, "Rick", 3L);

        assertFalse(saved().getVisibleToClient());
        assertEquals(RevisionAuthor.STAFF, saved().getAuthorKind());
    }

    @Test
    @DisplayName("a client's copy of a comment carries no visibility field to be misread later")
    void theClientsCopyHasNoFlag() {
        QuoteProposalComment row = QuoteProposalComment.builder()
            .quote(quote).anchor(CommentAnchor.DAY).dayNumber(3).body("Fine by us")
            .authorKind(RevisionAuthor.STAFF).visibleToClient(true).build();
        row.setId(9L);
        when(repository.findVisibleToClient(1L)).thenReturn(List.of(row));

        List<PortalComment> seen = service.forClient(quote);

        assertEquals(1, seen.size());
        assertNull(seen.get(0).visibleToClient(),
            "a field that is always true is a field somebody will later make meaningful");
    }

    // ------------------------------------------------------------------ the anchor

    @Test
    @DisplayName("a comment about a line is named by us, from the row, not by whoever is typing")
    void weNameTheThingBeingDiscussed() {
        service.addFromClient(journey(), lead(), "LINE", 3, 50L, "Too far from the gate");

        assertEquals("Kudu Lodge", saved().getTargetLabel(),
            "a client who could supply the label could write one the quotation contradicts");
        assertEquals(3, saved().getDayNumber());
        assertEquals(50L, saved().getTargetId());
    }

    @Test
    @DisplayName("a line that is not on that day is refused, like every other id in this module")
    void aLineFromSomewhereElseIsRefused() {
        PortalOutcome outcome =
            service.addFromClient(journey(), lead(), "LINE", 3, 999L, "Too far from the gate");

        assertFalse(outcome.done());
        assertEquals("NOT_ON_THIS_BOOKING", outcome.code());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("a day the quotation does not have is refused")
    void aDayThatIsNotThereIsRefused() {
        assertEquals("NOT_ON_THIS_BOOKING",
            service.addFromClient(journey(), lead(), "DAY", 11, null, "What about here?").code());
    }

    @Test
    @DisplayName("a comment about the whole quotation needs no day and is still recorded")
    void theWholeQuotationIsAValidThingToTalkAbout() {
        assertTrue(service.addFromClient(journey(), lead(), "WHOLE", null, null,
            "Can we do this in June instead?").done());
        assertEquals(CommentAnchor.WHOLE, saved().getAnchor());
        assertNull(saved().getDayNumber());
    }

    // ------------------------------------------------------------------ the pen

    @Test
    @DisplayName("a question is never a conflict, so commenting is not gated on who holds the pen")
    void theyMayAskWhileWeAreWorkingOnIt() {
        PortalJourney journey = journey();
        journey.setProposalState(ProposalState.WITH_OPERATOR);

        assertTrue(service.addFromClient(journey, lead(), "DAY", 3, null, "Any news?").done(),
            "a client who can see their quotation and may not ask about it has been handed a "
                + "document with a closed door beside it");
    }

    @Test
    @DisplayName("an empty comment is not a comment")
    void nothingToSayIsRefused() {
        assertEquals("EMPTY_COMMENT",
            service.addFromClient(journey(), lead(), "WHOLE", null, null, "   ").code());
    }

    @Test
    @DisplayName("an essay is sent by email, where they can keep their own copy")
    void anEssayIsRefusedWithSomewhereToPutIt() {
        PortalOutcome outcome = service.addFromClient(journey(), lead(), "WHOLE", null, null,
            "x".repeat(5000));

        assertEquals("COMMENT_TOO_LONG", outcome.code());
        assertTrue(outcome.message().contains("email"));
    }

    // ------------------------------------------------------------------ the scan

    /**
     * Nothing but the comment service touches the comment table.
     *
     * <p>The first version of this scanned for the unfiltered finder by name and immediately
     * caught {@code PortalEditingService}, which reads the travelling party through a method of
     * the same name on a different repository. That is the warning: a scan keyed on a method
     * name is keyed on a coincidence.
     *
     * <p>So the rule is the stronger and simpler one. One service owns the table, and it is the
     * service that knows which rows each side may read. Anything else in the portal that wants
     * comments has to go through it, and cannot choose the wrong query because it cannot see
     * the queries.
     */
    @Test
    @DisplayName("one service owns the comment table, so nothing else can choose the wrong query")
    void onlyOneServiceCanReadComments() throws IOException {
        List<String> offenders = new ArrayList<>();
        Path portal = Path.of("src/main/java/com/itineraryledger/kabengosafaris/ClientPortal");

        try (var files = Files.walk(portal)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                if (file.getFileName().toString().equals("PortalCommentService.java")) continue;
                if (Files.readString(file).contains("QuoteProposalCommentRepository")) {
                    offenders.add(file.getFileName().toString());
                }
            }
        }

        assertTrue(offenders.isEmpty(),
            "these reach the comment table directly and can read what the client was never "
                + "meant to see: " + offenders);
    }
}
