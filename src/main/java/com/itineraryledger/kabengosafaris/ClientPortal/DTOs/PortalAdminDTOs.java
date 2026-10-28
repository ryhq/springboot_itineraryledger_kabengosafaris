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
        boolean verified,
        LocalDateTime verifiedAt,
        String policyVersion
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

    /** One line of the access log, for the panel's activity list. */
    public record Activity(
        String section,
        String detail,
        String participantEmail,
        String ipAddress,
        LocalDateTime at
    ) {}
}
