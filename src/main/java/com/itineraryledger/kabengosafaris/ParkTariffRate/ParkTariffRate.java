package com.itineraryledger.kabengosafaris.ParkTariffRate;

import com.itineraryledger.kabengosafaris.ParkTariff.ParkTariff;
import com.itineraryledger.kabengosafaris.PaxAgeCategory.PaxAgeCategory;
import com.itineraryledger.kabengosafaris.PaxNationCategory.PaxNationCategory;
import com.itineraryledger.kabengosafaris.Season.Season;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ParkTariffRate Entity - Defines pricing rates for park tariffs
 *
 * The rate is determined by the combination of:
 * - ParkTariff (the park-tariff association)
 * - Season (pricing period: high/low/peak season)
 * - PaxNationCategory (nationality category: resident/non-resident/etc.)
 * - PaxAgeCategory (age category - optional based on charging basis)
 *
 * Example: "Serengeti + Park Entry Fee + High Season + Non-Resident + Adult = $70/person"
 *
 * Rate Selection Logic:
 * - For PER_PERSON tariffs: Rate = Season + NationCategory + AgeCategory
 * - For PER_VEHICLE/PER_GROUP tariffs: Rate = Season + NationCategory (highest priority pax)
 * - For FLAT_RATE tariffs: Rate = Season only
 *
 * Features:
 * - Dual pricing model (rack rate and STO rate for tour operators)
 * - Currency specification
 * - Unique constraint per rate combination
 * - Supports nullable age category for non-PER_PERSON tariffs
 */
@Entity
@Table(name = "park_tariff_rates", indexes = {
    @Index(name = "idx_park_tariff_rate_park_id", columnList = "park_id"),
    @Index(name = "idx_park_tariff_rate_tariff_id", columnList = "tariff_id"),
    @Index(name = "idx_park_tariff_rate_season_id", columnList = "season_id"),
    @Index(name = "idx_park_tariff_rate_nation_category_id", columnList = "nation_category_id"),
    @Index(name = "idx_park_tariff_rate_age_category_id", columnList = "age_category_id"),
    @Index(name = "idx_park_tariff_rate_is_active", columnList = "is_active")
}, uniqueConstraints = {
    @UniqueConstraint(
        name = "uk_park_tariff_rate_combination",
        columnNames = {"park_id", "tariff_id", "season_id", "nation_category_id", "age_category_id"}
    )
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkTariffRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The park-tariff association this rate belongs to
     * Composite reference to Park + Tariff
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
        @JoinColumn(name = "park_id", referencedColumnName = "park_id", nullable = false),
        @JoinColumn(name = "tariff_id", referencedColumnName = "tariff_id", nullable = false)
    })
    private ParkTariff parkTariff;

    /**
     * Season for which this rate applies
     * Global seasons are typically used for park pricing
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    /**
     * Nationality category for rate differentiation
     * E.g., Resident, East African, Non-Resident
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nation_category_id", nullable = false)
    private PaxNationCategory nationCategory;

    /**
     * Age category for PER_PERSON tariffs
     * NULL for non-PER_PERSON tariffs (vehicle, group, flat rate)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "age_category_id")
    private PaxAgeCategory ageCategory;

    /**
     * Rack rate - Public/published rate
     */
    @Column(name = "rack_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal rackRate;

    /**
     * STO rate - Special Tour Operator rate (usually discounted)
     * Can be null if no STO pricing applies
     */
    @Column(name = "sto_rate", precision = 10, scale = 2)
    private BigDecimal stoRate;

    /**
     * Currency code (ISO 4217)
     * Default: USD
     */
    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    /**
     * Optional notes about this specific rate
     * E.g., "Applies to groups of 10+"
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
