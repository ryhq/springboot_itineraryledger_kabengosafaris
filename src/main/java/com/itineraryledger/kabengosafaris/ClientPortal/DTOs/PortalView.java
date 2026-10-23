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
        String privacyHtml,
        String privacyVersion
    ) {}

    /** The trip itself. The open tier: no money anywhere in it. */
    public record Trip(
        String name,
        String code,
        String summary,
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
    public record Quote(
        String code,
        String title,
        String status,
        LocalDate validFrom,
        LocalDate validTo,
        boolean expired,
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
