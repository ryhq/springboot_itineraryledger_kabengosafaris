package com.itineraryledger.kabengosafaris.Invoice.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * PaymentReceiptDTO - Data for payment receipt PDF generation.
 * Root variable name in templates: "receipt"
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentReceiptDTO {

    // Payment details
    private String paymentId;
    private BigDecimal amount;
    private String currency;
    private String formattedAmount;
    private LocalDate paymentDate;
    private String paymentMethod;
    private String reference;
    private String notes;
    private String recordedByName;
    private LocalDate receiptDate;

    // Cross-currency support
    private String invoiceCurrency;
    private BigDecimal exchangeRate;
    private BigDecimal baseAmount;
    private String formattedBaseAmount;
    private boolean crossCurrency;

    // Bank account
    private String bankAccountName;

    // Invoice details
    private String invoiceCode;
    private String invoiceTitle;
    private String invoiceStatus;
    private String grandTotal;
    private String totalPaid;
    private String balanceRemaining;

    // Customer
    private CustomerDTO customer;

    // Safari
    private SafariDTO safari;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CustomerDTO {
        private String customerName;
        private String email;
        private String phone;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SafariDTO {
        private String name;
        private String code;
    }
}
