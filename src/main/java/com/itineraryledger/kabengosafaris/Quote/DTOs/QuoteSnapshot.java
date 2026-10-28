package com.itineraryledger.kabengosafaris.Quote.DTOs;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.itineraryledger.kabengosafaris.Itinerary.DTOs.FullItineraryDTO;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;

/**
 * What a quotation said at one moment, in a form that still means something a year later.
 *
 * <p>Built from {@link FullItineraryDTO}, which is what
 * {@code QuoteCostEstimationService.buildSyntheticDTO} produces and what the pricing engine reads.
 * One projection feeds the price and the history, so the sentence "the total went up because the
 * lodge changed" cannot be contradicted by the figures beside it.
 *
 * <p><strong>No identifiers, anywhere.</strong> The projection is full of obfuscated ids and every
 * one of them is dropped here. This installation mints a fresh Hashids salt on each restart unless
 * a deployment fixes one, so a stored id decodes to a different row after the next deploy and to
 * nothing after the row is gone. A snapshot has to survive both. Everything is therefore named,
 * and two snapshots are compared by day number and by name, which is also how a person compares
 * them.
 *
 * <p>It is also narrower than the projection on purpose: no cost, no margin, no supplier, no
 * internal note. A snapshot is served to the client as well as to the office, and a field that
 * cannot hold our cost cannot leak it.
 */
public record QuoteSnapshot(
    String quoteCode,
    Integer vehicles,
    List<Party> party,
    List<Day> days,
    List<Money> money
) {

    /** One line of the travelling party: so many of this nationality at this age. */
    public record Party(String nationality, String ageGroup, Integer count) {}

    /** One day of the trip, with everything on it that a client would notice changing. */
    public record Day(
        Integer number,
        String title,
        String endLocation,
        List<Stay> stays,
        List<Activity> activities,
        List<Park> parks,
        List<Flight> flights
    ) {}

    /**
     * A night somewhere.
     *
     * <p>{@code alternative} is carried because the alternatives are the menu at editing level
     * CHOOSE_OFFERED: a client swapping to one of them changes which row is the alternative, and
     * a diff that ignored the flag would report the swap as two unrelated removals.
     */
    public record Stay(
        String accommodation,
        String roomType,
        String roomStandard,
        String boardType,
        Integer rooms,
        boolean alternative
    ) {}

    /** Something done on a day, whether we are charging for it or not. */
    public record Activity(String name, boolean included, boolean optional) {}

    /** A park, and what was booked inside it. */
    public record Park(String name, List<String> activities, List<String> tariffs) {}

    /** A sector. Named by its endpoints, because that is what changes when the trip does. */
    public record Flight(String airline, String from, String to, Integer seats) {}

    /** The figures, per currency, exactly as the quotation carries them. */
    public record Money(String currency, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {}

    /**
     * Take the photograph.
     *
     * @param projection the quote's own day tree, from {@code buildSyntheticDTO}
     * @param quote      the quotation itself, which is where the totals live
     */
    public static QuoteSnapshot of(FullItineraryDTO projection, Quote quote) {
        List<Party> party = new ArrayList<>();
        if (projection != null && projection.getPaxList() != null) {
            for (FullItineraryDTO.PaxDTO p : projection.getPaxList()) {
                party.add(new Party(p.getNationCategoryName(), p.getAgeCategoryName(), p.getCount()));
            }
        }

        List<Day> days = new ArrayList<>();
        if (projection != null && projection.getDays() != null) {
            for (FullItineraryDTO.DayDTO d : projection.getDays()) {
                days.add(dayOf(d));
            }
        }

        return new QuoteSnapshot(
            quote == null ? null : quote.getQuoteCode(),
            projection == null ? null : projection.getCarCount(),
            party,
            days,
            moneyOf(quote));
    }

    private static Day dayOf(FullItineraryDTO.DayDTO d) {
        List<Stay> stays = new ArrayList<>();
        if (d.getAccommodations() != null) {
            for (FullItineraryDTO.DayAccommodationDTO a : d.getAccommodations()) {
                stays.add(new Stay(
                    a.getAccommodationName(),
                    a.getRoomTypeName(),
                    a.getRoomStandardName(),
                    a.getBoardTypeName(),
                    a.getRoomCount(),
                    Boolean.TRUE.equals(a.getIsAlternative())));
            }
        }

        List<Activity> activities = new ArrayList<>();
        if (d.getActivities() != null) {
            for (FullItineraryDTO.DayActivityDTO a : d.getActivities()) {
                activities.add(new Activity(
                    a.getActivityName(),
                    Boolean.TRUE.equals(a.getIsIncludedInPrice()),
                    Boolean.TRUE.equals(a.getIsOptional())));
            }
        }

        List<Park> parks = new ArrayList<>();
        if (d.getParks() != null) {
            for (FullItineraryDTO.DayParkDTO p : d.getParks()) {
                List<String> inside = new ArrayList<>();
                if (p.getActivities() != null) {
                    for (FullItineraryDTO.ParkActivityDTO pa : p.getActivities()) {
                        inside.add(pa.getActivityName());
                    }
                }
                List<String> tariffs = new ArrayList<>();
                if (p.getTariffs() != null) {
                    for (FullItineraryDTO.ParkTariffDTO pt : p.getTariffs()) {
                        tariffs.add(pt.getTariffName());
                    }
                }
                parks.add(new Park(p.getParkName(), inside, tariffs));
            }
        }

        List<Flight> flights = new ArrayList<>();
        if (d.getFlights() != null) {
            for (FullItineraryDTO.DayFlightDTO f : d.getFlights()) {
                flights.add(new Flight(
                    f.getAirlineName(), f.getOriginCode(), f.getDestinationCode(),
                    f.getPassengerCount()));
            }
        }

        return new Day(d.getDayNumber(), d.getTitle(), d.getEndLocation(),
            stays, activities, parks, flights);
    }

    /**
     * The totals, one line per currency.
     *
     * <p>Driven off the grand totals rather than off the subtotals, because a quotation always has
     * a grand total in every currency it prices in, and may have a discount in none of them.
     */
    private static List<Money> moneyOf(Quote quote) {
        List<Money> money = new ArrayList<>();
        if (quote == null || quote.getGrandTotals() == null) return money;

        for (Price total : quote.getGrandTotals()) {
            if (total == null || total.getCurrency() == null) continue;
            money.add(new Money(
                total.getCurrency(),
                amountIn(quote.getSubtotals(), total.getCurrency()),
                amountIn(quote.getDiscounts(), total.getCurrency()),
                total.getTotalPrice()));
        }
        return money;
    }

    private static BigDecimal amountIn(List<Price> prices, String currency) {
        if (prices == null) return null;
        return prices.stream()
            .filter(p -> p != null && currency.equals(p.getCurrency()))
            .map(Price::getTotalPrice)
            .findFirst()
            .orElse(null);
    }
}
