package com.itineraryledger.kabengosafaris.Quote;

import com.itineraryledger.kabengosafaris.Quote.DTOs.FullQuoteDTO;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices.QuoteStayGrouper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A quotation holds nights; a lodge books stays.
 *
 * <p>Three nights at one camp is one reservation, and sending it as three is how a property ends
 * up holding three one-night bookings for the same party. The grouping that turns one into the
 * other is pure arithmetic, so it is pinned here rather than discovered on a rooming list.
 */
class AQuotationGroupsItsNightsIntoStaysTest {

    private static FullQuoteDTO.DayStayDTO room(String lodge, String type, int count, int sleeps) {
        return FullQuoteDTO.DayStayDTO.builder()
            .accommodationName(lodge)
            .roomTypeName(type)
            .roomStandardName("Standard")
            .boardTypeName("Full Board")
            .roomCount(count)
            .sleeps(sleeps)
            .build();
    }

    private static FullQuoteDTO.DayDTO night(String date, FullQuoteDTO.DayStayDTO... rooms) {
        return FullQuoteDTO.DayDTO.builder()
            .date(LocalDate.parse(date))
            .isOvernight(true)
            .accommodations(List.of(rooms))
            .build();
    }

    private static FullQuoteDTO.DayDTO departureDay(String date) {
        return FullQuoteDTO.DayDTO.builder()
            .date(LocalDate.parse(date))
            .isOvernight(false)
            .accommodations(List.of())
            .build();
    }

    @Test
    @DisplayName("consecutive nights at one property are one stay")
    void consecutiveNightsMerge() {
        var stays = QuoteStayGrouper.group(List.of(
            night("2026-12-30", room("Kahawa House", "Double Room", 3, 6), room("Kahawa House", "Single Room", 1, 1)),
            night("2026-12-31", room("Kahawa House", "Double Room", 3, 6), room("Kahawa House", "Single Room", 1, 1)),
            night("2027-01-01", room("Kahawa House", "Double Room", 3, 6), room("Kahawa House", "Single Room", 1, 1))));

        assertEquals(1, stays.size(), "three nights at one lodge is one booking");
        var stay = stays.get(0);
        assertEquals("Kahawa House", stay.getAccommodationName());
        assertEquals(3, stay.getNights());
        assertEquals(LocalDate.parse("2026-12-30"), stay.getCheckIn());
        assertEquals(7, stay.getSleeps(), "six in the doubles, one in the single");
    }

    @Test
    @DisplayName("check-out is the morning after the last night")
    void checkOutIsTheMorningAfter() {
        var stays = QuoteStayGrouper.group(List.of(
            night("2027-01-08", room("Kahawa House", "Double Room", 2, 4)),
            night("2027-01-09", room("Kahawa House", "Double Room", 2, 4))));

        var stay = stays.get(0);
        assertEquals(LocalDate.parse("2027-01-08"), stay.getCheckIn());
        assertEquals(LocalDate.parse("2027-01-10"), stay.getCheckOut(),
            "the party leaves on the 10th; quoting the 9th here costs the lodge a night");
        assertEquals(2, stay.getNights());
    }

    @Test
    @DisplayName("a different property starts a new stay")
    void aPropertyChangeSplits() {
        var stays = QuoteStayGrouper.group(List.of(
            night("2027-01-02", room("Farm of Dreams Lodge", "Double/Twin", 2, 4)),
            night("2027-01-03", room("Osinon Tented Camp", "Double", 2, 4)),
            night("2027-01-04", room("Osinon Tented Camp", "Double", 2, 4))));

        assertEquals(2, stays.size());
        assertEquals("Farm of Dreams Lodge", stays.get(0).getAccommodationName());
        assertEquals(1, stays.get(0).getNights());
        assertEquals("Osinon Tented Camp", stays.get(1).getAccommodationName());
        assertEquals(2, stays.get(1).getNights());
    }

