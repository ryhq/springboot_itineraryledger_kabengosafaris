package com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Calculators;

import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.CostLineItemDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One currency's running total while a per-person charge is priced band by band.
 *
 * <p>A party can mix nationalities, and the rate cards price them in different money: a Tanzanian
 * citizen's Ngorongoro fee is held in TZS while the same gate charges a visitor in USD. The
 * calculators used to add every band into one figure and stamp it with whichever rate the loop
 * read last, so a single resident on an eight-person quote turned TZS 46,020 of crater fees into
 * USD 46,020. Nothing was logged. The quote simply tripled.
 *
 * <p>That it tripled is why anyone noticed. The same defect understating a total — a USD band
 * read last on a mostly-TZS party — looks exactly like a correct quote, and goes out.
 *
 * <p>Keeping a bucket per currency lets each one leave the calculator as its own line, which is
 * the shape every total downstream already groups by: {@code calculateGrandTotals} keys on the
 * line's currency, and {@code writeCondensed} writes one price per currency onto the quote item.
 * There is no exchange rate anywhere in costing, deliberately — see
 * {@code ItineraryBudgetLevelService}: "Subtracting one currency from another produces a number
 * and it is a lie." Two currencies therefore stay two numbers, all the way to the client.
 */
public final class CurrencyBucket {

    private BigDecimal sto = BigDecimal.ZERO;
    private BigDecimal rack = BigDecimal.ZERO;
    private int paxCount = 0;
    private final List<CostLineItemDTO.PaxShareDTO> shares = new ArrayList<>();

    /**
     * Open a bucket per currency, in the order the currencies were first seen.
     *
     * <p>First-seen order rather than a sorted or hashed one, so re-costing the same quote lists
     * its lines the same way twice. A set of line items that reshuffles between runs reads as a
     * change to whoever is comparing two versions of a quote.
     */
    public static Map<String, CurrencyBucket> ledger() {
        return new LinkedHashMap<>();
    }

    public void add(BigDecimal shareSto, BigDecimal shareRack, int count,
                    CostLineItemDTO.PaxShareDTO share) {
        sto = sto.add(shareSto);
        rack = rack.add(shareRack);
        paxCount += count;
        if (share != null) {
            shares.add(share);
        }
    }

    /**
     * Add a band without recording its breakdown.
     *
     * <p>The safari side does not keep per-band shares — a safari is costed for the party that
     * actually travelled, so there is no per-pax mode to attribute back to.
     */
    public void add(BigDecimal shareSto, BigDecimal shareRack, int count) {
        add(shareSto, shareRack, count, null);
    }

    /**
     * The shares, or null when none were recorded, which is what the DTO expects for "no
     * breakdown" — an empty list would read as a breakdown that found nothing.
     */
    public List<CostLineItemDTO.PaxShareDTO> sharesOrNull() {
        return shares.isEmpty() ? null : shares;
    }

    public BigDecimal sto() {
        return sto;
    }

    public BigDecimal rack() {
        return rack;
    }

    public int paxCount() {
        return paxCount;
    }

    public List<CostLineItemDTO.PaxShareDTO> shares() {
        return shares;
    }

    /**
     * The unit price to show beside the total.
     *
     * <p>An average, because the bands inside one currency can still be charged differently — an
     * adult and a youth pay different conservation fees in the same shillings. The bands
     * themselves are kept in {@link #shares()}, so per-pax mode attributes rather than averages.
     */
    public BigDecimal averageSto() {
        return average(sto);
    }

    public BigDecimal averageRack() {
        return average(rack);
    }

    private BigDecimal average(BigDecimal total) {
        return paxCount > 0
            ? total.divide(BigDecimal.valueOf(paxCount), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
    }
}
