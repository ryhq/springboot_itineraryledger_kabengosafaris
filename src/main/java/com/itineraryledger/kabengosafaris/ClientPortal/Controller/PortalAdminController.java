package com.itineraryledger.kabengosafaris.ClientPortal.Controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.itineraryledger.kabengosafaris.ClientPortal.DTOs.PortalAdminDTOs;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalAccessEvent;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalDevice;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalJourney;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalParticipant;
import com.itineraryledger.kabengosafaris.ClientPortal.Enums.EditingLevel;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalAccessEventRepository;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalDeviceRepository;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalJourneyRepository;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalAccessService;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalJourneyService;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The office's side of a client's link.
 *
 * <p>Deliberately a separate path space from the portal itself. {@code /api/portal/**} is
 * reached by clients holding a cookie and no account; this is reached by staff holding a JWT,
 * and the two must never be confused by a filter, a test or a reader.
 *
 * <p>No new permission names. A journey is an attribute of a quotation, so reading one needs
 * {@code PERM_READ_QUOTE} and changing one needs {@code PERM_UPDATE_QUOTE}. Permissions here are
 * matched by name against a catalogue of several hundred, and a name invented for two endpoints
 * is a name nobody will ever grant deliberately.
 */
@RestController
@RequestMapping("/api/portal-admin")
@RequiredArgsConstructor
@Slf4j
public class PortalAdminController {

    private final PortalJourneyRepository journeys;
    private final PortalDeviceRepository devices;
    private final PortalAccessEventRepository events;
    private final PortalJourneyService journeyService;
    private final PortalAccessService accessService;
    private final QuoteRepository quotes;
    private final IdObfuscator idObfuscator;

    /** What the quote's Portal tab shows. 204 when this quote has never been shared. */
    @GetMapping("/quotes/{quoteId}")
    @PreAuthorize("hasAuthority('PERM_READ_QUOTE')")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> forQuote(@PathVariable String quoteId) {
        PortalJourney journey = journeyFor(quoteId);
        if (journey == null) {
            return ResponseEntity.ok(ApiResponse.success(200,
                "This quotation has not been shared with the customer", null));
        }
        return ResponseEntity.ok(ApiResponse.success(200, "Portal journey retrieved", toDTO(journey)));
    }

    /**
     * Give this customer their link, or hand back the one they already have.
     *
     * <p>Idempotent on purpose. Pressing the button twice, or raising a second version of a
     * quote, must not produce a second address to the same trip: the client has the first one in
     * their inbox, and two links is how somebody ends up reading last month's price.
     */
    @PostMapping("/quotes/{quoteId}/publish")
    @PreAuthorize("hasAuthority('PERM_UPDATE_QUOTE')")
    @Transactional
    public ResponseEntity<ApiResponse<?>> publish(@PathVariable String quoteId) {
        Quote quote;
        try {
            quote = quotes.findById(idObfuscator.decodeId(quoteId)).orElse(null);
        } catch (Exception e) {
            quote = null;
        }
        if (quote == null) {
            return ResponseEntity.status(404).body(
                ApiResponse.error(404, "Quote not found: " + quoteId, "QUOTE_NOT_FOUND"));
        }

        PortalJourney journey = journeyService.publishFor(quote);
        if (journey == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400,
                "This quotation cannot be shared yet: it needs a customer with an email address. "
                    + "The link is useless without one, because the access code has nowhere to go.",
                "NO_CUSTOMER_EMAIL"));
        }
        return ResponseEntity.ok(ApiResponse.success(200,
            "The customer can now open this trip", toDTO(journey)));
    }

    /**
     * Withdraw the link, and every session already open on it.
     *
     * <p>The control for when something has gone wrong. Locking leaves them reading; revoking
     * closes the door.
     */
    @PostMapping("/journeys/{id}/revoke")
    @PreAuthorize("hasAuthority('PERM_UPDATE_QUOTE')")
    @Transactional
    public ResponseEntity<ApiResponse<?>> revoke(@PathVariable String id,
                                                 @RequestBody(required = false) Map<String, String> body) {
        PortalJourney journey = journey(id);
        if (journey == null) return notFound();

        String reason = body == null ? null : body.get("reason");
        return ResponseEntity.ok(ApiResponse.success(200,
            "Withdrawn. Every device signed in on this link has been signed out.",
            toDTO(journeyService.revoke(journey, reason))));
    }

    /**
     * Destroy the link and its whole record.
     *
     * <p>DELETE rather than another POST verb because that is what it is, and it takes
     * PERM_DELETE_QUOTE rather than PERM_UPDATE_QUOTE: withdrawing a link is day-to-day work,
     * throwing away who opened it is not.
     */
    @DeleteMapping("/journeys/{id}")
    @PreAuthorize("hasAuthority('PERM_DELETE_QUOTE')")
    @Transactional
    public ResponseEntity<ApiResponse<?>> delete(@PathVariable String id) {
        PortalJourney journey = journey(id);
        if (journey == null) return notFound();

        journeyService.delete(journey);
        return ResponseEntity.ok(ApiResponse.success(200,
            "Deleted. Publishing again will issue a new link, and the old one will never work.",
            null));
    }

    @PostMapping("/journeys/{id}/reopen")
    @PreAuthorize("hasAuthority('PERM_UPDATE_QUOTE')")
    @Transactional
    public ResponseEntity<ApiResponse<?>> reopen(@PathVariable String id) {
        PortalJourney journey = journey(id);
        if (journey == null) return notFound();
        return ResponseEntity.ok(ApiResponse.success(200,
            "Open again. The customer will need a fresh code, because the old sessions are gone.",
            toDTO(journeyService.reopen(journey))));
    }

    /** Freeze what the client may do without taking their link away. */
    @PostMapping("/journeys/{id}/lock")
    @PreAuthorize("hasAuthority('PERM_UPDATE_QUOTE')")
    @Transactional
    public ResponseEntity<ApiResponse<?>> lock(@PathVariable String id,
                                               @RequestParam(defaultValue = "true") boolean locked) {
        PortalJourney journey = journey(id);
        if (journey == null) return notFound();
        return ResponseEntity.ok(ApiResponse.success(200,
            locked ? "Frozen. They can still read it and can change nothing." : "Unfrozen.",
            toDTO(journeyService.setLocked(journey, locked))));
    }

    /**
     * How far this one client may reach into their own quotation.
     *
     * <p>Null hands it back to the company default, which is what most journeys should be on.
     * Setting it here is for the exception: a request that looks wrong is dropped to RESPOND, or
     * a client you know well is given the catalogue.
     */
    @PostMapping("/journeys/{id}/editing-level")
    @PreAuthorize("hasAuthority('PERM_UPDATE_QUOTE')")
    @Transactional
    public ResponseEntity<ApiResponse<?>> editingLevel(@PathVariable String id,
                                                       @RequestBody Map<String, String> body) {
        PortalJourney journey = journey(id);
        if (journey == null) return notFound();

        String wanted = body == null ? null : body.get("level");
        EditingLevel level = null;
        if (wanted != null && !wanted.isBlank() && !"INHERIT".equalsIgnoreCase(wanted)) {
            try {
                level = EditingLevel.valueOf(wanted.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400,
                    "Unknown editing level: " + wanted, "INVALID_EDITING_LEVEL"));
            }
        }

        return ResponseEntity.ok(ApiResponse.success(200,
            level == null ? "Back to the company default" : level.getDisplayName(),
            toDTO(journeyService.setEditingLevel(journey, level))));
    }

    /** End one browser's session from the office, for a client who has rung up worried. */
    @PostMapping("/journeys/{id}/devices/{deviceId}/revoke")
    @PreAuthorize("hasAuthority('PERM_UPDATE_QUOTE')")
    @Transactional
    public ResponseEntity<ApiResponse<?>> revokeDevice(@PathVariable String id,
                                                       @PathVariable String deviceId) {
        PortalJourney journey = journey(id);
        if (journey == null) return notFound();

        boolean done = accessService.revokeDevice(journey, idObfuscator.decodeId(deviceId));
        if (!done) {
            return ResponseEntity.status(404).body(
                ApiResponse.error(404, "That device is not on this journey", "DEVICE_NOT_FOUND"));
        }
        return ResponseEntity.ok(ApiResponse.success(200, "Signed out", toDTO(journey)));
    }

    /** What the client has opened. Empties three months after the trip; see the retention job. */
    @GetMapping("/journeys/{id}/activity")
    @PreAuthorize("hasAuthority('PERM_READ_QUOTE')")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> activity(@PathVariable String id,
                                                   @RequestParam(defaultValue = "50") int size) {
        PortalJourney journey = journey(id);
        if (journey == null) return notFound();

        List<PortalAdminDTOs.Activity> rows = new ArrayList<>();
        for (PortalAccessEvent event : events.findByJourneyIdOrderByCreatedAtDesc(
                journey.getId(), PageRequest.of(0, Math.min(Math.max(size, 1), 200)))) {
            rows.add(new PortalAdminDTOs.Activity(
                event.getSection(),
                event.getDetail(),
                event.getParticipant() == null ? null : event.getParticipant().getEmail(),
                event.getIpAddress(),
                event.getCreatedAt()));
        }
        return ResponseEntity.ok(ApiResponse.success(200, "Activity retrieved", rows));
    }

    // ------------------------------------------------------------------ internals

    private PortalJourney journeyFor(String quoteId) {
        try {
            return journeys.findByQuoteId(idObfuscator.decodeId(quoteId)).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private PortalJourney journey(String id) {
        try {
            return journeys.findById(idObfuscator.decodeId(id)).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private ResponseEntity<ApiResponse<?>> notFound() {
        return ResponseEntity.status(404).body(
            ApiResponse.error(404, "No such portal journey", "PORTAL_JOURNEY_NOT_FOUND"));
    }

    private PortalAdminDTOs.Journey toDTO(PortalJourney journey) {
        List<PortalAdminDTOs.Participant> participants = new ArrayList<>();
        for (PortalParticipant participant : journey.getParticipants()) {
            participants.add(new PortalAdminDTOs.Participant(
                idObfuscator.encodeId(participant.getId()),
                participant.getEmail(),
                participant.getDisplayName(),
                participant.getParticipantRole() == null ? null
                    : participant.getParticipantRole().getDisplayName(),
                participant.isVerified(),
                participant.getVerifiedAt(),
                participant.getPolicyVersion()));
        }

        List<PortalAdminDTOs.Device> deviceRows = new ArrayList<>();
        for (PortalDevice device : devices.findByJourneyId(journey.getId())) {
            if (device.getRevokedAt() != null) continue;
            if (device.getExpiresAt() != null && device.getExpiresAt().isBefore(LocalDateTime.now())) continue;
            deviceRows.add(new PortalAdminDTOs.Device(
                idObfuscator.encodeId(device.getId()),
                device.getParticipant() == null ? null : device.getParticipant().getEmail(),
                device.getLabel(),
                device.getIpAddress(),
                device.getCreatedAt(),
                device.getLastSeenAt(),
                device.getExpiresAt()));
        }

        return new PortalAdminDTOs.Journey(
            idObfuscator.encodeId(journey.getId()),
            accessService.linkFor(journey),
            journey.getToken(),
            journey.getStatus() == null ? null : journey.getStatus().getDisplayName(),
            journey.getProposalState() == null ? null : journey.getProposalState().getDisplayName(),
            Boolean.TRUE.equals(journey.getLocked()),
            journey.getEditingLevel() == null ? null : journey.getEditingLevel().name(),
            journeyService.levelOf(journey).name(),
            journeyService.levelOf(journey).getDisplayName(),
            journey.getCustomer() == null ? null : journey.getCustomer().getFullName(),
            journey.getQuote() == null ? null : journey.getQuote().getQuoteCode(),
            journey.getCreatedAt(),
            journey.getExpiresAt(),
            journey.getRevokedAt(),
            journey.getRevokedReason(),
            journey.getPurgeAfter(),
            participants,
            deviceRows,
            events.countByJourneyId(journey.getId()));
    }
}