    @Test
    @DisplayName("the same property with a different room mix is a second booking")
    void aRoomMixChangeSplits() {
        var stays = QuoteStayGrouper.group(List.of(
            night("2027-01-07", room("Kahawa House", "Double Room", 3, 6), room("Kahawa House", "Single Room", 1, 1)),
            night("2027-01-08", room("Kahawa House", "Double Room", 2, 4), room("Kahawa House", "Single Room", 1, 1)),
            night("2027-01-09", room("Kahawa House", "Double Room", 2, 4), room("Kahawa House", "Single Room", 1, 1))));

        assertEquals(2, stays.size(), "the party drops a room on the 8th - the lodge needs both lines");
        assertEquals(1, stays.get(0).getNights());
        assertEquals(7, stays.get(0).getSleeps());
        assertEquals(2, stays.get(1).getNights());
        assertEquals(5, stays.get(1).getSleeps());
        assertEquals(LocalDate.parse("2027-01-08"), stays.get(0).getCheckOut());
        assertEquals(LocalDate.parse("2027-01-08"), stays.get(1).getCheckIn());
    }

    @Test
    @DisplayName("a day nobody sleeps on breaks the run")
    void aDepartureDayBreaksTheRun() {
        var stays = QuoteStayGrouper.group(List.of(
            night("2027-01-05", room("Kahawa House", "Double Room", 2, 4)),
            departureDay("2027-01-06"),
            night("2027-01-07", room("Kahawa House", "Double Room", 2, 4))));

        assertEquals(2, stays.size(), "same lodge, but with a gap - that is two bookings");
        assertEquals(LocalDate.parse("2027-01-06"), stays.get(0).getCheckOut());
        assertEquals(LocalDate.parse("2027-01-07"), stays.get(1).getCheckIn());
    }

    @Test
    @DisplayName("a trip with no days has no stays")
    void nothingGroupsToNothing() {
        assertTrue(QuoteStayGrouper.group(List.of()).isEmpty());
        assertTrue(QuoteStayGrouper.group(null).isEmpty());
    }

    @Test
    @DisplayName("the whole trip collapses to the bookings a reservations clerk would make")
    void theWholeTripGroups() {
        var kahawaFull = new FullQuoteDTO.DayStayDTO[]{
            room("Kahawa House", "Double Room", 3, 6), room("Kahawa House", "Single Room", 1, 1)};
        var farm = new FullQuoteDTO.DayStayDTO[]{
            room("Farm of Dreams Lodge", "Double/Twin", 2, 4),
            room("Farm of Dreams Lodge", "Triple", 1, 3),
            room("Farm of Dreams Lodge", "Single", 1, 1)};
        var osinon = new FullQuoteDTO.DayStayDTO[]{
            room("Osinon Tented Camp", "Double", 2, 4),
            room("Osinon Tented Camp", "Triple", 1, 3),
            room("Osinon Tented Camp", "Single", 1, 1)};
        var kahawaFive = new FullQuoteDTO.DayStayDTO[]{
            room("Kahawa House", "Double Room", 2, 4), room("Kahawa House", "Single Room", 1, 1)};

        var stays = QuoteStayGrouper.group(List.of(
            night("2026-12-30", kahawaFull), night("2026-12-31", kahawaFull), night("2027-01-01", kahawaFull),
            night("2027-01-02", farm),
            night("2027-01-03", osinon), night("2027-01-04", osinon),
            night("2027-01-05", farm), night("2027-01-06", farm),
            night("2027-01-07", kahawaFull),
            night("2027-01-08", kahawaFive), night("2027-01-09", kahawaFive)));

        assertEquals(6, stays.size(), "eleven nights, six bookings");
        assertEquals(List.of(3, 1, 2, 2, 1, 2),
            stays.stream().map(FullQuoteDTO.StayDTO::getNights).toList());
        assertEquals(LocalDate.parse("2027-01-10"),
            stays.get(5).getCheckOut(), "and the last party leaves on the 10th");
    }
}
