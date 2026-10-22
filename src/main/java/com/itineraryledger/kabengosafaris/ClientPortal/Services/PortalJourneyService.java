package com.itineraryledger.kabengosafaris.ClientPortal.Services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalJourney;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalParticipant;
import com.itineraryledger.kabengosafaris.ClientPortal.Enums.JourneyStatus;
import com.itineraryledger.kabengosafaris.ClientPortal.Enums.ParticipantRole;
import com.itineraryledger.kabengosafaris.ClientPortal.Enums.ProposalState;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalDeviceRepository;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalJourneyRepository;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalParticipantRepository;
import com.itineraryledger.kabengosafaris.Customer.Entity.Customer;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Giving a client their link, and taking it away.
 *
 * <p>A journey is created from a quotation, because that is the moment there is something worth
 * showing: a customer, a trip, and a price. It is deliberately idempotent on (customer,
 * itinerary) so that pressing the button twice, or raising a second version of a quote, does not
 * hand the same person two links to the same trip and leave them guessing which is current.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortalJourneyService {

    /**
     * How long behavioural data outlives the trip.
     *
     * <p>Three months, as agreed. Long enough to answer a question about a trip that has just
     * happened, short enough that we are not holding somebody's browsing history indefinitely
     * because nobody got round to deciding.
     */
    private static final int BEHAVIOURAL_DATA_MONTHS = 3;

    private final PortalJourneyRepository journeys;
    private final PortalParticipantRepository participants;
    private final PortalDeviceRepository devices;
    private final PortalTokens tokens;
    private final PortalActivityService activity;

    /**
     * Create the link for a quotation, or return the one this client already has.
     *
     * @return the journey, or null when the quote has no customer, or the customer no address.
     *         Both are refusals rather than failures: a portal nobody can be told about is not
     *         worth creating, and the caller says why.
     */
    @Transactional
    public PortalJourney publishFor(Quote quote) {
        if (quote == null || quote.getCustomer() == null) return null;

        Customer customer = quote.getCustomer();
        String email = customer.getPrimaryEmail();
        if (email == null || email.isBlank()) {
            log.info("Quote {} cannot be shared: its customer has no email address",
                quote.getQuoteCode());
            return null;
        }

        Long itineraryId = quote.getItinerary() == null ? null : quote.getItinerary().getId();

        PortalJourney journey = itineraryId == null ? null
            : journeys.findByCustomerIdAndItineraryId(customer.getId(), itineraryId).orElse(null);

        if (journey == null) {
            journey = journeys.save(PortalJourney.builder()
                .token(tokens.newSecret())
                .customer(customer)
                .itinerary(quote.getItinerary())
                .quote(quote)
                .status(JourneyStatus.OPEN)
                .proposalState(ProposalState.OPEN_TO_CLIENT)
                .locked(false)
                .build());
            log.info("Opened a portal journey for customer {} on quote {}",
                customer.getCode(), quote.getQuoteCode());
        } else {
            /*
             * The same trip, a newer quotation. Point the existing link at it rather than issuing
             * a second one: the client has the first address in their inbox, and two links to one
             * trip is how somebody ends up reading last month's price.
             */
            journey.setQuote(quote);
            if (journey.getStatus() == JourneyStatus.CLOSED) {
                journey.setStatus(JourneyStatus.OPEN);
                journey.setPurgeAfter(null);
            }
            journey = journeys.save(journey);
        }

        ensureLead(journey, email, customer);
        return journey;
    }

    /** The customer is the lead by definition; anybody else is invited by them later. */
    private void ensureLead(PortalJourney journey, String email, Customer customer) {
        String normalised = email.trim().toLowerCase(Locale.ROOT);
        if (participants.findByJourneyIdAndEmail(journey.getId(), normalised).isPresent()) return;

        participants.save(PortalParticipant.builder()
            .journey(journey)
            .email(normalised)
            .displayName(displayNameOf(customer))
            .participantRole(ParticipantRole.LEAD)
            .isActive(true)
            .build());
    }

    /**
     * Withdraw the link entirely.
     *
     * <p>The control that matters when something has gone wrong: a forwarded link, an inbox
     * somebody else is reading, a request that turns out not to have come from the customer.
     * Every device dies with it, so a browser that was already trusted stops working too, which
     * is the whole point of revoking rather than merely locking.
     */
    @Transactional
    public PortalJourney revoke(PortalJourney journey, String reason) {
        journey.setStatus(JourneyStatus.REVOKED);
        journey.setRevokedAt(LocalDateTime.now());
        journey.setRevokedReason(reason);
        devices.revokeEveryDeviceOf(journey.getId(), LocalDateTime.now());
        activity.record(journey, null, null, "JOURNEY_REVOKED", reason, null, null);
        log.info("Revoked portal journey {}: {}", journey.getId(), reason);
        return journeys.save(journey);
    }

    /** Put a revoked or closed journey back into service. */
    @Transactional
    public PortalJourney reopen(PortalJourney journey) {
        journey.setStatus(JourneyStatus.OPEN);
        journey.setRevokedAt(null);
        journey.setRevokedReason(null);
        journey.setPurgeAfter(null);
        activity.record(journey, null, null, "JOURNEY_REOPENED", null, null, null);
        return journeys.save(journey);
    }

    /**
     * The trip is over, or the booking fell through. Start the clock on the behavioural data.
     *
     * <p>The date is stored rather than derived, so that moving a safari's dates afterwards
     * cannot silently extend how long somebody's browsing history is kept. It is a promise, and a
     * promise recomputed from changing inputs is not one.
     */
    @Transactional
    public PortalJourney close(PortalJourney journey, LocalDate endedOn) {
        LocalDate anchor = endedOn != null ? endedOn : LocalDate.now();
        journey.setStatus(JourneyStatus.CLOSED);
        journey.setPurgeAfter(anchor.plusMonths(BEHAVIOURAL_DATA_MONTHS));
        log.info("Closed portal journey {}; behavioural data expires {}",
            journey.getId(), journey.getPurgeAfter());
        return journeys.save(journey);
    }

    /** Freeze or unfreeze what a client may do, without taking their link away. */
    @Transactional
    public PortalJourney setLocked(PortalJourney journey, boolean locked) {
        journey.setLocked(locked);
        activity.record(journey, null, null, locked ? "JOURNEY_LOCKED" : "JOURNEY_UNLOCKED",
            null, null, null);
        return journeys.save(journey);
    }

    private String displayNameOf(Customer customer) {
        String first = customer.getFirstName();
        String last = customer.getLastName();
        if (first == null && last == null) return customer.getCompanyName();
        return ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
    }
}
