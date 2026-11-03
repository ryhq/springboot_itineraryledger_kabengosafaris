package com.itineraryledger.kabengosafaris.ClientPortal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.itineraryledger.kabengosafaris.ClientPortal.DTOs.PortalOutcome;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalJourney;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalParticipant;
import com.itineraryledger.kabengosafaris.ClientPortal.Enums.JourneyStatus;
import com.itineraryledger.kabengosafaris.ClientPortal.Enums.ParticipantRole;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalCodeRepository;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalDeviceRepository;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalParticipantRepository;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalActivityService;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalInvitationService;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalNotifier;
import com.itineraryledger.kabengosafaris.Security.RateLimit.RateLimiter;

/**
 * The organiser puts their family on their own booking. None of them can accept it.
 *
 * <p>Before this, a group trip was run by forwarding the link, which works and is the problem.
 * One person's session gets shared, so the new-device warning fires on people who are supposed
 * to be there and is learnt to be ignored; every reader appears in the access log as the lead;
 * and taking one person off means revoking the link for everybody.
 *
 * <p>Two halves, and the second is the one worth being careful about.
 *
 * <p><b>A reader decides nothing.</b> Not re-implemented here. {@code ParticipantRole.mayAct()}
 * already stands in front of every acting path, and the single rule this service obeys is that
 * it never creates anything else. A second lead would be a booking two people can accept, and a
 * contract accepted by somebody who was not authorised to accept it is a problem no software
 * unpicks afterwards.
 *
 * <p><b>An invitation is an email to an address nobody here typed.</b> That is the actual risk
 * of the feature. Unbounded, a trip link becomes a way of sending mail from the company's own
 * domain to any address somebody names, which is worth far more to a stranger than a quotation
 * is. Three limits hold it: lead-only, a cap per booking, and a rate per booking. Each is tested
 * below, and each was chosen because removing it leaves something worth abusing.
 */
class AnInvitedReaderReadsEverythingAndDecidesNothingTest {

    private PortalParticipantRepository participants;
    private PortalDeviceRepository devices;
    private PortalCodeRepository codes;
    private PortalNotifier notifier;
    private PortalActivityService activity;
    private RateLimiter rateLimiter;
    private PortalInvitationService service;

    private PortalJourney journey;
    private PortalParticipant lead;
    private List<PortalParticipant> onTheBooking;

    @BeforeEach
    void setUp() {
        participants = mock(PortalParticipantRepository.class);
        devices = mock(PortalDeviceRepository.class);
        codes = mock(PortalCodeRepository.class);
        notifier = mock(PortalNotifier.class);
        activity = mock(PortalActivityService.class);
        rateLimiter = mock(RateLimiter.class);

        service = new PortalInvitationService(participants, devices, codes, notifier,
            activity, rateLimiter);

        journey = PortalJourney.builder()
            .status(JourneyStatus.OPEN)
            .build();
        journey.setId(7L);

        lead = person(1L, "sarah@example.com", ParticipantRole.LEAD);
        onTheBooking = new ArrayList<>();
        onTheBooking.add(lead);

        when(participants.findByJourneyIdOrderByIdAsc(7L)).thenReturn(onTheBooking);
        when(participants.findByJourneyIdAndEmail(anyLong(), anyString()))
            .thenReturn(Optional.empty());
        when(participants.save(any(PortalParticipant.class)))
            .thenAnswer(call -> call.getArgument(0));
        /* nothing is throttled unless a test says so */
        when(rateLimiter.check(anyString(), anyInt(), any())).thenReturn(0L);
    }

    private PortalParticipant person(Long id, String email, ParticipantRole role) {
        PortalParticipant participant = PortalParticipant.builder()
            .journey(journey)
            .email(email)
            .participantRole(role)
            .build();
        participant.setId(id);
        participant.setIsActive(true);
        return participant;
    }

    private PortalParticipant savedParticipant() {
        ArgumentCaptor<PortalParticipant> saved = ArgumentCaptor.forClass(PortalParticipant.class);
        verify(participants).save(saved.capture());
        return saved.getValue();
    }

    // ------------------------------------------------------------------ a reader is a reader

