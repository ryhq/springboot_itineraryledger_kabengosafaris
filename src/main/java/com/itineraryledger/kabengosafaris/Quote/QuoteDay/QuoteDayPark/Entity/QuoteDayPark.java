package com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Entity.ItineraryDayPark.ParkEntryType;
import com.itineraryledger.kabengosafaris.Park.Park;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.Entity.QuoteDay;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkActivity.Entity.QuoteDayParkActivity;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkTariff.Entity.QuoteDayParkTariff;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * QuoteDayPark - parks visited on a quote's day.
 * Snapshotted from ItineraryDayPark. Operational fields live on SafariDayPark.
 */
@Entity
@Table(name = "quote_day_parks",
    indexes = {
        @Index(name = "idx_qday_park_day_id", columnList = "quote_day_id"),
        @Index(name = "idx_qday_park_park_id", columnList = "park_id"),
        @Index(name = "idx_qday_park_entry_type", columnList = "entry_type")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuoteDayPark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_day_id", nullable = false)
    @JsonIgnore
    private QuoteDay quoteDay;

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

    @OneToMany(mappedBy = "quoteDayPark", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("sortOrder ASC")
    private List<QuoteDayParkActivity> parkActivities = new ArrayList<>();

    @OneToMany(mappedBy = "quoteDayPark", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<QuoteDayParkTariff> parkTariffs = new ArrayList<>();

    public void addParkActivity(QuoteDayParkActivity parkActivity) {
        parkActivities.add(parkActivity);
        parkActivity.setQuoteDayPark(this);
    }

    public void removeParkActivity(QuoteDayParkActivity parkActivity) {
        parkActivities.remove(parkActivity);
        parkActivity.setQuoteDayPark(null);
    }

    public void addParkTariff(QuoteDayParkTariff parkTariff) {
        parkTariffs.add(parkTariff);
        parkTariff.setQuoteDayPark(this);
    }

    public void removeParkTariff(QuoteDayParkTariff parkTariff) {
        parkTariffs.remove(parkTariff);
        parkTariff.setQuoteDayPark(null);
    }

    @Transient
    public boolean isOvernightStay() {
        return entryType == ParkEntryType.SLEEP_OVER;
    }
}
