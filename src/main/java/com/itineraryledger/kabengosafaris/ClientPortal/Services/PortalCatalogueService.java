package com.itineraryledger.kabengosafaris.ClientPortal.Services;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRate;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRateRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRepository;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Core.SeasonResolverService;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.Entity.QuoteDay;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayAccommodation.Entity.QuoteDayAccommodation;
import com.itineraryledger.kabengosafaris.Season.Season;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * What else a client could sleep in, on a night whose route is already fixed.
 *
 * <p>Editing level CHOOSE_CATALOGUE. At CHOOSE_OFFERED a client picks between the alternatives
 * we put on their quotation; here they reach past those into everything we hold a rate for.
 * That is a real difference in what is exposed, which is why it is decided per product rather
 * than left on: anybody who can get a quotation can then read which lodges we work with and in
 * which season.
 *
 * <p><strong>Bounded by region and by date, and both bounds are load-bearing.</strong> The
 * region is what keeps the swap deliverable: the vehicle arrives from somewhere and leaves for
 * somewhere, and a lodge four hours away is not an alternative, it is a different trip. The
 * date is what keeps the option real: a rate that does not resolve for that night is a price we
 * cannot stand behind, so the option is not offered at all rather than offered and then refused.
 *
 * <p><strong>No figures, deliberately.</strong> Not one price crosses this boundary, and the
 * reason is not only that {@code stoRate} is our cost. The rack rate is not what the client
 * would pay either — commission and uplift are applied by the pricing engine, per quotation —
 * so showing it would be showing a number that is wrong in a way the client cannot detect.
 * They choose, the quotation reprices, and the total they are then shown is the real one.
 *
 * <p>An option is a RATE, not an accommodation. Room types, standards and board types belong to
 * one accommodation each, so "the same room, elsewhere" does not exist as a concept: what a
 * client picks is one concrete, priced combination.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortalCatalogueService {

    /**
     * How many options to hand back.
     *
     * <p>A region with forty lodges and six rate combinations each is two hundred and forty
     * rows, which is not a choice, it is a catalogue dump. Capped so the page stays a decision
     * rather than a search problem; the office can always offer a specific lodge by adding it
     * to the quotation as an alternative.
     */
    private static final int MOST_OPTIONS = 60;

    private final AccommodationRepository accommodations;
    private final AccommodationRateRepository rates;
    private final SeasonResolverService seasons;
    private final IdObfuscator idObfuscator;

    /**
     * One thing a client could choose, with nothing in it that is ours.
     *
     * <p>No price, no supplier, no cost basis, no internal grading. The id addresses a rate for
     * the length of this page; it is minted by this run of the application and is proved again
     * on the way back in, like every other id the portal hands out.
     */
    public record Option(
        String id,
        String accommodation,
        String region,
        String roomType,
        String roomStandard,
        String board,
        /** True for the one they already have, so the page can mark it rather than offer it. */
        boolean current
    ) {}

    /**
     * Everything bookable in place of this night.
     *
     * @param row the accommodation row on the quotation they are replacing
     */
    @Transactional(readOnly = true)
    public List<Option> optionsFor(Quote quote, QuoteDayAccommodation row) {
        List<Option> options = new ArrayList<>();
        if (quote == null || row == null || row.getAccommodation() == null) return options;

        String region = row.getAccommodation().getRegion();
        if (region == null || region.isBlank()) {
            /*
             * No region means no way to say what is near it. Offering the whole country would
             * be worse than offering nothing: the client would choose something we cannot drive
             * to and we would have to take it back.
             */
            log.info("Accommodation {} has no region, so the catalogue has no bound to search in",
                row.getAccommodation().getName());
            return options;
        }

        LocalDate night = dateOf(quote, row.getQuoteDay());
        if (night == null) {
            log.info("Quote {} has no start date, so no season can be resolved for its days",
                quote.getQuoteCode());
            return options;
        }

        Long currentId = row.getAccommodation().getId();
        Set<String> seen = new LinkedHashSet<>();

        for (Accommodation candidate : accommodations.findLiveInRegion(region)) {
            Season season = seasons.resolveAccommodationSeason(candidate.getId(), night);
            if (season == null) continue;   // nothing priced for that night; not an option

            for (AccommodationRate rate : rates.findByAccommodationIdAndSeasonIdAndIsActiveTrue(
                    candidate.getId(), season.getId())) {
                if (rate.getRoomType() == null || rate.getRoomStandard() == null
                        || rate.getBoardType() == null) {
                    continue;
                }

                /*
                 * One row per combination a client can tell apart. Two rates differing only in
                 * our own bookkeeping read as the same choice twice, and a list that repeats
                 * itself is one nobody trusts.
                 */
                String shape = candidate.getId() + "|" + rate.getRoomType().getName() + "|"
                    + rate.getRoomStandard().getName() + "|" + rate.getBoardType().getName();
                if (!seen.add(shape)) continue;

                options.add(new Option(
                    idObfuscator.encodeId(rate.getId()),
                    candidate.getName(),
                    candidate.getRegion(),
                    rate.getRoomType().getName(),
                    rate.getRoomStandard().getName(),
                    rate.getBoardType().getName(),
                    candidate.getId().equals(currentId)));

                if (options.size() >= MOST_OPTIONS) return options;
            }
        }
        return options;
    }

    /**
     * The night this day falls on.
     *
     * <p>Start date plus the day number, the same arithmetic every cost aggregator in this
     * application does. A quotation with no start date cannot resolve a season for anything, so
     * it has no catalogue either; that is honest rather than a limitation, because a price for
     * an unknown date is a guess.
     */
    public LocalDate dateOf(Quote quote, QuoteDay day) {
        if (quote == null || quote.getSafariStartDate() == null
                || day == null || day.getDayNumber() == null) {
            return null;
        }
        return quote.getSafariStartDate().plusDays(day.getDayNumber() - 1L);
    }

    /** The rate behind an option, re-read rather than trusted from the request. */
    @Transactional(readOnly = true)
    public AccommodationRate rateById(Long rateId) {
        if (rateId == null) return null;
        AccommodationRate rate = rates.findById(rateId).orElse(null);
        if (rate == null || !Boolean.TRUE.equals(rate.getIsActive())) return null;
        if (rate.getAccommodation() == null
                || !Boolean.TRUE.equals(rate.getAccommodation().getIsActive())) {
            return null;
        }
        return rate;
    }
}
