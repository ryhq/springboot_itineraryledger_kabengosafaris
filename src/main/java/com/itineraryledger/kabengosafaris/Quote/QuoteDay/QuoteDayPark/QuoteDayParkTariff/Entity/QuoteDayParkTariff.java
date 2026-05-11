package com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkTariff.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.itineraryledger.kabengosafaris.ParkTariff.ParkTariff;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.Entity.QuoteDayPark;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * QuoteDayParkTariff - tariff applied to a park visit on a quote.
 * Snapshotted from ItineraryDayParkTariff. Payment/receipt fields live on SafariDayParkTariff.
 */
@Entity
@Table(name = "quote_day_park_tariffs",
    indexes = {
        @Index(name = "idx_qdpt_day_park_id", columnList = "quote_day_park_id"),
        @Index(name = "idx_qdpt_park_id", columnList = "park_id"),
        @Index(name = "idx_qdpt_tariff_id", columnList = "tariff_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuoteDayParkTariff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_day_park_id", nullable = false)
    @JsonIgnore
    private QuoteDayPark quoteDayPark;

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
