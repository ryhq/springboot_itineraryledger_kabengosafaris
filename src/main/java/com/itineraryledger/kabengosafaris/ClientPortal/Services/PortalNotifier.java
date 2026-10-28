package com.itineraryledger.kabengosafaris.ClientPortal.Services;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.Backup.BackupSettings.BackupSettingsGetterServices;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalJourney;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalParticipant;
import com.itineraryledger.kabengosafaris.EmailEvent.Services.CustomerAcknowledgementSender;
import com.itineraryledger.kabengosafaris.EmailEvent.Services.EmailTemplateRenderer;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountServices.EmailSendingService;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Telling somebody that the pen has moved.
 *
 * <p>Without this the review loop is a loop nobody is in. A client who submits sees a sentence
 * on a page and nothing else happens anywhere; a member of staff who takes over leaves a
 * customer looking at controls that have quietly stopped working. Every state change in
 * {@link PortalProposalService} has somebody waiting on the other side of it, and this is the
 * only thing that reaches them.
 *
 * <p>Two audiences and therefore two mechanisms, deliberately. A client's message goes through
 * {@code CustomerAcknowledgementSender}, which translates it into their own language and tracks
 * delivery. An office message goes to the notification addresses, rendered and sent directly,
 * because translating an internal alert into a customer's language would be absurd and because
 * the office has no single address on the customer record to acknowledge to.
 *
 * <p><strong>Nothing here may fail a handover.</strong> The state change is the event; the email
 * is how somebody hears about it. A client whose submission is refused because our mail server
 * is unreachable has been given the worst of both. Every method swallows and logs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortalNotifier {

    private static final DateTimeFormatter WHEN =
        DateTimeFormatter.ofPattern("d MMMM yyyy 'at' HH:mm");

    private final CustomerAcknowledgementSender sender;
    private final EmailTemplateRenderer renderer;
    private final EmailSendingService mail;
    private final BackupSettingsGetterServices notificationSettings;
    private final PortalAccessService access;
    private final PortalChangeLogService changeLog;

    @Value("${app.management.base.url:}")
    private String managementBaseUrl;

    // ----------------------------------------------------------------- to the office

    /** They have asked us to look. Carries what they changed, which decides who picks it up. */
    public void proposalSubmitted(PortalJourney journey) {
        Map<String, String> variables = officeVariables(journey);
        variables.put("changeSummary", latestChanges(journey));
        variables.put("submittedAt", WHEN.format(LocalDateTime.now()));

        toTheOffice("PORTAL_PROPOSAL_SUBMITTED", journey,
            "Proposal waiting: " + customerNameOf(journey), variables);
    }

    /**
     * They have accepted.
     *
     * <p>The one that must not be missed. Pressing Accept does not move the quotation's own
     * status, so if this does not arrive a customer who has said yes is sitting in a state
     * nobody here is watching.
     */
    public void quoteAgreed(PortalJourney journey) {
        Map<String, String> variables = officeVariables(journey);
        variables.put("agreedAt", WHEN.format(LocalDateTime.now()));

        toTheOffice("PORTAL_QUOTE_AGREED", journey,
            "Accepted: " + customerNameOf(journey), variables);
    }

    /** They have declined. The reason is usually the most useful line in the message. */
    public void quoteDeclined(PortalJourney journey, String reason) {
        Map<String, String> variables = officeVariables(journey);
        variables.put("declineReason", reason == null ? "" : reason);
        variables.put("declinedAt", WHEN.format(LocalDateTime.now()));

        toTheOffice("PORTAL_QUOTE_DECLINED", journey,
            "Declined: " + customerNameOf(journey), variables);
    }

    // ----------------------------------------------------------------- to the client

    /** We have taken it over, so their controls have stopped working. Say so before they find out. */
    public void proposalInReview(PortalJourney journey, String consultantName) {
        PortalParticipant lead = journey.lead();
        if (lead == null) return;

        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("recipientName", nameOf(lead));
        variables.put("tripName", tripNameOf(journey));
        /*
         * Always supplied. This renderer has no inverted sections, so a variable left out of the
         * map prints as itself: a customer would read "{{consultantName}} started working on...".
         */
        variables.put("consultantName",
            consultantName == null || consultantName.isBlank() ? "We have" : consultantName + " has");
        variables.put("portalUrl", access.linkFor(journey));

        toTheClient("PORTAL_PROPOSAL_IN_REVIEW", journey, lead,
            "We are working on your proposal", variables);
    }

    /** It is back with them. Leads with what we changed, which is what they will look for. */
    public void proposalReturned(PortalJourney journey) {
        PortalParticipant lead = journey.lead();
        if (lead == null) return;

        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("recipientName", nameOf(lead));
        variables.put("tripName", tripNameOf(journey));
        variables.put("quoteTotal", totalOf(journey.getQuote()));
        variables.put("changeSummary", latestChanges(journey));
        variables.put("portalUrl", access.linkFor(journey));

        toTheClient("PORTAL_PROPOSAL_RETURNED", journey, lead,
            "Your proposal is back with you", variables);
    }

    // ----------------------------------------------------------------- the plumbing

    private void toTheOffice(String event, PortalJourney journey, String subject,
                             Map<String, String> variables) {
        try {
            List<String> recipients = notificationSettings.getNotificationEmails();
            if (recipients.isEmpty()) {
                log.warn("Nobody is configured to receive {}; the office will not hear about "
                    + "journey {}", event, journey.getId());
                return;
            }

            String html = renderer.renderTemplate(event, variables);
            for (String address : recipients) {
                try {
                    mail.sendHtmlEmail(address, subject, html);
                } catch (Exception e) {
                    log.warn("Could not send {} to {}: {}", event, address, e.toString());
                }
            }
        } catch (Exception e) {
            /* the handover already happened; this is how somebody hears about it, not whether */
            log.error("Could not tell the office about {} on journey {}: {}",
                event, journey.getId(), e.toString());
        }
    }

    private void toTheClient(String event, PortalJourney journey, PortalParticipant lead,
                             String subject, Map<String, String> variables) {
        try {
            sender.send(event, lead.getEmail(), subject, variables, languageOf(journey));
        } catch (Exception e) {
            log.error("Could not tell {} about {} on journey {}: {}",
                lead.getEmail(), event, journey.getId(), e.toString());
        }
    }

    private Map<String, String> officeVariables(PortalJourney journey) {
        Quote quote = journey.getQuote();
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("customerName", customerNameOf(journey));
        variables.put("tripName", tripNameOf(journey));
        variables.put("quoteCode", quote == null ? "" : orEmpty(quote.getQuoteCode()));
        variables.put("quoteTotal", totalOf(quote));
        variables.put("panelUrl", panelLinkFor(quote));
        return variables;
    }

    /**
     * What moved in the most recent round, as lines.
     *
     * <p>Plain text rather than markup. The sentences are built from staff-entered lodge and
     * activity names, and a change list is the one place in these messages where our own data
     * is interpolated into a customer's email; the template lays it out with {@code pre-line}
     * so nothing here has to be trusted as HTML.
     */
    private String latestChanges(PortalJourney journey) {
        try {
            List<PortalChangeLogService.Round> rounds = changeLog.forQuote(journey.getQuote());
            for (int i = rounds.size() - 1; i >= 0; i--) {
                if (rounds.get(i).changes().isEmpty()) continue;
                StringBuilder lines = new StringBuilder();
                for (PortalChangeLogService.Change change : rounds.get(i).changes()) {
                    if (lines.length() > 0) lines.append('\n');
                    lines.append(change.text());
                }
                return lines.toString();
            }
        } catch (Exception e) {
            log.warn("Could not summarise the changes on journey {}: {}",
                journey.getId(), e.toString());
        }
        /* empty rather than "no changes": the template drops the whole block when it is blank */
        return "";
    }

    /** Straight to the quotation's Portal tab, so nobody has to search for the code. */
    private String panelLinkFor(Quote quote) {
        if (quote == null || managementBaseUrl == null || managementBaseUrl.isBlank()) return "";
        return managementBaseUrl.replaceAll("/+$", "") + "/quotes";
    }

    private String totalOf(Quote quote) {
        if (quote == null || quote.getGrandTotals() == null) return "";
        for (Price total : quote.getGrandTotals()) {
            if (total == null || total.getTotalPrice() == null) continue;
            return String.format("%s %,.2f", orEmpty(total.getCurrency()), total.getTotalPrice());
        }
        return "";
    }

    private String customerNameOf(PortalJourney journey) {
        return journey.getCustomer() == null ? "A customer"
            : orEmpty(journey.getCustomer().getFullName());
    }

    private String tripNameOf(PortalJourney journey) {
        if (journey.getItinerary() != null && journey.getItinerary().getName() != null) {
            return journey.getItinerary().getName();
        }
        return journey.getQuote() == null ? "" : orEmpty(journey.getQuote().getTitle());
    }

    private String nameOf(PortalParticipant participant) {
        String name = participant.getDisplayName();
        return name == null || name.isBlank() ? "there" : name;
    }

    private String languageOf(PortalJourney journey) {
        return journey.getCustomer() == null ? null : journey.getCustomer().getPreferredLanguage();
    }

    private String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
