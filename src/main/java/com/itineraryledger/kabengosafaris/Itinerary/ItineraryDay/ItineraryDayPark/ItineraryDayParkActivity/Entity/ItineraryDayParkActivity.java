package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Entity.ItineraryDayPark;
import com.itineraryledger.kabengosafaris.ParkActivity.ParkActivity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ItineraryDayParkActivity Entity - Activities within a specific park visit
 *
 * Represents activities that are available in a specific park.
 * References the ParkActivity join entity (Park + Activity combination).
 */
@Entity
@Table(name = "itinerary_day_park_activities",
    indexes = {
        @Index(name = "idx_idpa_day_park_id", columnList = "itinerary_day_park_id"),
        @Index(name = "idx_idpa_park_id", columnList = "park_id"),
        @Index(name = "idx_idpa_activity_id", columnList = "activity_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryDayParkActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "itinerary_day_park_id", nullable = false)
    @JsonIgnore
    private ItineraryDayPark itineraryDayPark;

    /**
     * Reference to ParkActivity (park + activity combination)
     * Uses @JoinColumns for composite key reference
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
        @JoinColumn(name = "park_id", referencedColumnName = "park_id", nullable = false),
        @JoinColumn(name = "activity_id", referencedColumnName = "activity_id", nullable = false)
    })
    private ParkActivity parkActivity;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "duration_hours", precision = 5, scale = 2)
    private BigDecimal durationHours;

    @Column(name = "start_time", length = 10)
    private String startTime; // e.g., "06:00"

    @Column(name = "end_time", length = 10)
    private String endTime;

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
