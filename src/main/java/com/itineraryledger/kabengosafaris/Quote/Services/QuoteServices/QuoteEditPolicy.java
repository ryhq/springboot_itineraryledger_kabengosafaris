package com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalJourney;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalJourneyRepository;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.Enums.QuoteStatus;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.Entity.QuoteDay;

import lombok.RequiredArgsConstructor;

/**
 * One answer to "may this quotation be changed", for every caller that changes one.
 *
 * <p>Until this existed the question had two answers. {@code QuoteUpdateService} refused to edit
 * an ACCEPTED or CONVERTED quote, while {@code PUT /api/quote-day-accommodations/{id}} and its
 * three siblings checked nothing at all — so the header of a converted quotation was frozen and
 * its prices were not. A safari already sold could be silently re-priced by editing the lodge on
 * day four, and the quotation the customer agreed to would no longer say what they agreed to.
 *
 * <p>That hole has to close before a client can be handed any editing of their own, because a
 * portal lock is unenforceable while a second door stands open. It is worth closing regardless.
 *
 * <p>The decision folds together five things that were scattered or nowhere:
 * <ul>
 *   <li><b>status</b> — a decided quotation is a record of what was decided</li>
 *   <li><b>superseded</b> — a quote with a {@code nextVersion} has been replaced; edit the one
 *       that replaced it, or the two disagree and nobody can say which was sent</li>
 *   <li><b>isActive</b> — withdrawn from use</li>
 *   <li><b>frozen</b> — the operator's override on the client's link</li>
 *   <li><b>with the operator</b> — staff have taken the pen; a client edit arriving now would
 *       overwrite a review already in progress</li>
 * </ul>
 *
 * <p>{@code isValid} is deliberately NOT here. An expired validity date means the price needs
 * revisiting, which is a reason to edit a quotation rather than a reason to forbid it.
 */
@Service
@RequiredArgsConstructor
public class QuoteEditPolicy {

    private final PortalJourneyRepository journeys;

    /**
     * Who is asking. The same quotation is open to one and closed to the other at several
     * points, and a policy that cannot tell them apart has to pick the laxer answer for both.
     */
    public enum Actor {
        /** somebody signed in to the panel */
        STAFF,
        /** somebody holding a portal link */
        CLIENT
    }

    /**
     * The answer, with the sentence to show for it.
     *
     * <p>A bare boolean would make every caller invent its own wording for a refusal, and the
     * reason is the useful half: "this quotation has been converted to a safari" tells somebody
     * what to do next, and "cannot edit" does not.
     */
    public record Decision(boolean allowed, String message, String code) {

        public static Decision allow() {
            return new Decision(true, null, null);
        }

        public static Decision refuse(String message, String code) {
            return new Decision(false, message, code);
        }
    }

    /** The statuses that mean a decision has been recorded and must not move under it. */
    private static boolean isDecided(QuoteStatus status) {
        return status == QuoteStatus.ACCEPTED
            || status == QuoteStatus.REJECTED
            || status == QuoteStatus.EXPIRED
            || status == QuoteStatus.CANCELLED
            || status == QuoteStatus.CONVERTED;
    }

    /**
     * May this quotation's day tree — its accommodations, activities, parks and flights — change?
     *
     * <p>Separate from editing the header because the day tree is what the price is computed
     * from. A title can be tidied on a sent quotation without changing what anybody owes; a
     * lodge cannot.
     */
    public Decision canEditDayTree(Quote quote, Actor actor) {
        if (quote == null) {
            return Decision.refuse("This quotation no longer exists", "QUOTE_NOT_FOUND");
        }

        if (isDecided(quote.getStatus())) {
            return Decision.refuse(
                String.format("This quotation is %s, so its prices cannot change. "
                    + "Raise a new version if something has to move.",
                    quote.getStatus().getDisplayName().toLowerCase()),
                "QUOTE_DECIDED");
        }

        if (quote.getNextVersion() != null) {
            return Decision.refuse(
                "A newer version of this quotation has replaced it. Edit that one instead.",
                "QUOTE_SUPERSEDED");
        }

        if (Boolean.FALSE.equals(quote.getIsActive())) {
            return Decision.refuse(
                "This quotation has been withdrawn from use.",
                "QUOTE_INACTIVE");
        }

        return portalDecision(quote, actor);
    }

    /**
     * What the client's link says, which only ever narrows the answer above.
     *
     * <p>A quotation with no portal journey is the normal case and reaches none of this. Frozen
     * stops both sides, because freezing is what an operator does when a request smells wrong
     * and a staff edit at that moment is as likely to be the mistake as the client's. Whether
     * the client in particular may act is the proposal state's business, and that is where the
     * rule about not overwriting a review in progress lives.
     */
    private Decision portalDecision(Quote quote, Actor actor) {
        PortalJourney journey = quote.getId() == null ? null
            : journeys.findByQuoteId(quote.getId()).orElse(null);

        if (journey == null) return Decision.allow();

        if (Boolean.TRUE.equals(journey.getLocked())) {
            return Decision.refuse(
                "This quotation is frozen while it is looked at. Unfreeze it on the Portal tab.",
                "QUOTE_FROZEN");
        }

        if (actor == Actor.CLIENT
            && (journey.getProposalState() == null || !journey.getProposalState().clientMayEdit())) {
            return Decision.refuse(
                "We have started reviewing this. You will be able to make changes again once we "
                    + "have sent it back to you.",
                "WITH_OPERATOR");
        }

        return Decision.allow();
    }

    /**
     * The refusal to return straight from a controller-facing service, or null to carry on.
     *
     * <p>A policy handing back an HTTP response is a compromise, and a deliberate one: the
     * alternative is the same four lines of unwrapping repeated at fourteen call sites, and
     * fourteen chances to word the refusal differently or forget it. Two lines at the call site
     * is a guard somebody will actually add to the fifteenth.
     */
    public ResponseEntity<ApiResponse<?>> refusalFor(QuoteDay day, Actor actor) {
        return asRefusal(canEditDayTree(day, actor));
    }

    /** The same, for a caller that already holds the quotation. */
    public ResponseEntity<ApiResponse<?>> refusalFor(Quote quote, Actor actor) {
        return asRefusal(canEditDayTree(quote, actor));
    }

    private ResponseEntity<ApiResponse<?>> asRefusal(Decision decision) {
        return decision.allowed() ? null
            : ResponseEntity.badRequest()
                .body(ApiResponse.error(400, decision.message(), decision.code()));
    }

    /** The same question starting from a row of the day tree, which is how callers hold it. */
    public Decision canEditDayTree(QuoteDay day, Actor actor) {
        if (day == null || day.getQuote() == null) {
            return Decision.refuse("This day no longer belongs to a quotation", "QUOTE_NOT_FOUND");
        }
        return canEditDayTree(day.getQuote(), actor);
    }
}
