package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Entity.ItineraryDayPark.ParkEntryType;
import com.itineraryledger.kabengosafaris.Park.Park;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.Entity.SafariDay;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.Entity.SafariDayParkActivity;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkTariff.Entity.SafariDayParkTariff;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SafariDayPark Entity - Parks visited on a specific safari day
 *
 * Copied from ItineraryDayPark when Safari is created.
 * Tracks which parks are visited on each day of the safari,
 * including the type of visit (transit, day trip, overnight).
 *
 * Safari-specific additions:
 * - actualArrivalTime: When the group actually arrived
 * - actualDepartureTime: When the group actually departed
 * - entryReceiptNumber: Park entry receipt/ticket number
 * - wildlifeSightings: Notable wildlife seen during the visit
 * - visitNotes: Notes about the actual visit
 */
@Entity
@Table(name = "safari_day_parks",
    indexes = {
        @Index(name = "idx_safari_day_park_day_id", columnList = "safari_day_id"),
        @Index(name = "idx_safari_day_park_park_id", columnList = "park_id"),
        @Index(name = "idx_safari_day_park_entry_type", columnList = "entry_type")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafariDayPark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "safari_day_id", nullable = false)
    @JsonIgnore
    private SafariDay safariDay;

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

    // ========================
    // SAFARI-SPECIFIC FIELDS
    // ========================

    /**
     * Actual time of arrival at the park
     */
    @Column(name = "actual_arrival_time", length = 10)
    private String actualArrivalTime;

    /**
     * Actual time of departure from the park
     */
    @Column(name = "actual_departure_time", length = 10)
    private String actualDepartureTime;

    /**
     * Park entry receipt or ticket number
     */
    @Column(name = "entry_receipt_number", length = 100)
    private String entryReceiptNumber;

    /**
     * Notable wildlife sightings during the visit
     */
    @Lob
    @Column(name = "wildlife_sightings", columnDefinition = "TEXT")
    private String wildlifeSightings;

    /**
     * Notes about the actual park visit
     */
    @Lob
    @Column(name = "visit_notes", columnDefinition = "TEXT")
    private String visitNotes;

    /**
     * Whether entry fees have been paid
     */
    @Builder.Default
    @Column(name = "fees_paid", nullable = false)
    private Boolean feesPaid = false;

    /**
     * When fees were paid
     */
    @Column(name = "fees_paid_at")
    private LocalDateTime feesPaidAt;

    /**
     * Weather conditions during the visit
     */
    @Column(name = "weather_conditions", length = 200)
    private String weatherConditions;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ========================
    // RELATIONSHIPS
    // ========================

    @OneToMany(mappedBy = "safariDayPark", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("sortOrder ASC")
    private List<SafariDayParkActivity> parkActivities = new ArrayList<>();

    @OneToMany(mappedBy = "safariDayPark", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SafariDayParkTariff> parkTariffs = new ArrayList<>();

    // ========================
    // HELPER METHODS
    // ========================

    public void addParkActivity(SafariDayParkActivity parkActivity) {
        parkActivities.add(parkActivity);
        parkActivity.setSafariDayPark(this);
    }

    public void removeParkActivity(SafariDayParkActivity parkActivity) {
        parkActivities.remove(parkActivity);
        parkActivity.setSafariDayPark(null);
    }

    public void addParkTariff(SafariDayParkTariff parkTariff) {
        parkTariffs.add(parkTariff);
        parkTariff.setSafariDayPark(this);
    }

    public void removeParkTariff(SafariDayParkTariff parkTariff) {
        parkTariffs.remove(parkTariff);
        parkTariff.setSafariDayPark(null);
    }

    /**
     * Check if this is an overnight stay
     */
    @Transient
    public boolean isOvernightStay() {
        return entryType == ParkEntryType.SLEEP_OVER;
    }

    /**
     * Mark fees as paid
     */
    public void markFeesPaid(String receiptNumber) {
        this.feesPaid = true;
        this.feesPaidAt = LocalDateTime.now();
        this.entryReceiptNumber = receiptNumber;
    }
}
