package com.itineraryledger.kabengosafaris.ClientPortal.DTOs;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Everything the client's own pages are allowed to know.
 *
 * <p>Written from scratch rather than reusing {@code FullQuoteDTO} or {@code QuoteDayTreeDTO},
 * and that is the whole point of the file. Those two carry {@code internalNotes} on the quote and
 * on every day, {@code agentCommissionPercentage}, {@code marginUpliftPercentage},
 * {@code isStoRate} and the per-night operational notes staff write to each other. Any of them
 * reaching a client is a disclosure, and reusing a rich DTO "and not printing the bad fields" is
 * a promise that lasts until the next person adds a field to a template.
 *
 * <p>So these records have no field that could hold a cost. There is nothing to leave out,
 * nothing to remember, and {@code NoPortalResponseCarriesOurCostTest} fails the build if a name
 * from that family appears here.
 *
 * <p>Records rather than classes: a client view is a snapshot assembled per request, and nothing
 * should be able to mutate one halfway down a template.
 */
public final class PortalView {

    private PortalView() {}

    /** The whole page model, with whichever parts this caller has earned. */
    public record Page(
        String companyName,
        /**
         * The brand as a block of CSS custom properties, built and validated in Java.
         *
         * <p>Not interpolated into the stylesheet by the template engine. Thymeleaf's CSS
         * inlining escapes the output, so {@code #1c7a58} arrives as {@code \#1c7a58}, every
         * custom property is invalid, and the result is a page whose buttons are white text on
         * no background: invisible, with nothing in any log to say why.
         */
        String brandCss,
        String logoUrl,
        String journeyToken,
        boolean verified,
        String participantName,
        Trip trip,
        Quote quote,
        Invoice invoice,
        List<Document> documents,
        List<Device> devices,
        Proposal proposal,
        String privacyHtml,
        String privacyVersion
    ) {}

    /**
     * Where this quotation stands, and what the client may do about it right now.
     *
     * <p>The three booleans are computed here rather than inferred from {@code state} by the
     * page. A front end that works out for itself when a button is allowed is a second copy of
     * the rule, and the second copy is the one that goes stale: a button offered when the answer
     * is no produces an error the client cannot act on, and one withheld when the answer is yes
     * produces a telephone call.
     */
    public record Proposal(
        String state,
        String stateLabel,
        /** What to tell the client it means, in their terms rather than ours. */
        String sentence,
        boolean mayEdit,
        boolean mayDecide,
        /** False for an invited reader, who may read everything and decide nothing. */
        boolean isLead
    ) {}

    /** The trip itself. The open tier: no money anywhere in it. */
    public record Trip(
        String name,
        String code,
        String summary,
        /**
         * The photograph at the top of the page.
         *
         * <p>An absolute URL to a PUBLISHED image module, which is why it can be a plain src: a
         * park or accommodation photograph is already on the public website and carries nothing
         * private. Client DOCUMENTS are the opposite and are streamed through the API instead.
         */
        String heroImageUrl,
        Integer totalDays,
        Integer totalNights,
        LocalDate startDate,
        LocalDate endDate,
        String startLocation,
        String endLocation,
        List<Day> days
    ) {}

    public record Day(
        Integer dayNumber,
        LocalDate date,
        /** the night's lodge, or failing that the park they are in. Published media only */
        String imageUrl,
        String title,
        String description,
        String morning,
        String afternoon,
        String evening,
        String meals,
        boolean overnight,
        /** Where they sleep, by name. Not the rate, not the board cost, not the supplier. */
        String stay,
        List<String> parks
    ) {}

    /**
     * The quotation, as the client is entitled to see it.
     *
     * <p>Published figures only. There is no field here for operator cost, margin, commission or
     * uplift, because there is no reading of the brief under which a client sees those.
     */
    /**
     * One night's options: what they have, and what else we offered for it.
     *
     * <p>On the quotation rather than on the trip, because the trip page is the open tier and
     * the alternatives are part of what was quoted. Ids are obfuscated and are minted by this
     * run of the application for this page; they are a way of naming a row in the next request,
     * never a claim that the row is theirs. That is proved again on the way back in.
     */
    public record DayChoice(
        Integer dayNumber,
        String dayTitle,
        List<StayOption> stays,
        /** Optional ones only. What is part of the trip is not a line to be deleted. */
        List<ActivityOption> activities
    ) {}

    public record StayOption(
        String id,
        String name,
        String roomType,
        String board,
        Integer rooms,
        boolean chosen
    ) {}

    public record ActivityOption(String id, String name, boolean included) {}

    /** One kind of traveller and how many of them. The count is theirs; the kind is not. */
    public record PartyLine(String id, String label, Integer count) {}

    public record Quote(
        String code,
        String title,
        String status,
        LocalDate validFrom,
        LocalDate validTo,
        boolean expired,
        /**
         * Whether the controls are drawn at all, and why not when they are not.
         *
         * <p>Both come from the one method the edits themselves ask, so a control cannot appear
         * and then refuse.
         */
        boolean editable,
        String notEditableBecause,
        /**
         * Whether this client may reach past the alternatives on their quotation.
         *
         * <p>Carried so the page knows whether to draw the control at all. The endpoint answers
         * an empty list either way, but a button that reliably produces nothing is a button
         * that teaches people the page is broken.
         */
        boolean catalogue,
        List<DayChoice> dayChoices,
        List<PartyLine> party,
        Integer travellers,
        List<Money> totals,
        List<Money> perGuest,
        List<String> included,
        List<String> notIncluded,
        String notes
    ) {}

    public record Invoice(
        String code,
        String status,
        LocalDate dueDate,
        List<Money> total,
        List<Money> paid,
        List<Money> balance,
        boolean settled
    ) {}

    /** One figure in one currency, already formatted, so no template has to decide. */
    public record Money(String currency, BigDecimal amount, String formatted) {}

    public record Document(
        String id,
        String title,
        String kind,
        LocalDateTime createdAt
    ) {}

    /**
     * A browser that has been trusted.
     *
     * <p>Shown to the client with its address and when it was last used, because a trusted device
     * is only safe to the extent that the person it belongs to can see it and end it.
     */
    public record Device(
        String id,
        String label,
        String ipAddress,
        LocalDateTime lastSeenAt,
        boolean current
    ) {}
}
