package com.itineraryledger.kabengosafaris.Expense.DTOs;

import com.itineraryledger.kabengosafaris.Invoice.Enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateExpensePaymentDTO {

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3)
    private String currency;

    @NotNull(message = "Payment date is required")
    private LocalDate paymentDate;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    /**
     * Target expense currency this payment settles. Required when the
     * expense has multiple grand-total currencies; auto-inferred when there's
     * only one or when {@link #currency} matches a grand-total currency.
     */
    @Size(min = 3, max = 3)
    private String expenseCurrency;

    /** Required for cross-currency. Defaults to 1 for same-currency. */
    private BigDecimal exchangeRate;

    /** Optional. Obfuscated id of the bank account that funded the payment. */
    private String bankAccountId;

    private String reference;
    private String notes;

    /**
     * Record the bill first, if it is still a draft.
     *
     * Wanting to pay a bill IS the signal that the bill is real, so making somebody leave the
     * drawer, find another button and come back is ceremony rather than safety. Explicit rather
     * than automatic: the caller says it meant to promote the bill, and both happen in one
     * transaction so a bill can never end up recorded with no payment against it.
     */
    private Boolean markRecorded;
}
