package com.itineraryledger.kabengosafaris.Quote.QuoteDay.Services;

import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.Entity.QuoteDay;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.Repository.QuoteDayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * The one place that decides what number and what date each of a quote's days carries.
 *
 * <p>Three operations move days around - adding one, deleting one, reordering them - and each
 * used to renumber in its own loop. A quote day now also carries a date, so there would be two
 * things for three loops to get wrong independently. There is one loop here instead.
 *
 * <p><b>The rule, stated once:</b> days are numbered 1..n in the order given, and day <i>n</i>
 * falls on {@code quote.safariStartDate + (n - 1)}. So {@code safariStartDate} is by definition
 * the date of day 1 - inserting a day in front of the trip moves it back, and the caller doing
 * the inserting is the one that moves it.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class QuoteDayCalendarService {

    private final QuoteDayRepository quoteDayRepository;

    /**
     * Far enough above any real day number that the parking numbers cannot collide with the
     * final ones. A trip is at most a few weeks; this is four figures.
     */
    private static final int PARKING_OFFSET = 10_000;

    /**
     * Renumber and re-date a quote's days in their current {@code dayNumber} order.
     * Use after an insert or a delete, where the relative order is already right.
     */
    @Transactional
    public void recalculate(Quote quote) {
        recalculate(quote, quoteDayRepository.findByQuoteIdOrderByDayNumberAsc(quote.getId()));
    }

    /**
     * Renumber and re-date, taking the order from the list rather than from the rows.
     *
     * <p>Written as two passes because {@code uk_quote_day_number} is a live unique constraint on
     * {@code (quote_id, day_number)}: moving day 2 into position 1 while a day 1 still exists is
     * a duplicate key, and MySQL checks that per statement rather than at commit. So every row is
     * first parked on a number nothing else can hold, and only then given its real one.
     */
    @Transactional
    public void recalculate(Quote quote, List<QuoteDay> orderedDays) {
        if (orderedDays == null || orderedDays.isEmpty()) return;

        int parking = PARKING_OFFSET;
        for (QuoteDay day : orderedDays) {
            day.setDayNumber(parking++);
        }
        quoteDayRepository.saveAllAndFlush(orderedDays);

        LocalDate start = quote.getSafariStartDate();
        int n = 1;
        for (QuoteDay day : orderedDays) {
            day.setDayNumber(n);
            day.setDayTag("Day " + n);
            /* No start date means no dates. A quotation nobody has dated states none, rather
             * than printing a date the rest of the system does not believe. */
            day.setDayDate(start == null ? null : start.plusDays(n - 1L));
            n++;
        }
        quoteDayRepository.saveAllAndFlush(orderedDays);

        log.debug("Quote {}: renumbered {} days from {}", quote.getId(), orderedDays.size(), start);
    }

    /** The date a given day number falls on, or null when the quote carries no start date. */
    public LocalDate dateOf(Quote quote, int dayNumber) {
        LocalDate start = quote.getSafariStartDate();
        return start == null ? null : start.plusDays(dayNumber - 1L);
    }
}
