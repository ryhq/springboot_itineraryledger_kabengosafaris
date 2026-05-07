package com.itineraryledger.kabengosafaris.Expense.Entity;

import com.itineraryledger.kabengosafaris.BankAccount.Entity.BankAccount;
import com.itineraryledger.kabengosafaris.Invoice.Enums.PaymentMethod;
import com.itineraryledger.kabengosafaris.User.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ExpensePayment — a single outgoing payment we made to a vendor against an
 * Expense. Mirrors Payment exactly with cross-currency exchange-rate support.
 */
@Entity
@Table(name = "expense_payments",
    indexes = {
        @Index(name = "idx_ep_expense_id", columnList = "expense_id"),
        @Index(name = "idx_ep_date", columnList = "payment_date"),
        @Index(name = "idx_ep_expense_currency", columnList = "expense_currency")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpensePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    /** Payment amount in {@link #currency}. */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    /** ISO 4217 currency the payment was actually made in. */
    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 50)
    private PaymentMethod paymentMethod;

    /**
     * Currency this payment settles in expense-currency space. For
     * same-currency payments this equals {@link #currency}; for cross-currency
     * payments it is the expense's grand-total currency we're reducing.
     */
    @Column(name = "expense_currency", length = 3)
    private String expenseCurrency;

    /** 1 currency = exchangeRate × expenseCurrency. */
    @Column(name = "exchange_rate", precision = 19, scale = 6)
    private BigDecimal exchangeRate;

    /** amount × exchangeRate, kept for fast aggregation. */
    @Column(name = "base_amount", precision = 15, scale = 2)
    private BigDecimal baseAmount;

    /** Optional: company bank account that funded the payment. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id")
    private BankAccount bankAccount;

    /** External ref: bank txn id, mobile-money id, cheque #, etc. */
    @Column(length = 200)
    private String reference;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_id")
    private User recordedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
