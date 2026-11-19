package com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices;

import com.itineraryledger.kabengosafaris.Quote.DTOs.FullQuoteDTO;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A lodge books a <b>stay</b>, not a sequence of nights.
 *
 * <p>The quotation holds nights - one row per day, which is the right shape for pricing and the
 * wrong shape for a reservation. A property asked to hold rooms wants one line: who, from when,
 * until when, how many rooms of what. Three separate nights at the same camp is one booking, and
 * sending it as three is how a lodge ends up holding three one-night reservations.
 *
 * <p>Pure arithmetic over the document's own day list, so it needs no Spring and no database and
 * can be tested as plain objects.
 */
public final class QuoteStayGrouper {

    private QuoteStayGrouper() {
    }

    /**
     * Collapse a quotation's nights into the stays a lodge would recognise.
     *
     * <p>A new stay starts when <b>the property changes, the room mix changes, or a day without an
     * overnight breaks the run</b>. The room mix counts: a party that drops from four rooms to
     * three partway through is two bookings to the property, not one, and rolling them together
     * would understate the first half and overstate the second.
     *
     * <p>Check-out is <b>the morning after the last night</b>. Stating the last night's own date
     * as the check-out is the commonest way a rooming list is read wrong, and it costs a night.
     */
    public static List<FullQuoteDTO.StayDTO> group(List<FullQuoteDTO.DayDTO> days) {
        List<FullQuoteDTO.StayDTO> stays = new ArrayList<>();
        if (days == null || days.isEmpty()) return stays;

        FullQuoteDTO.StayDTO open = null;
        String openKey = null;
        LocalDate lastNight = null;

        for (FullQuoteDTO.DayDTO day : days) {
            List<FullQuoteDTO.DayStayDTO> rooms = day.getAccommodations();
            boolean sleepsHere = Boolean.TRUE.equals(day.getIsOvernight())
                && rooms != null && !rooms.isEmpty();

            if (!sleepsHere) {
                /* A day nobody sleeps on ends whatever was running, even if the same lodge
                 * reappears afterwards - that is a second booking with a gap in between. */
                open = null;
                openKey = null;
                lastNight = null;
                continue;
            }

            String key = keyOf(rooms);
            if (open != null && key.equals(openKey)) {
                open.setNights(open.getNights() + 1);
                lastNight = day.getDate() != null ? day.getDate() : lastNight;
                open.setCheckOut(lastNight != null ? lastNight.plusDays(1) : null);
                continue;
            }

            lastNight = day.getDate();
            open = FullQuoteDTO.StayDTO.builder()
                .accommodationName(rooms.get(0).getAccommodationName())
                .checkIn(day.getDate())
                .checkOut(day.getDate() != null ? day.getDate().plusDays(1) : null)
                .nights(1)
                .rooms(new ArrayList<>(rooms))
                .sleeps(rooms.stream()
                    .map(FullQuoteDTO.DayStayDTO::getSleeps)
                    .filter(java.util.Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .sum())
                .build();
            openKey = key;
            stays.add(open);
        }

        return stays;
    }

    /**
     * What makes two nights the same booking: the property, and the exact mix of rooms.
     *
     * <p>Built from the row's own fields rather than its id, because the rows are per-night and a
     * second night is always a different row describing the same thing.
     */
    private static String keyOf(List<FullQuoteDTO.DayStayDTO> rooms) {
        return rooms.stream()
            .map(r -> String.join("|",
                String.valueOf(r.getAccommodationName()),
                String.valueOf(r.getRoomTypeName()),
                String.valueOf(r.getRoomStandardName()),
                String.valueOf(r.getBoardTypeName()),
                String.valueOf(r.getRoomCount())))
            .sorted()
            .collect(Collectors.joining(" + "));
    }
}
