package com.itineraryledger.kabengosafaris.Inclusion.Services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.Inclusion.Entity.InclusionItem;
import com.itineraryledger.kabengosafaris.Inclusion.Entity.InclusionsSource;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Invoice.InvoiceInclusion.Entity.InvoiceInclusion;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryInclusion.Entity.ItineraryInclusion;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.QuoteInclusion.Entity.QuoteInclusion;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.SafariInclusion.Entity.SafariInclusion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The chain: Itinerary → Quotation → Safari → Invoice.
 *
 * <p>Each document copies from the one immediately above it and owns what it has from that moment.
 * Add two lines on a quote and the itinerary still has five, the quote has seven, and the safari
 * built from that quote inherits seven — not the itinerary's five. That is the whole point: the
 * promise is negotiated on the quote, and what the customer agreed to is what the trip must be
 * delivered and billed against.
 *
 * <p>Every copy takes the wording as <strong>text</strong>, not a foreign key. A key would mean one
 * {@code UPDATE inclusion_items SET label = …} rewrote every document ever sent, including ones
 * already rendered to PDF and emailed — the customer's copy and ours would disagree, and ours would
 * be the one that looked altered.
 *
 * <p>Disabled catalogue lines are dropped at the moment of copying, and never afterwards. A line
 * taken out of service stops reaching NEW paperwork immediately; paperwork already produced keeps
 * its own copy and is untouched.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InclusionSnapshotService {

    /* ---------------------------------------------------------------- quote */

    /** Itinerary → Quote, at generation. */
    public int itineraryToQuote(Itinerary itinerary, Quote quote) {
        if (itinerary == null || quote == null) return 0;

        quote.getInclusionList().clear();
        int order = 1;
        for (ItineraryInclusion row : safe(itinerary.getInclusionList())) {
            InclusionItem item = row.getInclusionItem();
            if (!usable(item)) continue;
            quote.addInclusion(QuoteInclusion.builder()
                .inclusionItem(item)
                .label(item.getLabel())
                .category(item.getCategory())
                .claimAppliesTo(item.getClaimAppliesTo())
                .isIncluded(row.getIsIncluded())
                .sortOrder(order++)
                .build());
        }
        stamp(quote::setInclusionsSource, quote::setInclusionsSyncedAt);
        return order - 1;
    }

    /* --------------------------------------------------------------- safari */

    /**
     * Quote → Safari.
     *
     * <p>The one that will be written wrong if anybody hurries. A safari created from a quote must
     * read the QUOTE's rows, not {@code quote.getItinerary()}'s: the two disagree the moment
     * somebody edits the quote, and the quote is what the customer accepted. The safari template
     * already has this mistake elsewhere — {@code full_safari_modern.html:718} renders
     * {@code safari.itinerary.highlightsList} — so it is one copy-paste away.
     */
    public int quoteToSafari(Quote quote, Safari safari) {
        if (quote == null || safari == null) return 0;

        safari.getInclusionList().clear();
        int order = 1;
        for (QuoteInclusion row : safe(quote.getInclusionList())) {
            safari.addInclusion(SafariInclusion.builder()
                .inclusionItem(row.getInclusionItem())
                .label(row.getLabel())
                .category(row.getCategory())
                .claimAppliesTo(row.getClaimAppliesTo())
                .isIncluded(row.getIsIncluded())
                .sortOrder(order++)
                .build());
        }
        stamp(safari::setInclusionsSource, safari::setInclusionsSyncedAt);
        return order - 1;
    }

    /** Itinerary → Safari, for a trip booked without a quote. */
    public int itineraryToSafari(Itinerary itinerary, Safari safari) {
        if (itinerary == null || safari == null) return 0;

        safari.getInclusionList().clear();
        int order = 1;
        for (ItineraryInclusion row : safe(itinerary.getInclusionList())) {
            InclusionItem item = row.getInclusionItem();
            if (!usable(item)) continue;
            safari.addInclusion(SafariInclusion.builder()
                .inclusionItem(item)
                .label(item.getLabel())
                .category(item.getCategory())
                .claimAppliesTo(item.getClaimAppliesTo())
                .isIncluded(row.getIsIncluded())
                .sortOrder(order++)
                .build());
        }
        stamp(safari::setInclusionsSource, safari::setInclusionsSyncedAt);
        return order - 1;
    }

    /* -------------------------------------------------------------- invoice */

    /**
     * Safari → Invoice, falling back to the quote.
     *
     * <p>The fallback is for trips booked before the chain existed, whose safari has no rows. It
     * must not throw on those: an invoice that cannot be raised is worse than one that states a
     * little less than it might.
     */
    public int toInvoice(Safari safari, Quote quote, Invoice invoice) {
        if (invoice == null) return 0;

        invoice.getInclusionList().clear();
        int order = 1;

        List<SafariInclusion> fromSafari = safari == null ? List.of() : safe(safari.getInclusionList());
        if (!fromSafari.isEmpty()) {
            for (SafariInclusion row : fromSafari) {
                invoice.addInclusion(InvoiceInclusion.builder()
                    .inclusionItem(row.getInclusionItem())
                    .label(row.getLabel())
                    .category(row.getCategory())
                    .claimAppliesTo(row.getClaimAppliesTo())
                    .isIncluded(row.getIsIncluded())
                    .sortOrder(order++)
                    .build());
            }
        } else {
            for (QuoteInclusion row : quote == null ? List.<QuoteInclusion>of() : safe(quote.getInclusionList())) {
                invoice.addInclusion(InvoiceInclusion.builder()
                    .inclusionItem(row.getInclusionItem())
                    .label(row.getLabel())
                    .category(row.getCategory())
                    .claimAppliesTo(row.getClaimAppliesTo())
                    .isIncluded(row.getIsIncluded())
                    .sortOrder(order++)
                    .build());
            }
        }

        if (order > 1) stamp(invoice::setInclusionsSource, invoice::setInclusionsSyncedAt);
        return order - 1;
    }

    /* --------------------------------------------------------------- shared */

    /**
     * A line is carried forward only while its catalogue entry is enabled.
     *
     * <p>Checked at copy time and nowhere else. Checking it on read would mean disabling a line
     * silently blanked it on quotes already sent, which is the opposite of what a snapshot is for.
     */
    private boolean usable(InclusionItem item) {
        return item != null
            && !Boolean.FALSE.equals(item.getIsActive())
            && item.getLabel() != null
            && !item.getLabel().isBlank();
    }

    private <T> List<T> safe(List<T> list) {
        return list == null ? List.of() : list;
    }

    private void stamp(
        java.util.function.Consumer<InclusionsSource> source,
        java.util.function.Consumer<LocalDateTime> syncedAt
    ) {
        source.accept(InclusionsSource.INHERITED);
        syncedAt.accept(LocalDateTime.now());
    }
}
