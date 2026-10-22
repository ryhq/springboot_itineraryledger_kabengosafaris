package com.itineraryledger.kabengosafaris.ClientPortal.Services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.ClientPortal.DTOs.PortalView;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalDevice;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalJourney;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalDeviceRepository;
import com.itineraryledger.kabengosafaris.CompanyProfile.Services.CompanyIdentityService;
import com.itineraryledger.kabengosafaris.Inclusion.DTOs.InclusionLineDTO;
import com.itineraryledger.kabengosafaris.Inclusion.Services.InclusionSnapshotService;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceServices.InvoicePaymentAggregationService;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Legal.Entity.LegalDocument;
import com.itineraryledger.kabengosafaris.Legal.Entity.LegalDocumentType;
import com.itineraryledger.kabengosafaris.Legal.Services.LegalDocumentService;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.Entity.QuoteDay;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayAccommodation.Entity.QuoteDayAccommodation;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.Entity.QuoteDayPark;
import com.itineraryledger.kabengosafaris.Quote.QuotePax.Entity.QuotePax;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Turning a journey into the pages a client is allowed to see.
 *
 * <p>Two tiers, decided here and nowhere else. The <strong>trip</strong> is assembled for anybody
 * holding the link, because a client showing their family where they are going should not have to
 * perform a verification first, and there is no money in it. Everything with a figure or a
 * personal detail on it is assembled only when {@code canReadPrivate} is true.
 *
 * <p>Every field is copied across by hand into {@link PortalView}, which has no field that could
 * hold an operator cost. That is deliberate and slightly tedious: the alternative is handing a
 * template a rich DTO and trusting every future edit of that template not to print
 * {@code internalNotes}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortalViewService {

    private final CompanyIdentityService company;
    private final InclusionSnapshotService inclusions;
    private final InvoicePaymentAggregationService payments;
    private final LegalDocumentService legal;
    private final PortalDeviceRepository devices;
    private final IdObfuscator idObfuscator;

    @Transactional(readOnly = true)
    public PortalView.Page build(PortalAccessService.Access access) {
        PortalJourney journey = access.journey();
        boolean verified = access.canReadPrivate();

        LegalDocument privacy = legal.published(LegalDocumentType.PRIVACY_POLICY).orElse(null);
        CompanyIdentityService.Snapshot brand = company.snapshot();

        return new PortalView.Page(
            brand.name(),
            brand.accent(),
            brand.accentDark(),
            brand.accentSoft(),
            brand.logoUrl(),
            journey.getToken(),
            verified,
            verified ? access.participant().getDisplayName() : null,
            trip(journey),
            verified ? quote(journey.getQuote()) : null,
            verified ? invoice(journey.getInvoice()) : null,
            verified ? documents(journey) : List.of(),
            verified ? devices(journey, access.device()) : List.of(),
            privacy == null ? null : legal.renderHtml(privacy.getBody()),
            privacy == null ? null : privacy.getVersion());
    }

    // ----------------------------------------------------------------- the trip

    /**
     * The days, from the quotation's own snapshot where there is one.
     *
     * <p>A quote carries a full copy of the itinerary it was built from, and that copy is what
     * this client is actually being sold: their rooms, their chosen lodges. Showing the blueprint
     * instead would show them somebody else's trip.
     */
    private PortalView.Trip trip(PortalJourney journey) {
        Quote quote = journey.getQuote();
        Itinerary itinerary = journey.getItinerary();

        if (quote != null && quote.getDays() != null && !quote.getDays().isEmpty()) {
            LocalDate start = quote.getSafariStartDate();
            List<PortalView.Day> days = new ArrayList<>();
            for (QuoteDay day : quote.getDays()) {
                days.add(dayOf(day, start));
            }
            return new PortalView.Trip(
                itinerary == null ? quote.getTitle() : itinerary.getName(),
                itinerary == null ? null : itinerary.getCode(),
                itinerary == null ? quote.getDescription() : itinerary.getDescription(),
                days.size(),
                Math.max(0, days.size() - 1),
                start,
                start == null ? null : start.plusDays(Math.max(0, days.size() - 1)),
                itinerary == null ? null : itinerary.getStartLocation(),
                itinerary == null ? null : itinerary.getEndLocation(),
                days);
        }

        if (itinerary == null) return null;
        /* No snapshot: show the product, which is at least honest about what they enquired on. */
        return new PortalView.Trip(
            itinerary.getName(), itinerary.getCode(), itinerary.getDescription(),
            itinerary.getTotalDays(), itinerary.getTotalNights(),
            null, null, itinerary.getStartLocation(), itinerary.getEndLocation(), List.of());
    }

    private PortalView.Day dayOf(QuoteDay day, LocalDate start) {
        String stay = null;
        if (day.getAccommodations() != null) {
            for (QuoteDayAccommodation accommodation : day.getAccommodations()) {
                /*
                 * The booked one only. The alternatives are a commercial conversation, not a
                 * promise, and listing them on a client's own trip page reads as a choice they
                 * have already been given.
                 */
                if (Boolean.TRUE.equals(accommodation.getIsAlternative())) continue;
                if (accommodation.getAccommodation() != null) {
                    stay = accommodation.getAccommodation().getName();
                    break;
                }
            }
        }

        List<String> parks = new ArrayList<>();
        if (day.getParks() != null) {
            for (QuoteDayPark park : day.getParks()) {
                if (park.getPark() != null && park.getPark().getName() != null) {
                    parks.add(park.getPark().getName());
                }
            }
        }

        LocalDate date = (start == null || day.getDayNumber() == null)
            ? null : start.plusDays(day.getDayNumber() - 1L);

        /*
         * specialNotes is NOT here, and that is not an oversight. It is the field the office
         * writes "confirm before quoting" into, and it has already reached a client once on a
         * printed itinerary.
         */
        return new PortalView.Day(
            day.getDayNumber(), date, day.getTitle(), day.getDescription(),
            day.getMorningActivities(), day.getAfternoonActivities(), day.getEveningActivities(),
            day.getMealsIncluded(), Boolean.TRUE.equals(day.getIsOvernight()), stay, parks);
    }

    // ----------------------------------------------------------------- the money

    private PortalView.Quote quote(Quote quote) {
        if (quote == null) return null;

        int travellers = 0;
        if (quote.getPaxList() != null) {
            for (QuotePax pax : quote.getPaxList()) {
                travellers += pax.getCount() == null ? 0 : pax.getCount();
            }
        }

        List<PortalView.Money> totals = money(quote.getGrandTotals());
        List<PortalView.Money> perGuest = new ArrayList<>();
        if (travellers > 0) {
            for (Price price : safe(quote.getGrandTotals())) {
                if (price.getTotalPrice() == null) continue;
                BigDecimal each = price.getTotalPrice()
                    .divide(BigDecimal.valueOf(travellers), 2, RoundingMode.HALF_UP);
                perGuest.add(new PortalView.Money(price.getCurrency(), each,
                    format(price.getCurrency(), each)));
            }
        }

        List<String> included = new ArrayList<>();
        List<String> notIncluded = new ArrayList<>();
        for (InclusionLineDTO line : inclusions.linesOf(quote)) {
            (line.isIncluded() ? included : notIncluded).add(line.getLabel());
        }

        boolean expired = quote.getValidTo() != null && quote.getValidTo().isBefore(LocalDate.now());

        /*
         * customerNotes, never internalNotes. The two sit side by side on the entity and differ
         * by one word in the getter.
         */
        return new PortalView.Quote(
            quote.getQuoteCode(), quote.getTitle(),
            quote.getStatus() == null ? null : quote.getStatus().getDisplayName(),
            quote.getValidFrom(), quote.getValidTo(), expired,
            travellers == 0 ? null : travellers,
            totals, perGuest, included, notIncluded, quote.getCustomerNotes());
    }

    private PortalView.Invoice invoice(Invoice invoice) {
        if (invoice == null) return null;

        List<PortalView.Money> balances = money(payments.computeBalances(invoice));
        boolean settled = balances.stream()
            .allMatch(m -> m.amount() == null || m.amount().signum() <= 0);

        return new PortalView.Invoice(
            invoice.getInvoiceCode(),
            invoice.getStatus() == null ? null : invoice.getStatus().getDisplayName(),
            invoice.getDueDate(),
            money(invoice.getGrandTotals()),
            money(payments.computeAmountsPaid(invoice)),
            balances,
            settled);
    }

    // ----------------------------------------------------------------- the rest

    private List<PortalView.Document> documents(PortalJourney journey) {
        List<PortalView.Document> out = new ArrayList<>();

        if (journey.getQuote() != null && journey.getQuote().getDocuments() != null) {
            journey.getQuote().getDocuments().stream()
                .filter(document -> Boolean.TRUE.equals(document.getIsActive()))
                .forEach(document -> out.add(new PortalView.Document(
                    idObfuscator.encodeId(document.getId()),
                    document.getTitle(),
                    "QUOTE",
                    document.getCreatedAt())));
        }
        return out;
    }

    private List<PortalView.Device> devices(PortalJourney journey, PortalDevice current) {
        List<PortalView.Device> out = new ArrayList<>();
        for (PortalDevice device : devices.findByJourneyId(journey.getId())) {
            if (!device.isUsable()) continue;
            out.add(new PortalView.Device(
                idObfuscator.encodeId(device.getId()),
                device.getLabel(),
                device.getIpAddress(),
                device.getLastSeenAt(),
                current != null && current.getId().equals(device.getId())));
        }
        return out;
    }

    private List<PortalView.Money> money(List<Price> prices) {
        List<PortalView.Money> out = new ArrayList<>();
        for (Price price : safe(prices)) {
            out.add(new PortalView.Money(price.getCurrency(), price.getTotalPrice(),
                format(price.getCurrency(), price.getTotalPrice())));
        }
        return out;
    }

    private List<Price> safe(List<Price> prices) {
        return prices == null ? List.of() : prices;
    }

    /** Grouped thousands, two decimals, the code in front. No locale guessing. */
    private String format(String currency, BigDecimal amount) {
        if (amount == null) return null;
        return (currency == null ? "" : currency + " ")
            + String.format("%,.2f", amount.setScale(2, RoundingMode.HALF_UP));
    }
}
