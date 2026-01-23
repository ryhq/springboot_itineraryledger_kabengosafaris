package com.itineraryledger.kabengosafaris.Quotation.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Quotation.Enums.DiscountType;
import com.itineraryledger.kabengosafaris.Quotation.Enums.QuotationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * FullQuotationDTO - Complete quotation data with all nested entities
 * Used for detailed quotation views and PDF generation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FullQuotationDTO {

    private String id;
    private String code;
    private String name;

    // ========================
    // CUSTOMER
    // ========================

    private CustomerInfo customer;

    // ========================
    // ITINERARY
    // ========================

    private ItineraryInfo itinerary;

    // ========================
    // STATUS & VERSIONING
    // ========================

    private QuotationStatus status;
    private String statusDisplayName;
    private String statusDescription;
    private Integer version;
    private String parentQuotationId;
    private String parentQuotationCode;

    // ========================
    // TRIP DETAILS
    // ========================

    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalDays;
    private Integer totalNights;
    private String daysNightsDisplay;
    private Integer totalPax;

    // ========================
    // PRICING
    // ========================

    private String currency;
    private BigDecimal exchangeRate;
    private BigDecimal subtotal;
    private DiscountType discountType;
    private String discountTypeDisplayName;
    private BigDecimal discountValue;
    private BigDecimal discountAmount;
    private String discountReason;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal depositRequired;
    private BigDecimal depositPercentage;
    private BigDecimal perPersonCost;

    // ========================
    // COST BREAKDOWN BY TYPE
    // ========================

    private BigDecimal accommodationTotal;
    private BigDecimal parkFeesTotal;
    private BigDecimal activitiesTotal;
    private BigDecimal transportTotal;
    private BigDecimal otherTotal;

    // ========================
    // VALIDITY
    // ========================

    private LocalDate validUntil;
    private Boolean isExpired;
    private Integer daysUntilExpiry;

    // ========================
    // LIFECYCLE
    // ========================

    private LocalDateTime sentAt;
    private LocalDateTime viewedAt;
    private LocalDateTime respondedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime rejectedAt;
    private String rejectionReason;

    // ========================
    // CONTENT
    // ========================

    private String termsAndConditions;
    private List<String> inclusions;
    private List<String> exclusions;
    private String internalNotes;
    private String customerNotes;

    // ========================
    // PAX CONFIGURATION
    // ========================

    private List<QuotationPaxDTO> paxList;

    // ========================
    // LINE ITEMS
    // ========================

    /**
     * All line items in order
     */
    private List<QuotationLineItemDTO> lineItems;

    /**
     * Line items grouped by day number
     * Key: day number (0 for overall items)
     */
    private Map<Integer, List<QuotationLineItemDTO>> lineItemsByDay;

    // ========================
    // ASSIGNMENT
    // ========================

    private UserInfo assignedTo;
    private UserInfo createdBy;

    // ========================
    // METADATA
    // ========================

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ========================
    // ACTIONS
    // ========================

    private Boolean canSend;
    private Boolean canRevise;
    private Boolean canAccept;
    private Boolean canConvertToSafari;

    // ========================
    // NESTED DTOs
    // ========================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CustomerInfo {
        private String id;
        private String code;
        private String displayName;
        private String email;
        private String phone;
        private String customerType;
        private String nationality;
        private String country;
        private String preferredCurrency;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ItineraryInfo {
        private String id;
        private String code;
        private String name;
        private Integer totalDays;
        private Integer totalNights;
        private String tripType;
        private String budgetCategory;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserInfo {
        private String id;
        private String firstName;
        private String lastName;
        private String fullName;
        private String email;
    }
}
