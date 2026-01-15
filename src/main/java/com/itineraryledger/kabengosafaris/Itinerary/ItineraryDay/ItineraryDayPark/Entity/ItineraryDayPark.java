package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Entity.ItineraryDay;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.Entity.ItineraryDayParkActivity;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.Entity.ItineraryDayParkTariff;
import com.itineraryledger.kabengosafaris.Park.Park;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ItineraryDayPark Entity - Parks visited on a specific day
 *
 * Tracks which parks are visited on each day of the itinerary,
 * including the type of visit (transit, day trip, overnight).
 */
@Entity
@Table(name = "itinerary_day_parks",
    indexes = {
        @Index(name = "idx_itinerary_day_park_day_id", columnList = "itinerary_day_id"),
        @Index(name = "idx_itinerary_day_park_park_id", columnList = "park_id"),
        @Index(name = "idx_itinerary_day_park_entry_type", columnList = "entry_type")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryDayPark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "itinerary_day_id", nullable = false)
    @JsonIgnore
    private ItineraryDay itineraryDay;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "park_id", nullable = false)
    private Park park;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 50)
    @Builder.Default
    private ParkEntryType entryType = ParkEntryType.DAY_TRIP;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "arrival_time", length = 10)
    private String arrivalTime;

    @Column(name = "departure_time", length = 10)
    private String departureTime;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ========================
    // RELATIONSHIPS
    // ========================

    @OneToMany(mappedBy = "itineraryDayPark", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("sortOrder ASC")
    private List<ItineraryDayParkActivity> parkActivities = new ArrayList<>();

    @OneToMany(mappedBy = "itineraryDayPark", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ItineraryDayParkTariff> parkTariffs = new ArrayList<>();

    // ========================
    // HELPER METHODS
    // ========================

    public void addParkActivity(ItineraryDayParkActivity parkActivity) {
        parkActivities.add(parkActivity);
        parkActivity.setItineraryDayPark(this);
    }

    public void removeParkActivity(ItineraryDayParkActivity parkActivity) {
        parkActivities.remove(parkActivity);
        parkActivity.setItineraryDayPark(null);
    }

    public void addParkTariff(ItineraryDayParkTariff parkTariff) {
        parkTariffs.add(parkTariff);
        parkTariff.setItineraryDayPark(this);
    }

    public void removeParkTariff(ItineraryDayParkTariff parkTariff) {
        parkTariffs.remove(parkTariff);
        parkTariff.setItineraryDayPark(null);
    }

    /**
     * Check if this is an overnight stay
     */
    @Transient
    public boolean isOvernightStay() {
        return entryType == ParkEntryType.SLEEP_OVER;
    }

    // ========================
    // ENUM
    // ========================

    public enum ParkEntryType {
        TRANSIT("Transit", "Passing through without extended stay"),
        DAY_TRIP("Day Trip", "Full day visit without overnight stay"),
        SLEEP_OVER("Sleep Over", "Overnight stay in the park");

        private final String displayName;
        private final String description;

        ParkEntryType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }
}
