package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Entity.ItineraryDayPark;
import com.itineraryledger.kabengosafaris.ParkTariff.ParkTariff;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * ItineraryDayParkTariff Entity - Tariffs for a specific park visit
 *
 * Represents the tariffs/fees that apply to a park visit.
 * References the ParkTariff join entity (Park + Tariff combination).
 * E.g., "Park Entry Fee", "Conservation Fee", "Concession Fee"
 */
@Entity
@Table(name = "itinerary_day_park_tariffs",
    indexes = {
        @Index(name = "idx_idpt_day_park_id", columnList = "itinerary_day_park_id"),
        @Index(name = "idx_idpt_park_id", columnList = "park_id"),
        @Index(name = "idx_idpt_tariff_id", columnList = "tariff_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryDayParkTariff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "itinerary_day_park_id", nullable = false)
    @JsonIgnore
    private ItineraryDayPark itineraryDayPark;

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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
