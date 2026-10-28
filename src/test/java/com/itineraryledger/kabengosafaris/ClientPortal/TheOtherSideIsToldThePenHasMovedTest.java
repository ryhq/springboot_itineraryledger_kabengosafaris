package com.itineraryledger.kabengosafaris.ClientPortal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.Backup.BackupSettings.BackupSettingsGetterServices;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalJourney;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalParticipant;
import com.itineraryledger.kabengosafaris.ClientPortal.Enums.JourneyStatus;
import com.itineraryledger.kabengosafaris.ClientPortal.Enums.ParticipantRole;
import com.itineraryledger.kabengosafaris.ClientPortal.Enums.ProposalState;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalJourneyRepository;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalAccessService;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalActivityService;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalChangeLogService;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalNotifier;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalProposalService;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountServices.EmailSendingService;
import com.itineraryledger.kabengosafaris.EmailEvent.Services.CustomerAcknowledgementSender;
import com.itineraryledger.kabengosafaris.EmailEvent.Services.EmailTemplateRenderer;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices.QuoteRevisionService;

/**
 * Every handover reaches the person waiting on the other side of it.
 *
 * <p>The review loop was built before any of this and it worked perfectly, in the sense that the
 * states moved and the tests passed. It was also a loop nobody was in. A client pressed Submit
 * and saw a sentence on a page; nothing happened anywhere else, and the proposal sat until
 * somebody here happened to open the right tab. A member of staff took it over and the client's
 * controls simply stopped responding, with the explanation available only on a page they had no
 * reason to reload.
 *
 * <p>So what is pinned here is not that an email is nice to have. It is that each of the five
 * transitions has somebody waiting, and which side that is.
 *
 * <p>The second half is the opposite worry. An email must never be able to undo the thing it is
 * announcing: a client whose submission is refused because our mail host is unreachable has been
 * given the worst of both, and the state change is the event while the message is only how
 * somebody hears about it.
 */
class TheOtherSideIsToldThePenHasMovedTest {

    // ------------------------------------------------------------------ who is told

    private PortalNotifier notifier;

    private PortalProposalService serviceOn(PortalJourney journey) {
        notifier = mock(PortalNotifier.class);
        PortalJourneyRepository journeys = mock(PortalJourneyRepository.class);
        when(journeys.save(any(PortalJourney.class))).thenReturn(journey);

        return new PortalProposalService(journeys, mock(QuoteRevisionService.class),
            mock(PortalActivityService.class), notifier);
    }

    private PortalJourney journeyIn(ProposalState state) {
        Quote quote = new Quote();
        quote.setId(1L);

        PortalJourney journey = PortalJourney.builder()
            .quote(quote).status(JourneyStatus.OPEN).proposalState(state).locked(false).build();
        journey.setId(7L);
        return journey;
    }

    private PortalParticipant lead() {
        return PortalParticipant.builder()
            .email("sarah@example.com").participantRole(ParticipantRole.LEAD).build();
    }

    @Test
    @DisplayName("submitting tells the office, because nothing else will")
    void submittingReachesTheOffice() {
        PortalJourney journey = journeyIn(ProposalState.OPEN_TO_CLIENT);
        serviceOn(journey).submit(journey, lead());

        verify(notifier).proposalSubmitted(journey);
    }

    @Test
    @DisplayName("accepting tells the office, and that message is load-bearing")
    void acceptingReachesTheOffice() {
        PortalJourney journey = journeyIn(ProposalState.RETURNED);
        serviceOn(journey).agree(journey, lead());

        verify(notifier).quoteAgreed(journey);
    }

    @Test
    @DisplayName("declining tells the office, with the reason, or a lost customer is silent")
    void decliningReachesTheOfficeWithTheReason() {
        PortalJourney journey = journeyIn(ProposalState.RETURNED);
        serviceOn(journey).decline(journey, lead(), "Over budget");

        verify(notifier).quoteDeclined(journey, "Over budget");
    }

    @Test
    @DisplayName("taking it over tells the client, before they find their controls dead")
    void takingOverReachesTheClient() {
        PortalJourney journey = journeyIn(ProposalState.SUBMITTED);
        serviceOn(journey).takeOver(journey);

        verify(notifier).proposalInReview(eq(journey), any());
    }

    @Test
    @DisplayName("handing it back tells the client")
    void returningReachesTheClient() {
        PortalJourney journey = journeyIn(ProposalState.WITH_OPERATOR);
        serviceOn(journey).returnToClient(journey);

        verify(notifier).proposalReturned(journey);
    }

