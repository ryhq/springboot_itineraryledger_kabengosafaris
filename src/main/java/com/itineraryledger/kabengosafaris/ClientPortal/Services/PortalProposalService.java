package com.itineraryledger.kabengosafaris.ClientPortal.Services;

import java.time.LocalDateTime;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.ClientPortal.DTOs.PortalOutcome;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalJourney;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalParticipant;
import com.itineraryledger.kabengosafaris.ClientPortal.Enums.ProposalState;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalJourneyRepository;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.Entity.QuoteRevision;
import com.itineraryledger.kabengosafaris.Quote.Enums.RevisionAuthor;
import com.itineraryledger.kabengosafaris.Quote.Enums.RevisionReason;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices.QuoteRevisionService;
import com.itineraryledger.kabengosafaris.User.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Passing the pen, and writing down each time it moves.
 *
 * <p>Exactly one side may change a quotation at any moment, and every handover is deliberate.
 * The two failures this exists to avoid are both worse than slowness. If both sides can edit,
 * there are two versions of one quotation and no way to say which the customer agreed to. If
 * submitting locks the client out, then a client who spots their own mistake one second after
 * sending it has to email us to ask for the pen back, which is the correspondence the portal was
 * built to end.
 *
 * <p>So <strong>submitting is a signal, not a lock</strong>. The client keeps the pen through
 * SUBMITTED and can keep editing and submit again. It only moves when a member of staff
 * deliberately takes it, and it only comes back when they deliberately return it.
 *
 * <p><strong>Deciding does not move the quotation's own status.</strong> AGREED and DECLINED are
 * recorded here and the office is told; a member of staff then runs the existing
 * {@code QuoteStatusService} path. Three reasons, and the first is sufficient: that path is
 * audited, and {@code audit_logs.user_id} is not nullable, so a client's click would either fail
 * the insert or be stamped with somebody else's name. Accepting also only runs from SENT, and
 * acceptance in this business starts a deposit invoice and a confirmation to the lodges, which
 * is not something to fire off a button press nobody has read yet.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortalProposalService {

    private final PortalJourneyRepository journeys;
    private final QuoteRevisionService revisions;
    private final PortalActivityService activity;
    private final PortalNotifier notifier;

    // ----------------------------------------------------------------- the client

    /**
     * "Please look at this."
     *
     * <p>Does not lock anything. See the class note; this is the single most important property
     * of the loop and the easiest one to lose by accident later.
     */
    @Transactional
    public PortalOutcome submit(PortalJourney journey, PortalParticipant by) {
        PortalOutcome refusal = clientMayAct(journey);
        if (refusal != null) return refusal;

        if (!journey.getProposalState().clientMayEdit()) {
            return PortalOutcome.refuse(
                "We have started reviewing this. You will be able to make changes again once we "
                    + "have sent it back to you.",
                "WITH_OPERATOR", journey);
        }

        move(journey, ProposalState.SUBMITTED, null);
        record(journey, RevisionAuthor.CLIENT, RevisionReason.SUBMITTED, nameOf(by), null);
        log(journey, by, "PROPOSAL_SUBMITTED");
        notifier.proposalSubmitted(journey);

        return PortalOutcome.ok(
            "Thank you. We have it, and we will come back to you. You can still make changes "
                + "until we start looking at it.",
            journey);
    }

    /** The client accepts. Recorded here; the paperwork is moved by a person. */
    @Transactional
    public PortalOutcome agree(PortalJourney journey, PortalParticipant by) {
        PortalOutcome refusal = clientMayAct(journey);
        if (refusal != null) return refusal;

        if (!journey.getProposalState().clientMayDecide()) {
            return PortalOutcome.refuse(
                "This is with us at the moment. We will send it back to you shortly.",
                "WITH_OPERATOR", journey);
        }

        move(journey, ProposalState.AGREED, null);
        record(journey, RevisionAuthor.CLIENT, RevisionReason.AGREED, nameOf(by), null);
        log(journey, by, "PROPOSAL_AGREED");
        notifier.quoteAgreed(journey);

        return PortalOutcome.ok(
            "Thank you. We will confirm everything and send you what happens next.", journey);
    }

    /** The client declines. The link stays open: declining is not leaving. */
    @Transactional
    public PortalOutcome decline(PortalJourney journey, PortalParticipant by, String reason) {
        PortalOutcome refusal = clientMayAct(journey);
        if (refusal != null) return refusal;

        if (!journey.getProposalState().clientMayDecide()) {
            return PortalOutcome.refuse(
                "This is with us at the moment. We will send it back to you shortly.",
                "WITH_OPERATOR", journey);
        }

        move(journey, ProposalState.DECLINED, null);
        record(journey, RevisionAuthor.CLIENT, RevisionReason.DECLINED, nameOf(by), null);
        log(journey, by, "PROPOSAL_DECLINED", reason);
        notifier.quoteDeclined(journey, reason);

        return PortalOutcome.ok("Thank you for telling us. We will be in touch.", journey);
    }

    // ----------------------------------------------------------------- the office

    /**
     * Take the pen.
     *
     * <p>The revision recorded here is what the client's view was taken from, so the panel can
     * say "taken over from version 4" rather than leaving somebody to work out what they are
     * looking at. From this moment a client edit is refused by {@code QuoteEditPolicy} rather
     * than quietly overwriting a review in progress.
     *
     * <p>Allowed on a frozen journey. Freezing stops the client acting; it is not a reason the
     * office cannot pick the thing up, and a frozen journey nobody can take over is a journey
     * nobody can unfreeze from a sensible state.
     */
    @Transactional
    public PortalOutcome takeOver(PortalJourney journey) {
        if (journey == null || !journey.isOpenable()) {
            return PortalOutcome.refuse("This link is closed.", "PORTAL_LINK_UNAVAILABLE", journey);
        }

        if (journey.getProposalState() != null && journey.getProposalState().isFinal()) {
            return PortalOutcome.refuse(
                "The customer has already " + journey.getProposalState().getDisplayName().toLowerCase()
                    + ". Raise a new version rather than reopening this one.",
                "PROPOSAL_FINAL", journey);
        }

        if (journey.getProposalState() == ProposalState.WITH_OPERATOR) {
            return PortalOutcome.refuse("Somebody here already has this.", "ALREADY_HELD", journey);
        }

        User staff = currentUser();
        QuoteRevision claimed = record(journey, RevisionAuthor.STAFF, RevisionReason.TAKEN_OVER,
            nameOf(staff), staff == null ? null : staff.getId());

        move(journey, ProposalState.WITH_OPERATOR, staff);
        journey.setHeldFromRevision(claimed == null ? null
            : Long.valueOf(claimed.getRevisionNumber()));
        journeys.save(journey);
        log(journey, null, "PROPOSAL_TAKEN_OVER");
        notifier.proposalInReview(journey, nameOf(staff));

        return PortalOutcome.ok("You have this one. The customer is read only until you send it back.",
            journey);
    }

    /**
     * Give it back.
     *
     * <p>Explicit, and the panel shows the age of anything sitting in WITH_OPERATOR, because the
     * failure of a loop like this is never a wrong state. It is a quotation nobody remembered to
     * return, with a customer on the other end who has no way to ask for it.
     */
    @Transactional
    public PortalOutcome returnToClient(PortalJourney journey) {
        if (journey == null || !journey.isOpenable()) {
            return PortalOutcome.refuse("This link is closed.", "PORTAL_LINK_UNAVAILABLE", journey);
        }

        if (journey.getProposalState() != ProposalState.WITH_OPERATOR) {
            return PortalOutcome.refuse("Nobody here is holding this one.", "NOT_HELD", journey);
        }

        User staff = currentUser();
        move(journey, ProposalState.RETURNED, null);
        record(journey, RevisionAuthor.STAFF, RevisionReason.RETURNED,
            nameOf(staff), staff == null ? null : staff.getId());
        log(journey, null, "PROPOSAL_RETURNED");
        notifier.proposalReturned(journey);

        return PortalOutcome.ok("Sent back to the customer.", journey);
    }

    // ----------------------------------------------------------------- the plumbing

    /** Every client action asks the same question, so it is asked in one place. */
    private PortalOutcome clientMayAct(PortalJourney journey) {
        if (journey == null || !journey.isOpenable()) {
            return PortalOutcome.refuse("This link is not available.", "PORTAL_LINK_UNAVAILABLE", journey);
        }
        if (!journey.isActionable()) {
            return PortalOutcome.refuse(
                "This booking is on hold with us at the moment. Please get in touch.",
                "JOURNEY_LOCKED", journey);
        }
        if (journey.getProposalState() != null && journey.getProposalState().isFinal()) {
            return PortalOutcome.refuse(
                "This has already been " + journey.getProposalState().getDisplayName().toLowerCase()
                    + ". Get in touch and we will pick it up from there.",
                "PROPOSAL_FINAL", journey);
        }
        return null;
    }

    private void move(PortalJourney journey, ProposalState to, User heldBy) {
        journey.setProposalState(to);
        journey.setProposalStateAt(LocalDateTime.now());
        journey.setHeldByUserId(heldBy == null ? null : heldBy.getId());
        if (heldBy == null) journey.setHeldFromRevision(null);
        journeys.save(journey);
    }

    /**
     * Write down what the quotation said at the handover.
     *
     * <p>Null is tolerated. A snapshot that cannot be written is a gap in the history; a handover
     * that fails because of it leaves the pen in nobody's hand, with one side waiting for the
     * other. The gap is logged at error inside {@code QuoteRevisionService}.
     */
    private QuoteRevision record(PortalJourney journey, RevisionAuthor author,
                                 RevisionReason reason, String name, Long userId) {
        Quote quote = journey.getQuote();
        if (quote == null) return null;
        return revisions.record(quote, author, reason, name, userId, journey);
    }

    private void log(PortalJourney journey, PortalParticipant by, String section) {
        log(journey, by, section, null);
    }

    private void log(PortalJourney journey, PortalParticipant by, String section, String detail) {
        activity.record(journey, by, null, section, detail, null, null);
    }

    private String nameOf(PortalParticipant participant) {
        if (participant == null) return null;
        return participant.getDisplayName() != null && !participant.getDisplayName().isBlank()
            ? participant.getDisplayName()
            : participant.getEmail();
    }

    private String nameOf(User user) {
        if (user == null) return null;
        String full = ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
            + (user.getLastName() == null ? "" : user.getLastName())).trim();
        return full.isBlank() ? user.getUsername() : full;
    }

    /** Null for a portal caller, which is correct: no client is a user of this application. */
    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) return null;
        Object principal = authentication.getPrincipal();
        return principal instanceof User user ? user : null;
    }
}
