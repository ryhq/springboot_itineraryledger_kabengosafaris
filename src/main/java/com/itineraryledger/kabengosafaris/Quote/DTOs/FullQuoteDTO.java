package com.itineraryledger.kabengosafaris.Quote.DTOs;

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
    private BigDecimal taxPercentage;
    private BigDecimal discountPercentage;
    private String discountReason;

    // Markup (bakes into per-line-item unit prices; never a separate line)
    private BigDecimal agentCommissionPercentage;
    private String agentCommissionReason;
    private BigDecimal marginUpliftPercentage;
    private String marginUpliftReason;

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
}
