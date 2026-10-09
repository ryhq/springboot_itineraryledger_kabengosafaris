package com.itineraryledger.kabengosafaris.CreditNote.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.CreditNote.Enums.CreditNoteStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * FullCreditNoteDTO - Complete credit note data with all nested entities
 *
 * This is the shape the FULL_CREDIT_NOTE template and its schema have always described.
 * CreditNoteDTO, the flat list/detail DTO, carries only ids and codes for the customer and the
 * invoice, so a document rendered from it printed a credit note that could not say who it was for
 * or what it credited. The template is unchanged; this is the model it was written against.
 *
 * Structure:
 * CreditNote
 * ├── customer (who the credit belongs to)
 * ├── invoice (the invoice it comes off)
 * ├── lineItems (what is being credited, with multi-currency prices)
 * ├── subtotals (by currency)
 * ├── taxes (by currency)
 * └── totals (by currency)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FullCreditNoteDTO {

    // ========================
    // CREDIT NOTE FIELDS
    // ========================
    private String id;
    private String creditNoteCode;
    private String title;
    private String description;
    private CreditNoteStatus status;
    private String statusDisplayName;

    // ========================
    // PRICING DETAILS
    // ========================
    private BigDecimal taxPercentage;

    // ========================
    // DATES
    // ========================
    private LocalDate issueDate;
    private LocalDate sentDate;
    private LocalDate consumedDate;

    // ========================
    // NOTES
    // ========================
    private String reason;
    private String customerNotes;

    /** Never rendered on the client's copy; present so an internal preview can show it. */
    private String internalNotes;

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
    private InvoiceDTO invoice;
    private List<LineItemDTO> lineItems;
    private List<PriceDTO> subtotals;
    private List<PriceDTO> taxes;
    private List<PriceDTO> totals;

    // ========================
    // SUMMARY STATISTICS
    // ========================
    private Integer totalLineItemsCount;
    private Integer totalCurrenciesCount;

    /** Drives the per-currency columns in the template, so it must never be null. */
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
        private String customerCode;
        private String customerName;
        private String email;
        private String phone;
        private String nationality;
        private String address;
        private String city;
        private String country;
    }

    /**
     * The invoice this credit comes off
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class InvoiceDTO {
        private String id;
        private String invoiceCode;
        private String title;
        private String status;
        private String statusDisplayName;
        private LocalDate issueDate;
        private LocalDate dueDate;
        private List<PriceDTO> grandTotals;
    }

    /**
     * Credit note line item
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LineItemDTO {
        private String id;
        private String itemType;
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
