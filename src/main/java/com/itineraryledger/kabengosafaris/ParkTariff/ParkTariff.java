package com.itineraryledger.kabengosafaris.ParkTariff;

import com.itineraryledger.kabengosafaris.Park.Park;
import com.itineraryledger.kabengosafaris.Tariff.Tariff;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * ParkTariff Join Entity - Manages many-to-many relationship between Parks and Tariffs
 *
 * This entity represents the association between parks and tariffs, allowing:
 * - A park to have multiple tariffs (e.g., entry fee, conservation fee)
 * - A tariff to be applied to multiple parks
 * - Unique constraints to prevent duplicate associations
 * - Independent lifecycle (deleting a park or tariff doesn't affect the other)
 *
 * Rates (ParkTariffRate) are defined per ParkTariff combination,
 * further broken down by season and pax categories.
 */
@Entity
@Table(name = "parks_tariffs",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_park_tariff", columnNames = {"park_id", "tariff_id"})
    },
    indexes = {
        @Index(name = "idx_park_tariff_park_id", columnList = "park_id"),
        @Index(name = "idx_park_tariff_tariff_id", columnList = "tariff_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(ParkTariffId.class)
public class ParkTariff {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "park_id", nullable = false)
    private Park park;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tariff_id", nullable = false)
    private Tariff tariff;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Optional notes about this tariff in this specific park
     * E.g., "Applies only to Seronera gate"
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
