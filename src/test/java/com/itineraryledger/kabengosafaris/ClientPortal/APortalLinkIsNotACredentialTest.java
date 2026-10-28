package com.itineraryledger.kabengosafaris.ClientPortal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalDevice;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalJourney;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalParticipant;
import com.itineraryledger.kabengosafaris.ClientPortal.Enums.JourneyStatus;
import com.itineraryledger.kabengosafaris.ClientPortal.Enums.ParticipantRole;
import com.itineraryledger.kabengosafaris.ClientPortal.Enums.ProposalState;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalAccessService;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalTokens;

/**
 * The link in a client's email opens the trip and nothing that costs money.
 *
 * <p>Assume the URL is public. It is forwarded to a sister, pasted into a group chat, fetched by
 * a preview bot, logged by a mail gateway, and left in the history of a hotel computer. None of
 * that is misuse; it is what links do. The design therefore has to survive it, and what makes it
 * survivable is that the token is an address and the code is the key.
 *
 * <p>Each test here is one sentence of that, written so it fails if somebody later makes the
 * reasonable-looking change that undoes it.
 */
class APortalLinkIsNotACredentialTest {

    private static final Path TOKENS = Path.of(
        "src/main/java/com/itineraryledger/kabengosafaris/ClientPortal/Services/PortalTokens.java");
    private static final Path JOURNEY_SERVICE = Path.of(
        "src/main/java/com/itineraryledger/kabengosafaris/ClientPortal/Services/PortalJourneyService.java");
    private static final Path ACCESS_SERVICE = Path.of(
        "src/main/java/com/itineraryledger/kabengosafaris/ClientPortal/Services/PortalAccessService.java");
    private static final Path VIEW_SERVICE = Path.of(
        "src/main/java/com/itineraryledger/kabengosafaris/ClientPortal/Services/PortalViewService.java");

    private final PortalTokens tokens = new PortalTokens();

    @Test
    @DisplayName("a journey token is random and unguessable, never an obfuscated id")
    void theTokenIsItsOwnSecret() throws Exception {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            String secret = tokens.newSecret();
            assertEquals(43, secret.length(),
                "32 random bytes as url-safe Base64 is 43 characters; " + secret.length()
                    + " means the size changed and nobody said why");
            assertTrue(secret.matches("[A-Za-z0-9_-]+"),
                "a token goes in a URL, so it cannot carry padding or a slash: " + secret);
            assertTrue(seen.add(secret), "two identical tokens in 500 draws is not random");
        }

