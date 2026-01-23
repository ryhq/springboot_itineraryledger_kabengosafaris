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

/**
 * QuotationDTO - Response DTO for quotation summary data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuotationDTO {

    private String id; // Obfuscated ID
    private String code;
    private String name;

    // Customer
    private String customerId;
    private String customerDisplayName;
    private String customerEmail;

    // Itinerary
    private String itineraryId;
    private String itineraryCode;
    private String itineraryName;

    // Status
    private QuotationStatus status;
    private String statusDisplayName;
    private String statusDescription;
    private Integer version;

    // Revision
    private String parentQuotationId;
    private String parentQuotationCode;

    // Trip Details
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalDays;
    private Integer totalNights;
    private String daysNightsDisplay;
    private Integer totalPax;

    // Pricing Summary
    private String currency;
    private BigDecimal subtotal;
    private DiscountType discountType;
    private String discountTypeDisplayName;
    private BigDecimal discountValue;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal depositRequired;
    private BigDecimal depositPercentage;
    private BigDecimal perPersonCost;

    // Validity
    private LocalDate validUntil;
    private Boolean isExpired;
    private Integer daysUntilExpiry;

    // Lifecycle
    private LocalDateTime sentAt;
    private LocalDateTime viewedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime rejectedAt;

    // Counts
    private Integer paxCount;
    private Integer lineItemCount;

    // Assignment
    private String assignedToId;
    private String assignedToName;

    // Metadata
    private String createdById;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Actions
    private Boolean canSend;
    private Boolean canRevise;
    private Boolean canAccept;
    private Boolean canConvertToSafari;
}
