package com.itineraryledger.kabengosafaris.ClientPortal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.ClientPortal.DTOs.PortalAdminDTOs;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalJourney;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalParticipant;
import com.itineraryledger.kabengosafaris.ClientPortal.Enums.JourneyStatus;
import com.itineraryledger.kabengosafaris.ClientPortal.Enums.ParticipantRole;
import com.itineraryledger.kabengosafaris.ClientPortal.Enums.PortalSection;
import com.itineraryledger.kabengosafaris.ClientPortal.Enums.ProposalState;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalAccessEventRepository;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalInsightService;

/**
 * "Have they looked at it?" must not be answered with our own clicks.
 *
 * <p>The access log holds everything that happens to a journey, and that includes us: a
 * consultant freezing the quotation, the office taking a proposal over, a link being withdrawn,
 * an editing level changed. Counting rows to answer whether the customer is interested folds
 * all of that in and produces a number that is confidently wrong — a quotation nobody has opened
 * reading as busy because three people here have been tidying it up.
 *
 * <p>That is the same failure as the stats rule everywhere else in this application: never
 * surface a figure nothing writes, and never surface one that writes something else. It is also
 * why {@code PortalSection} became an enum carrying its own kind. A string cannot be asked what
 * it means, so the classification would have lived in whichever service needed it, and the
 * second copy of it would have been the one that went stale.
 *
 * <p>The other half of this file is the sentences. Each one has to be a fact the rows support
 * exactly, naming the count it came from, because a consultant will act on them: "a code has
 * been sent three times and nobody has got in" is worth a telephone call, and an engagement
 * score out of ten is worth nothing at all.
 */
class TheLogSaysWhatTheCustomerDidNotWhatWeDidTest {

    private PortalAccessEventRepository events;
    private PortalInsightService service;
    private PortalJourney journey;

    @BeforeEach
    void setUp() {
        events = mock(PortalAccessEventRepository.class);
        service = new PortalInsightService(events);

        journey = PortalJourney.builder()
            .status(JourneyStatus.OPEN)
            .proposalState(ProposalState.OPEN_TO_CLIENT)
            .participants(new ArrayList<>())
            .build();
        journey.setId(7L);
        journey.setCreatedAt(LocalDateTime.now().minusDays(6));

        when(events.countBySection(anyLong())).thenReturn(List.of());
        when(events.countByParticipant(anyLong(), any())).thenReturn(List.of());
        when(events.lastOf(anyLong(), any())).thenReturn(null);
        when(events.countSince(anyLong(), any(), any())).thenReturn(0L);
    }

    private Object[] row(PortalSection section, long count, LocalDateTime first, LocalDateTime last) {
        return new Object[] { section.name(), count, first, last };
    }

    private void log(Object[]... rows) {
        when(events.countBySection(7L)).thenReturn(List.of(rows));
    }

    private PortalAdminDTOs.Insight insight() {
        return service.forJourney(journey);
    }

    // ------------------------------------------------------------------ whose activity is it

    @Test
    @DisplayName("a journey only we have touched has not been opened by anybody")
    void ourOwnClicksAreNotTheirInterest() {
        LocalDateTime now = LocalDateTime.now();
        log(row(PortalSection.JOURNEY_LEVEL, 2, now, now),
            row(PortalSection.JOURNEY_LOCKED, 1, now, now),
            row(PortalSection.PROPOSAL_TAKEN_OVER, 1, now, now),
            row(PortalSection.PROPOSAL_RETURNED, 1, now, now));

        PortalAdminDTOs.Insight insight = insight();

        assertFalse(insight.everOpened(),
            "five rows on this journey and not one of them is the customer");
        assertEquals(0, insight.reads());
        assertEquals(0, insight.actions());
        assertTrue(insight.sections().isEmpty(), "our own work is not a section they read");
        assertTrue(insight.signals().stream().anyMatch(s -> s.contains("Nobody has opened")));
    }

    @Test
    @DisplayName("reading is reading and deciding is deciding, counted apart")
    void readingAndDecidingAreCountedApart() {
        LocalDateTime now = LocalDateTime.now();
        log(row(PortalSection.TRIP, 4, now.minusDays(3), now),
            row(PortalSection.QUOTE, 3, now.minusDays(2), now),
            row(PortalSection.EDIT_ROOMS, 2, now, now),
            row(PortalSection.JOURNEY_LEVEL, 9, now, now));

        PortalAdminDTOs.Insight insight = insight();

        assertTrue(insight.everOpened());
        assertEquals(7, insight.reads(), "the nine level changes are ours");
        assertEquals(2, insight.actions());
    }

