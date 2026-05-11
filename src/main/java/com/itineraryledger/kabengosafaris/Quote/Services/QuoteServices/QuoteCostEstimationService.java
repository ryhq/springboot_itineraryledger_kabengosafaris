package com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices;

import com.itineraryledger.kabengosafaris.Itinerary.DTOs.FullItineraryDTO;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.ItineraryCostEstimationDTO;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.ItineraryCostEstimationDTO.CostLineItem;
import com.itineraryledger.kabengosafaris.Itinerary.Services.ItineraryCostEstimationService;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.Entity.QuoteItem;
import com.itineraryledger.kabengosafaris.Quote.Enums.QuoteItemType;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.Entity.QuoteDay;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayAccommodation.Entity.QuoteDayAccommodation;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayActivity.Entity.QuoteDayActivity;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.Entity.QuoteDayPark;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkActivity.Entity.QuoteDayParkActivity;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkTariff.Entity.QuoteDayParkTariff;
import com.itineraryledger.kabengosafaris.Quote.QuotePax.Entity.QuotePax;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteItemRepository;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteRepository;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteTotalsCalculationService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * QuoteCostEstimationService — derives the {@link QuoteItem} price lines
 * from the Quote's own day-tree × pax mix (instead of from the source
 * Itinerary). Reuses {@link ItineraryCostEstimationService}'s pricing
 * engine by building a synthetic {@link FullItineraryDTO} from the Quote
 * tree and delegating.
 *
 * Triggered from:
 *   - {@code POST /api/quotes/{id}/recalculate-items} (manual)
 *   - Every Quote-tree write (day / pax / accommodation / activity /
 *     park / park-activity / park-tariff CRUD) via the Phase-D services.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class QuoteCostEstimationService {

    private final QuoteRepository quoteRepository;
    private final QuoteItemRepository quoteItemRepository;
    private final ItineraryCostEstimationService itineraryCostEstimationService;
    private final QuoteTotalsCalculationService totalsCalculationService;
    private final IdObfuscator idObfuscator;

    /**
     * Recalculate a Quote's items from its current day-tree × pax mix.
     * Idempotent: drops existing items and rebuilds from scratch.
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> recalculateItemsForQuote(String quoteIdObfuscated) {
        try {
            Long quoteId = idObfuscator.decodeId(quoteIdObfuscated);
            if (quoteId == null) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid quote ID", "INVALID_QUOTE_ID"));
            }
            int items = recalculate(quoteId);
            return ResponseEntity.ok(ApiResponse.success(200,
                    "Quote items recalculated: " + items + " items", items));
        } catch (Exception e) {
            log.error("Error recalculating items for quote {}", quoteIdObfuscated, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to recalculate items", "RECALC_FAILED"));
        }
    }

    /**
     * Internal recalc used by both the explicit endpoint and the per-CRUD
     * triggers. Returns the count of items written.
     *
     * Silently no-ops for legacy quotes (no day-tree, no pax) — those keep
     * whatever items were generated at creation time.
     */
    @Transactional
    public int recalculate(Long quoteId) {
        Quote quote = quoteRepository.findById(quoteId).orElse(null);
        if (quote == null) {
            log.warn("recalculate: quote {} not found", quoteId);
            return 0;
        }
        if (quote.getDays() == null || quote.getDays().isEmpty()) {
            log.info("recalculate: quote {} has no day snapshot (legacy); skipping", quote.getQuoteCode());
            return 0;
        }

        // 1. Build synthetic FullItineraryDTO from the Quote tree
        FullItineraryDTO synthetic = buildSyntheticDTO(quote);

        // 2. Delegate to the existing pricing engine
        ResponseEntity<ApiResponse<?>> resp = itineraryCostEstimationService.estimateCostsFromDTO(
                synthetic, quote.getSafariStartDate(), quote.getIsStoRate(), null);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            log.warn("recalculate: cost estimation failed for quote {}", quote.getQuoteCode());
            return 0;
        }
        Object data = resp.getBody().getData();
        if (!(data instanceof ItineraryCostEstimationDTO)) {
            log.warn("recalculate: unexpected estimation payload for quote {}", quote.getQuoteCode());
            return 0;
        }
        ItineraryCostEstimationDTO estimation = (ItineraryCostEstimationDTO) data;

        // 3. Drop existing items and write fresh ones from the estimation
        quoteItemRepository.deleteByQuoteId(quoteId);
        quoteItemRepository.flush();
        int written = persistItems(quote, estimation);

        // 4. Refresh subtotals / taxes / discounts / grand totals
        totalsCalculationService.recalculateTotals(quote);

        log.info("Quote {} recalculated: {} items, {} currencies",
                quote.getQuoteCode(), written, estimation.getSubtotalByCurrency() != null
                        ? estimation.getSubtotalByCurrency().size() : 1);
        return written;
    }

    /**
     * Trigger recalc by Quote id. Swallows exceptions and just logs them —
     * call sites use this as a side-effect on top of a CRUD operation, and
     * the CRUD itself should succeed even if pricing fails.
     */
    public void triggerRecalc(Long quoteId) {
        try {
            recalculate(quoteId);
        } catch (Exception e) {
            log.warn("Best-effort recalc failed for quote {}: {}", quoteId, e.getMessage());
        }
    }

    // =====================================================================
    // Synthetic DTO builder — maps the Quote tree onto FullItineraryDTO
    // =====================================================================

    private FullItineraryDTO buildSyntheticDTO(Quote quote) {
        FullItineraryDTO dto = new FullItineraryDTO();
        // Carry the source Itinerary identity over so the pricing engine's
        // log/output reads sensibly; the engine doesn't otherwise care.
        if (quote.getItinerary() != null) {
            dto.setId(idObfuscator.encodeId(quote.getItinerary().getId()));
            dto.setCode(quote.getItinerary().getCode());
            dto.setName(quote.getItinerary().getName());
            dto.setCarCount(quote.getItinerary().getCarCount());
        }
        int totalDays = quote.getDays() != null ? quote.getDays().size() : 0;
        dto.setTotalDays(totalDays);
        dto.setTotalNights(Math.max(0, totalDays - 1));

        // ---- pax ----
        List<FullItineraryDTO.PaxDTO> paxList = new ArrayList<>();
        if (quote.getPaxList() != null) {
            for (QuotePax qp : quote.getPaxList()) {
                FullItineraryDTO.PaxDTO p = new FullItineraryDTO.PaxDTO();
                p.setId(idObfuscator.encodeId(qp.getId()));
                if (qp.getNationCategory() != null) {
                    p.setNationCategoryId(idObfuscator.encodeId(qp.getNationCategory().getId()));
                    p.setNationCategoryName(qp.getNationCategory().getName());
                }
                if (qp.getAgeCategory() != null) {
                    p.setAgeCategoryId(idObfuscator.encodeId(qp.getAgeCategory().getId()));
                    p.setAgeCategoryName(qp.getAgeCategory().getName());
                }
                p.setCount(qp.getCount());
                p.setNotes(qp.getNotes());
                paxList.add(p);
            }
        }
        dto.setPaxList(paxList);

        // ---- days ----
        List<FullItineraryDTO.DayDTO> dayDTOs = new ArrayList<>();
        if (quote.getDays() != null) {
            for (QuoteDay day : quote.getDays()) {
                dayDTOs.add(buildDayDTO(day));
            }
        }
        dto.setDays(dayDTOs);
        return dto;
    }

    private FullItineraryDTO.DayDTO buildDayDTO(QuoteDay day) {
        FullItineraryDTO.DayDTO d = new FullItineraryDTO.DayDTO();
        d.setId(idObfuscator.encodeId(day.getId()));
        d.setDayNumber(day.getDayNumber());
        d.setDayTag(day.getDayTag());
        d.setTitle(day.getTitle());
        d.setDescription(day.getDescription());
        d.setMorningActivities(day.getMorningActivities());
        d.setAfternoonActivities(day.getAfternoonActivities());
        d.setEveningActivities(day.getEveningActivities());
        d.setWildlifeHighlights(day.getWildlifeHighlights());
        d.setScenicHighlights(day.getScenicHighlights());
        d.setSpecialNotes(day.getSpecialNotes());
        d.setStartLocation(day.getStartLocation());
        d.setEndLocation(day.getEndLocation());
        d.setDistanceKm(day.getDistanceKm());
        d.setIsOvernight(day.getIsOvernight());
        d.setMealsIncluded(day.getMealsIncluded());

        // accommodations
        List<FullItineraryDTO.DayAccommodationDTO> accs = new ArrayList<>();
        if (day.getAccommodations() != null) {
            for (QuoteDayAccommodation a : day.getAccommodations()) {
                FullItineraryDTO.DayAccommodationDTO ad = new FullItineraryDTO.DayAccommodationDTO();
                ad.setId(idObfuscator.encodeId(a.getId()));
                if (a.getAccommodation() != null) {
                    ad.setAccommodationId(idObfuscator.encodeId(a.getAccommodation().getId()));
                    ad.setAccommodationName(a.getAccommodation().getName());
                }
                if (a.getRoomType() != null) {
                    ad.setRoomTypeId(idObfuscator.encodeId(a.getRoomType().getId()));
                    ad.setRoomTypeName(a.getRoomType().getName());
                }
                if (a.getRoomStandard() != null) {
                    ad.setRoomStandardId(idObfuscator.encodeId(a.getRoomStandard().getId()));
                    ad.setRoomStandardName(a.getRoomStandard().getName());
                }
                if (a.getBoardType() != null) {
                    ad.setBoardTypeId(idObfuscator.encodeId(a.getBoardType().getId()));
                    ad.setBoardTypeName(a.getBoardType().getName());
                }
                ad.setRoomCount(a.getRoomCount());
                ad.setIsAlternative(a.getIsAlternative());
                ad.setNotes(a.getNotes());
                accs.add(ad);
            }
        }
        d.setAccommodations(accs);

        // standalone activities
        List<FullItineraryDTO.DayActivityDTO> acts = new ArrayList<>();
        if (day.getActivities() != null) {
            for (QuoteDayActivity act : day.getActivities()) {
                FullItineraryDTO.DayActivityDTO actDTO = new FullItineraryDTO.DayActivityDTO();
                actDTO.setId(idObfuscator.encodeId(act.getId()));
                if (act.getActivity() != null) {
                    actDTO.setActivityId(idObfuscator.encodeId(act.getActivity().getId()));
                    actDTO.setActivityName(act.getActivity().getName());
                }
                actDTO.setSortOrder(act.getSortOrder());
                actDTO.setDurationHours(act.getDurationHours());
                actDTO.setStartTime(act.getStartTime());
                actDTO.setEndTime(act.getEndTime());
                actDTO.setNotes(act.getNotes());
                actDTO.setIsIncludedInPrice(act.getIsIncludedInPrice());
                actDTO.setIsOptional(act.getIsOptional());
                acts.add(actDTO);
            }
        }
        d.setActivities(acts);

        // parks (with park-activities + park-tariffs)
        List<FullItineraryDTO.DayParkDTO> parks = new ArrayList<>();
        if (day.getParks() != null) {
            for (QuoteDayPark park : day.getParks()) {
                FullItineraryDTO.DayParkDTO p = new FullItineraryDTO.DayParkDTO();
                p.setId(idObfuscator.encodeId(park.getId()));
                if (park.getPark() != null) {
                    p.setParkId(idObfuscator.encodeId(park.getPark().getId()));
                    p.setParkName(park.getPark().getName());
                }
                p.setEntryType(park.getEntryType());
                if (park.getEntryType() != null) {
                    p.setEntryTypeDisplayName(park.getEntryType().getDisplayName());
                }
                p.setSortOrder(park.getSortOrder());
                p.setArrivalTime(park.getArrivalTime());
                p.setDepartureTime(park.getDepartureTime());
                p.setNotes(park.getNotes());

                // park-activities
                List<FullItineraryDTO.ParkActivityDTO> pacts = new ArrayList<>();
                if (park.getParkActivities() != null) {
                    for (QuoteDayParkActivity pa : park.getParkActivities()) {
                        FullItineraryDTO.ParkActivityDTO paDTO = new FullItineraryDTO.ParkActivityDTO();
                        paDTO.setId(idObfuscator.encodeId(pa.getId()));
                        if (pa.getParkActivity() != null) {
                            if (pa.getParkActivity().getActivity() != null) {
                                paDTO.setActivityId(idObfuscator.encodeId(pa.getParkActivity().getActivity().getId()));
                                paDTO.setActivityName(pa.getParkActivity().getActivity().getName());
                            }
                        }
                        paDTO.setSortOrder(pa.getSortOrder());
                        paDTO.setDurationHours(pa.getDurationHours());
                        paDTO.setStartTime(pa.getStartTime());
                        paDTO.setEndTime(pa.getEndTime());
                        paDTO.setNotes(pa.getNotes());
                        paDTO.setIsIncludedInPrice(pa.getIsIncludedInPrice());
                        pacts.add(paDTO);
                    }
                }
                p.setActivities(pacts);

                // park-tariffs
                List<FullItineraryDTO.ParkTariffDTO> ptars = new ArrayList<>();
                if (park.getParkTariffs() != null) {
                    for (QuoteDayParkTariff pt : park.getParkTariffs()) {
                        FullItineraryDTO.ParkTariffDTO ptDTO = new FullItineraryDTO.ParkTariffDTO();
                        ptDTO.setId(idObfuscator.encodeId(pt.getId()));
                        if (pt.getParkTariff() != null && pt.getParkTariff().getTariff() != null) {
                            ptDTO.setTariffId(idObfuscator.encodeId(pt.getParkTariff().getTariff().getId()));
                            ptDTO.setTariffName(pt.getParkTariff().getTariff().getName());
                        }
                        ptDTO.setNotes(pt.getNotes());
                        ptDTO.setIsIncludedInPrice(pt.getIsIncludedInPrice());
                        ptars.add(ptDTO);
                    }
                }
                p.setTariffs(ptars);

                parks.add(p);
            }
        }
        d.setParks(parks);
        return d;
    }

    // =====================================================================
    // Persist derived items
    // =====================================================================

    /**
     * Turn a cost-estimation breakdown into condensed QuoteItem rows — one
     * row per (QuoteItemType, currency), with the breakdown text preserving
     * the underlying line-item names so the PDF still reads honestly.
     */
    private int persistItems(Quote quote, ItineraryCostEstimationDTO estimation) {
        int written = 0;
        written += writeCondensed(quote, estimation.getAccommodationCosts() != null
                ? estimation.getAccommodationCosts().getItems() : null,
                QuoteItemType.ACCOMMODATION, "Accommodation");
        written += writeCondensed(quote, estimation.getParkFeeCosts() != null
                ? estimation.getParkFeeCosts().getItems() : null,
                QuoteItemType.PARK_FEE, "Park Fees");
        written += writeCondensed(quote, estimation.getActivityCosts() != null
                ? estimation.getActivityCosts().getItems() : null,
                QuoteItemType.ACTIVITY, "Activities");
        return written;
    }

    private int writeCondensed(Quote quote, List<CostLineItem> lineItems,
                               QuoteItemType type, String displayName) {
        if (lineItems == null || lineItems.isEmpty()) return 0;

        Map<String, BigDecimal> totalsByCurrency = new LinkedHashMap<>();
        boolean anyRateMissing = false;
        List<String> names = new ArrayList<>();
        for (CostLineItem li : lineItems) {
            if (li.getCurrency() == null) continue;
            BigDecimal total = li.getTotalPrice() != null
                    ? li.getTotalPrice()
                    : (li.getUnitPrice() != null && li.getQuantity() != null
                        ? li.getUnitPrice().multiply(BigDecimal.valueOf(li.getQuantity()))
                        : BigDecimal.ZERO);
            totalsByCurrency.merge(li.getCurrency(), total, BigDecimal::add);
            if (Boolean.FALSE.equals(li.getRateFound())) anyRateMissing = true;
            if (li.getItemName() != null && !li.getItemName().isBlank() && !names.contains(li.getItemName())) {
                names.add(li.getItemName());
            }
        }
        if (totalsByCurrency.isEmpty()) return 0;

        List<Price> prices = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : totalsByCurrency.entrySet()) {
            Price p = new Price();
            p.setCurrency(entry.getKey());
            p.setQuantity(1);
            p.setUnitPrice(entry.getValue());
            p.setTotalPrice(entry.getValue());
            if (anyRateMissing) p.setBreakdown("Some rates were estimated");
            prices.add(p);
        }

        QuoteItem item = QuoteItem.builder()
                .quote(quote)
                .itemType(type)
                .itemName(displayName)
                .description(names.isEmpty() ? null : String.join(", ", names))
                .displayOrder(0)
                .prices(prices)
                .isActive(true)
                .build();
        quoteItemRepository.save(item);
        return 1;
    }
}
