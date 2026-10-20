package com.itineraryledger.kabengosafaris.Quote.DTOs;

import com.itineraryledger.kabengosafaris.Inclusion.DTOs.InclusionLineDTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Quote.Enums.QuoteItemType;
import com.itineraryledger.kabengosafaris.Quote.Enums.QuoteStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * FullQuoteDTO - Complete quote data with all nested entities for PDF generation
 *
 * Structure:
 * Quote
 * ├── customer (customer information)
 * ├── itinerary (itinerary summary)
 * ├── items (quote line items with multi-currency prices)
 * ├── subtotals (by currency)
 * ├── taxes (by currency)
 * ├── discounts (by currency)
 * └── grandTotals (by currency)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FullQuoteDTO {

    // ========================
    // QUOTE FIELDS
    // ========================
    private String id;
    private String quoteCode;
    private String title;
    private String description;
    private QuoteStatus status;
    private String statusDisplayName;
    private Integer version;
    private String versionNotes;

    // ========================
    // PRICING DETAILS
    // ========================
    private Boolean isStoRate;
    /** Vehicles this quote is priced for — its own, not the itinerary's current figure. */
    private Integer carCount;
    private BigDecimal taxPercentage;

    /** Which line categories the tax applies to. Null = all of them. */
    private String taxAppliesTo;
    private BigDecimal discountPercentage;
    private String discountReason;

    /** Which line categories the discount comes off ("ACCOMMODATION,ACTIVITY"). Null = all. */
    private String discountAppliesTo;

    // Markup (bakes into per-line-item unit prices; never a separate line)
    private BigDecimal agentCommissionPercentage;
    private String agentCommissionReason;
    private BigDecimal marginUpliftPercentage;
    private String marginUpliftReason;

    /** Which line categories the margin uplift applies to. Null = all. */
    private String marginUpliftAppliesTo;

    private Boolean condenseItems;

    // ========================
    // VALIDITY AND DATES
    // ========================
    private LocalDate safariStartDate;
    private LocalDate sentDate;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Boolean isValid;
    private String validityStatusMessage;

    // ========================
    // PAYMENT TERMS
    // ========================
    private BigDecimal depositPercentage;
    private LocalDate depositDueDate;
    private LocalDate fullPaymentDueDate;

    // ========================
    // NOTES
    // ========================
    private String customerNotes;
    private String internalNotes;

    // ========================
    // APPROVAL
    // ========================
    private String approverName;
    private String approvedByName;
    private LocalDateTime approvedAt;
    private String approvalNotes;

    // ========================
    // AUDIT
    // ========================
    private Boolean isActive;
    private String createdByName;
    private String updatedByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ========================
    // WHO IS TRAVELLING
    // ========================

    /**
     * How many people this price is for.
     *
     * <p>Without it no quote template can divide, which is why no quote has ever shown a
     * per-guest figure. The number a client asks for first is what it costs each of them.
     */
    private Integer totalPax;

    /** The pax bands behind that count, so a mixed party is visible rather than averaged away. */
    private List<PaxBandDTO> paxBands;

    /**
     * The grand total divided by the headcount, one entry per currency.
     *
     * <p>This divides the quote's OWN grand total, tax and discount included, so
     * {@code perGuest × totalPax} comes back to the figure printed above it. A band-aware split
     * taken from the cost estimation would be more precise per head and would not reconcile with
     * the total on the same page, which is the worse failure on a document a client checks.
     */
    private List<PriceDTO> perGuestTotals;

    /**
     * True when every traveller sits in one pax band, so the division is a price rather than
     * an average.
     *
     * <p>A non-resident adult and a resident child do not cost the same, so with more than one
     * band the figure is an average and the template must say so. Printing "per guest" over an
     * average invites a client to multiply it by their own headcount and arrive somewhere we
     * never quoted.
     */
    private Boolean perGuestIsExact;

    // ========================
    // NESTED DATA
    // ========================
    private CustomerDTO customer;
    private ItineraryDTO itinerary;
    private List<QuoteItemDTO> items;
    private List<PriceDTO> subtotals;
    private List<PriceDTO> taxes;
    private List<PriceDTO> discounts;
    private List<PriceDTO> grandTotals;

    // ========================
    // SUMMARY STATISTICS
    // ========================
    private Integer totalItemsCount;
    private Integer totalCurrenciesCount;
    private List<String> currencies;

    // ========================
    // NESTED DTO CLASSES
    // ========================

    /**
     * Customer information
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CustomerDTO {
        private String id;
        private String customerName;
        private String email;
        private String phone;
        private String nationality;
        private String address;
        private String city;
        private String country;
    }

    /**
     * Itinerary summary
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ItineraryDTO {
        private String id;
        private String name;
        private String code;
        private String status;
        private String statusDisplayName;
        private String tripType;
        private String tripTypeDisplayName;
        private String budgetCategory;
        private String budgetCategoryDisplayName;
        private Integer totalDays;
        private Integer totalNights;
        private String description;
        private String startLocation;
        private String endLocation;
    }

    /**
     * Quote line item
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class QuoteItemDTO {
        private String id;
        private QuoteItemType itemType;
        private String itemTypeDisplayName;
        private String itemName;
        private String description;
        private Integer displayOrder;
        private List<PriceDTO> prices;
        private Boolean isActive;
    }

    /**
     * Price in a specific currency
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PriceDTO {
        private String currency;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
        private String breakdown;
        private String formattedUnitPrice;
        private String formattedTotalPrice;
    }

    /**
     * One pax band on the quote: a nationality and age bracket, and how many of them.
     *
     * <p>Park fees turn on both, so these are the categories the price was actually built from,
     * not a cosmetic grouping.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PaxBandDTO {
        private String nationCategoryName;
        private String ageCategoryName;
        private Integer count;

        /** "Non-Resident Adult", for a template that wants one string rather than two. */
        public String getLabel() {
            if (nationCategoryName == null && ageCategoryName == null) {
                return null;
            }
            if (nationCategoryName == null) {
                return ageCategoryName;
            }
            if (ageCategoryName == null) {
                return nationCategoryName;
            }
            return nationCategoryName + " " + ageCategoryName;
        }
    }

    /**
     * The sentence printed beside the per-guest figure.
     *
     * <p>A per-guest price is only true at the headcount it was worked out for: two vehicles and
     * the per-group charges spread across however many people are going, so the same trip costs
     * less each when one more comes. Saying "per guest" and stopping invites the reader to
     * multiply it by a different number than we did.
     *
     * <p>A getter rather than a stored field, because SpEL resolves getters and there is nothing
     * here to keep in step with the value it describes.
     */
    public String getPerGuestBasisLabel() {
        if (totalPax == null || totalPax <= 0) {
            return null;
        }
        String head = Boolean.FALSE.equals(perGuestIsExact) ? "Average per guest" : "Per guest";
        return head + ", based on " + totalPax + (totalPax == 1 ? " traveller" : " travelling");
    }

    /**
     * The tax scope in words, for the document the client reads.
     *
     * <p>A scoped tax does not divide into the subtotal: 18% charged on the beds alone is 9.7% of
     * the trip, and a client checking the arithmetic finds a percentage that is not there. The
     * enum names are for us; this is the sentence for them.
     *
     * <p>A getter rather than a stored field, because SpEL resolves getters and there is nothing
     * here to keep in step with the value it describes.
     */
    public String getTaxAppliesToLabel() {
        return com.itineraryledger.kabengosafaris.Quote.Enums.QuoteItemTypeScope.describe(taxAppliesTo);
    }

    /**
     * The discount scope in words, for the document the client reads.
     *
     * <p>Same problem as the tax and worse, because a discount is a promise: 10% off the parts we
     * sell is 7.92% of a quote carrying park fees, and a client who works out the percentage and
     * finds 7.92 has been given no way to see that the 10% was kept. This says what it came off.
     */
    public String getDiscountAppliesToLabel() {
        return com.itineraryledger.kabengosafaris.Quote.Enums.QuoteItemTypeScope.describe(discountAppliesTo);
    }

    // =====================================================================
    // WHAT THE PRICE COVERS
    // =====================================================================

    /**
     * The lines this document states about its own price, snapshotted when it was produced.
     *
     * <p>Never read live from the catalogue: a customer holds a copy of this document, and a
     * catalogue edit that changed what our copy says would make ours the one that looked altered.
     */
    private java.util.List<InclusionLineDTO> inclusions;

    /**
     * What is included, as plain lines.
     *
     * <p>A getter rather than a stored field, the same shape as {@code highlightsList}: SpEL
     * resolves getters, so a template can iterate a value nothing has to store or keep in step.
     */
    public java.util.List<String> getInclusionsList() {
        return linesWhere(true);
    }

    /** What is not included, as plain lines. Stated on the document, never derived from the above. */
    public java.util.List<String> getExclusionsList() {
        return linesWhere(false);
    }

    private java.util.List<String> linesWhere(boolean included) {
        if (inclusions == null || inclusions.isEmpty()) return java.util.List.of();
        return inclusions.stream()
            .filter(line -> line != null && line.isIncluded() == included)
            .map(InclusionLineDTO::getLabel)
            .filter(label -> label != null && !label.isBlank())
            .map(String::trim)
            .toList();
    }

}
