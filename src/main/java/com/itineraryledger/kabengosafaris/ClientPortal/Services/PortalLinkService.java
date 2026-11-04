package com.itineraryledger.kabengosafaris.ClientPortal.Services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalJourney;
import com.itineraryledger.kabengosafaris.Feature.Feature;
import com.itineraryledger.kabengosafaris.Feature.FeatureService;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The one way the rest of the application asks for a customer's own link.
 *
 * <p>Until now there was no way at all. Sharing a quotation created the journey and the link sat
 * in the panel waiting for somebody to copy it into a message by hand, while the history said
 * "Sent to the customer" beside a view count of nought. Two readings of the same screen: the
 * customer is ignoring us, and nobody ever told the customer.
 *
 * <p>So the quotation email carries it. That is the message the customer is already expecting,
 * it already carries the PDF of the same quotation, and it goes to the address the access code
 * will go to.
 *
 * <p><strong>Nothing here may stop a quotation being sent.</strong> The email is the thing that
 * matters; the link is an addition to it. The feature may be off, the customer may have no
 * address, the portal may be misconfigured — in every case this answers with an empty string and
 * the email goes out exactly as it did before. An operator finding out that the portal is
 * misconfigured by having a customer's quotation fail to send would be a far worse outcome than
 * the link being absent from it.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortalLinkService {

    private final FeatureService features;
    private final PortalJourneyService journeys;
    private final PortalAccessService access;

    /**
     * The customer's link for this quotation, creating it if this is the first time.
     *
     * <p>Publishing here rather than requiring somebody to have pressed Share first, because a
     * variable that is sometimes filled in and sometimes not is the kind of thing nobody notices
     * is empty until a customer asks where their trip page is. Publishing is idempotent: a
     * quotation already shared keeps the address the customer may already hold, which matters
     * because two links to one trip is how somebody ends up reading last month's price.
     *
     * @return the address, or an empty string when there is not one to give
     */
    @Transactional
    public String linkFor(Quote quote) {
        try {
            if (quote == null) return "";
            if (!features.isEnabled(Feature.CLIENT_PORTAL)) return "";

            PortalJourney journey = journeys.publishFor(quote);
            if (journey == null) {
                /* already logged by publishFor, which knows why */
                return "";
            }

            String link = access.linkFor(journey);
            return link == null ? "" : link;
        } catch (Exception e) {
            log.warn("Could not work out the portal link for quote {}: {}",
                quote == null ? "?" : quote.getQuoteCode(), e.toString());
            return "";
        }
    }
}