    @Test
    @DisplayName("a refused handover tells nobody, because nothing happened")
    void aRefusalIsNotAnnounced() {
        PortalJourney journey = journeyIn(ProposalState.WITH_OPERATOR);
        PortalProposalService service = serviceOn(journey);

        service.submit(journey, lead());

        org.mockito.Mockito.verifyNoInteractions(notifier);
    }

    // ------------------------------------------------------------------ nothing fails the handover

    private PortalNotifier notifierWhoseMailIsBroken(List<String> recipients) {
        EmailSendingService mail = mock(EmailSendingService.class);
        doThrow(new IllegalStateException("connection refused"))
            .when(mail).sendHtmlEmail(anyString(), anyString(), anyString());

        CustomerAcknowledgementSender sender = mock(CustomerAcknowledgementSender.class);
        doThrow(new IllegalStateException("connection refused"))
            .when(sender).send(anyString(), anyString(), anyString(), any(), any());

        BackupSettingsGetterServices settings = mock(BackupSettingsGetterServices.class);
        when(settings.getNotificationEmails()).thenReturn(recipients);

        PortalAccessService access = mock(PortalAccessService.class);
        when(access.linkFor(any())).thenReturn("https://mytrip.example.com/portal/abc");

        PortalChangeLogService changes = mock(PortalChangeLogService.class);
        when(changes.forQuote(any())).thenReturn(List.of());

        return new PortalNotifier(sender, mock(EmailTemplateRenderer.class), mail, settings,
            access, changes);
    }

    @Test
    @DisplayName("a mail host that is down does not undo a handover")
    void brokenMailDoesNotBreakTheLoop() {
        PortalJourney journey = journeyIn(ProposalState.OPEN_TO_CLIENT);
        journey.getParticipants().add(lead());
        PortalNotifier broken = notifierWhoseMailIsBroken(List.of("office@example.com"));

        assertDoesNotThrow(() -> broken.proposalSubmitted(journey));
        assertDoesNotThrow(() -> broken.quoteAgreed(journey));
        assertDoesNotThrow(() -> broken.quoteDeclined(journey, "no reason"));
        assertDoesNotThrow(() -> broken.proposalInReview(journey, "Rick"));
        assertDoesNotThrow(() -> broken.proposalReturned(journey),
            "the state already moved; this is how somebody hears about it, not whether");
    }

    @Test
    @DisplayName("no notification address configured is a warning, not a failure")
    void nobodyConfiguredIsNotAnError() {
        PortalJourney journey = journeyIn(ProposalState.OPEN_TO_CLIENT);
        PortalNotifier nobody = notifierWhoseMailIsBroken(List.of());

        assertDoesNotThrow(() -> nobody.proposalSubmitted(journey));
    }

    // ------------------------------------------------------------------ the scan

    /**
     * Every registered event can be previewed from the panel.
     *
     * <p>{@code EmailTemplateTestService} throws on an event it has no case for, and the panel
     * offers "send me a test" beside every event in the list. So an event registered without a
     * case is a button that produces a 500, and the only person who will find it is whoever is
     * checking the wording of a template before a release.
     */
    @Test
    @DisplayName("every email event the application registers can be sent as a test")
    void everyEventCanBePreviewed() throws IOException {
        String initializer = Files.readString(Path.of("src/main/java/com/itineraryledger/"
            + "kabengosafaris/Initializers/EmailEventInitializer.java"));
        String tester = Files.readString(Path.of("src/main/java/com/itineraryledger/"
            + "kabengosafaris/EmailEvent/Services/EmailTemplateTestService.java"));

        Set<String> registered = new TreeSet<>();
        Matcher m = Pattern.compile("initializeEvent\\(\\s*\"([A-Z0-9_]+)\"").matcher(initializer);
        while (m.find()) registered.add(m.group(1));

        Set<String> previewable = new TreeSet<>();
        Matcher c = Pattern.compile("case \"([A-Z0-9_]+)\":").matcher(tester);
        while (c.find()) previewable.add(c.group(1));

        assertTrue(registered.size() > 30, "the scan found almost no events; the pattern has rotted");

        List<String> missing = new ArrayList<>(registered);
        missing.removeAll(previewable);
        assertEquals(List.of(), missing,
            "these events are registered but cannot be previewed, so the panel's test button "
                + "throws for them: " + missing);
    }
}
