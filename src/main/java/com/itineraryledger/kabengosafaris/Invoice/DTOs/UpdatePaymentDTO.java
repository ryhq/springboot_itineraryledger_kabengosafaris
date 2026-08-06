package com.itineraryledger.kabengosafaris.Invoice.DTOs;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.itineraryledger.kabengosafaris.Invoice.Enums.PaymentMethod;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating an existing Payment record.
 * All fields are optional — only provided fields are updated.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePaymentDTO {

    private BigDecimal amount;
    private String currency;
    private LocalDate paymentDate;
    /*
     * Enums arrive as Strings so a blank can CLEAR the field; null still means
     * "leave unchanged". Bound as the enum itself, an empty value makes Jackson
     * reject the whole request body, which is how a set value became impossible
     * to unset (see the charging-basis fix).
     */
    private String paymentMethod;

    /**
     * The invoice currency this payment settles.
     * When changed, exchangeRate and baseAmount are recomputed.
     */
    private String invoiceCurrency;

    /**
     * Exchange rate: 1 unit of payment currency = exchangeRate units of invoiceCurrency.
     */
    private BigDecimal exchangeRate;

    /**
     * Optional bank account (obfuscated ID) that received this payment deposit.
     * Pass empty string "" to unlink the current bank account.
     */
    private String bankAccountId;

    private String reference;
    private String notes;

    /**
     * Force update even if it would break the fully-paid status
     * on a safari that has already progressed past FULLY_PAID.
     */
    @Builder.Default
    private Boolean force = false;
}
