package com.itineraryledger.kabengosafaris.Safari.DTOs;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * What this safari cost us, read from its expenses, and the margin against what
 * we invoiced.
 *
 * The quote said what the trip should cost and the invoice says what the customer
 * owes. Neither says what we actually paid — a lodge swapped mid-trip, a park fee
 * that went up, a second vehicle nobody planned. That gap is the difference
 * between the margin we quoted and the margin we made, and it can only be
 * answered by the bills.
 *
 * Everything is per currency and never added across them. A margin is only stated
 * for a currency that has both sides: revenue in USD against costs in TZS is two
 * facts, not one profit.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SafariCostingDTO {

    /** Live expenses only; a cancelled bill is kept for audit but cost nothing. */
    private Integer expenseCount;

    /** How many still have money owing to the supplier. */
    private Integer unpaidExpenseCount;

    /** NOTHING_RECORDED · OWING · SETTLED */
    private String supplierStatus;
    private String supplierStatusDisplayName;

    private List<Price> expensed;
    private List<Price> paidOut;
    private List<Price> owedToSuppliers;

    /** One per category (ACCOMMODATION, PARK_FEE, …), largest first. */
    private List<CategoryTotal> byCategory;

    /** Only where a currency has both revenue and cost. */
    private List<MarginLine> margin;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CategoryTotal {
        private String category;
        private String categoryDisplayName;
        private List<Price> expensed;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MarginLine {
        private String currency;
        /** invoiced to the customer */
        private BigDecimal revenue;
        /** billed by suppliers */
        private BigDecimal cost;
        private BigDecimal margin;
        /** margin as a share of revenue; absent when there is no revenue to share */
        private BigDecimal marginPercent;
    }
}
