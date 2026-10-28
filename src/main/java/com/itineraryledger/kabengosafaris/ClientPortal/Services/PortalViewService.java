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
import com.itineraryledger.kabengosafaris.ClientPortal.DTOs.PortalOutcome;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayActivity.Entity.QuoteDayActivity;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalJourney;
import com.itineraryledger.kabengosafaris.ClientPortal.Enums.ProposalState;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalDeviceRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationImageRepository;
import com.itineraryledger.kabengosafaris.CompanyProfile.Services.CompanyIdentityService;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.Repository.ItineraryImageRepository;
import com.itineraryledger.kabengosafaris.Park.Repositories.ParkImageRepository;
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
    private final PortalEditingService editing;
    private final PortalDeviceRepository devices;
    private final IdObfuscator idObfuscator;
    private final ItineraryImageRepository itineraryImages;
    private final AccommodationImageRepository accommodationImages;
    private final ParkImageRepository parkImages;

    /**
     * This API's own public address, for building image URLs.
     *
     * <p>The portal is served from a different hostname, so a relative path would resolve
     * against that one and every photograph would be a broken box.
     */
    @org.springframework.beans.factory.annotation.Value("${app.base.url:}")
    private String apiBaseUrl;

    @Transactional(readOnly = true)
    public PortalView.Page build(PortalAccessService.Access access) {
        PortalJourney journey = access.journey();
        boolean verified = access.canReadPrivate();

        LegalDocument privacy = legal.published(LegalDocumentType.PRIVACY_POLICY).orElse(null);
        CompanyIdentityService.Snapshot brand = company.snapshot();

        return new PortalView.Page(
            brand.name(),
            brandCss(brand),
            brand.logoUrl(),
            journey.getToken(),
            verified,
            verified ? access.participant().getDisplayName() : null,
            trip(journey),
            verified ? quote(journey.getQuote(), journey) : null,
            verified ? invoice(journey.getInvoice()) : null,
            verified ? documents(journey) : List.of(),
            verified ? devices(journey, access.device()) : List.of(),
            verified ? proposal(access) : null,
            privacy == null ? null : legal.renderHtml(privacy.getBody()),
            privacy == null ? null : privacy.getVersion());
    }

    /**
     * Where the quotation stands, in the client's terms.
     *
     * <p>The sentences matter more than they look. A client who presses a button and is refused
     * has learnt that the page is unreliable; a client who is told "we are looking at this and
     * will come back to you" has been answered. Same state, same two seconds, opposite feeling
     * about the company.
     *
     * <p>The booleans come from the state machine itself rather than being inferred here, so
     * what the page offers and what the API will accept cannot drift apart.
     */
    private PortalView.Proposal proposal(PortalAccessService.Access access) {
        PortalJourney journey = access.journey();
        ProposalState state = journey.getProposalState();
        if (state == null) return null;

        boolean lead = access.participant() != null && access.participant().mayAct();
        boolean actionable = journey.isActionable();

        String sentence = switch (state) {
            case OPEN_TO_CLIENT -> "This is with you. Tell us when you are happy with it.";
            case SUBMITTED -> "We have it and will come back to you. You can still make changes "
                + "until we start looking at it.";
            case WITH_OPERATOR -> "We are working on this. You will be able to make changes "
                + "again when we send it back.";
            case RETURNED -> "This is back with you, with our answer.";
            case AGREED -> "Agreed. We will confirm everything and tell you what happens next.";
            case DECLINED -> "You have told us this is not for you. Get in touch any time.";
        };

        return new PortalView.Proposal(
            state.name(),
            state.getDisplayName(),
            sentence,
            lead && actionable && state.clientMayEdit(),
            lead && actionable && state.clientMayDecide(),
            lead);
    }

    /**
     * The company's colours, as CSS custom properties, built here rather than in the template.
     *
     * <p>The template engine cannot do this. Its CSS inlining escapes what it writes, so a hex
     * colour comes out as {@code \#1c7a58}, which is not a colour: every property silently
     * fails and the page renders with no accent and invisible buttons. Nothing logs it, because
     * from the server's point of view the page rendered perfectly.
     *
     * <p>Each value is checked against what a colour can look like before it goes anywhere near
     * a page. The company record is edited by staff rather than by the public, so this is not
     * the first line of defence; it is the one that means a typo produces the default instead of
     * a broken stylesheet, and that nothing typed into a settings field can close a style block.
     */
    private String brandCss(CompanyIdentityService.Snapshot brand) {
        return ":root{"
            + "--accent:" + colour(brand.accent(), "#1f6f4a") + ";"
            + "--accent-dark:" + colour(brand.accentDark(), "#164c34") + ";"
            + "--accent-soft:" + colour(brand.accentSoft(), "#f1f6f3") + ";"
            + "}";
    }

    /** A hex, rgb() or named colour, or the fallback. Nothing else reaches the page. */
    private String colour(String value, String fallback) {
        if (value == null) return fallback;
        String trimmed = value.trim();
        return trimmed.matches("#[0-9a-fA-F]{3,8}|rgba?\\([0-9.,%\\s]+\\)|[a-zA-Z]{3,20}")
            ? trimmed
            : fallback;
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
                hero(itinerary, days),
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
            hero(itinerary, List.of()),
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
            day.getDayNumber(), date, dayImage(day), day.getTitle(), day.getDescription(),
            day.getMorningActivities(), day.getAfternoonActivities(), day.getEveningActivities(),
            day.getMealsIncluded(), Boolean.TRUE.equals(day.getIsOvernight()), stay, parks);
    }

    /**
     * The photograph at the top of the page.
     *
     * <p>The itinerary's own primary image if it has one, because that is the picture the office
     * chose for this trip. Otherwise the first day that has one, so a product nobody has
     * photographed still looks like somewhere rather than like a form.
     */
    private String hero(Itinerary itinerary, List<PortalView.Day> days) {
        if (itinerary != null) {
            String own = itineraryImages.findPrimaryByItineraryId(itinerary.getId())
                .filter(image -> Boolean.TRUE.equals(image.getIsActive()))
                .map(image -> imageUrl("itinerary-images", image.getFileName()))
                .orElse(null);
            if (own != null) return own;
        }
        return days.stream()
            .map(PortalView.Day::imageUrl)
            .filter(java.util.Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    /**
     * A picture for one day: where they sleep, or failing that where they are.
     *
     * <p>The lodge first, because that is the thing a client is choosing between and the thing
     * they want to see. The park second. Only PUBLISHED image modules are used, which is why
     * these can be plain {@code src} attributes on a page: those photographs are already on the
     * public website. Client documents are the opposite and are streamed through the API.
     */
    private String dayImage(QuoteDay day) {
        if (day.getAccommodations() != null) {
            for (QuoteDayAccommodation stay : day.getAccommodations()) {
                if (Boolean.TRUE.equals(stay.getIsAlternative())) continue;
                if (stay.getAccommodation() == null) continue;
                String url = accommodationImages
                    .findPrimaryByAccommodationId(stay.getAccommodation().getId())
                    .filter(image -> Boolean.TRUE.equals(image.getIsActive()))
                    .map(image -> imageUrl("accommodation-images", image.getFileName()))
                    .orElse(null);
                if (url != null) return url;
            }
        }
        if (day.getParks() != null) {
            for (QuoteDayPark park : day.getParks()) {
                if (park.getPark() == null) continue;
                String url = parkImages.findPrimaryByParkId(park.getPark().getId())
                    .filter(image -> Boolean.TRUE.equals(image.getIsActive()))
                    .map(image -> imageUrl("park-images", image.getFileName()))
                    .orElse(null);
                if (url != null) return url;
            }
        }
        return null;
    }

    /**
     * Absolute, because the portal is served from a different hostname than this API.
     *
     * <p>By file name rather than by id: an obfuscated id is re-salted on every restart, so a
     * URL built from one stops resolving after the next deploy, and a page that was fine
     * yesterday fills with broken images.
     */
    private String imageUrl(String module, String fileName) {
        if (fileName == null || apiBaseUrl == null || apiBaseUrl.isBlank()) return null;
        return apiBaseUrl.replaceAll("/+$", "") + "/api/" + module + "/file/" + fileName;
    }

    // ----------------------------------------------------------------- the money

    private PortalView.Quote quote(Quote quote, PortalJourney journey) {
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
        /*
         * The one method the edits themselves ask, so a control cannot be drawn that then
         * refuses. A client who decides what they want and is then told no has been treated
         * worse than one who was never offered the choice.
         */
        PortalOutcome refusal = editing.whyNotEditable(journey);

        return new PortalView.Quote(
            quote.getQuoteCode(), quote.getTitle(),
            quote.getStatus() == null ? null : quote.getStatus().getDisplayName(),
            quote.getValidFrom(), quote.getValidTo(), expired,
            refusal == null,
            refusal == null ? null : refusal.message(),
            dayChoices(quote),
            partyLines(quote),
            travellers == 0 ? null : travellers,
            totals, perGuest, included, notIncluded, quote.getCustomerNotes());
    }

    /**
     * What they may choose between, night by night.
     *
     * <p>Only days that actually offer a choice appear. A day with one lodge and no optional
     * activity has nothing to decide, and a list of fourteen days of which two are clickable
     * buries the two.
     */
    private List<PortalView.DayChoice> dayChoices(Quote quote) {
        List<PortalView.DayChoice> choices = new ArrayList<>();
        if (quote.getDays() == null) return choices;

        for (QuoteDay day : quote.getDays()) {
            List<PortalView.StayOption> stays = new ArrayList<>();
            if (day.getAccommodations() != null) {
                for (QuoteDayAccommodation stay : day.getAccommodations()) {
                    stays.add(new PortalView.StayOption(
                        idObfuscator.encodeId(stay.getId()),
                        stay.getAccommodation() == null ? null : stay.getAccommodation().getName(),
                        stay.getRoomType() == null ? null : stay.getRoomType().getName(),
                        stay.getBoardType() == null ? null : stay.getBoardType().getName(),
                        stay.getRoomCount(),
                        !Boolean.TRUE.equals(stay.getIsAlternative())));
                }
            }

            List<PortalView.ActivityOption> optional = new ArrayList<>();
            if (day.getActivities() != null) {
                for (QuoteDayActivity act : day.getActivities()) {
                    if (!Boolean.TRUE.equals(act.getIsOptional())) continue;
                    optional.add(new PortalView.ActivityOption(
                        idObfuscator.encodeId(act.getId()),
                        act.getActivity() == null ? null : act.getActivity().getName(),
                        Boolean.TRUE.equals(act.getIsIncludedInPrice())));
                }
            }

            boolean worthShowing = stays.size() > 1 || !optional.isEmpty()
                || (stays.size() == 1 && stays.get(0).rooms() != null);
            if (!worthShowing) continue;

            choices.add(new PortalView.DayChoice(
                day.getDayNumber(), day.getTitle(), stays, optional));
        }
        return choices;
    }

    /**
     * The travelling party, one line per kind of traveller.
     *
     * <p>The count is theirs to change; the kind is not. Which nationality band and which age
     * band apply decides which tariff is charged, and a client guessing at it produces a price
     * neither side can explain afterwards.
     */
    private List<PortalView.PartyLine> partyLines(Quote quote) {
        List<PortalView.PartyLine> lines = new ArrayList<>();
        if (quote.getPaxList() == null) return lines;

        for (QuotePax pax : quote.getPaxList()) {
            String nation = pax.getNationCategory() == null ? "" : pax.getNationCategory().getName();
            String age = pax.getAgeCategory() == null ? "" : pax.getAgeCategory().getName();
            lines.add(new PortalView.PartyLine(
                idObfuscator.encodeId(pax.getId()),
                (nation + " " + age).trim(),
                pax.getCount()));
        }
        return lines;
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
