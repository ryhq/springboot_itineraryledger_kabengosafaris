package com.itineraryledger.kabengosafaris.Invoice.Entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
 * The invoice status auto-transitions based on total payments vs grand total.
 */
@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payment_invoice_id", columnList = "invoice_id"),
    @Index(name = "idx_payment_date", columnList = "payment_date")
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
