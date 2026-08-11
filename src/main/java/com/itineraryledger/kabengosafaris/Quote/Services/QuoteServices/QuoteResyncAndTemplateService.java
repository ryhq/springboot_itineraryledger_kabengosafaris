package com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices;

import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Entity.ItineraryDay;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.Entity.ItineraryDayAccommodation;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.Entity.ItineraryDayActivity;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Entity.ItineraryDayPark;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.Entity.ItineraryDayParkActivity;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.Entity.ItineraryDayParkTariff;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryPax.Entity.ItineraryPax;
import com.itineraryledger.kabengosafaris.Itinerary.Repository.ItineraryRepository;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.Entity.QuoteDay;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayAccommodation.Entity.QuoteDayAccommodation;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayActivity.Entity.QuoteDayActivity;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.Entity.QuoteDayPark;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkActivity.Entity.QuoteDayParkActivity;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkTariff.Entity.QuoteDayParkTariff;
import com.itineraryledger.kabengosafaris.Quote.QuotePax.Entity.QuotePax;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * QuoteResyncAndTemplateService — two inverse operations:
 *
 *   1. {@link #saveAsItinerary} promotes a Quote's customised day-tree
 *      back into the Itinerary catalog as a new template. Useful when
 *      sales hand-builds a great variant on a quote and wants to reuse
 *      it for future customers.
 *
 *   2. {@link #resyncFromItinerary} wipes a Quote's snapshot and re-
 *      snapshots from the source Itinerary. Destructive: discards all
 *      per-customer customisations. Intended for "the customer changed
 *      their mind back to the standard package" or "the master template
 *      was corrected after this quote was created and we want to pick
 *      up the fix".
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class QuoteResyncAndTemplateService {

    private final QuoteRepository quoteRepository;
    private final ItineraryRepository itineraryRepository;
    private final QuoteFromItineraryGenerationService quoteFromItineraryGenerationService;
    private final QuoteCostEstimationService quoteCostEstimationService;
    private final IdObfuscator idObfuscator;

    // =====================================================================
    // 1. Save Quote → new Itinerary template
    // =====================================================================

    @Transactional
    public ResponseEntity<ApiResponse<?>> saveAsItinerary(
            String quoteIdObfuscated,
            String newName,
            String newDescription
    ) {
        try {
            Long quoteId = idObfuscator.decodeId(quoteIdObfuscated);
            if (quoteId == null) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid quote ID", "INVALID_QUOTE_ID"));
            }
            Quote quote = quoteRepository.findById(quoteId).orElse(null);
            if (quote == null) {
                return ResponseEntity.status(404).body(
                        ApiResponse.error(404, "Quote not found", "QUOTE_NOT_FOUND"));
            }
            if (quote.getDays() == null || quote.getDays().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400,
                                "Quote has no day snapshot to save as a template. Legacy quotes are not supported.",
                                "QUOTE_HAS_NO_SNAPSHOT"));
            }

            Itinerary source = quote.getItinerary();

            /*
             * The new template describes THIS QUOTE, not the itinerary the quote
             * was priced from. The quote has been negotiated — a day dropped, a
             * lodge changed, a different pax split — and copying the source's
             * header would produce a template whose summary contradicted its own
             * days.
             *
             * So every field is taken from the quote where the quote has an
             * answer, and from the source only where it does not: trip type,
             * budget category and highlights live on an itinerary alone, and the
             * car count is the one the quote was actually priced with.
             */
            List<QuoteDay> orderedDays = quote.getDays().stream()
                    .sorted(java.util.Comparator.comparing(
                            QuoteDay::getDayNumber,
                            java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                    .collect(java.util.stream.Collectors.toList());

            int totalDays = orderedDays.size();
            // the days say where the guests sleep; a trip that ends with a
            // fly-out has one fewer night than days-minus-one would claim
            int totalNights = (int) orderedDays.stream()
                    .filter(d -> Boolean.TRUE.equals(d.getIsOvernight()))
                    .count();

            String startLocation = orderedDays.stream()
                    .map(QuoteDay::getStartLocation)
                    .filter(v -> v != null && !v.isBlank())
                    .findFirst()
                    .orElse(source != null ? source.getStartLocation() : null);

            String endLocation = orderedDays.stream()
                    .map(QuoteDay::getEndLocation)
                    .filter(v -> v != null && !v.isBlank())
                    .reduce((first, second) -> second)
                    .orElse(source != null ? source.getEndLocation() : null);

            String name = newName != null && !newName.isBlank()
                    ? newName.trim()
                    : (quote.getTitle() != null && !quote.getTitle().isBlank()
                            ? quote.getTitle()
                            : "Quote " + quote.getQuoteCode());

            String description = newDescription != null && !newDescription.isBlank()
                    ? newDescription
                    : (quote.getDescription() != null && !quote.getDescription().isBlank()
                            ? quote.getDescription()
                            : (source != null ? source.getDescription() : null));

            Itinerary fresh = Itinerary.builder()
                    .name(name)
                    .status(Itinerary.ItineraryStatus.DRAFT)
                    // an itinerary-only classification; the quote inherited it
                    .tripType(source != null ? source.getTripType() : null)
                    .budgetCategory(source != null ? source.getBudgetCategory() : null)
                    .totalDays(totalDays)
                    .totalNights(totalNights)
                    // the vehicle count the quote's own prices were worked out on
                    .carCount(source != null && source.getCarCount() != null ? source.getCarCount() : 1)
                    .description(description)
                    .highlights(source != null ? source.getHighlights() : null)
                    .startLocation(startLocation)
                    .endLocation(endLocation)
                    .isActive(true)
                    .build();

            copyQuotePaxIntoItinerary(quote, fresh);
            copyQuoteDaysIntoItinerary(quote, fresh);

            Itinerary saved = itineraryRepository.save(fresh);
            saved.setCode(saved.generateCode());
            saved = itineraryRepository.save(saved);

            log.info("Saved Quote {} as new Itinerary {} ({})",
                    quote.getQuoteCode(), saved.getCode(), saved.getId());

            Map<String, Object> body = new HashMap<>();
            body.put("itineraryId", idObfuscator.encodeId(saved.getId()));
            body.put("itineraryCode", saved.getCode());
            body.put("itineraryName", saved.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.success(201,
                            "Quote saved as new Itinerary template (status DRAFT). Publish from the Itinerary view when ready.",
                            body));
        } catch (Exception e) {
            log.error("Failed to save quote as itinerary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to save as template: " + e.getMessage(), "SAVE_AS_TEMPLATE_FAILED"));
        }
    }

    // =====================================================================
    // 2. Re-sync Quote from its source Itinerary
    // =====================================================================

    @Transactional
    public ResponseEntity<ApiResponse<?>> resyncFromItinerary(String quoteIdObfuscated) {
        try {
            Long quoteId = idObfuscator.decodeId(quoteIdObfuscated);
            if (quoteId == null) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid quote ID", "INVALID_QUOTE_ID"));
            }
            Quote quote = quoteRepository.findById(quoteId).orElse(null);
            if (quote == null) {
                return ResponseEntity.status(404).body(
                        ApiResponse.error(404, "Quote not found", "QUOTE_NOT_FOUND"));
            }
            if (quote.getItinerary() == null) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400,
                                "Quote has no source itinerary to re-sync from",
                                "QUOTE_HAS_NO_ITINERARY"));
            }
            Itinerary itinerary = itineraryRepository.findById(quote.getItinerary().getId()).orElse(null);
            if (itinerary == null) {
                return ResponseEntity.status(404).body(
                        ApiResponse.error(404, "Source itinerary no longer exists", "ITINERARY_NOT_FOUND"));
            }

            // Wipe existing snapshot — orphanRemoval=true on Quote.days /
            // Quote.paxList drops the child rows on flush.
            if (quote.getDays() != null) quote.getDays().clear();
            if (quote.getPaxList() != null) quote.getPaxList().clear();
            quoteRepository.saveAndFlush(quote);

            // Re-snapshot from the (possibly updated) Itinerary
            quoteFromItineraryGenerationService.snapshotItineraryIntoQuote(itinerary, quote);
            quoteRepository.save(quote);

            // Re-derive items from the fresh snapshot
            int items = quoteCostEstimationService.recalculate(quoteId);
            log.info("Re-synced quote {} from itinerary {}: {} items written",
                    quote.getQuoteCode(), itinerary.getCode(), items);

            Map<String, Object> body = new HashMap<>();
            body.put("quoteId", idObfuscator.encodeId(quote.getId()));
            body.put("itineraryCode", itinerary.getCode());
            body.put("daysSnapshotted", quote.getDays() != null ? quote.getDays().size() : 0);
            body.put("paxSnapshotted", quote.getPaxList() != null ? quote.getPaxList().size() : 0);
            body.put("itemsWritten", items);
            return ResponseEntity.ok(ApiResponse.success(200,
                    "Quote re-synced from itinerary — all previous customisations have been discarded.",
                    body));
        } catch (Exception e) {
            log.error("Failed to re-sync quote from itinerary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to re-sync: " + e.getMessage(), "RESYNC_FAILED"));
        }
    }

    // =====================================================================
    // Quote → Itinerary deep-copy helpers (inverse of QuoteFromItinerary*)
    // =====================================================================

    private void copyQuotePaxIntoItinerary(Quote quote, Itinerary itinerary) {
        if (quote.getPaxList() == null) return;
        for (QuotePax qp : quote.getPaxList()) {
            ItineraryPax ip = ItineraryPax.builder()
                    .nationCategory(qp.getNationCategory())
                    .ageCategory(qp.getAgeCategory())
                    .count(qp.getCount())
                    .notes(qp.getNotes())
                    .build();
            itinerary.addPax(ip);
        }
    }

    private void copyQuoteDaysIntoItinerary(Quote quote, Itinerary itinerary) {
        if (quote.getDays() == null) return;
        for (QuoteDay qd : quote.getDays()) {
            ItineraryDay id = ItineraryDay.builder()
                    .dayNumber(qd.getDayNumber())
                    .dayTag(qd.getDayTag())
                    .title(qd.getTitle())
                    .description(qd.getDescription())
                    .morningActivities(qd.getMorningActivities())
                    .afternoonActivities(qd.getAfternoonActivities())
                    .eveningActivities(qd.getEveningActivities())
                    .wildlifeHighlights(qd.getWildlifeHighlights())
                    .scenicHighlights(qd.getScenicHighlights())
                    .specialNotes(qd.getSpecialNotes())
                    .startLocation(qd.getStartLocation())
                    .endLocation(qd.getEndLocation())
                    .distanceKm(qd.getDistanceKm())
                    .isOvernight(qd.getIsOvernight())
                    .mealsIncluded(qd.getMealsIncluded())
                    .internalNotes(qd.getInternalNotes())
                    .build();

            copyDayActivities(qd, id);
            copyDayAccommodations(qd, id);
            copyDayParks(qd, id);

            itinerary.addDay(id);
        }
    }

    private void copyDayActivities(QuoteDay qd, ItineraryDay id) {
        if (qd.getActivities() == null) return;
        for (QuoteDayActivity a : qd.getActivities()) {
            ItineraryDayActivity ia = ItineraryDayActivity.builder()
                    .activity(a.getActivity())
                    .sortOrder(a.getSortOrder())
                    .durationHours(a.getDurationHours())
                    .startTime(a.getStartTime())
                    .endTime(a.getEndTime())
                    .notes(a.getNotes())
                    .isIncludedInPrice(a.getIsIncludedInPrice())
                    .isOptional(a.getIsOptional())
                    .build();
            id.addActivity(ia);
        }
    }

    private void copyDayAccommodations(QuoteDay qd, ItineraryDay id) {
        if (qd.getAccommodations() == null) return;
        for (QuoteDayAccommodation acc : qd.getAccommodations()) {
            ItineraryDayAccommodation ia = ItineraryDayAccommodation.builder()
                    .accommodation(acc.getAccommodation())
                    .roomType(acc.getRoomType())
                    .roomStandard(acc.getRoomStandard())
                    .boardType(acc.getBoardType())
                    .roomCount(acc.getRoomCount())
                    .isAlternative(acc.getIsAlternative())
                    .notes(acc.getNotes())
                    .build();
            id.addAccommodation(ia);
        }
    }

    private void copyDayParks(QuoteDay qd, ItineraryDay id) {
        if (qd.getParks() == null) return;
        for (QuoteDayPark p : qd.getParks()) {
            ItineraryDayPark ip = ItineraryDayPark.builder()
                    .park(p.getPark())
                    .entryType(p.getEntryType())
                    .sortOrder(p.getSortOrder())
                    .arrivalTime(p.getArrivalTime())
                    .departureTime(p.getDepartureTime())
                    .notes(p.getNotes())
                    .build();
            copyParkActivities(p, ip);
            copyParkTariffs(p, ip);
            id.addPark(ip);
        }
    }

    private void copyParkActivities(QuoteDayPark p, ItineraryDayPark ip) {
        if (p.getParkActivities() == null) return;
        for (QuoteDayParkActivity pa : p.getParkActivities()) {
            ItineraryDayParkActivity ipa = ItineraryDayParkActivity.builder()
                    .parkActivity(pa.getParkActivity())
                    .sortOrder(pa.getSortOrder())
                    .durationHours(pa.getDurationHours())
                    .notes(pa.getNotes())
                    .isIncludedInPrice(pa.getIsIncludedInPrice())
                    .build();
            ip.addParkActivity(ipa);
        }
    }

    private void copyParkTariffs(QuoteDayPark p, ItineraryDayPark ip) {
        if (p.getParkTariffs() == null) return;
        for (QuoteDayParkTariff pt : p.getParkTariffs()) {
            ItineraryDayParkTariff ipt = ItineraryDayParkTariff.builder()
                    .parkTariff(pt.getParkTariff())
                    .notes(pt.getNotes())
                    .isIncludedInPrice(pt.getIsIncludedInPrice())
                    .build();
            ip.addParkTariff(ipt);
        }
    }
}
