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
    private PaymentMethod paymentMethod;
    private String reference;
    private String notes;

    /**
     * Force update even if it would break the fully-paid status
     * on a safari that has already progressed past FULLY_PAID.
     */
    @Builder.Default
    private Boolean force = false;
}
