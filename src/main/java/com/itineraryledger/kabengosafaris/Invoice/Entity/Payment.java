package com.itineraryledger.kabengosafaris.Invoice.Entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.itineraryledger.kabengosafaris.BankAccount.Entity.BankAccount;
import com.itineraryledger.kabengosafaris.Invoice.Enums.PaymentMethod;
import com.itineraryledger.kabengosafaris.User.User;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payment entity representing an individual payment recorded against an Invoice.
 *
 * Each payment tracks: amount, currency, date, method, and reference.
 * For cross-currency payments (e.g. EUR payment on a USD invoice), the exchangeRate
 * converts the payment amount to the invoice currency: baseAmount = amount × exchangeRate.
 * The invoice status auto-transitions based on total baseAmount vs grand total.
 */
@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payment_invoice_id", columnList = "invoice_id"),
    @Index(name = "idx_payment_date", columnList = "payment_date"),
    @Index(name = "idx_payment_invoice_currency", columnList = "invoice_currency")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    /**
     * Payment amount
     */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    /**
     * Currency code (ISO 4217)
     */
    @Column(nullable = false, length = 3)
    private String currency;

    /**
     * Date the payment was made
     */
    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    /**
     * Payment method used
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PaymentMethod paymentMethod;

    /**
     * The invoice currency this payment settles (one of the invoice's grandTotal currencies).
     * For same-currency payments this equals {@link #currency}.
     * For cross-currency payments this is the target invoice currency (e.g. "USD").
     */
    @Column(name = "invoice_currency", length = 3)
    private String invoiceCurrency;

    /**
     * Exchange rate: 1 unit of payment currency = exchangeRate units of invoiceCurrency.
     * Defaults to 1.000000 for same-currency payments.
     * Example: EUR payment on USD invoice with rate 1.08 means 1 EUR = 1.08 USD.
     */
    @Column(name = "exchange_rate", precision = 19, scale = 6)
    private BigDecimal exchangeRate;

    /**
     * The equivalent amount in invoiceCurrency: amount × exchangeRate.
     * This is the value used for balance calculations against the invoice grand total.
     * For same-currency payments, baseAmount == amount.
     */
    @Column(name = "base_amount", precision = 15, scale = 2)
    private BigDecimal baseAmount;

    /**
     * Optional bank account that received this payment.
     * Null for cash-in-hand or when the deposit account is not tracked.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id")
    private BankAccount bankAccount;

    /**
     * External payment reference (bank ref, transaction ID, receipt number, etc.)
     */
    @Column(length = 200)
    private String reference;

    /**
     * Optional notes about this payment
     */
    @Column(columnDefinition = "TEXT")
    private String notes;

    // =====================================================================
    // AUDIT FIELDS
    // =====================================================================

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
