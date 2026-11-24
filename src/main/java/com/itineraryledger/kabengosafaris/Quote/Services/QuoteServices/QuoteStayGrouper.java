package com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices;

import com.itineraryledger.kabengosafaris.Quote.DTOs.FullQuoteDTO;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
     * <p><b>A night can be split across two properties</b>, and that is the case this has to get
     * right: when a lodge cannot hold the whole party, part of it sleeps next door. The night's
     * rows are therefore grouped by property first, and each property's run is tracked on its own.
     * Reading the night as one booking named after whichever row came first would send a lodge a
     * rooming list of rooms it never agreed to hold.
     *
     * <p>Within a property, a new stay starts when <b>the room mix changes, or a night away breaks
     * the run</b>. The room mix counts: a party that drops from four rooms to three partway through
     * is two bookings to the property, not one, and rolling them together would understate the
     * first half and overstate the second.
     *
     * <p>Check-out is <b>the morning after the last night</b>. Stating the last night's own date
     * as the check-out is the commonest way a rooming list is read wrong, and it costs a night.
     */
    public static List<FullQuoteDTO.StayDTO> group(List<FullQuoteDTO.DayDTO> days) {
        List<FullQuoteDTO.StayDTO> stays = new ArrayList<>();
        if (days == null || days.isEmpty()) return stays;

        /* One run per property, in the order the properties were first slept at. */
        Map<String, OpenStay> running = new LinkedHashMap<>();

        for (FullQuoteDTO.DayDTO day : days) {
            List<FullQuoteDTO.DayStayDTO> rooms = day.getAccommodations();
            boolean sleepsHere = Boolean.TRUE.equals(day.getIsOvernight())
                && rooms != null && !rooms.isEmpty();

            if (!sleepsHere) {
                /* A day nobody sleeps on ends whatever was running, even if the same lodge
                 * reappears afterwards - that is a second booking with a gap in between. */
                running.clear();
                continue;
            }

            Map<String, List<FullQuoteDTO.DayStayDTO>> tonight = byProperty(rooms);

            for (Map.Entry<String, List<FullQuoteDTO.DayStayDTO>> entry : tonight.entrySet()) {
                String property = entry.getKey();
                List<FullQuoteDTO.DayStayDTO> propertyRooms = entry.getValue();
                String key = keyOf(propertyRooms);

                OpenStay open = running.get(property);
                if (open != null && open.key.equals(key)) {
                    open.stay.setNights(open.stay.getNights() + 1);
                    if (day.getDate() != null) open.lastNight = day.getDate();
                    open.stay.setCheckOut(open.lastNight != null ? open.lastNight.plusDays(1) : null);
                    continue;
                }

                FullQuoteDTO.StayDTO stay = FullQuoteDTO.StayDTO.builder()
                    .accommodationName(propertyRooms.get(0).getAccommodationName())
                    .checkIn(day.getDate())
                    .checkOut(day.getDate() != null ? day.getDate().plusDays(1) : null)
                    .nights(1)
                    .rooms(new ArrayList<>(propertyRooms))
                    .sleeps(propertyRooms.stream()
                        .map(FullQuoteDTO.DayStayDTO::getSleeps)
                        .filter(java.util.Objects::nonNull)
                        .mapToInt(Integer::intValue)
                        .sum())
                    .build();
                running.put(property, new OpenStay(stay, key, day.getDate()));
                stays.add(stay);
            }

            /* A property nobody slept at tonight has had its run broken, whoever else did. */
            running.keySet().retainAll(tonight.keySet());
        }

        return stays;
    }

    /** Tonight's rows split by the property that has to hold them, first appearance first. */
    private static Map<String, List<FullQuoteDTO.DayStayDTO>> byProperty(
        List<FullQuoteDTO.DayStayDTO> rooms) {
        Map<String, List<FullQuoteDTO.DayStayDTO>> grouped = new LinkedHashMap<>();
        for (FullQuoteDTO.DayStayDTO room : rooms) {
            grouped.computeIfAbsent(String.valueOf(room.getAccommodationName()),
                k -> new ArrayList<>()).add(room);
        }
        return grouped;
    }

    /**
     * What makes two nights the same booking at one property: the exact mix of rooms.
     *
     * <p>Built from the row's own fields rather than its id, because the rows are per-night and a
     * second night is always a different row describing the same thing.
     */
    private static String keyOf(List<FullQuoteDTO.DayStayDTO> rooms) {
        return rooms.stream()
            .map(r -> String.join("|",
                String.valueOf(r.getRoomTypeName()),
                String.valueOf(r.getRoomStandardName()),
                String.valueOf(r.getBoardTypeName()),
                String.valueOf(r.getRoomCount())))
            .sorted()
            .collect(Collectors.joining(" + "));
    }

    /** A booking still being extended night by night. */
    private static final class OpenStay {
        private final FullQuoteDTO.StayDTO stay;
        private final String key;
        private LocalDate lastNight;

        private OpenStay(FullQuoteDTO.StayDTO stay, String key, LocalDate lastNight) {
            this.stay = stay;
            this.key = key;
            this.lastNight = lastNight;
        }
    }
}
