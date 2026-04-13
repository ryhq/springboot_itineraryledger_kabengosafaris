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

    private String reference;
    private String notes;

    /**
     * Whether to send a payment receipt email to the customer (default false).
     */
    @Builder.Default
    private Boolean notifyCustomer = false;
}
