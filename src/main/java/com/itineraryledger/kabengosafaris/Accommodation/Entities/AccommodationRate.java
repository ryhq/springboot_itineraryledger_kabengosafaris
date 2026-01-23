package com.itineraryledger.kabengosafaris.Accommodation.Entities;

import com.itineraryledger.kabengosafaris.Season.Season;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AccommodationRate Entity - Defines pricing rates for accommodation combinations
 *
 * The rate is determined by the combination of:
 * - Accommodation (the property)
 * - Season (pricing period: high/low/peak season)
 * - AccommodationRoomType (bed configuration: single/double/twin)
 * - AccommodationRoomStandard (room quality: standard/deluxe/suite)
 * - AccommodationBoardType (meal plan: room only/B&B/half board/full board)
 *
 * Example: "Serena Hotel + High Season + Double Room + Deluxe + Full Board = $350/night"
 *
 * IMPROVED from old version:
 * - Uses self-referencing Accommodation (no separate branch entity!)
 * - Simplified to focus on core pricing (rack rate and STO rate)
 * - Dual pricing model for direct customers and tour operators
 */
@Entity
@Table(name = "accommodation_rates", indexes = {
    @Index(name = "idx_accommodation_rate_accommodation_id", columnList = "accommodation_id"),
    @Index(name = "idx_accommodation_rate_season_id", columnList = "season_id"),
    @Index(name = "idx_accommodation_rate_room_type_id", columnList = "room_type_id"),
    @Index(name = "idx_accommodation_rate_room_standard_id", columnList = "room_standard_id"),
    @Index(name = "idx_accommodation_rate_board_type_id", columnList = "board_type_id"),
    @Index(name = "idx_accommodation_rate_is_active", columnList = "is_active")
}, uniqueConstraints = {
    @UniqueConstraint(
        name = "uk_accommodation_rate_combination",
        columnNames = {"accommodation_id", "season_id", "room_type_id", "room_standard_id", "board_type_id"}
    )
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccommodationRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "accommodation_id", nullable = false)
    private Accommodation accommodation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_type_id", nullable = false)
    private AccommodationRoomType roomType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_standard_id", nullable = false)
    private AccommodationRoomStandard roomStandard;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "board_type_id", nullable = false)
    private AccommodationBoardType boardType;

    // Core pricing information
    @Column(name = "rack_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal rackRate; // Public/published rate

    @Column(name = "sto_rate", precision = 10, scale = 2)
    private BigDecimal stoRate; // Special Tour Operator rate (discounted)

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD"; // ISO 4217 currency code

    /**
     * Indicates how the rate is charged:
     * - TRUE (default): Per Person Sharing (PPS) - common in safari lodges/camps
     * - FALSE: Per Room - common in hotels/guesthouses
     *
     * Example:
     * - Per Person: Double room at $150/person = $300 total for 2 guests
     * - Per Room: Double room at $250/room = $250 total regardless of occupancy
     */
    @Builder.Default
    @Column(name = "is_per_person", nullable = false)
    private Boolean isPerPerson = true;

    /**
     * Optional notes about this specific rate
     */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * Whether this rate is currently active
     */
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ========================
    // HELPER METHODS
    // ========================

    /**
     * Get the effective rate (uses STO rate if available, else rack rate)
     */
    @Transient
    public BigDecimal getEffectiveRate(boolean useStoRate) {
        if (useStoRate && stoRate != null) {
            return stoRate;
        }
        return rackRate;
    }

    /**
     * Get profit amount (Rack Rate - STO Rate)
     *
     * Rack Rate = price charged to customer (revenue)
     * STO Rate = cost paid on behalf of customer (expense)
     * Profit = Revenue - Expense
     */
    @Transient
    public BigDecimal getProfitAmount() {
        if (stoRate == null || rackRate == null) {
            return BigDecimal.ZERO;
        }
        return rackRate.subtract(stoRate);
    }

    /**
     * Get profit percentage ((Rack Rate - STO Rate) / Rack Rate * 100)
     *
     * Represents the profit margin as a percentage of the charged price.
     */
    @Transient
    public BigDecimal getProfitPercentage() {
        if (stoRate == null || rackRate == null || rackRate.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return rackRate.subtract(stoRate)
            .divide(rackRate, 4, java.math.RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
    }

    /**
     * Check if STO rate is available
     */
    @Transient
    public boolean hasStoRate() {
        return stoRate != null && stoRate.compareTo(BigDecimal.ZERO) > 0;
    }
}
