package com.itineraryledger.kabengosafaris.Invoice.DTOs;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One payer's share of a split invoice, as the panel and the client paperwork read it.
 *
 * <p>{@code amountPaid}, {@code balance} and {@code status} are computed at read time from the
 * payments attached to this share. They are not columns, so they cannot disagree with the money.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InvoiceShareDTO {

    private String id;
    private String invoiceId;
    private String invoiceCode;

    private String payerName;
    private String payerEmail;
    private String customerId;
    private String customerName;

    private BigDecimal amount;
    private String currency;
    private String shareReference;
    private String notes;
    private Integer displayOrder;
    private Boolean isActive;

    /** Summed from this share's payments, never stored. */
    private BigDecimal amountPaid;
    /** amount − amountPaid. Negative means this payer has sent more than their share. */
    private BigDecimal balance;
    /** UNPAID · PARTIALLY_PAID · PAID, derived from the two figures above. */
    private String status;
    private Integer paymentCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
