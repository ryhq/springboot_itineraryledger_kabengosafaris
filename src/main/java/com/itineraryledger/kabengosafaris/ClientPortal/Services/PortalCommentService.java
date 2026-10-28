package com.itineraryledger.kabengosafaris.ClientPortal.Services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.ClientPortal.DTOs.PortalComment;
import com.itineraryledger.kabengosafaris.ClientPortal.DTOs.PortalOutcome;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalJourney;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalParticipant;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.Entity.QuoteProposalComment;
import com.itineraryledger.kabengosafaris.Quote.Enums.CommentAnchor;
import com.itineraryledger.kabengosafaris.Quote.Enums.RevisionAuthor;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.Entity.QuoteDay;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayAccommodation.Entity.QuoteDayAccommodation;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayActivity.Entity.QuoteDayActivity;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteProposalCommentRepository;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices.QuoteRevisionService;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The conversation attached to a quotation, part by part.
 *
 * <p>Commenting is deliberately <strong>not</strong> gated on who holds the pen. Editing is
 * about not overwriting each other's work; a question is never a conflict, and a client who can
 * see their quotation but is told they may not ask about it has been given a document with a
 * closed door beside it. So the only gates here are: the link works, they are verified, and
 * what they are pointing at is on their own booking.
 *
 * <p>The two read paths are the part worth being careful about. The office sees everything; the
 * client sees what is marked visible. That filter lives in the repository query rather than
 * here, so a caller cannot forget it.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortalCommentService {

    /**
     * Longer than a sentence, shorter than an essay.
     *
     * <p>Not arbitrary: beyond this somebody is writing an email, and an email is better written
     * in an email client where they can keep a copy. A hard limit also stops a text column being
     * a place to paste a megabyte.
     */
    private static final int LONGEST_COMMENT = 4000;

    private final QuoteProposalCommentRepository comments;
    private final QuoteRevisionService revisions;
    private final PortalActivityService activity;
    private final IdObfuscator idObfuscator;

    // ----------------------------------------------------------------- the client

    /**
     * Something the client wants to say about one part of their trip.
     *
     * <p>Always visible, and always recorded as theirs. There is no private option, because
     * there is nowhere for a client to keep a secret from the only other party, and a checkbox
     * suggesting otherwise would be a lie told by a form.
     */
    @Transactional
    public PortalOutcome addFromClient(PortalJourney journey, PortalParticipant by,
                                       String anchorName, Integer dayNumber, Long targetId,
                                       String body) {
        if (journey == null || !journey.isOpenable()) {
            return PortalOutcome.refuse("This link is not available.",
                "PORTAL_LINK_UNAVAILABLE", journey);
        }

        Quote quote = journey.getQuote();
        if (quote == null) {
            return PortalOutcome.refuse("There is no quotation to comment on yet.",
                "NO_QUOTE", journey);
        }

        String text = body == null ? "" : body.trim();
        if (text.isEmpty()) {
            return PortalOutcome.refuse("Write something first.", "EMPTY_COMMENT", journey);
        }
        if (text.length() > LONGEST_COMMENT) {
            return PortalOutcome.refuse(
                "That is longer than this box takes. Send it to us by email and we will put it "
                    + "on the file.",
                "COMMENT_TOO_LONG", journey);
        }

        CommentAnchor anchor = anchorOf(anchorName);
        Target target = resolve(quote, anchor, dayNumber, targetId);
        if (target == null) {
            /* the same answer as a row that does not exist; neither reveals the other */
            return PortalOutcome.refuse("That is not on this booking.",
                "NOT_ON_THIS_BOOKING", journey);
        }

        comments.save(QuoteProposalComment.builder()
            .quote(quote)
            .journey(journey)
            .revisionNumber(revisionNumberOf(quote))
            .anchor(anchor)
            .dayNumber(target.dayNumber())
            .targetId(target.id())
            .targetLabel(target.label())
            .body(text)
            .authorKind(RevisionAuthor.CLIENT)
            .authorName(nameOf(by))
            .visibleToClient(true)
            .build());

        activity.record(journey, by, null, "COMMENT_ADDED", target.label(), null, null);

        return PortalOutcome.ok("Thank you. We can see that beside the day you meant.", journey);
    }

    /** What the client may read: everything either side has said in the open. */
    @Transactional(readOnly = true)
    public List<PortalComment> forClient(Quote quote) {
        if (quote == null || quote.getId() == null) return List.of();
        return view(comments.findVisibleToClient(quote.getId()), false);
    }

    // ----------------------------------------------------------------- the office

    /** Something we want to say, to them or about it. */
    @Transactional
    public QuoteProposalComment addFromStaff(Quote quote, PortalJourney journey,
                                             String anchorName, Integer dayNumber, Long targetId,
                                             String body, boolean visibleToClient,
                                             String authorName, Long authorUserId) {
        if (quote == null) return null;

        String text = body == null ? "" : body.trim();
        if (text.isEmpty() || text.length() > LONGEST_COMMENT) return null;

        CommentAnchor anchor = anchorOf(anchorName);
        Target target = resolve(quote, anchor, dayNumber, targetId);
        if (target == null) return null;

        return comments.save(QuoteProposalComment.builder()
            .quote(quote)
            .journey(journey)
            .revisionNumber(revisionNumberOf(quote))
            .anchor(anchor)
            .dayNumber(target.dayNumber())
            .targetId(target.id())
            .targetLabel(target.label())
            .body(text)
            .authorKind(RevisionAuthor.STAFF)
            .authorName(authorName)
            .authorUserId(authorUserId)
            .visibleToClient(visibleToClient)
            .build());
    }

    /** Everything, including what we said among ourselves. */
    @Transactional(readOnly = true)
    public List<PortalComment> forOffice(Quote quote) {
        if (quote == null || quote.getId() == null) return List.of();
        return view(comments.findByQuoteIdOrderByIdAsc(quote.getId()), true);
    }

    /** Dealt with. Kept rather than deleted, because the thread is the record of the review. */
    @Transactional
    public boolean resolve(Quote quote, Long commentId, String by, boolean resolved) {
        QuoteProposalComment comment = comments.findById(commentId).orElse(null);
        if (comment == null || quote == null || comment.getQuote() == null
                || !comment.getQuote().getId().equals(quote.getId())) {
            return false;
        }
        comment.setResolvedAt(resolved ? LocalDateTime.now() : null);
        comment.setResolvedBy(resolved ? by : null);
        comments.save(comment);
        return true;
    }

    public long openCount(Quote quote) {
        return quote == null || quote.getId() == null ? 0
            : comments.countByQuoteIdAndResolvedAtIsNull(quote.getId());
    }

    // ----------------------------------------------------------------- the plumbing

    /** What a comment turned out to be about, once we had checked it was theirs. */
    private record Target(Integer dayNumber, Long id, String label) {}

    /**
     * Prove the thing being pointed at is on this quotation, and name it ourselves.
     *
     * <p>The label is resolved here rather than accepted from the caller. A client who could
     * supply it could write "Kudu Lodge" against a day that has no Kudu Lodge, and the record of
     * the conversation would then disagree with the quotation it is about.
     */
    private Target resolve(Quote quote, CommentAnchor anchor, Integer dayNumber, Long targetId) {
        if (anchor == CommentAnchor.WHOLE) {
            return new Target(null, null, null);
        }

        QuoteDay day = dayOf(quote, dayNumber);
        if (day == null) return null;

        if (anchor == CommentAnchor.DAY) {
            return new Target(day.getDayNumber(), null, dayLabel(day));
        }

        if (targetId == null) return null;

        if (day.getAccommodations() != null) {
            for (QuoteDayAccommodation stay : day.getAccommodations()) {
                if (targetId.equals(stay.getId())) {
                    return new Target(day.getDayNumber(), stay.getId(),
                        stay.getAccommodation() == null ? dayLabel(day)
                            : stay.getAccommodation().getName());
                }
            }
        }
        if (day.getActivities() != null) {
            for (QuoteDayActivity act : day.getActivities()) {
                if (targetId.equals(act.getId())) {
                    return new Target(day.getDayNumber(), act.getId(),
                        act.getActivity() == null ? dayLabel(day) : act.getActivity().getName());
                }
            }
        }
        return null;
    }

    private QuoteDay dayOf(Quote quote, Integer dayNumber) {
        if (dayNumber == null || quote.getDays() == null) return null;
        for (QuoteDay day : quote.getDays()) {
            if (dayNumber.equals(day.getDayNumber())) return day;
        }
        return null;
    }

    private String dayLabel(QuoteDay day) {
        return day.getTitle() == null || day.getTitle().isBlank()
            ? "Day " + day.getDayNumber() : day.getTitle();
    }

    private CommentAnchor anchorOf(String name) {
        if (name == null) return CommentAnchor.WHOLE;
        try {
            return CommentAnchor.valueOf(name.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return CommentAnchor.WHOLE;
        }
    }

    private Integer revisionNumberOf(Quote quote) {
        return revisions.latest(quote.getId())
            .map(r -> r.getRevisionNumber())
            .orElse(null);
    }

    private String nameOf(PortalParticipant participant) {
        if (participant == null) return null;
        return participant.getDisplayName() != null && !participant.getDisplayName().isBlank()
            ? participant.getDisplayName() : participant.getEmail();
    }

    /**
     * @param withVisibility true for the office. A client's copy carries no visibility flag at
     *                       all: every row they can see is visible by construction, and a field
     *                       that is always true is a field somebody will later make meaningful.
     */
    private List<PortalComment> view(List<QuoteProposalComment> rows, boolean withVisibility) {
        List<PortalComment> out = new ArrayList<>();
        for (QuoteProposalComment c : rows) {
            out.add(new PortalComment(
                idObfuscator.encodeId(c.getId()),
                c.getAnchor() == null ? null : c.getAnchor().name(),
                c.getDayNumber(),
                c.getTargetLabel(),
                c.getBody(),
                c.getAuthorKind() == null ? null : c.getAuthorKind().name(),
                c.getAuthorName(),
                c.getRevisionNumber(),
                c.isResolved(),
                withVisibility ? Boolean.TRUE.equals(c.getVisibleToClient()) : null,
                c.getCreatedAt()));
        }
        return out;
    }
}
