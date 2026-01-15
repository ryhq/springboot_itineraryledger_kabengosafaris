package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.itineraryledger.kabengosafaris.Activity.Activity;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Entity.ItineraryDay;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ItineraryDayActivity Entity - Standalone activities per day
 *
 * Represents activities that are not tied to a specific park.
 * E.g., "City tour", "Cultural visit", "Airport transfer"
 */
@Entity
@Table(name = "itinerary_day_activities",
    indexes = {
        @Index(name = "idx_itinerary_day_activity_day_id", columnList = "itinerary_day_id"),
        @Index(name = "idx_itinerary_day_activity_activity_id", columnList = "activity_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryDayActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "itinerary_day_id", nullable = false)
    @JsonIgnore
    private ItineraryDay itineraryDay;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

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
