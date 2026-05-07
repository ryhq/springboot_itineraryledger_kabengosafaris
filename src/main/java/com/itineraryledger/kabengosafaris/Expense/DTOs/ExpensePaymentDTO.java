package com.itineraryledger.kabengosafaris.Expense.DTOs;

import com.itineraryledger.kabengosafaris.Invoice.Enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpensePaymentDTO {

    private String id;

    private String expenseId;
    private String expenseCode;

    private BigDecimal amount;
    private String currency;
    private LocalDate paymentDate;
    private PaymentMethod paymentMethod;
    private String paymentMethodDisplayName;

    private String expenseCurrency;
    private BigDecimal exchangeRate;
    private BigDecimal baseAmount;

    private String bankAccountId;
    private String bankAccountName;
    private String bankAccountCode;

    private String reference;
    private String notes;

    private String recordedById;
    private String recordedByName;
    private LocalDateTime createdAt;
}
