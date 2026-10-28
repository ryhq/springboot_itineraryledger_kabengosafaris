package com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalJourney;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.FullItineraryDTO;
import com.itineraryledger.kabengosafaris.Quote.DTOs.QuoteSnapshot;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.Entity.QuoteRevision;
import com.itineraryledger.kabengosafaris.Quote.Enums.RevisionAuthor;
import com.itineraryledger.kabengosafaris.Quote.Enums.RevisionReason;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteRevisionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Taking the photograph, and finding an old one.
 *
 * <p>Every snapshot in this application comes from here, so there is one answer to "what did the
 * quotation say then" and one place where the projection is chosen. The projection is
 * {@link QuoteCostEstimationService#buildSyntheticDTO(Quote)}, which is also what the price is
 * computed from, so the history and the figures cannot drift apart.
 *
 * <p>Snapshots are taken at handovers only. See {@link RevisionReason}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuoteRevisionService {

    /**
     * How many times to retry a number that somebody else took first.
     *
     * <p>Two people pressing a button in the same second is rare and entirely possible: a client
     * submitting at the moment a member of staff takes over. The unique key is what stops them
     * both becoming version 4, and this is what stops the loser being an error on somebody's
     * screen.
     */
    private static final int NUMBER_ATTEMPTS = 3;

    private final QuoteRevisionRepository revisions;
    private final QuoteCostEstimationService costEstimation;
    private final ObjectMapper objectMapper;

    /**
     * The quotation as it stands right now, unsaved.
     *
     * <p>What the last recorded round is compared against to answer "what has changed since you
     * last saw this", which is the question the portal actually asks. Reads lazy collections, so
     * the caller holds a transaction.
     */
    @Transactional(readOnly = true)
    public QuoteSnapshot current(Quote quote) {
        if (quote == null) return null;
        FullItineraryDTO projection = costEstimation.buildSyntheticDTO(quote);
        return QuoteSnapshot.of(projection, quote);
    }

    /**
     * Write down what the quotation says, and why we are writing it down.
     *
     * @return the revision, or null if it could not be written
     *
     * <p>Null rather than an exception, and the callers carry on. A handover is a real event with
     * a consequence either side is waiting on; failing it because the history could not be written
     * would leave the pen in nobody's hand, which is strictly worse than a gap in the history. The
     * gap is logged at error so it is not a silent one.
     */
    @Transactional
    public QuoteRevision record(Quote quote, RevisionAuthor takenBy, RevisionReason reason,
                                String authorName, Long authorUserId, PortalJourney journey) {
        if (quote == null || quote.getId() == null) return null;

        String json;
        try {
            json = objectMapper.writeValueAsString(current(quote));
        } catch (Exception e) {
            log.error("Could not snapshot quote {} for a {} revision: {}",
                quote.getQuoteCode(), reason, e.toString());
            return null;
        }

        for (int attempt = 1; attempt <= NUMBER_ATTEMPTS; attempt++) {
            Integer highest = revisions.highestNumberFor(quote.getId());
            int next = highest == null ? 1 : highest + 1;
            try {
                QuoteRevision saved = revisions.saveAndFlush(QuoteRevision.builder()
                    .quote(quote)
                    .journey(journey)
                    .revisionNumber(next)
                    .takenBy(takenBy)
                    .reason(reason)
                    .authorName(authorName)
                    .authorUserId(authorUserId)
                    .snapshot(json)
                    .build());
                log.info("Quote {} revision {} recorded: {} by {}",
                    quote.getQuoteCode(), next, reason, takenBy);
                return saved;
            } catch (DataIntegrityViolationException race) {
                if (attempt == NUMBER_ATTEMPTS) {
                    log.error("Quote {} revision {} lost {} races for its number",
                        quote.getQuoteCode(), next, NUMBER_ATTEMPTS);
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Make sure there is something to compare against.
     *
     * <p>Called when a quotation is shared. Without a baseline the first change a client makes has
     * no before, so the first thing they are shown about their own edit is nothing at all.
     * Idempotent: sharing twice, or pointing an existing link at a newer quotation, does not write
     * a second baseline.
     */
    @Transactional
    public QuoteRevision ensureBaseline(Quote quote, PortalJourney journey) {
        if (quote == null || quote.getId() == null) return null;
        if (revisions.existsByQuoteId(quote.getId())) {
            return revisions.findFirstByQuoteIdOrderByRevisionNumberDesc(quote.getId()).orElse(null);
        }
        return record(quote, RevisionAuthor.SYSTEM, RevisionReason.PUBLISHED,
            null, null, journey);
    }

    /** Every round of this quotation, oldest first. */
    @Transactional(readOnly = true)
    public List<QuoteRevision> history(Long quoteId) {
        return quoteId == null ? List.of() : revisions.findByQuoteIdOrderByRevisionNumberAsc(quoteId);
    }

    @Transactional(readOnly = true)
    public Optional<QuoteRevision> latest(Long quoteId) {
        return quoteId == null ? Optional.empty() : revisions.findFirstByQuoteIdOrderByRevisionNumberDesc(quoteId);
    }

    @Transactional(readOnly = true)
    public Optional<QuoteRevision> numbered(Long quoteId, Integer number) {
        if (quoteId == null || number == null) return Optional.empty();
        return revisions.findByQuoteIdAndRevisionNumber(quoteId, number);
    }

    /**
     * Read a stored snapshot back.
     *
     * <p>Returns null rather than throwing when the JSON cannot be read, because one unreadable
     * row from an older shape of this record must not take the whole change list down with it.
     */
    public QuoteSnapshot read(QuoteRevision revision) {
        if (revision == null || revision.getSnapshot() == null) return null;
        try {
            return objectMapper.readValue(revision.getSnapshot(), QuoteSnapshot.class);
        } catch (Exception e) {
            log.warn("Quote revision {} holds a snapshot this version cannot read: {}",
                revision.getId(), e.toString());
            return null;
        }
    }
}
