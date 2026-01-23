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
 * CreateQuotationFromItineraryDTO - Request DTO for creating a quotation from an itinerary template
 *
 * This DTO is used when creating a quotation by copying structure and calculating
 * costs from an existing itinerary template.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateQuotationFromItineraryDTO {

    @NotBlank(message = "Customer ID is required")
    private String customerId; // Obfuscated customer ID

    /**
     * Optional custom name (defaults to itinerary name)
     */
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    // ========================
    // TRIP DATES
    // ========================

    /**
     * Start date for the safari (required for cost calculation)
     */
    @NotBlank(message = "Start date is required")
    private LocalDate startDate;

    // ========================
    // PRICING OPTIONS
    // ========================

    /**
     * Use STO (Special Tour Operator) rates or Rack rates
     */
    @Builder.Default
    private Boolean useStoRate = true;

    /**
     * Quotation currency
     */
    @Builder.Default
    private String currency = "USD";

    /**
     * Exchange rate to base currency (if different from USD)
     */
    private BigDecimal exchangeRate;

    // ========================
    // DISCOUNT
    // ========================

    @Builder.Default
    private DiscountType discountType = DiscountType.NONE;

    private BigDecimal discountValue;

    private String discountReason;

    // ========================
    // TAX & DEPOSIT
    // ========================

    /**
     * Tax rate percentage (e.g., 18.00 for 18%)
     */
    private BigDecimal taxRate;

    /**
     * Deposit percentage (e.g., 50.00 for 50%)
     */
    @Builder.Default
    private BigDecimal depositPercentage = new BigDecimal("50.00");

    // ========================
    // VALIDITY
    // ========================

    /**
     * Number of days the quotation is valid
     */
    @Builder.Default
    private Integer validityDays = 14;

    // ========================
    // CONTENT
    // ========================

    private String termsAndConditions;

    private String inclusions;

    private String exclusions;

    private String internalNotes;

    private String customerNotes;

    // ========================
    // ASSIGNMENT
    // ========================

    private String assignedToId;

    // ========================
    // PAX OVERRIDE
    // ========================

    /**
     * Optional: Override the itinerary pax configuration
     * If not provided, uses the itinerary's pax configuration
     */
    @Valid
    private List<CreateQuotationPaxDTO> paxList;
}