    @Test
    @DisplayName("getting in is neither: a code is not a page they read")
    void verificationIsNotReading() {
        LocalDateTime now = LocalDateTime.now();
        log(row(PortalSection.VERIFY_PAGE, 3, now, now),
            row(PortalSection.VERIFY_CODE_SENT, 2, now, now));

        PortalAdminDTOs.Insight insight = insight();

        assertFalse(insight.everOpened(),
            "somebody who asked for a code and never got in has not read anything");
        assertEquals(0, insight.reads());
        assertTrue(insight.sections().isEmpty());
    }

    @Test
    @DisplayName("the busiest thing they looked at comes first")
    void sectionsAreOrderedByHowOftenTheyCameBack() {
        LocalDateTime now = LocalDateTime.now();
        log(row(PortalSection.TRIP, 2, now, now),
            row(PortalSection.QUOTE, 9, now, now),
            row(PortalSection.INVOICE, 1, now, now));

        List<PortalAdminDTOs.SectionCount> sections = insight().sections();

        assertEquals("QUOTE", sections.get(0).key());
        assertEquals(9, sections.get(0).count());
        assertEquals("READING", sections.get(0).kind());
        assertEquals("INVOICE", sections.get(2).key());
    }

    // ------------------------------------------------------------------ the sentences

    @Test
    @DisplayName("a code sent and nobody in means it is in a spam folder, and says so")
    void aCodeThatNeverArrived() {
        LocalDateTime now = LocalDateTime.now();
        log(row(PortalSection.TRIP, 1, now, now),
            row(PortalSection.VERIFY_CODE_SENT, 3, now, now));

        List<String> signals = insight().signals();

        assertTrue(signals.stream().anyMatch(s -> s.contains("3 times") && s.contains("spam")),
            "the most common support call a portal produces, and the client never reports it: "
                + "from their side the website simply did not work. Got: " + signals);
    }

    @Test
    @DisplayName("a code that did arrive produces no such sentence")
    void aVerifiedClientIsNotChasedAboutTheirCode() {
        LocalDateTime now = LocalDateTime.now();
        log(row(PortalSection.VERIFY_CODE_SENT, 3, now, now),
            row(PortalSection.VERIFY_OK, 1, now, now),
            row(PortalSection.TRIP, 2, now, now));

        assertTrue(insight().signals().stream().noneMatch(s -> s.contains("spam")));
    }

    @Test
    @DisplayName("wrong codes are mentioned, with what usually causes them")
    void wrongCodesAreExplained() {
        LocalDateTime now = LocalDateTime.now();
        log(row(PortalSection.VERIFY_FAILED, 4, now, now),
            row(PortalSection.VERIFY_OK, 1, now, now),
            row(PortalSection.TRIP, 1, now, now));

        assertTrue(insight().signals().stream()
            .anyMatch(s -> s.contains("4 wrong codes") && s.contains("most recent")));
    }

    @Test
    @DisplayName("an address that is not on the booking is worth an operator seeing")
    void anUnknownAddressIsSurfaced() {
        LocalDateTime now = LocalDateTime.now();
        log(row(PortalSection.VERIFY_UNKNOWN_ADDRESS, 2, now, now),
            row(PortalSection.TRIP, 1, now, now));

        assertTrue(insight().signals().stream()
            .anyMatch(s -> s.contains("not on this booking") && s.contains("withdraw")));
    }

    @Test
    @DisplayName("we sent it back and they have not looked since")
    void returnedAndUnread() {
        LocalDateTime now = LocalDateTime.now();
        journey.setProposalState(ProposalState.RETURNED);
        journey.setProposalStateAt(now.minusDays(4));
        log(row(PortalSection.TRIP, 3, now.minusDays(9), now.minusDays(8)));
        when(events.lastOf(anyLong(), any())).thenReturn(now.minusDays(8));
        when(events.countSince(anyLong(), any(), any())).thenReturn(0L);

        assertTrue(insight().signals().stream().anyMatch(s -> s.contains("Sent back")),
            "a proposal answered and not read is the one state nothing else in the panel shows");
    }

    @Test
    @DisplayName("sent back and they HAVE looked produces no chase")
    void returnedAndRead() {
        LocalDateTime now = LocalDateTime.now();
        journey.setProposalState(ProposalState.RETURNED);
        journey.setProposalStateAt(now.minusDays(4));
        log(row(PortalSection.TRIP, 3, now.minusDays(9), now.minusHours(2)));
        when(events.lastOf(anyLong(), any())).thenReturn(now.minusHours(2));
        when(events.countSince(anyLong(), any(), any())).thenReturn(5L);

        assertTrue(insight().signals().stream().noneMatch(s -> s.contains("Sent back")));
    }

    // ------------------------------------------------------------------ nothing is invented

