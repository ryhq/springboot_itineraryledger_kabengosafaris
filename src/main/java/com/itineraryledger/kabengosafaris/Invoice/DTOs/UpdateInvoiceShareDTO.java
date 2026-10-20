package com.itineraryledger.kabengosafaris.Invoice.DTOs;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A patch to one share. Null means "leave it alone", matching every other update DTO here, so a
 * click-to-edit save of one field cannot clobber the rest.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInvoiceShareDTO {

    @Size(max = 200)
    private String payerName;

    @Email(message = "That payer email is not a valid address")
    @Size(max = 255)
    private String payerEmail;

    private String customerId;

    @DecimalMin(value = "0.01", message = "A share must be for more than nothing")
    private BigDecimal amount;

    @Size(min = 3, max = 3)
    private String currency;

    @Size(max = 100)
    private String shareReference;

    private String notes;

    private Integer displayOrder;

    private Boolean isActive;
}
