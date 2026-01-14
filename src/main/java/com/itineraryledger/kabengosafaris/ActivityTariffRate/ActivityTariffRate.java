package com.itineraryledger.kabengosafaris.ActivityTariffRate;

import com.itineraryledger.kabengosafaris.Activity.Activity;
import com.itineraryledger.kabengosafaris.Park.Park;
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
 * ActivityTariffRate Entity - Defines pricing rates for activity tariffs
 *
 * The rate is determined by the combination of:
 * - Activity (the activity being priced)
 * - Park (optional - for park-specific activity pricing)
 * - Season (pricing period: high/low/peak season)
 * - PaxNationCategory (nationality category: resident/non-resident/etc.)
 * - PaxAgeCategory (optional - for PER_PERSON charging basis activities)
 *
 * Key Features:
 * - Park is OPTIONAL: Activities can have global rates (no park) or park-specific rates
 * - PaxAgeCategory is OPTIONAL: Only required for PER_PERSON activities
 * - Dual pricing: rack rate (public) and STO rate (tour operators)
 *
 * Examples:
 * - "Game Drive + High Season + Non-Resident + Adult = $50/person" (global rate)
 * - "Game Drive + Serengeti + High Season + Non-Resident + Adult = $70/person" (park-specific)
 * - "Vehicle Transfer + High Season + Non-Resident = $150/vehicle" (no age category)
 *
 * Rate Selection Logic:
 * - For PER_PERSON activities: Rate = Activity + [Park] + Season + NationCategory + AgeCategory
 * - For PER_VEHICLE/PER_GROUP activities: Rate = Activity + [Park] + Season + NationCategory
 */
@Entity
@Table(name = "activity_tariff_rates", indexes = {
    @Index(name = "idx_activity_tariff_rate_activity_id", columnList = "activity_id"),
    @Index(name = "idx_activity_tariff_rate_park_id", columnList = "park_id"),
    @Index(name = "idx_activity_tariff_rate_season_id", columnList = "season_id"),
    @Index(name = "idx_activity_tariff_rate_nation_category_id", columnList = "nation_category_id"),
    @Index(name = "idx_activity_tariff_rate_age_category_id", columnList = "age_category_id"),
    @Index(name = "idx_activity_tariff_rate_is_active", columnList = "is_active")
}, uniqueConstraints = {
    @UniqueConstraint(
        name = "uk_activity_tariff_rate_combination",
        columnNames = {"activity_id", "park_id", "season_id", "nation_category_id", "age_category_id"}
    )
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityTariffRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The activity this rate applies to
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    /**
     * Optional park for park-specific rates
     * NULL = Global rate (applies when activity is used anywhere)
     * NOT NULL = Park-specific rate (applies only when activity is used at this park)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "park_id")
    private Park park;

    /**
     * Season for which this rate applies
     * Global seasons are typically used for activity pricing
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
     * Age category - required for PER_PERSON activities, null for others
     * E.g., Child, Youth, Adult
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
     * Check if this is a global rate (not park-specific)
     */
    @Transient
    public boolean isGlobalRate() {
        return park == null;
    }

    /**
     * Check if this is a park-specific rate
     */
    @Transient
    public boolean isParkSpecificRate() {
        return park != null;
    }

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