    @Test
    @DisplayName("never done anything is null, never a zero that reads like a measurement")
    void nothingToMeasureFromIsNull() {
        PortalAdminDTOs.Insight insight = insight();

        assertNull(insight.firstOpenedAt());
        assertNull(insight.lastSeenAt());
        assertNull(insight.daysSinceLastSeen(),
            "zero days since last seen says they were here today");
        assertFalse(insight.everOpened());
    }

    @Test
    @DisplayName("there is no score, and no field anybody could price a trip by")
    void noInventedFigures() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/itineraryledger/"
            + "kabengosafaris/ClientPortal/DTOs/PortalAdminDTOs.java"));
        int insight = source.indexOf("public record Insight(");
        String body = source.substring(insight, source.indexOf("/** One line of the access log", insight));

        for (String invented : List.of("score", "rating", "likelihood", "engagement",
                "probability", "interestLevel", "percentage")) {
            assertFalse(body.toLowerCase(Locale.ROOT).contains(invented),
                "every field here must be a count, a timestamp or a sentence naming its count: "
                    + invented + " is a number wearing a figure's clothes");
        }
    }

    // ------------------------------------------------------------------ the vocabulary itself

    @Test
    @DisplayName("every section says what kind of thing it is, so nothing is counted by accident")
    void everySectionIsClassified() {
        for (PortalSection section : PortalSection.values()) {
            if (section == PortalSection.OTHER) continue;
            assertNotNull(section.getKind());
            assertFalse(section.getKind() == PortalSection.Kind.UNKNOWN,
                section.name() + " was added without deciding whether it is the customer or us. "
                    + "That decision cannot be deferred: whichever count it lands in is wrong.");
            assertFalse(section.getLabel() == null || section.getLabel().isBlank(),
                section.name() + " has no label, so the log will show the raw key");
        }
    }

    @Test
    @DisplayName("a key written by an older version is readable and counted as nothing")
    void anUnknownKeyIsNotAnError() {
        assertSame(PortalSection.OTHER, PortalSection.of("SOMETHING_WE_RENAMED"));
        assertSame(PortalSection.OTHER, PortalSection.of(null));
        assertFalse(PortalSection.OTHER.isTheirs(),
            "a row nobody can classify must not be counted as the customer looking");
    }

    /**
     * The client's own page says which section it opened, and the key arrives from the client.
     *
     * <p>Without the check this test pins, a page could post PROPOSAL_AGREED and the office
     * would read a row in the log saying a customer accepted a quotation they never saw.
     */
    @Test
    @DisplayName("a client may report looking at something, never deciding something")
    void aPageViewCannotForgeADecision() throws IOException {
        assertFalse(PortalSection.PROPOSAL_AGREED.isTheClientLooking());
        assertFalse(PortalSection.EDIT_STAY.isTheClientLooking());
        assertFalse(PortalSection.JOURNEY_REVOKED.isTheClientLooking());
        assertTrue(PortalSection.QUOTE.isTheClientLooking());

        String source = Files.readString(Path.of("src/main/java/com/itineraryledger/"
            + "kabengosafaris/ClientPortal/Controller/PortalApiController.java"));
        int endpoint = source.indexOf("/{token}/seen");
        assertTrue(endpoint > 0, "the endpoint has moved or been renamed");

        String method = source.substring(endpoint, endpoint + 1800);
        assertTrue(method.contains("isTheClientLooking()"),
            "the only thing standing between a client's JSON body and a forged decision in our "
                + "own log");
    }

    // ------------------------------------------------------------------ per person

    @Test
    @DisplayName("somebody invited who has never opened it shows as nought, not as missing")
    void anInvitedReaderWhoNeverCameIsStillListed() {
        PortalParticipant lead = PortalParticipant.builder()
            .journey(journey).email("sarah@example.com")
            .participantRole(ParticipantRole.LEAD).build();
        lead.setId(1L);
        lead.setIsActive(true);
        lead.setVerifiedAt(LocalDateTime.now().minusDays(3));

        PortalParticipant reader = PortalParticipant.builder()
            .journey(journey).email("tom@example.com")
            .participantRole(ParticipantRole.READER).build();
        reader.setId(2L);
        reader.setIsActive(true);

        journey.getParticipants().add(lead);
        journey.getParticipants().add(reader);

        LocalDateTime now = LocalDateTime.now();
        when(events.countByParticipant(anyLong(), any()))
            .thenReturn(List.<Object[]>of(new Object[] { 1L, 6L, now.minusDays(3), now }));

        List<PortalAdminDTOs.PersonActivity> people = insight().people();

        assertEquals(2, people.size());
        assertEquals(6, people.get(0).events());
        assertTrue(people.get(0).verified());
        assertEquals(0, people.get(1).events(),
            "the reader who was invited and never came is the interesting row; leaving them out "
                + "of the list is how nobody notices");
        assertFalse(people.get(1).verified());
        assertNull(people.get(1).lastAt());
    }
}
