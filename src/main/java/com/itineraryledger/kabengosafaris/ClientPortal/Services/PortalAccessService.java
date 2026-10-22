package com.itineraryledger.kabengosafaris.ClientPortal.Services;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalCode;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalDevice;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalJourney;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalParticipant;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalCodeRepository;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalDeviceRepository;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalJourneyRepository;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalParticipantRepository;
import com.itineraryledger.kabengosafaris.EmailEvent.Services.CustomerAcknowledgementSender;
import com.itineraryledger.kabengosafaris.Legal.Services.LegalDocumentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Who is at the browser, and may they see this booking.
 *
 * <p>The whole access model is three sentences. The link in the client's hand is an
 * <strong>address</strong>: it says which booking a page is about and opens only the parts that
 * are not sensitive. The <strong>key</strong> is a short code sent to an address already on the
 * customer record and typed into the page they already have open. Once typed, that browser is
 * <strong>trusted for a while</strong>, visibly, and either side can end it.
 *
 * <p>Several deliberate refusals, each of which is a thing that goes wrong in portals:
 *
 * <ul>
 *   <li><strong>No clickable login link.</strong> Email is full of machines that click things,
 *       and any of them can spend a one-time link before the client reaches it.</li>
 *   <li><strong>No answer that distinguishes a known address from an unknown one.</strong>
 *       Otherwise the page is a tool for discovering who books with us.</li>
 *   <li><strong>No code to an address the office did not already hold.</strong> A client can only
 *       verify to an address already recorded as a participant on that journey; typing a new one
 *       sends nothing, however plausible it looks.</li>
 *   <li><strong>No session that outlives its journey.</strong> A device is bound to one
 *       participant on one journey and can see nothing else, not even that client's other
 *       bookings.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortalAccessService {

    private final PortalJourneyRepository journeyRepository;
    private final PortalParticipantRepository participantRepository;
    private final PortalCodeRepository codeRepository;
    private final PortalDeviceRepository deviceRepository;
    private final PortalTokens tokens;
    private final PortalActivityService activity;
    private final CustomerAcknowledgementSender sender;
    private final LegalDocumentService legalDocuments;

    @Value("${portal.code.expiry.minutes:15}")
    private int codeExpiryMinutes;

    @Value("${portal.code.max.attempts:5}")
    private int maxAttempts;

    @Value("${portal.device.lifetime.days:30}")
    private int deviceLifetimeDays;

    /** Where the client's own page lives, for the links inside the emails we send them. */
    @Value("${portal.base.url:}")
    private String portalBaseUrl;

    /**
     * What a caller is allowed to do, having presented whatever it presented.
     *
     * <p>One object rather than a handful of booleans, because every page asks the same question
     * and a page that asks a different one is a hole.
     */
    public record Access(PortalJourney journey, PortalParticipant participant, PortalDevice device) {

        /** The link resolved and is not revoked or expired. The open tier. */
        public boolean canReadTrip() {
            return journey != null && journey.isOpenable();
        }

        /** Somebody has proved they can read an inbox on this booking. The verified tier. */
        public boolean canReadPrivate() {
            return canReadTrip() && participant != null && participant.isVerified()
                && device != null && device.isUsable();
        }

        /** They may change or decide something: verified, the lead, and nothing frozen. */
        public boolean canAct() {
            return canReadPrivate() && journey.isActionable() && participant.mayAct();
        }
    }

    /** Resolve a link and a cookie into what the caller may see. Neither has to be present. */
    @Transactional(readOnly = true)
    public Access resolve(String journeyToken, String deviceToken) {
        PortalJourney journey = journeyToken == null ? null
            : journeyRepository.findByToken(journeyToken).orElse(null);
        if (journey == null) return new Access(null, null, null);

        if (deviceToken == null || deviceToken.isBlank()) {
            return new Access(journey, null, null);
        }

        PortalDevice device = deviceRepository.findByTokenHash(tokens.hash(deviceToken)).orElse(null);
        if (device == null || !device.isUsable()) {
            return new Access(journey, null, null);
        }

        /*
         * The check that actually matters. A device proves who somebody is; it does not prove
         * which booking they asked for. Without this line, a client holding one valid session
         * could paste any journey token and read it.
         */
        PortalParticipant participant = device.getParticipant();
        if (participant == null
                || participant.getJourney() == null
                || !participant.getJourney().getId().equals(journey.getId())) {
            log.warn("A portal device for journey {} was presented against journey {}",
                participant == null || participant.getJourney() == null
                    ? "unknown" : participant.getJourney().getId(),
                journey.getId());
            return new Access(journey, null, null);
        }

        return new Access(journey, participant, device);
    }

    /**
     * Someone has typed an address into the verification page.
     *
     * <p>Returns nothing either way. The caller says the same sentence whether or not the address
     * belongs to this booking, because an answer that differs is a tool for finding out who our
     * customers are.
     */
    @Transactional
    public void requestCode(PortalJourney journey, String email, String ipAddress) {
        if (journey == null || !journey.isOpenable() || email == null || email.isBlank()) return;

        String normalised = email.trim().toLowerCase(Locale.ROOT);
        Optional<PortalParticipant> found =
            participantRepository.findByJourneyIdAndEmail(journey.getId(), normalised);

        if (found.isEmpty() || !Boolean.TRUE.equals(found.get().getIsActive())) {
            /*
             * Logged, because a run of these against one journey is worth an operator seeing, and
             * nothing is sent. A code to an address the office never recorded would turn the
             * portal into a way of mailing strangers from our domain.
             */
            log.info("Portal code requested for an address not on journey {}", journey.getId());
            activity.record(journey, null, null, "VERIFY_UNKNOWN_ADDRESS", null, ipAddress, null);
            return;
        }

        PortalParticipant participant = found.get();

        /* A new code kills the old one, so two live codes never exist for one person. */
        codeRepository.spendEveryOutstandingCode(participant.getId(), LocalDateTime.now());

        String code = tokens.newCode();
        codeRepository.save(PortalCode.builder()
            .participant(participant)
            .codeHash(tokens.hash(code))
            .expiresAt(LocalDateTime.now().plusMinutes(codeExpiryMinutes))
            .attempts(0)
            .ipAddress(ipAddress)
            .build());

        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("recipientName", displayNameOf(participant));
        variables.put("accessCode", code);
        variables.put("expiryMinutes", String.valueOf(codeExpiryMinutes));
        variables.put("tripName", tripNameOf(journey));
        variables.put("portalUrl", linkFor(journey));

        sender.send("PORTAL_ACCESS_CODE", participant.getEmail(),
            "Your access code", variables, languageOf(journey));

        activity.record(journey, participant, null, "VERIFY_CODE_SENT", null, ipAddress, null);
    }

    /** What came back from a verification attempt, and why. */
    public record Verification(boolean accepted, String deviceToken, boolean newDevice, String reason) {
        static Verification no(String reason) { return new Verification(false, null, false, reason); }
    }

    /**
     * Check a code and, if it is right, trust this browser for a while.
     *
     * <p>Every refusal returns the same sentence to the caller. The distinctions below are for the
     * log, not for whoever is typing.
     */
    @Transactional
    public Verification verify(PortalJourney journey, String email, String code,
                               String ipAddress, String userAgent, boolean rememberDevice) {
        if (journey == null || !journey.isOpenable()) return Verification.no("journey not open");
        if (email == null || code == null) return Verification.no("missing");

        String normalised = email.trim().toLowerCase(Locale.ROOT);
        PortalParticipant participant =
            participantRepository.findByJourneyIdAndEmail(journey.getId(), normalised).orElse(null);
        if (participant == null || !Boolean.TRUE.equals(participant.getIsActive())) {
            return Verification.no("no such participant");
        }

        PortalCode stored = codeRepository
            .findFirstByParticipantIdAndUsedAtIsNullOrderByCreatedAtDesc(participant.getId())
            .orElse(null);
        if (stored == null) return Verification.no("no outstanding code");
        if (!stored.isUsable(maxAttempts)) return Verification.no("code expired or exhausted");

        if (!tokens.matches(code.trim(), stored.getCodeHash())) {
            /*
             * Count the attempt before returning. A wrong code that costs nothing is a code that
             * can be guessed a million times.
             */
            stored.setAttempts(stored.getAttempts() == null ? 1 : stored.getAttempts() + 1);
            codeRepository.save(stored);
            activity.record(journey, participant, null, "VERIFY_FAILED", null, ipAddress, userAgent);
            return Verification.no("wrong code");
        }

        stored.setUsedAt(LocalDateTime.now());
        codeRepository.save(stored);

        boolean first = !participant.isVerified();
        if (first) {
            participant.setVerifiedAt(LocalDateTime.now());
            /*
             * Stamp which privacy notice they were shown. "The policy as it stands today" is not
             * an answer to what somebody agreed to, and the notice is editable in the panel.
             */
            participant.setPolicyVersion(legalDocuments.publishedPrivacyVersion());
            participant.setPolicyAcceptedAt(LocalDateTime.now());
            participantRepository.save(participant);
        }

        /*
         * A shared or public computer gets a session that dies with the browser: no row, no
         * cookie to leave behind. The caller sets a session cookie instead of a persistent one.
         */
        String deviceToken = tokens.newSecret();
        PortalDevice device = deviceRepository.save(PortalDevice.builder()
            .participant(participant)
            .tokenHash(tokens.hash(deviceToken))
            .label(tokens.deviceLabel(userAgent))
            .ipAddress(ipAddress)
            .userAgent(trim(userAgent, 500))
            .expiresAt(LocalDateTime.now().plusDays(rememberDevice ? deviceLifetimeDays : 1))
            .build());

        activity.record(journey, participant, device, "VERIFY_OK", null, ipAddress, userAgent);
        tellThemAboutTheNewDevice(journey, participant, device);

        return new Verification(true, deviceToken, true, null);
    }

    /**
     * One short email per new device, saying when and roughly where.
     *
     * <p>This is the most valuable control in the whole design and the cheapest. A forwarded link
     * being used by somebody else produces no error, no warning and no clue anywhere: this email
     * is how anybody finds out.
     */
    private void tellThemAboutTheNewDevice(PortalJourney journey, PortalParticipant participant,
                                           PortalDevice device) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("recipientName", displayNameOf(participant));
        variables.put("deviceLabel", device.getLabel() == null ? "a new device" : device.getLabel());
        variables.put("ipAddress", device.getIpAddress() == null ? "an unknown address" : device.getIpAddress());
        variables.put("openedAt", device.getCreatedAt().toString());
        variables.put("tripName", tripNameOf(journey));
        variables.put("portalUrl", linkFor(journey));

        sender.send("PORTAL_NEW_DEVICE", participant.getEmail(),
            "Your trip was opened on a new device", variables, languageOf(journey));
    }

    /** Touch the session, cheaply, so the devices page can say when it was last used. */
    @Transactional
    public void touch(PortalDevice device, String ipAddress) {
        if (device == null) return;
        device.setLastSeenAt(LocalDateTime.now());
        if (ipAddress != null) device.setIpAddress(ipAddress);
        deviceRepository.save(device);
    }

    /** The client ending one of their own sessions from the devices page. */
    @Transactional
    public boolean revokeDevice(PortalJourney journey, Long deviceId) {
        PortalDevice device = deviceRepository.findById(deviceId).orElse(null);
        if (device == null || device.getParticipant() == null
                || device.getParticipant().getJourney() == null
                || !device.getParticipant().getJourney().getId().equals(journey.getId())) {
            /* not theirs to revoke, and we do not say which of the two reasons it was */
            return false;
        }
        device.setRevokedAt(LocalDateTime.now());
        deviceRepository.save(device);
        activity.record(journey, device.getParticipant(), null, "DEVICE_REVOKED",
            device.getLabel(), null, null);
        return true;
    }

    private String displayNameOf(PortalParticipant participant) {
        if (participant.getDisplayName() != null && !participant.getDisplayName().isBlank()) {
            return participant.getDisplayName();
        }
        PortalJourney journey = participant.getJourney();
        if (journey != null && journey.getCustomer() != null) {
            String first = journey.getCustomer().getFirstName();
            if (first != null && !first.isBlank()) return first;
        }
        return "there";
    }

    private String tripNameOf(PortalJourney journey) {
        if (journey.getItinerary() != null && journey.getItinerary().getName() != null) {
            return journey.getItinerary().getName();
        }
        return "your trip";
    }

    private String languageOf(PortalJourney journey) {
        return journey.getCustomer() == null ? null : journey.getCustomer().getPreferredLanguage();
    }

    /** The address we print in an email. Blank when nobody has configured where the portal lives. */
    public String linkFor(PortalJourney journey) {
        if (portalBaseUrl == null || portalBaseUrl.isBlank()) return "";
        String base = portalBaseUrl.endsWith("/")
            ? portalBaseUrl.substring(0, portalBaseUrl.length() - 1)
            : portalBaseUrl;
        return base + "/portal/" + journey.getToken();
    }

    private String trim(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
