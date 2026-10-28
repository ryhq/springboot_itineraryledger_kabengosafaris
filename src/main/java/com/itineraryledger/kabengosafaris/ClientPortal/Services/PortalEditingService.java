package com.itineraryledger.kabengosafaris.ClientPortal.Services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.ClientPortal.DTOs.PortalOutcome;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalJourney;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalParticipant;
import com.itineraryledger.kabengosafaris.ClientPortal.Enums.EditingLevel;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayAccommodation.Entity.QuoteDayAccommodation;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayAccommodation.Repository.QuoteDayAccommodationRepository;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayActivity.Entity.QuoteDayActivity;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayActivity.Repository.QuoteDayActivityRepository;
import com.itineraryledger.kabengosafaris.Quote.QuotePax.Entity.QuotePax;
import com.itineraryledger.kabengosafaris.Quote.QuotePax.Repository.QuotePaxRepository;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices.QuoteCostEstimationService;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices.QuoteEditPolicy;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices.QuoteEditPolicy.Actor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The four things a client may change about their own quotation.
 *
 * <p>Swap a night to an alternative we already offered. Change how many rooms. Include or drop
 * an optional activity. Change how many people are travelling. That is the whole of editing
 * level CHOOSE_OFFERED, and the list is short on purpose: every one of these is something the
 * client was already shown and we have already priced, so none of them can produce a trip we
 * cannot deliver or a figure we cannot stand behind.
 *
 * <p><strong>There is deliberately no way to rebuild the trip.</strong> Adding, removing or
 * reordering days would put the quotation's day tree out of step with the itinerary it was
 * snapshotted from, and a client cannot be left holding an incoherent copy of a product. They
 * say it in words and we build it: a suggestion is a sentence, a broken day tree is an afternoon.
 *
 * <p>Every method asks the same three questions in the same order, and the order matters.
 * <ol>
 *   <li><b>May this quotation change at all?</b> {@link QuoteEditPolicy} as the CLIENT, which is
 *       the single answer shared with the panel: decided, superseded, withdrawn, frozen, or
 *       with the operator.</li>
 *   <li><b>May this client reach this far?</b> The resolved editing level.</li>
 *   <li><b>Is this row theirs?</b> Walked from the row back to the quotation on their journey.
 *       An id in a request is a request, not a claim, and this is the failure that actually
 *       happens in portals: everything else is right and one endpoint takes an id from the body
 *       and trusts it.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortalEditingService {

    /**
     * The most people one line of the party may hold.
     *
     * <p>Not a business rule so much as a guard against a typed zero or a pasted phone number
     * re-pricing a safari into six figures. A real group larger than this is a conversation.
     */
    private static final int MOST_TRAVELLERS_PER_LINE = 30;

    /** The most rooms one night may hold, for the same reason. */
    private static final int MOST_ROOMS = 20;

    private final QuoteEditPolicy editPolicy;
    private final QuoteDayAccommodationRepository accommodations;
    private final QuoteDayActivityRepository activities;
    private final QuotePaxRepository party;
    private final QuoteCostEstimationService costEstimation;
    private final PortalJourneyService journeys;
    private final PortalActivityService activity;

    // ----------------------------------------------------------------- the four edits

    /**
     * Take one of the alternatives we offered for a night.
     *
     * <p>A swap rather than a replacement: the row they chose becomes the booked one and the row
     * that was booked becomes the alternative. Nothing is deleted, so the menu we offered is
     * still there and they can change their mind back, and the change history reads as one swap
     * rather than as a removal and an addition.
     */
    @Transactional
    public PortalOutcome chooseStay(PortalJourney journey, PortalParticipant by, Long rowId) {
        PortalOutcome refusal = whyNotEditable(journey);
        if (refusal != null) return refusal;

        QuoteDayAccommodation chosen = accommodations.findById(rowId).orElse(null);
        if (!belongsTo(journey, chosen)) return notYours(journey);

        if (!Boolean.TRUE.equals(chosen.getIsAlternative())) {
            return PortalOutcome.ok("That is already the one you have.", journey);
        }

        List<QuoteDayAccommodation> onThatNight =
            accommodations.findByQuoteDayIdOrderByIdAsc(chosen.getQuoteDay().getId());

        for (QuoteDayAccommodation row : onThatNight) {
            if (!Boolean.TRUE.equals(row.getIsAlternative())) {
                row.setIsAlternative(true);
                accommodations.save(row);
            }
        }
        chosen.setIsAlternative(false);
        accommodations.save(chosen);

        return done(journey, by, "EDIT_STAY", nameOf(chosen),
            "Changed. Your price below has been worked out again.");
    }

    /** How many rooms on one night. */
    @Transactional
    public PortalOutcome setRooms(PortalJourney journey, PortalParticipant by,
                                  Long rowId, Integer rooms) {
        PortalOutcome refusal = whyNotEditable(journey);
        if (refusal != null) return refusal;

        if (rooms == null || rooms < 1 || rooms > MOST_ROOMS) {
            return PortalOutcome.refuse(
                "Please choose between 1 and " + MOST_ROOMS + " rooms, or tell us what you need.",
                "OUT_OF_RANGE", journey);
        }

        QuoteDayAccommodation row = accommodations.findById(rowId).orElse(null);
        if (!belongsTo(journey, row)) return notYours(journey);

        if (Boolean.TRUE.equals(row.getIsAlternative())) {
            return PortalOutcome.refuse(
                "Choose that one first, then say how many rooms you need.",
                "NOT_THE_CHOSEN_STAY", journey);
        }

        row.setRoomCount(rooms);
        accommodations.save(row);

        return done(journey, by, "EDIT_ROOMS", nameOf(row) + " x" + rooms,
            "Changed. Your price below has been worked out again.");
    }

    /**
     * Take or leave an optional activity.
     *
     * <p>Only ones already marked optional. An activity that is part of the trip is part of what
     * we quoted and part of what the vehicle and the days were planned around; dropping it is a
     * different trip, not a smaller bill.
     */
    @Transactional
    public PortalOutcome setActivityIncluded(PortalJourney journey, PortalParticipant by,
                                             Long rowId, boolean included) {
        PortalOutcome refusal = whyNotEditable(journey);
        if (refusal != null) return refusal;

        QuoteDayActivity row = activities.findById(rowId).orElse(null);
        if (!belongsTo(journey, row)) return notYours(journey);

        if (!Boolean.TRUE.equals(row.getIsOptional())) {
            return PortalOutcome.refuse(
                "That one is part of the trip. Tell us if you would rather not do it and we "
                    + "will see what the day looks like without it.",
                "NOT_OPTIONAL", journey);
        }

        row.setIsIncludedInPrice(included);
        activities.save(row);

        return done(journey, by, included ? "EDIT_ACTIVITY_ADDED" : "EDIT_ACTIVITY_DROPPED",
            row.getActivity() == null ? null : row.getActivity().getName(),
            included ? "Added. Your price below has been worked out again."
                     : "Taken off. Your price below has been worked out again.");
    }

    /**
     * How many people are travelling, on one line of the party.
     *
     * <p>The count only. Adding a kind of traveller the quotation does not already carry means
     * choosing a nationality and an age band, which decides which tariff applies and is a
     * decision with a wrong answer; a client guessing it produces a price neither side can
     * explain. They ask, and we add the line.
     */
    @Transactional
    public PortalOutcome setPartySize(PortalJourney journey, PortalParticipant by,
                                      Long rowId, Integer count) {
        PortalOutcome refusal = whyNotEditable(journey);
        if (refusal != null) return refusal;

        if (count == null || count < 0 || count > MOST_TRAVELLERS_PER_LINE) {
            return PortalOutcome.refuse(
                "Please choose between 0 and " + MOST_TRAVELLERS_PER_LINE
                    + " travellers on that line, or tell us about the group.",
                "OUT_OF_RANGE", journey);
        }

        QuotePax row = party.findById(rowId).orElse(null);
        if (row == null || !sameQuote(journey, row.getQuote())) return notYours(journey);

        if (count == 0 && onlyLineWithAnybody(journey.getQuote(), row)) {
            return PortalOutcome.refuse(
                "Somebody has to be travelling. Tell us if the trip is off and we will close it.",
                "NOBODY_TRAVELLING", journey);
        }

        row.setCount(count);
        party.save(row);

        return done(journey, by, "EDIT_PARTY", label(row) + " x" + count,
            "Changed. Your price below has been worked out again.");
    }

    // ----------------------------------------------------------------- the three questions

    /**
     * May this quotation change, and may this client reach this far?
     *
     * <p>Public, and the one answer. The page asks it to decide whether to draw the controls
     * at all, and every edit asks it before doing anything, so what is offered and what is
     * accepted cannot drift apart. A control that appears and then refuses is worse than no
     * control, because the client has already decided what they want.
     *
     * <p>The policy first and the level second, because the policy's refusals are the ones with
     * something for the client to do about them. "We have started reviewing this" is an answer;
     * "you cannot change anything" to somebody whose quotation has already been converted tells
     * them nothing and invites the telephone call.
     */
    public PortalOutcome whyNotEditable(PortalJourney journey) {
        if (journey == null || !journey.isOpenable()) {
            return PortalOutcome.refuse("This link is not available.",
                "PORTAL_LINK_UNAVAILABLE", journey);
        }

        QuoteEditPolicy.Decision decision =
            editPolicy.canEditDayTree(journey.getQuote(), Actor.CLIENT);
        if (!decision.allowed()) {
            return PortalOutcome.refuse(decision.message(), decision.code(), journey);
        }

        EditingLevel level = journeys.levelOf(journey);
        if (!level.allowsChanges()) {
            return PortalOutcome.refuse(
                "This quotation is here for you to read and answer. Tell us what you would like "
                    + "changed and we will do it.",
                "LEVEL_RESPOND_ONLY", journey);
        }

        return null;
    }

    /**
     * Is this row on the quotation behind this link?
     *
     * <p>Walked from the row rather than looked up from the journey, because the row is what the
     * caller named and the row is what has to be proved. The portal's rule is that no endpoint
     * takes an id that could address another journey, and this is where that rule is kept for
     * the four that have to.
     */
    private boolean belongsTo(PortalJourney journey, QuoteDayAccommodation row) {
        return row != null && row.getQuoteDay() != null
            && sameQuote(journey, row.getQuoteDay().getQuote());
    }

    private boolean belongsTo(PortalJourney journey, QuoteDayActivity row) {
        return row != null && row.getQuoteDay() != null
            && sameQuote(journey, row.getQuoteDay().getQuote());
    }

    private boolean sameQuote(PortalJourney journey, Quote quote) {
        return quote != null && journey.getQuote() != null
            && quote.getId() != null
            && quote.getId().equals(journey.getQuote().getId());
    }

    /** The same answer as a row that does not exist, so neither is discoverable from the other. */
    private PortalOutcome notYours(PortalJourney journey) {
        return PortalOutcome.refuse("That is not on this booking.", "NOT_ON_THIS_BOOKING", journey);
    }

    // ----------------------------------------------------------------- afterwards

    /**
     * Re-price, write it down, and say so.
     *
     * <p>The recalculation is not optional and not deferred. A client who changes a lodge and
     * sees the old total has been shown a figure that is wrong, and they will remember the one
     * they were shown.
     */
    private PortalOutcome done(PortalJourney journey, PortalParticipant by,
                               String section, String detail, String message) {
        costEstimation.recalculate(journey.getQuote().getId());
        activity.record(journey, by, null, section, detail, null, null);
        return PortalOutcome.ok(message, journey);
    }

    private boolean onlyLineWithAnybody(Quote quote, QuotePax row) {
        return party.findByQuoteIdOrderByIdAsc(quote.getId()).stream()
            .filter(line -> !line.getId().equals(row.getId()))
            .noneMatch(line -> line.getCount() != null && line.getCount() > 0);
    }

    private String nameOf(QuoteDayAccommodation row) {
        return row.getAccommodation() == null ? "a stay" : row.getAccommodation().getName();
    }

    private String label(QuotePax row) {
        String nation = row.getNationCategory() == null ? "" : row.getNationCategory().getName();
        String age = row.getAgeCategory() == null ? "" : row.getAgeCategory().getName();
        return (nation + " " + age).trim();
    }
}
