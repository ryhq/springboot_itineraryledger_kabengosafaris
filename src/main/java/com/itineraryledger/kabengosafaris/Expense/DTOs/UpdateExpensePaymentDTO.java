package com.itineraryledger.kabengosafaris.Expense.DTOs;

import com.itineraryledger.kabengosafaris.Invoice.Enums.PaymentMethod;
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
public class UpdateExpensePaymentDTO {

    private BigDecimal amount;

    @Size(min = 3, max = 3)
    private String currency;

    private LocalDate paymentDate;
    private PaymentMethod paymentMethod;

    @Size(min = 3, max = 3)
    private String expenseCurrency;

    private BigDecimal exchangeRate;

    /** Pass "" to detach the bank account; null = leave unchanged. */
    private String bankAccountId;

    private String reference;
    private String notes;

    /** Allow breaking PAID expenses if true. */
    @Builder.Default
    private Boolean force = false;
}
