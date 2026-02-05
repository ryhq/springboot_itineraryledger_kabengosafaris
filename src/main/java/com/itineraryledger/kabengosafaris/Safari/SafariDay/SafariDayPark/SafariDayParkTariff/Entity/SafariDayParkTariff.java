package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkTariff.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.itineraryledger.kabengosafaris.ParkTariff.ParkTariff;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.Entity.SafariDayPark;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * SafariDayParkTariff Entity - Tariffs for a specific park visit
 *
 * Copied from ItineraryDayParkTariff when Safari is created.
 * Represents the tariffs/fees that apply to a park visit.
 * References the ParkTariff join entity (Park + Tariff combination).
 * E.g., "Park Entry Fee", "Conservation Fee", "Concession Fee"
 *
 * Safari-specific additions:
 * - isPaid: Whether this tariff has been paid
 * - paidAt: When the tariff was paid
 * - receiptNumber: Payment receipt number
 * - paymentNotes: Notes about the payment
 * - paxCount: Number of pax this tariff applies to
 * - isWaived: Whether this tariff was waived
 * - waiverReason: Reason for waiving the tariff
 */
@Entity
@Table(name = "safari_day_park_tariffs",
    indexes = {
        @Index(name = "idx_sdpt_day_park_id", columnList = "safari_day_park_id"),
        @Index(name = "idx_sdpt_park_id", columnList = "park_id"),
        @Index(name = "idx_sdpt_tariff_id", columnList = "tariff_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafariDayParkTariff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "safari_day_park_id", nullable = false)
    @JsonIgnore
    private SafariDayPark safariDayPark;

    /**
     * Reference to ParkTariff (park + tariff combination)
     * Uses @JoinColumns for composite key reference
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
        @JoinColumn(name = "park_id", referencedColumnName = "park_id", nullable = false),
        @JoinColumn(name = "tariff_id", referencedColumnName = "tariff_id", nullable = false)
    })
    private ParkTariff parkTariff;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    @Column(name = "is_included_in_price", nullable = false)
    private Boolean isIncludedInPrice = true;

    // ========================
    // SAFARI-SPECIFIC FIELDS
    // ========================

    /**
     * Whether this tariff has been paid
     */
    @Builder.Default
    @Column(name = "is_paid", nullable = false)
    private Boolean isPaid = false;

    /**
     * When the tariff was paid
     */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /**
     * Payment receipt number
     */
    @Column(name = "receipt_number", length = 100)
    private String receiptNumber;

    /**
     * Notes about the payment
     */
    @Lob
    @Column(name = "payment_notes", columnDefinition = "TEXT")
    private String paymentNotes;

    /**
     * Number of pax this tariff applies to
     */
    @Column(name = "pax_count")
    private Integer paxCount;

    /**
     * Whether this tariff was waived
     */
    @Builder.Default
    @Column(name = "is_waived", nullable = false)
    private Boolean isWaived = false;

    /**
     * Reason for waiving the tariff
     */
    @Column(name = "waiver_reason", length = 500)
    private String waiverReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ========================
    // HELPER METHODS
    // ========================

    /**
     * Mark tariff as paid
     */
    public void markPaid(String receipt) {
        this.isPaid = true;
        this.paidAt = LocalDateTime.now();
        this.receiptNumber = receipt;
    }

    /**
     * Waive this tariff with reason
     */
    public void waive(String reason) {
        this.isWaived = true;
        this.waiverReason = reason;
    }
}
