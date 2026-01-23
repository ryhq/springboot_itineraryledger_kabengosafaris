package com.itineraryledger.kabengosafaris.Quotation.DTOs;

import com.itineraryledger.kabengosafaris.Quotation.Enums.DiscountType;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * UpdateQuotationDTO - Request DTO for updating a quotation
 * All fields are optional - only provided fields will be updated
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateQuotationDTO {

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

    private String currency;

    private BigDecimal exchangeRate;

    private DiscountType discountType;

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

    private String inclusions;

    private String exclusions;

    private String internalNotes;

    private String customerNotes;

    // ========================
    // ASSIGNMENT
    // ========================

    private String assignedToId; // Obfuscated user ID
}