    @Test
    @DisplayName("an invited person is a READER, and there is no way to make a second lead")
    void aninviteeIsAlwaysAReader() {
        PortalOutcome outcome = service.invite(journey, lead, "brother@example.com", "Tom", "1.2.3.4");

        assertTrue(outcome.done(), outcome.message());
        PortalParticipant created = savedParticipant();
        assertEquals(ParticipantRole.READER, created.getParticipantRole());
        assertFalse(created.mayAct(), "a reader who may act is a second person who can accept");
        assertSame(lead, created.getInvitedBy(), "who added them is the record of where the "
            + "address came from");
        assertNotNull(created.getInvitedAt());
    }

    @Test
    @DisplayName("the role itself refuses to act, which is where that rule actually lives")
    void theRoleIsTheEnforcement() {
        assertFalse(ParticipantRole.READER.mayAct());
        assertTrue(ParticipantRole.LEAD.mayAct());
    }

    @Test
    @DisplayName("the service can create no other role, however it is called")
    void nothingButAReaderIsEverCreated() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/itineraryledger/"
            + "kabengosafaris/ClientPortal/Services/PortalInvitationService.java"));
        int body = source.indexOf("public class PortalInvitationService");

        /*
         * Assignment, not mention. LEAD appears here several times on purpose, every one of them
         * a comparison that REFUSES to touch the lead's own row. What must never appear is the
         * constant being written to a participant: that is a booking two people can accept.
         */
        String rest = source.substring(body);
        assertFalse(rest.contains("setParticipantRole(ParticipantRole.LEAD"),
            "a second lead is a booking two people can accept");
        assertFalse(rest.contains(".participantRole(ParticipantRole.LEAD"),
            "a second lead is a booking two people can accept");
        assertTrue(rest.contains(".participantRole(ParticipantRole.READER)"),
            "the only role this service creates");
    }

    // ------------------------------------------------------------------ only the lead invites

    @Test
    @DisplayName("a reader cannot invite, or the list has no owner and no end")
    void areaderCannotInvite() {
        PortalParticipant reader = person(2L, "tom@example.com", ParticipantRole.READER);

        PortalOutcome outcome = service.invite(journey, reader, "friend@example.com", null, null);

        assertFalse(outcome.done());
        assertEquals("PORTAL_NOT_THE_LEAD", outcome.code());
        verify(notifier, never()).readerInvited(any(), any(), any());
        verify(participants, never()).save(any());
    }

    @Test
    @DisplayName("nobody at all cannot invite either")
    void anUnverifiedCallerCannotInvite() {
        assertFalse(service.invite(journey, null, "friend@example.com", null, null).done());
        verify(notifier, never()).readerInvited(any(), any(), any());
    }

    @Test
    @DisplayName("a closed booking invites nobody")
    void aclosedBookingInvitesNobody() {
        journey.setStatus(JourneyStatus.REVOKED);

        PortalOutcome outcome = service.invite(journey, lead, "friend@example.com", null, null);

        assertFalse(outcome.done());
        assertEquals("PORTAL_CLOSED", outcome.code());
        verify(notifier, never()).readerInvited(any(), any(), any());
    }

    // ------------------------------------------------------------------ the address

    @Test
    @DisplayName("something that is not an address is refused before anything is sent")
    void rubbishIsRefusedBeforeTheMail() {
        for (String bad : List.of("", "   ", "nobody", "no body@example.com", "a@b c")) {
            PortalOutcome outcome = service.invite(journey, lead, bad, null, null);
            assertFalse(outcome.done(), "accepted: [" + bad + "]");
            assertEquals("PORTAL_BAD_ADDRESS", outcome.code());
        }
        assertFalse(service.invite(journey, lead, null, null, null).done());
        verify(notifier, never()).readerInvited(any(), any(), any());
    }

    @Test
    @DisplayName("the address is stored the way the rest of the portal looks it up")
    void theAddressIsNormalised() {
        service.invite(journey, lead, "  Tom.Brother@Example.COM ", null, null);

        assertEquals("tom.brother@example.com", savedParticipant().getEmail(),
            "a code is requested by an address the client types; a stored one that differs by "
                + "case is a reader who can never verify");
    }

    @Test
    @DisplayName("somebody already on the booking is told so, not silently invited twice")
    void aduplicateIsRefusedPlainly() {
        PortalParticipant reader = person(2L, "tom@example.com", ParticipantRole.READER);
        onTheBooking.add(reader);
        when(participants.findByJourneyIdAndEmail(7L, "tom@example.com"))
            .thenReturn(Optional.of(reader));

        PortalOutcome outcome = service.invite(journey, lead, "TOM@example.com", null, null);

        assertFalse(outcome.done());
        assertEquals("PORTAL_ALREADY_INVITED", outcome.code());
        verify(notifier, never()).readerInvited(any(), any(), any());
    }

    @Test
    @DisplayName("the lead inviting themselves is told whose address it is")
    void theLeadCannotInviteThemselves() {
        when(participants.findByJourneyIdAndEmail(7L, "sarah@example.com"))
            .thenReturn(Optional.of(lead));

        PortalOutcome outcome = service.invite(journey, lead, "sarah@example.com", null, null);

        assertFalse(outcome.done());
        assertEquals("PORTAL_ALREADY_THE_LEAD", outcome.code());
    }

    @Test
    @DisplayName("re-inviting somebody who was taken off brings their row back, never a second one")
    void awithdrawnReaderComesBackAsTheSameRow() {
        PortalParticipant removed = person(2L, "tom@example.com", ParticipantRole.READER);
        removed.setIsActive(false);
        when(participants.findByJourneyIdAndEmail(7L, "tom@example.com"))
            .thenReturn(Optional.of(removed));

        PortalOutcome outcome = service.invite(journey, lead, "tom@example.com", null, null);

        assertTrue(outcome.done(), outcome.message());
        PortalParticipant saved = savedParticipant();
        assertSame(removed, saved, "the address is unique per booking, so a second row throws");
        assertTrue(Boolean.TRUE.equals(saved.getIsActive()));
    }

    // ------------------------------------------------------------------ the limits

    @Test
    @DisplayName("a booking takes a family, not a mailing list")
    void thecapHolds() {
        for (long i = 0; i < PortalInvitationService.MAX_READERS; i++) {
            onTheBooking.add(person(100 + i, "reader" + i + "@example.com", ParticipantRole.READER));
        }

        PortalOutcome outcome = service.invite(journey, lead, "one.more@example.com", null, null);

        assertFalse(outcome.done());
        assertEquals("PORTAL_TOO_MANY_READERS", outcome.code());
        verify(notifier, never()).readerInvited(any(), any(), any());
        assertEquals(0, service.placesLeft(journey));
    }

    @Test
    @DisplayName("somebody taken off the booking does not go on holding a place")
    void awithdrawnReaderFreesTheirPlace() {
        for (long i = 0; i < PortalInvitationService.MAX_READERS; i++) {
            PortalParticipant reader =
                person(100 + i, "reader" + i + "@example.com", ParticipantRole.READER);
            if (i == 0) reader.setIsActive(false);
            onTheBooking.add(reader);
        }

        assertEquals(1, service.placesLeft(journey));
        assertTrue(service.invite(journey, lead, "one.more@example.com", null, null).done());
    }

    @Test
    @DisplayName("invitations come out at a few an hour, counted per booking")
    void therateIsLimited() {
        when(rateLimiter.check(eq("portal-invite:7"), anyInt(), any())).thenReturn(300L);

        PortalOutcome outcome = service.invite(journey, lead, "friend@example.com", null, null);

        assertFalse(outcome.done());
        assertEquals("PORTAL_INVITE_RATE_LIMITED", outcome.code());
        verify(notifier, never()).readerInvited(any(), any(), any());
        verify(participants, never()).save(any());
    }

    // ------------------------------------------------------------------ taking somebody off

    @Test
    @DisplayName("taking a reader off ends their session, or it removed nothing")
    void removalEndsTheSession() {
        PortalParticipant reader = person(2L, "tom@example.com", ParticipantRole.READER);
        onTheBooking.add(reader);
        when(participants.findById(2L)).thenReturn(Optional.of(reader));

        PortalOutcome outcome = service.withdraw(journey, lead, 2L, "1.2.3.4");

        assertTrue(outcome.done(), outcome.message());
        assertFalse(Boolean.TRUE.equals(reader.getIsActive()));
        verify(devices).revokeEveryDeviceOfParticipant(eq(2L), any(LocalDateTime.class));
        verify(codes).spendEveryOutstandingCode(eq(2L), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("a reader cannot take anybody off, including themselves")
    void areaderCannotRemove() {
        PortalParticipant reader = person(2L, "tom@example.com", ParticipantRole.READER);

        PortalOutcome outcome = service.withdraw(journey, reader, 2L, null);

        assertFalse(outcome.done());
        assertEquals("PORTAL_NOT_THE_LEAD", outcome.code());
        verify(devices, never()).revokeEveryDeviceOfParticipant(anyLong(), any());
    }

    @Test
    @DisplayName("an id from another booking is refused, the same as every other id a client sends")
    void anidFromAnotherBookingIsRefused() {
        PortalJourney somebodyElses = PortalJourney.builder().status(JourneyStatus.OPEN).build();
        somebodyElses.setId(99L);

        PortalParticipant theirReader = PortalParticipant.builder()
            .journey(somebodyElses)
            .email("stranger@example.com")
            .participantRole(ParticipantRole.READER)
            .build();
        theirReader.setId(42L);
        when(participants.findById(42L)).thenReturn(Optional.of(theirReader));

        PortalOutcome outcome = service.withdraw(journey, lead, 42L, null);

        assertFalse(outcome.done());
        assertEquals("PORTAL_NOT_ON_THIS_BOOKING", outcome.code());
        verify(devices, never()).revokeEveryDeviceOfParticipant(anyLong(), any());
    }

    @Test
    @DisplayName("an id naming nobody gets the same answer as one naming somebody else")
    void anunknownIdIsNotDistinguished() {
        when(participants.findById(anyLong())).thenReturn(Optional.empty());

        PortalOutcome outcome = service.withdraw(journey, lead, 12345L, null);

        assertFalse(outcome.done());
        assertEquals("PORTAL_NOT_ON_THIS_BOOKING", outcome.code());
    }

    @Test
    @DisplayName("the lead cannot be taken off their own booking, by them or by the office")
    void theleadCannotBeRemoved() {
        when(participants.findById(1L)).thenReturn(Optional.of(lead));

        PortalOutcome byTheLead = service.withdraw(journey, lead, 1L, null);
        assertFalse(byTheLead.done());
        assertEquals("PORTAL_CANNOT_REMOVE_THE_LEAD", byTheLead.code());

        PortalOutcome byTheOffice = service.withdrawAsStaff(journey, lead);
        assertFalse(byTheOffice.done());
        assertEquals("PORTAL_CANNOT_REMOVE_THE_LEAD", byTheOffice.code());

        verify(devices, never()).revokeEveryDeviceOfParticipant(anyLong(), any());
    }

    @Test
    @DisplayName("the office removes a reader down the same path the client's button takes")
    void theofficeUsesTheSameRemoval() {
        PortalParticipant reader = person(2L, "tom@example.com", ParticipantRole.READER);
        onTheBooking.add(reader);

        PortalOutcome outcome = service.withdrawAsStaff(journey, reader);

        assertTrue(outcome.done(), outcome.message());
        assertFalse(Boolean.TRUE.equals(reader.getIsActive()));
        verify(devices).revokeEveryDeviceOfParticipant(eq(2L), any(LocalDateTime.class));
    }

    // ------------------------------------------------------------------ what the message carries

    @Test
    @DisplayName("the invitation carries a link and no figure")
    void theinvitationCarriesNothingPrivate() throws IOException {
        String template = Files.readString(Path.of("src/main/resources/templates/"
            + "email-templates/portal_invitation_default.html"));

        for (String forbidden : List.of("{{quoteTotal", "{{accessCode", "{{balance",
                "{{amount", "{{price", "{{total")) {
            assertFalse(template.contains(forbidden),
                "an invitation goes to somebody who has not proved their address yet: " + forbidden);
        }
        assertTrue(template.contains("{{portalUrl}}"), "without the link it is not an invitation");
        assertTrue(template.contains("{{inviterName}}"),
            "an unexplained email about a safari from an unfamiliar company reads as something "
                + "to delete, so it has to say who added them");
    }

    @Test
    @DisplayName("every variable the invitation prints is one the event declares")
    void thetemplateAndTheSchemaAgree() throws IOException {
        String template = Files.readString(Path.of("src/main/resources/templates/"
            + "email-templates/portal_invitation_default.html"));
        String schema = Files.readString(Path.of("src/main/resources/schemas/"
            + "email-events/portal-invitation-schema.json"));

        /* company.* is resolved by the third pass for every event and is not declared per event */
        for (String name : List.of("recipientName", "inviterName", "tripName", "portalUrl")) {
            assertTrue(template.contains(name), "the template stopped using " + name);
            assertTrue(schema.contains("\"" + name + "\""),
                name + " prints as itself when nothing supplies it: this renderer has no "
                    + "inverted sections and no default for an undeclared variable");
        }
    }
}
