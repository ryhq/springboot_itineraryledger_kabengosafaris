package com.itineraryledger.kabengosafaris.Invoice.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Invoice.Enums.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * FullInvoiceDTO - Complete invoice data with all nested entities
 *
 * Structure:
 * Invoice
 * ├── customer (customer information)
 * ├── safari (safari summary)
 * ├── lineItems (invoice line items with multi-currency prices)
 * ├── subtotals (by currency)
 * ├── taxes (by currency)
 * ├── discounts (by currency)
 * ├── grandTotals (by currency)
 * ├── amountsPaid (by currency)
 * ├── balances (by currency)
 * └── bankAccounts (active bank accounts matching invoice currencies)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FullInvoiceDTO {

    // ========================
    // INVOICE FIELDS
    // ========================
    private String id;
    private String invoiceCode;
    private String title;
    private String description;
    private InvoiceStatus status;
    private String statusDisplayName;

    // ========================
    // PRICING DETAILS
    // ========================
    private BigDecimal taxPercentage;
    private BigDecimal discountPercentage;
    private String discountReason;

    // Markup (bakes into per-line-item unit prices; not a separate line)
    private BigDecimal agentCommissionPercentage;
    private String agentCommissionReason;
    private BigDecimal marginUpliftPercentage;
    private String marginUpliftReason;

    // ========================
    // DATES
    // ========================
    private LocalDate issueDate;
    private LocalDate dueDate;
    private LocalDate sentDate;
    private LocalDate paidDate;
    private Boolean isOverdue;

    // ========================
    // NOTES
    // ========================
    private String customerNotes;
    private String internalNotes;
    private String paymentTerms;

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
    private SafariDTO safari;
    private List<LineItemDTO> lineItems;
    private List<PriceDTO> subtotals;
    private List<PriceDTO> taxes;
    private List<PriceDTO> discounts;
    private List<PriceDTO> grandTotals;
    private List<PriceDTO> amountsPaid;
    private List<PriceDTO> balances;
    private List<BankAccountDTO> bankAccounts;

    // ========================
    // SUMMARY STATISTICS
    // ========================
    private Integer totalLineItemsCount;
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
     * Safari summary
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SafariDTO {
        private String id;
        private String name;
        private String code;
        private String state;
        private String stateDisplayName;
        private Integer totalDays;
        private Integer totalNights;
        private LocalDate startDate;
        private LocalDate endDate;
        private String description;
        private String startLocation;
        private String endLocation;
    }

    /**
     * Invoice line item
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

    /**
     * Bank account information for payment
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BankAccountDTO {
        private String accountName;
        private String accountHolderName;
        private String bankName;
        private String bankBranch;
        private String branchAddress;
        private String branchCity;
        private String branchCountry;
        private String accountNumber;
        private String currency;
        private String swiftBicCode;
        private String iban;
        private String routingNumber;
        private String sortCode;
        private String intermediaryBankName;
        private String intermediarySwiftCode;
        private String invoiceDisplayNotes;
    }
}
