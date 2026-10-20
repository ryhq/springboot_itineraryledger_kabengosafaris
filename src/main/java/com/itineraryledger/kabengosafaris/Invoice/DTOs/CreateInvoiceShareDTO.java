package com.itineraryledger.kabengosafaris.Invoice.DTOs;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * What it takes to make somebody responsible for part of an invoice.
 *
 * <p>A name is required and a customer record is not: the people splitting a family booking are
 * usually not customers in their own right, and demanding five half-empty customer records before
 * five bank transfers can be collected is how a feature goes unused.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInvoiceShareDTO {

    @NotBlank(message = "A share needs a payer name — it is what goes on their receipt")
    @Size(max = 200)
    private String payerName;

    @Email(message = "That payer email is not a valid address")
    @Size(max = 255)
    private String payerEmail;

    /** Optional. Set it when the payer is already a customer, so their history joins up. */
    private String customerId;

    @NotNull(message = "A share needs an amount")
    @DecimalMin(value = "0.01", message = "A share must be for more than nothing")
    private BigDecimal amount;

    /** Defaults to the invoice's own currency when left out. */
    @Size(min = 3, max = 3)
    private String currency;

    @Size(max = 100)
    private String shareReference;

    private String notes;

    private Integer displayOrder;
}
