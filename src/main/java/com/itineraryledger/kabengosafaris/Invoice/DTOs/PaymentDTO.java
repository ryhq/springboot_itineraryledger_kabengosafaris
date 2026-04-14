package com.itineraryledger.kabengosafaris.Invoice.DTOs;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.itineraryledger.kabengosafaris.Invoice.Enums.PaymentMethod;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Payment responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDTO {

    private String id;

    // Invoice relationship
    private String invoiceId;
    private String invoiceCode;

    // Payment details
    private BigDecimal amount;
    private String currency;
    private LocalDate paymentDate;

    private PaymentMethod paymentMethod;
    private String paymentMethodDisplayName;

    // Cross-currency support
    private String invoiceCurrency;
    private BigDecimal exchangeRate;
    private BigDecimal baseAmount;

    // Bank account
    private String bankAccountId;
    private String bankAccountName;
    private String bankAccountCode;

    private String reference;
    private String notes;

    // Audit
    private String recordedById;
    private String recordedByName;
    private LocalDateTime createdAt;
}
