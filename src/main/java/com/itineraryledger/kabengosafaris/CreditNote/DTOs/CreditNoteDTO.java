package com.itineraryledger.kabengosafaris.CreditNote.DTOs;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.itineraryledger.kabengosafaris.CreditNote.Enums.ConsumptionMethod;
import com.itineraryledger.kabengosafaris.CreditNote.Enums.CreditNoteStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for CreditNote responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditNoteDTO {

    private String id;
    private String creditNoteCode;
    private String title;
    private String description;

    // Invoice relationship
    private String invoiceId;
    private String invoiceCode;

    // Customer relationship
    private String customerId;
    private String customerName;

    // Status
    private CreditNoteStatus status;
    private String statusDisplayName;

    // Multi-currency totals
    private List<PriceDTO> subtotals;
    private List<PriceDTO> taxes;
    private List<PriceDTO> totals;

    // Pricing details
    private BigDecimal taxPercentage;

    // Dates
    private LocalDate issueDate;
    private LocalDate sentDate;
    private LocalDate consumedDate;

    // Consumption
    private ConsumptionMethod consumptionMethod;
    private String consumptionMethodDisplayName;
    private String consumptionNotes;

    // Notes
    private String reason;
    private String internalNotes;
    private String customerNotes;

    // Counts
    private int lineItemCount;

    // Additional
    private Boolean isActive;

    // Audit
    private String createdById;
    private String createdByName;
    private String updatedById;
    private String updatedByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Nested DTO for price display in credit note responses.
     * Includes formatted strings for direct UI rendering.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
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
