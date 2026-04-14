package com.itineraryledger.kabengosafaris.Invoice.DTOs;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.itineraryledger.kabengosafaris.Invoice.Enums.PaymentMethod;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new Payment record
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePaymentDTO {

    @NotBlank(message = "Invoice ID is required")
    private String invoiceId;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotNull(message = "Payment date is required")
    private LocalDate paymentDate;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    /**
     * The invoice currency this payment settles (one of the invoice's grandTotal currencies).
     * Optional — auto-inferred when the invoice has a single currency in grandTotals,
     * or when the payment currency matches a grandTotal currency.
     * Required for cross-currency payments on multi-currency invoices.
     */
    private String invoiceCurrency;

    /**
     * Exchange rate: 1 unit of payment currency = exchangeRate units of invoiceCurrency.
     * Required when payment currency differs from invoiceCurrency.
     * Auto-set to 1 when payment currency equals invoiceCurrency.
     */
    private BigDecimal exchangeRate;

    /**
     * Optional bank account (obfuscated ID) that received this payment deposit.
     * Null for cash-in-hand or when the deposit account is not tracked.
     */
    private String bankAccountId;

    private String reference;
    private String notes;

    /**
     * Whether to send a payment receipt email to the customer (default false).
     */
    @Builder.Default
    private Boolean notifyCustomer = false;
}
