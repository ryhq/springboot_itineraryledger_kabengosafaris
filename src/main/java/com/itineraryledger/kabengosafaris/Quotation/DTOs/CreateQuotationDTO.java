package com.itineraryledger.kabengosafaris.Quotation.DTOs;

import com.itineraryledger.kabengosafaris.Quotation.Enums.DiscountType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * CreateQuotationDTO - Request DTO for creating a new quotation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateQuotationDTO {

    @NotBlank(message = "Customer ID is required")
    private String customerId; // Obfuscated customer ID

    /**
     * Optional: Create quotation from an itinerary template
     */
    private String itineraryId; // Obfuscated itinerary ID

    @NotBlank(message = "Quotation name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    // ========================
    // TRIP DETAILS
    // ========================

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer totalDays;

    private Integer totalNights;

    // ========================
    // PRICING
    // ========================

    @Builder.Default
    private String currency = "USD";

    private BigDecimal exchangeRate;

    @Builder.Default
    private DiscountType discountType = DiscountType.NONE;

    private BigDecimal discountValue;

    private String discountReason;

    private BigDecimal taxRate;

    private BigDecimal depositPercentage;

    // ========================
    // VALIDITY
    // ========================

    private LocalDate validUntil;

    // ========================
    // CONTENT
    // ========================

    private String termsAndConditions;

    /**
     * What's included (JSON array or comma-separated list)
     */
    private String inclusions;

    /**
     * What's not included (JSON array or comma-separated list)
     */
    private String exclusions;

    private String internalNotes;

    private String customerNotes;

    // ========================
    // ASSIGNMENT
    // ========================

    private String assignedToId; // Obfuscated user ID

    // ========================
    // PAX CONFIGURATION
    // ========================

    @Valid
    private List<CreateQuotationPaxDTO> paxList;

    // ========================
    // LINE ITEMS
    // ========================

    @Valid
    private List<CreateQuotationLineItemDTO> lineItems;
}