        /*
         * The failure this prevents: somebody reaches for the id obfuscator because every other
         * id in the application uses it. Hashids encode the primary key. They are sequential,
         * they are not secret, and this application mints a new salt on every restart, so such a
         * token would be both guessable and dead after the next deploy.
         */
        String journeyService = Files.readString(JOURNEY_SERVICE);
        assertTrue(journeyService.contains("tokens.newSecret()"),
            "the journey token must come from the random generator");
        assertFalse(journeyService.contains("idObfuscator"),
            "the journey token must not be derived from the record id");
    }

    @Test
    @DisplayName("bearer secrets are stored hashed, and compared in constant time")
    void secretsAreHashedAndComparedSafely() throws Exception {
        String source = Files.readString(TOKENS);

        assertTrue(source.contains("SHA-256"), "the stored form of a secret must be a hash");
        assertTrue(source.contains("MessageDigest.isEqual"),
            "comparing a code with equals leaks how much of it was right, one character at a "
            + "time. The constant-time comparison is MessageDigest.isEqual");

        assertNotEquals(tokens.hash("123456"), "123456", "the hash is not the value");
        assertTrue(tokens.matches("123456", tokens.hash("123456")));
        assertFalse(tokens.matches("123457", tokens.hash("123456")));
        assertFalse(tokens.matches(null, tokens.hash("123456")));
        /*
         * A null stored hash must never match. It is the shape of a row that was written wrong,
         * and "nothing equals nothing" would turn that into a way in.
         */
        assertFalse(tokens.matches("123456", null));

        String access = Files.readString(ACCESS_SERVICE);
        assertTrue(access.contains("tokens.hash(deviceToken)"),
            "the device cookie is looked up by hash, so a stolen table is not a set of sessions");
        assertTrue(access.contains(".codeHash(tokens.hash(code))"),
            "the verification code is stored hashed for the same reason");
    }

    @Test
    @DisplayName("the link alone opens the trip and nothing else")
    void theOpenTierCarriesNoMoney() {
        PortalJourney journey = journey();

        /* nothing presented but the link */
        PortalAccessService.Access anonymous = new PortalAccessService.Access(journey, null, null);
        assertTrue(anonymous.canReadTrip(), "the trip is readable; that is the point of the tier");
        assertFalse(anonymous.canReadPrivate(), "the quotation is not");
        assertFalse(anonymous.canAct(), "and nothing can be decided");

        /*
         * The view service must not assemble what it will not show. Building the quotation and
         * then leaving it out of a template is a promise that lasts until somebody edits the
         * template.
         */
        String source = sourceWithoutComments(VIEW_SERVICE);
        /*
         * A pattern rather than the exact call, for the quotation only. It gained an argument
         * when clients were given something to edit, and the literal this used to match would
         * have had to be retyped for a change that did not touch the gate. What is being
         * protected is the gate: built for a verified caller, null for everybody else.
         */
        assertTrue(java.util.regex.Pattern.compile("verified \\? quote\\(.*?\\) : null")
                .matcher(source).find(),
            "the quotation must only be built for a verified caller");
        assertTrue(source.contains("verified ? invoice(journey.getInvoice()) : null"),
            "and the invoice");
        assertTrue(source.contains("verified ? documents(journey) : List.of()"),
            "and the documents");
    }

    @Test
    @DisplayName("a session is bound to one journey, and proves nothing about another")
    void aSessionCannotCrossToAnotherJourney() {
        PortalJourney theirs = journey();
        theirs.setId(1L);
        PortalJourney somebodyElses = journey();
        somebodyElses.setId(2L);

        PortalParticipant participant = PortalParticipant.builder()
            .id(10L).journey(theirs).email("her@example.test")
            .participantRole(ParticipantRole.LEAD).isActive(true)
            .verifiedAt(LocalDateTime.now())
            .build();

        PortalDevice device = PortalDevice.builder()
            .id(100L).participant(participant).tokenHash("whatever")
            .expiresAt(LocalDateTime.now().plusDays(30))
            .build();

        /* the right journey: everything works */
        PortalAccessService.Access own = new PortalAccessService.Access(theirs, participant, device);
        assertTrue(own.canReadPrivate());

        /*
         * The resolve path refuses to pair a device with a journey it does not belong to, so this
         * combination cannot be produced by the service. The assertion is that the Access object
         * itself is not the thing deciding, because a caller that builds one by hand must not be
         * able to grant itself access either.
         */
        String source = sourceWithoutComments(ACCESS_SERVICE);
        assertTrue(source.contains("!participant.getJourney().getId().equals(journey.getId())"),
            "a device proves who somebody is, not which booking they asked for. Without this "
            + "comparison one valid session could read any journey by pasting its token");
    }

    @Test
    @DisplayName("an expired or revoked link opens nothing, whatever cookie is presented")
    void anEndedJourneyIsOver() {
        PortalParticipant participant = PortalParticipant.builder()
            .id(10L).email("her@example.test").participantRole(ParticipantRole.LEAD)
            .isActive(true).verifiedAt(LocalDateTime.now()).build();
        PortalDevice device = PortalDevice.builder()
            .id(100L).participant(participant).tokenHash("x")
            .expiresAt(LocalDateTime.now().plusDays(30)).build();

        PortalJourney revoked = journey();
        revoked.setStatus(JourneyStatus.REVOKED);
        participant.setJourney(revoked);
        assertFalse(new PortalAccessService.Access(revoked, participant, device).canReadTrip(),
            "a withdrawn link is the control for when something has gone wrong; a trusted device "
            + "must not survive it");

        PortalJourney expired = journey();
        expired.setExpiresAt(LocalDateTime.now().minusDays(1));
        assertFalse(new PortalAccessService.Access(expired, participant, device).canReadTrip());

        PortalJourney locked = journey();
        locked.setLocked(true);
        PortalAccessService.Access frozen =
            new PortalAccessService.Access(locked, participant, device);
        assertTrue(frozen.canReadTrip(), "a frozen journey can still be read");
        assertFalse(frozen.canAct(), "and nothing can be changed or decided on it");
    }

    @Test
    @DisplayName("an invited reader may read and may not decide")
    void readersDoNotDecide() {
        PortalJourney journey = journey();
        PortalParticipant reader = PortalParticipant.builder()
            .id(11L).journey(journey).email("brother@example.test")
            .participantRole(ParticipantRole.READER).isActive(true)
            .verifiedAt(LocalDateTime.now()).build();
        PortalDevice device = PortalDevice.builder()
            .id(101L).participant(reader).tokenHash("y")
            .expiresAt(LocalDateTime.now().plusDays(30)).build();

        PortalAccessService.Access access = new PortalAccessService.Access(journey, reader, device);
        assertTrue(access.canReadPrivate(),
            "an invited traveller sees the trip and what is owed; that is why they were invited");
        assertFalse(access.canAct(),
            "the contract is with one person. A booking accepted by somebody who was not "
            + "authorised to accept it cannot be unpicked afterwards");
    }

    // ------------------------------------------------------------------ helpers

    private PortalJourney journey() {
        PortalJourney journey = PortalJourney.builder()
            .id(1L)
            .token("a-token")
            .status(JourneyStatus.OPEN)
            .proposalState(ProposalState.OPEN_TO_CLIENT)
            .locked(false)
            .build();
        return journey;
    }

    /** Comments explain the rule; they must not be able to satisfy it. */
    private String sourceWithoutComments(Path file) {
        try {
            return Files.readString(file)
                .replaceAll("(?s)/\\*.*?\\*/", "")
                .replaceAll("(?m)^\\s*//.*$", "");
        } catch (Exception e) {
            throw new IllegalStateException("could not read " + file, e);
        }
    }
}
