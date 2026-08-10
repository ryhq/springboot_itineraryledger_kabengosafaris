package com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A costing, as a document.
 *
 * The cost estimate on screen answers three questions at once — what it costs,
 * what it sells for, and what could not be priced — and a template needs all
 * three plus the header that says WHAT was costed. This carries them together,
 * with both breakdowns computed (by day and by pax band) so one template can
 * print either without the caller having chosen a mode in advance.
 *
 * <p><b>Deliberately not itinerary-shaped.</b> The subject is described by a
 * generic header — name, code, kind, dates, party — rather than by itinerary
 * fields, so a safari costing can fill the same document and reuse the same
 * templates when the Safari module lands. Nothing here says "itinerary" except
 * the value of {@code subjectKind}.
 *
 * <p>Every figure appears in both STO and rack form, and the margin between
 * them is precomputed rather than left to Thymeleaf arithmetic: a template that
 * has to subtract is a template that can subtract wrongly, and the number it
 * would get wrong is the profit.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FullCostEstimationDTO {

    /* ----------------------------- the subject ---------------------------- */

    /** Obfuscated id of whatever was costed. */
    private String subjectId;

    /** ITINERARY or SAFARI — what kind of thing this costing is for. */
    private String subjectKind;

    /** Its code, e.g. 'ITI-14D13N-1053'. */
    private String subjectCode;

    /** Its name, e.g. 'Ultimate Northern Tanzania'. */
    private String subjectName;

    /** Trip type and budget tier, for the document header. */
    private String tripType;
    private String budgetCategory;

    private Integer totalDays;
    private Integer totalNights;
    private Integer carCount;

    /* ------------------------------ the basis ----------------------------- */

    /** The date the pricing was run for — seasons make this load-bearing. */
    private LocalDate startDate;
    private LocalDate endDate;

    /** When the figures were worked out, so a printed sheet is attributable. */
    private String estimatedAt;

    /** Total guests across every band. */
    private Integer totalPax;

    /** The bands themselves, with counts. */
    @Builder.Default
    private List<PaxCategoryCostDTO> paxCostDetails = new ArrayList<>();

    /* ----------------------------- the figures ---------------------------- */

    /** Day by day, in order, each with its own lines and per-currency totals. */
    @Builder.Default
    private List<DayCostDetailDTO> dayCostDetails = new ArrayList<>();

    /** One block per currency: category splits, grand totals, and the margin. */
    @Builder.Default
    private List<CostTotalsDTO> totals = new ArrayList<>();

    /* ------------------------------ the gaps ------------------------------ */

    /** True when anything could not be priced — the totals are then partial. */
    private Boolean hasIncompleteRates;

    /** Every unpriceable item, with the day and season that could not resolve. */
    @Builder.Default
    private List<RateIssueLogDTO> rateIssues = new ArrayList<>();

    /** How many issues, so a template can say so without counting a list. */
    private Integer rateIssueCount;

    /* ---------------------------- saved summary --------------------------- */

    /**
     * What was last stored for this subject, if anything, so a document can
     * show today's figure beside the one that was agreed.
     */
    @Builder.Default
    private List<ItineraryCostSummaryDTO> savedSummary = new ArrayList<>();

    /**
     * Totals for one currency, with the margin worked out.
     *
     * Currencies are never mixed: park fees in USD and a lodge in TZS are two
     * numbers, and adding them would be a lie a template cannot detect.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CostTotalsDTO {
        private String currency;

        private BigDecimal accommodationSto;
        private BigDecimal accommodationRack;
        private BigDecimal parkFeesSto;
        private BigDecimal parkFeesRack;
        private BigDecimal activitiesSto;
        private BigDecimal activitiesRack;

        private BigDecimal grandTotalSto;
        private BigDecimal grandTotalRack;

        /** rack − STO. What the trip earns at the published price. */
        private BigDecimal grossProfit;

        /** The same as a percentage of rack, to one decimal place. */
        private BigDecimal marginPercent;

        /** grandTotalSto ÷ totalPax — the per-head cost, precomputed. */
        private BigDecimal stoPerPax;

        /** grandTotalRack ÷ totalPax — the per-head selling price. */
        private BigDecimal rackPerPax;
    }
}
