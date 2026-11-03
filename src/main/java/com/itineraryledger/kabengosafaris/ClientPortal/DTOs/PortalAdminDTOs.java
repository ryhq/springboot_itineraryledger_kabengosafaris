package com.itineraryledger.kabengosafaris.ClientPortal.DTOs;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * What the office sees about a client's link.
 *
 * <p>The opposite problem from {@link PortalView}: here the operational detail is the point.
 * Which addresses can open it, which browsers have, from where, and what they have looked at.
 * None of it is shown to the client except their own device list.
 */
public final class PortalAdminDTOs {

    private PortalAdminDTOs() {}

    public record Journey(
        String id,
        /** the whole address, so somebody can paste it into a message to the client */
        String link,
        String token,
        String status,
        String proposalState,
        /**
         * The enum constant, beside the sentence.
         *
         * <p>Both, because the panel has to decide which buttons to offer and a display name is
         * a presentation decision that may be reworded; matching on one is how a button quietly
         * stops appearing after somebody improves the wording.
         */
        String proposalStateKey,
        LocalDateTime proposalStateAt,
        /** True when the customer is waiting on us, which is what the panel sorts and nags on. */
        boolean waitingOnUs,
        /** Which version we took over from, when somebody here is holding it. */
        Long heldFromRevision,
        boolean locked,
        /** what this journey sets, or null when it inherits */
        String editingLevel,
        /** what that actually resolves to, which is the answer somebody needs */
        String effectiveLevel,
        String effectiveLevelLabel,
        /** JOURNEY, ITINERARY, COMPANY or FROZEN: which of them actually decided it. */
        String levelSource,
        String customerName,
        String quoteCode,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        LocalDateTime revokedAt,
        String revokedReason,
        /** when the behavioural data goes, once the journey has closed */
        LocalDate purgeAfter,
        List<Participant> participants,
        List<Device> devices,
        long viewCount
    ) {}

    public record Participant(
        String id,
        String email,
        String displayName,
        String role,
        /** LEAD or READER, beside the sentence, for the same reason the proposal state is. */
        String roleKey,
        boolean verified,
        LocalDateTime verifiedAt,
        String policyVersion,
        /**
         * Who put them on the booking, and when.
         *
         * <p>Null for the lead, who was put there by us. For a reader this is the only record of
         * where the address came from, which is the first question asked when somebody rings to
         * say they are receiving emails about a trip that is not theirs.
         */
        String invitedByEmail,
        LocalDateTime invitedAt,
        /**
         * False for somebody who has been taken off.
         *
         * <p>Kept in the list rather than filtered out of it: a reader who was removed is part
         * of the history of who could see the booking, and the row is what carries that.
         */
        boolean active
    ) {}

    public record Device(
        String id,
        String participantEmail,
        String label,
        String ipAddress,
        LocalDateTime createdAt,
        LocalDateTime lastSeenAt,
        LocalDateTime expiresAt
    ) {}

    /**
     * What the access log is worth knowing, rather than the log itself.
     *
     * <p>Every field is a count, a timestamp, or a sentence naming the count it came from. There
     * is deliberately no score and no "interest level": an invented number wearing a figure's
     * clothes is the one thing on this screen somebody might price a trip by.
     */
    public record Insight(
        /** The question the whole thing exists for: has the customer opened it at all? */
        boolean everOpened,
        LocalDateTime firstOpenedAt,
        /** The last thing THEY did. Our own clicks on their journey are not activity. */
        LocalDateTime lastSeenAt,
        /** Null when they have never done anything, rather than a misleading zero. */
        Long daysSinceLastSeen,
        long reads,
        long actions,
        List<SectionCount> sections,
        List<PersonActivity> people,
        /** Sentences for a consultant: what to do about this booking today. */
        List<String> signals
    ) {}

    public record SectionCount(
        String key,
        String label,
        /** READING or DECIDING. The panel groups on it rather than guessing from the key. */
        String kind,
        long count,
        LocalDateTime lastAt
    ) {}

    public record PersonActivity(
        String email,
        String role,
        boolean verified,
        boolean active,
        long events,
        LocalDateTime firstAt,
        LocalDateTime lastAt
    ) {}

    /** One line of the access log, for the panel's activity list. */
    public record Activity(
        String section,
        String detail,
        String participantEmail,
        String ipAddress,
        LocalDateTime at
    ) {}
}
