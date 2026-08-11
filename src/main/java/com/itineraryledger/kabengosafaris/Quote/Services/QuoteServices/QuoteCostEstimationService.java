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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
     * Coalesce key on the active transaction — multiple {@code triggerRecalc}
     * calls inside one transaction (e.g. a service touching several
     * children in sequence) collapse to a single recalc, scheduled to fire
     * after the transaction commits. Call-sites don't need to know.
     */
    private static final String COALESCE_KEY = QuoteCostEstimationService.class.getName() + ".pendingQuoteIds";

    /**
     * Trigger recalc by Quote id. Coalesces within the active transaction:
     * the first call for each quoteId schedules an after-commit recalc;
     * subsequent calls in the same transaction are no-ops. If there's no
     * active transaction (rare — call-sites are all @Transactional), runs
     * synchronously. Swallows exceptions either way so a pricing failure
     * never breaks the CRUD it's piggy-backing on.
     */
    public void triggerRecalc(Long quoteId) {
        if (quoteId == null) return;

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // No transaction — recalc immediately; legacy fallback.
            try { recalculateBestEffort(quoteId); }
            catch (Exception e) { log.warn("Sync recalc failed for quote {}: {}", quoteId, e.getMessage()); }
            return;
        }

        @SuppressWarnings("unchecked")
        Set<Long> pending = (Set<Long>) TransactionSynchronizationManager.getResource(COALESCE_KEY);
        if (pending == null) {
            pending = new HashSet<>();
            TransactionSynchronizationManager.bindResource(COALESCE_KEY, pending);
            final Set<Long> capturedPending = pending;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    for (Long id : capturedPending) {
                        try { recalculateBestEffort(id); }
                        catch (Exception e) { log.warn("Post-commit recalc failed for quote {}: {}", id, e.getMessage()); }
                    }
                }
                @Override
                public void afterCompletion(int status) {
                    if (TransactionSynchronizationManager.hasResource(COALESCE_KEY)) {
                        TransactionSynchronizationManager.unbindResource(COALESCE_KEY);
                    }
                }
            });
        }
        pending.add(quoteId);
    }

    /**
     * Run a recalc in its own transaction (REQUIRES_NEW) — used after the
     * caller's transaction has committed, so we get a clean view of the
     * data and don't conflict with the original session.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int recalculateBestEffort(Long quoteId) {
        return recalculate(quoteId);
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
     *
     * Each persisted unit price is the rack-rate cost multiplied by
     * {@code (1 + agentCommission% + marginUplift%) / 100} so the customer-
     * facing line-item price is already inflated — there is no separate
     * "Markup" line on the PDF.
     */
    private int persistItems(Quote quote, ItineraryCostEstimationDTO estimation) {
        BigDecimal multiplier = computeMarkupMultiplier(quote);
        boolean condense = Boolean.TRUE.equals(quote.getCondenseItems());
        /*
         * displayOrder is 1-based and unique within a quote, which is what the
         * create endpoint has always assumed (max + 1) and what the reorder
         * endpoint now writes. It used to restart at 0 per type here, and every
         * condensed row was written as 0 — so three rows shared one position and
         * "sort by displayOrder" answered arbitrarily.
         */
        int written = 0;
        List<CostLineItem> accommodation = estimation.getAccommodationCosts() != null
                ? estimation.getAccommodationCosts().getItems() : null;
        List<CostLineItem> parkFees = estimation.getParkFeeCosts() != null
                ? estimation.getParkFeeCosts().getItems() : null;
        List<CostLineItem> activities = estimation.getActivityCosts() != null
                ? estimation.getActivityCosts().getItems() : null;

        if (condense) {
            written += writeCondensed(quote, accommodation, QuoteItemType.ACCOMMODATION, "Accommodation", multiplier, written + 1);
            written += writeCondensed(quote, parkFees, QuoteItemType.PARK_FEE, "Park Fees", multiplier, written + 1);
            written += writeCondensed(quote, activities, QuoteItemType.ACTIVITY, "Activities", multiplier, written + 1);
        } else {
            written += writePerLine(quote, accommodation, QuoteItemType.ACCOMMODATION, multiplier, written + 1);
            written += writePerLine(quote, parkFees, QuoteItemType.PARK_FEE, multiplier, written + 1);
            written += writePerLine(quote, activities, QuoteItemType.ACTIVITY, multiplier, written + 1);
        }
        return written;
    }

    /**
     * Persist one QuoteItem per cost-estimation line item, preserving the
     * day/pax context in the row's description. Used when the Quote was
     * generated with condense=false (the default), so the customer sees the
     * full breakdown rather than a per-type roll-up.
     */
    private int writePerLine(Quote quote, List<CostLineItem> lineItems,
                             QuoteItemType type, BigDecimal multiplier, int firstOrder) {
        if (lineItems == null || lineItems.isEmpty()) return 0;
        int displayOrder = firstOrder;
        int written = 0;
        for (CostLineItem li : lineItems) {
            if (li.getCurrency() == null) continue;
            BigDecimal baseUnit = li.getUnitPrice() != null ? li.getUnitPrice() : BigDecimal.ZERO;
            BigDecimal inflatedUnit = baseUnit.multiply(multiplier)
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            int qty = li.getQuantity() != null ? li.getQuantity() : 1;

            Price p = new Price();
            p.setCurrency(li.getCurrency());
            p.setQuantity(qty);
            p.setUnitPrice(inflatedUnit);
            p.setTotalPrice(inflatedUnit.multiply(BigDecimal.valueOf(qty)));
            if (Boolean.FALSE.equals(li.getRateFound())) {
                p.setBreakdown("Rate not found — estimated");
            }

            String description = buildLineDescription(li);

            QuoteItem item = QuoteItem.builder()
                    .quote(quote)
                    .itemType(type)
                    .itemName(li.getItemName() != null ? li.getItemName() : type.name())
                    .description(description)
                    .displayOrder(displayOrder++)
                    .prices(new ArrayList<>(List.of(p)))
                    .isActive(true)
                    .build();
            quoteItemRepository.save(item);
            written++;
        }
        return written;
    }

    /**
     * (1 + agentCommission% + marginUplift%) / 100. Defaults to 1 (no markup)
     * when both fields are null or zero.
     */
    private BigDecimal computeMarkupMultiplier(Quote quote) {
        BigDecimal commission = quote.getAgentCommissionPercentage() != null
                ? quote.getAgentCommissionPercentage() : BigDecimal.ZERO;
        BigDecimal uplift = quote.getMarginUpliftPercentage() != null
                ? quote.getMarginUpliftPercentage() : BigDecimal.ZERO;
        BigDecimal totalPct = commission.add(uplift);
        if (totalPct.signum() == 0) return BigDecimal.ONE;
        return BigDecimal.ONE.add(totalPct.divide(BigDecimal.valueOf(100), 6, java.math.RoundingMode.HALF_UP));
    }

    /**
     * Build a customer-facing description for a per-line QuoteItem row.
     * Includes the day number, the full resource name (the same string used
     * for the item header), and a "charged per X" clause derived from the
     * pax category — e.g. for a park fee:
     *
     *   "Day 2 • Conservation Fee - Serengeti National Park, charged per person."
     *
     * The clause is omitted when paxCategory is null/blank, and a leading
     * "per " is added when the category itself doesn't already start with
     * it (so "Adult" becomes "per adult", but "Per Person" stays as
     * "per person").
     */
    private String buildLineDescription(CostLineItem li) {
        StringBuilder desc = new StringBuilder();
        if (li.getDayNumber() != null) {
            desc.append("Day ").append(li.getDayNumber());
        }
        if (li.getItemName() != null && !li.getItemName().isBlank()) {
            if (desc.length() > 0) desc.append(" • ");
            desc.append(li.getItemName());
        }
        if (li.getPaxCategory() != null && !li.getPaxCategory().isBlank()) {
            String pax = li.getPaxCategory().trim().toLowerCase(java.util.Locale.ROOT);
            String chargedClause = pax.startsWith("per ")
                    ? "charged " + pax
                    : "charged per " + pax;
            if (desc.length() > 0) desc.append(", ");
            desc.append(chargedClause).append(".");
        } else if (desc.length() > 0) {
            desc.append(".");
        }
        return desc.length() > 0 ? desc.toString() : null;
    }

    private int writeCondensed(Quote quote, List<CostLineItem> lineItems,
                               QuoteItemType type, String displayName, BigDecimal multiplier,
                               int order) {
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
            // Apply markup multiplier in-place; rack base is the input,
            // inflated price is what gets stored.
            BigDecimal inflated = entry.getValue().multiply(multiplier)
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            Price p = new Price();
            p.setCurrency(entry.getKey());
            p.setQuantity(1);
            p.setUnitPrice(inflated);
            p.setTotalPrice(inflated);
            if (anyRateMissing) p.setBreakdown("Some rates were estimated");
            prices.add(p);
        }

        QuoteItem item = QuoteItem.builder()
                .quote(quote)
                .itemType(type)
                .itemName(displayName)
                .description(names.isEmpty() ? null : String.join(", ", names))
                .displayOrder(order)
                .prices(prices)
                .isActive(true)
                .build();
        quoteItemRepository.save(item);
        return 1;
    }
}
