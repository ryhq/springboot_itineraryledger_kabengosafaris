package com.itineraryledger.kabengosafaris.ClientPortal.Services;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.ClientPortal.DTOs.PortalOutcome;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalJourney;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalParticipant;
import com.itineraryledger.kabengosafaris.ClientPortal.Enums.ParticipantRole;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalCodeRepository;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalDeviceRepository;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalParticipantRepository;
import com.itineraryledger.kabengosafaris.Security.RateLimit.RateLimiter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The lead traveller putting somebody else on their own booking.
 *
 * <p>A group trip has one organiser and several people who all want to see where they are
 * sleeping and what they owe. Today they see it because the organiser forwards the link, which
 * works and is the problem: a forwarded link is one person's session being shared, so the new
 * device warning fires on somebody who is supposed to be there, every reader looks like the lead
 * in the access log, and withdrawing one of them means revoking the link for all of them.
 *
 * <p>An invitation replaces that with a row. The reader gets their own participant record, their
 * own code to their own address, their own trusted browser and their own line in the log, and the
 * organiser can take one of them off without disturbing anybody else.
 *
 * <p><strong>A reader decides nothing.</strong> That is not enforced here — it is enforced by
 * {@link ParticipantRole#mayAct()}, which every acting path already asks, and this service simply
 * never creates anything but a READER. There is deliberately no way to invite a second lead: the
 * contract is with one person, and a booking accepted by somebody who was not authorised to
 * accept it is a problem no software unpicks afterwards.
 *
 * <p><strong>An invitation is an email to an address nobody here typed.</strong> That is the
 * whole risk of the feature and the reason for the three limits below. A lead, and only a lead,
 * may send one; a journey holds at most {@link #MAX_READERS} of them; and they come out at a few
 * an hour. Without those, a booking link is a way of sending mail from the company's own domain
 * to any address somebody cares to name, which is a far more attractive thing to find than a
 * quotation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortalInvitationService {

    /**
     * How many readers one booking may carry.
     *
     * <p>A number rather than no number. Family-sized, because that is what the feature is for,
     * and a party larger than this is a conversation with the office rather than a form.
     */
    public static final int MAX_READERS = 8;

    /** Deliberately the same shape the customer record accepts, so one cannot be invited and then rejected. */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final PortalParticipantRepository participants;
    private final PortalDeviceRepository devices;
    private final PortalCodeRepository codes;
    private final PortalNotifier notifier;
    private final PortalActivityService activity;
    private final RateLimiter rateLimiter;

    /*
     * Initialised as well as injected. A zero here would not fail: it would quietly refuse every
     * invitation on the booking, which is the kind of configuration mistake that looks like the
     * feature not working.
     */
    @Value("${portal.invitations.per.hour:6}")
    private int invitationsPerHour = 6;

    // ------------------------------------------------------------------ reading

    /** Everybody on this booking, lead first, in the order they joined it. */
    @Transactional(readOnly = true)
    public List<PortalParticipant> peopleOn(PortalJourney journey) {
        if (journey == null) return List.of();
        return participants.findByJourneyIdOrderByIdAsc(journey.getId());
    }

    /** How many more this booking may take, which is what the page needs to know. */
    @Transactional(readOnly = true)
    public int placesLeft(PortalJourney journey) {
        return Math.max(0, MAX_READERS - countReaders(journey));
    }

    // ------------------------------------------------------------------ inviting

    /**
     * Put somebody on the booking as a reader, and send them their own link.
     *
     * <p>The invitation carries the link and nothing else. No code, no session, no figure: the
     * person who receives it proves their own address through the ordinary verification flow
     * before they see a price, so forwarding the invitation on gets the next person no further
     * than the trip page any recipient of the original link already reaches.
     */
    @Transactional
    public PortalOutcome invite(PortalJourney journey, PortalParticipant inviter,
                                String email, String displayName, String ipAddress) {
        if (journey == null || !journey.isOpenable()) {
            return PortalOutcome.refuse("This booking is no longer open.", "PORTAL_CLOSED", journey);
        }
        if (inviter == null || !inviter.mayAct()) {
            return PortalOutcome.refuse(
                "Only the lead traveller on this booking can invite somebody to it.",
                "PORTAL_NOT_THE_LEAD", journey);
        }

        String normalised = normalise(email);
        if (normalised == null) {
            return PortalOutcome.refuse("That does not look like an email address.",
                "PORTAL_BAD_ADDRESS", journey);
        }

        Optional<PortalParticipant> already =
            participants.findByJourneyIdAndEmail(journey.getId(), normalised);

        /*
         * Somebody already on the booking.
         *
         * Said plainly rather than quietly resent. The lead knows who they invited; a silent
         * resend to an address they have mistyped into an existing one tells them nothing, and
         * the address-probing argument that governs the verification page does not apply here,
         * because the person asking is the person who owns the booking.
         */
        if (already.isPresent() && Boolean.TRUE.equals(already.get().getIsActive())) {
            PortalParticipant existing = already.get();
            if (existing.getParticipantRole() == ParticipantRole.LEAD) {
                return PortalOutcome.refuse("That is your own address.",
                    "PORTAL_ALREADY_THE_LEAD", journey);
            }
            return PortalOutcome.refuse(nameFor(existing) + " is already on this booking.",
                "PORTAL_ALREADY_INVITED", journey);
        }

        if (countReaders(journey) >= MAX_READERS) {
            return PortalOutcome.refuse(
                "This booking already has " + MAX_READERS + " people reading it. Take one off "
                    + "first, or ask us and we will add them.",
                "PORTAL_TOO_MANY_READERS", journey);
        }

        /*
         * Counted per booking, not per sender.
         *
         * The sender is a verified participant, so a per-sender limit sounds tighter and is not:
         * the thing being protected is the inbox of whoever is named, and the booking is what
         * both the lead and anybody who has taken their session have in common.
         */
        long wait = rateLimiter.check("portal-invite:" + journey.getId(),
            invitationsPerHour, Duration.ofHours(1));
        if (wait > 0) {
            log.warn("Too many invitations from journey {}; {}s to wait", journey.getId(), wait);
            return PortalOutcome.refuse(
                "That is a lot of invitations at once. Please try again a little later.",
                "PORTAL_INVITE_RATE_LIMITED", journey);
        }

        /*
         * A withdrawn reader who is invited again is the same row brought back, because the
         * unique key is the address on the booking. Inserting a second one throws, and catching
         * that to recover would be recovering from a state we can simply read.
         */
        PortalParticipant reader = already.orElseGet(() -> PortalParticipant.builder()
            .journey(journey)
            .email(normalised)
            .participantRole(ParticipantRole.READER)
            .build());

        reader.setIsActive(true);
        reader.setParticipantRole(ParticipantRole.READER);
        reader.setInvitedBy(inviter);
        reader.setInvitedAt(LocalDateTime.now());
        if (displayName != null && !displayName.isBlank()) {
            reader.setDisplayName(displayName.trim());
        }
        participants.save(reader);

        notifier.readerInvited(journey, reader, inviter);
        activity.record(journey, inviter, null, "INVITE_SENT", normalised, ipAddress, null);

        log.info("Journey {} invited a reader: {} people on the booking now",
            journey.getId(), countReaders(journey) + 1);

        return PortalOutcome.ok("We have sent " + nameFor(reader) + " a link to this trip.", journey);
    }

    // ------------------------------------------------------------------ withdrawing

    /**
     * Take a reader back off the booking.
     *
     * <p>Their trusted browsers end at the same moment, and that is the point of the method
     * rather than a tidy-up after it. A participant marked inactive whose session is still live
     * has not been removed from anything: they keep reading until the device expires, which is
     * weeks, and the person who pressed the button believes otherwise.
     */
    @Transactional
    public PortalOutcome withdraw(PortalJourney journey, PortalParticipant actor,
                                  Long participantId, String ipAddress) {
        if (journey == null) {
            return PortalOutcome.refuse("This booking is no longer open.", "PORTAL_CLOSED", journey);
        }
        if (actor == null || !actor.mayAct()) {
            return PortalOutcome.refuse(
                "Only the lead traveller on this booking can do that.",
                "PORTAL_NOT_THE_LEAD", journey);
        }

        PortalParticipant reader = participantId == null ? null
            : participants.findById(participantId).orElse(null);

        /*
         * Walked back from the row to the journey, exactly as an edit is. An id that names
         * somebody on another booking and an id that names nobody get the same answer.
         */
        if (reader == null || reader.getJourney() == null
                || !reader.getJourney().getId().equals(journey.getId())) {
            return PortalOutcome.refuse("That person is not on this booking.",
                "PORTAL_NOT_ON_THIS_BOOKING", journey);
        }
        if (reader.getParticipantRole() == ParticipantRole.LEAD) {
            return PortalOutcome.refuse(
                "The lead traveller cannot be taken off their own booking.",
                "PORTAL_CANNOT_REMOVE_THE_LEAD", journey);
        }

        return remove(journey, reader, actor, ipAddress);
    }

    /**
     * The same removal, asked for by the office.
     *
     * <p>Staff need it because the request arrives by telephone at least as often as it arrives
     * through the page — somebody's address was wrong, or the person it belongs to has asked to
     * be taken off. It runs the identical path so the two cannot drift apart; what differs is
     * only who is allowed to ask.
     */
    @Transactional
    public PortalOutcome withdrawAsStaff(PortalJourney journey, PortalParticipant reader) {
        if (journey == null || reader == null || reader.getJourney() == null
                || !reader.getJourney().getId().equals(journey.getId())) {
            return PortalOutcome.refuse("That person is not on this booking.",
                "PORTAL_NOT_ON_THIS_BOOKING", journey);
        }
        if (reader.getParticipantRole() == ParticipantRole.LEAD) {
            return PortalOutcome.refuse(
                "The lead traveller cannot be taken off their own booking. Revoke the whole "
                    + "link instead.",
                "PORTAL_CANNOT_REMOVE_THE_LEAD", journey);
        }
        return remove(journey, reader, null, null);
    }

    private PortalOutcome remove(PortalJourney journey, PortalParticipant reader,
                                 PortalParticipant actor, String ipAddress) {
        reader.setIsActive(false);
        participants.save(reader);

        /* the session goes with the row, or the removal removed nothing */
        int ended = devices.revokeEveryDeviceOfParticipant(reader.getId(), LocalDateTime.now());
        codes.spendEveryOutstandingCode(reader.getId(), LocalDateTime.now());

        activity.record(journey, actor, null, "INVITE_WITHDRAWN", reader.getEmail(), ipAddress, null);
        log.info("A reader was taken off journey {}; {} device(s) ended", journey.getId(), ended);

        return PortalOutcome.ok(nameFor(reader) + " can no longer open this trip.", journey);
    }

    // ------------------------------------------------------------------ small things

    private int countReaders(PortalJourney journey) {
        return (int) peopleOn(journey).stream()
            .filter(p -> p.getParticipantRole() == ParticipantRole.READER)
            .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
            .count();
    }

    private String normalise(String email) {
        if (email == null) return null;
        String trimmed = email.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty() || trimmed.length() > 255) return null;
        return EMAIL_PATTERN.matcher(trimmed).matches() ? trimmed : null;
    }

    static String nameFor(PortalParticipant participant) {
        if (participant == null) return "They";
        String name = participant.getDisplayName();
        return name != null && !name.isBlank() ? name.trim() : participant.getEmail();
    }
}
