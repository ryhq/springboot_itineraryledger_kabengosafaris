package com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices;

import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.Entity.QuoteDay;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayAccommodation.Entity.QuoteDayAccommodation;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayActivity.Entity.QuoteDayActivity;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayFlight.Entity.QuoteDayFlight;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.Entity.QuoteDayPark;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkActivity.Entity.QuoteDayParkActivity;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkTariff.Entity.QuoteDayParkTariff;
import com.itineraryledger.kabengosafaris.Quote.QuoteInclusion.Entity.QuoteInclusion;
import com.itineraryledger.kabengosafaris.Quote.QuotePax.Entity.QuotePax;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * One quotation's own trip, copied onto another.
 *
 * <p>A new version is the same sale repriced, so it has to start as a faithful copy of the one
 * it replaces. {@code QuoteVersionService} copied the line items and nothing else: no days, no
 * travelling party, no inclusions. The result looked right on the document and was hollow
 * underneath, because the day tree is what the price is computed FROM. {@code recalculate} skips
 * a quote with no days, so a v2 could never be repriced: every edit to it changed the trip and
 * left the total exactly as it was.
 *
 * <p>That was survivable while only staff touched a quotation. It is not survivable now. The
 * portal's change history is built from the same day tree, so a v2's history was blank; the
 * client's editing controls are built from it, so there was nothing to edit; and a client who
 * changed something would have been shown an unmoved price.
 *
 * <p><strong>Copied from the previous QUOTE, never re-snapshotted from the itinerary.</strong>
 * The two are not the same thing after the first edit, and they diverge by design: the day tree
 * on a quotation is the customer's own copy, which is exactly why the portal is allowed to let
 * them change it. Re-reading the itinerary would silently throw away every choice they made and
 * hand them back the product as it is sold to strangers.
 *
 * <p>References travel as references. An accommodation, a route, a fare, a tariff are catalogue
 * rows shared by every quotation that names them; what is copied is the decision — this lodge,
 * this board basis, this many rooms, this markup — and never the catalogue row itself.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuoteTreeCopyService {

    /**
     * Copy the travelling party, the inclusions and the whole day tree.
     *
     * <p>The caller saves. Every collection cascades from {@code Quote}, so adding to the target
     * is enough and the ids are assigned on flush.
     *
     * @return how many days were copied, for the log and the caller's message
     */
    public int copyTree(Quote source, Quote target) {
        if (source == null || target == null) return 0;

        copyParty(source, target);
        copyInclusions(source, target);
        int days = copyDays(source, target);

        log.info("Copied {} days, {} party lines and {} inclusions from {} onto {}",
            days, target.getPaxList().size(), target.getInclusionList().size(),
            source.getQuoteCode(), target.getQuoteCode());
        return days;
    }

    /**
     * Who is travelling.
     *
     * <p>Copied from the quotation rather than the itinerary for the reason in the class note,
     * and it matters most here: the party is the commonest thing a customer changes, and it is
     * the single biggest lever on the price.
     */
    private void copyParty(Quote source, Quote target) {
        if (source.getPaxList() == null) return;
        for (QuotePax pax : source.getPaxList()) {
            target.getPaxList().add(QuotePax.builder()
                .quote(target)
                .nationCategory(pax.getNationCategory())
                .ageCategory(pax.getAgeCategory())
                .count(pax.getCount())
                .notes(pax.getNotes())
                .build());
        }
    }

    /**
     * What the price covers.
     *
     * <p>From the previous quotation, not from the itinerary, because an inclusion line can be
     * edited on a quotation — a concession promised to this customer belongs to this sale and
     * has to survive into the version that replaces it.
     */
    private void copyInclusions(Quote source, Quote target) {
        if (source.getInclusionList() == null) return;
        for (QuoteInclusion inclusion : source.getInclusionList()) {
            target.addInclusion(QuoteInclusion.builder()
                .inclusionItem(inclusion.getInclusionItem())
                .label(inclusion.getLabel())
                .category(inclusion.getCategory())
                .claimAppliesTo(inclusion.getClaimAppliesTo())
                .isIncluded(inclusion.getIsIncluded())
                .sortOrder(inclusion.getSortOrder())
                .build());
        }
    }

    private int copyDays(Quote source, Quote target) {
        if (source.getDays() == null) return 0;

        for (QuoteDay day : source.getDays()) {
            QuoteDay copy = QuoteDay.builder()
                .dayNumber(day.getDayNumber())
                .dayTag(day.getDayTag())
                .title(day.getTitle())
                .description(day.getDescription())
                .morningActivities(day.getMorningActivities())
                .afternoonActivities(day.getAfternoonActivities())
                .eveningActivities(day.getEveningActivities())
                .wildlifeHighlights(day.getWildlifeHighlights())
                .scenicHighlights(day.getScenicHighlights())
                .specialNotes(day.getSpecialNotes())
                .startLocation(day.getStartLocation())
                .endLocation(day.getEndLocation())
                .distanceKm(day.getDistanceKm())
                .isOvernight(day.getIsOvernight())
                .mealsIncluded(day.getMealsIncluded())
                .internalNotes(day.getInternalNotes())
                .build();

            copyAccommodations(day, copy);
            copyActivities(day, copy);
            copyParks(day, copy);
            copyFlights(day, copy);

            copy.setQuote(target);
            target.getDays().add(copy);
        }
        return target.getDays().size();
    }

    /**
     * The nights, including the alternatives.
     *
     * <p>{@code isAlternative} travels, and it is not a detail. The alternatives are the menu a
     * client chooses from at editing level CHOOSE_OFFERED, so dropping them would hand the new
     * version a customer with nothing left to choose between, having just told them they could.
     */
    private void copyAccommodations(QuoteDay source, QuoteDay target) {
        if (source.getAccommodations() == null) return;
        for (QuoteDayAccommodation stay : source.getAccommodations()) {
            target.addAccommodation(QuoteDayAccommodation.builder()
                .accommodation(stay.getAccommodation())
                .roomType(stay.getRoomType())
                .roomStandard(stay.getRoomStandard())
                .boardType(stay.getBoardType())
                .roomCount(stay.getRoomCount())
                .isAlternative(stay.getIsAlternative())
                .notes(stay.getNotes())
                .build());
        }
    }

    /** {@code isOptional} travels for the same reason the alternatives do. */
    private void copyActivities(QuoteDay source, QuoteDay target) {
        if (source.getActivities() == null) return;
        for (QuoteDayActivity activity : source.getActivities()) {
            target.addActivity(QuoteDayActivity.builder()
                .activity(activity.getActivity())
                .sortOrder(activity.getSortOrder())
                .durationHours(activity.getDurationHours())
                .startTime(activity.getStartTime())
                .endTime(activity.getEndTime())
                .notes(activity.getNotes())
                .isIncludedInPrice(activity.getIsIncludedInPrice())
                .isOptional(activity.getIsOptional())
                .build());
        }
    }

    private void copyParks(QuoteDay source, QuoteDay target) {
        if (source.getParks() == null) return;
        for (QuoteDayPark park : source.getParks()) {
            QuoteDayPark copy = QuoteDayPark.builder()
                .park(park.getPark())
                .entryType(park.getEntryType())
                .sortOrder(park.getSortOrder())
                .arrivalTime(park.getArrivalTime())
                .departureTime(park.getDepartureTime())
                .notes(park.getNotes())
                .build();

            if (park.getParkActivities() != null) {
                for (QuoteDayParkActivity inside : park.getParkActivities()) {
                    copy.addParkActivity(QuoteDayParkActivity.builder()
                        .parkActivity(inside.getParkActivity())
                        .sortOrder(inside.getSortOrder())
                        .durationHours(inside.getDurationHours())
                        .notes(inside.getNotes())
                        .isIncludedInPrice(inside.getIsIncludedInPrice())
                        .build());
                }
            }

            /*
             * The tariffs, which are the park fees. Forgetting these would not look like a bug:
             * the days would all be there and the total would simply be a few hundred dollars
             * short, on a document that supersedes one already sent.
             */
            if (park.getParkTariffs() != null) {
                for (QuoteDayParkTariff tariff : park.getParkTariffs()) {
                    copy.addParkTariff(QuoteDayParkTariff.builder()
                        .parkTariff(tariff.getParkTariff())
                        .notes(tariff.getNotes())
                        .isIncludedInPrice(tariff.getIsIncludedInPrice())
                        .build());
                }
            }

            target.addPark(copy);
        }
    }

    /**
     * The sectors, with the markup decision made about this trip.
     *
     * <p>The fare travels as a reference, never as a copy of its figures: the price is frozen in
     * the quotation's own line items, and copying the numbers here as well would give one price
     * two homes and no rule for which wins. {@code markupType} and {@code markupValue} DO travel,
     * because they are a decision about this client rather than a figure about the flight.
     */
    private void copyFlights(QuoteDay source, QuoteDay target) {
        if (source.getFlights() == null) return;
        for (QuoteDayFlight flight : source.getFlights()) {
            target.addFlight(QuoteDayFlight.builder()
                .flightRoute(flight.getFlightRoute())
                .flightFare(flight.getFlightFare())
                .passengerCount(flight.getPassengerCount())
                .isAlternative(flight.getIsAlternative())
                .isIncludedInPrice(flight.getIsIncludedInPrice())
                .markupType(flight.getMarkupType())
                .markupValue(flight.getMarkupValue())
                .sortOrder(flight.getSortOrder())
                .notes(flight.getNotes())
                .build());
        }
    }
}
