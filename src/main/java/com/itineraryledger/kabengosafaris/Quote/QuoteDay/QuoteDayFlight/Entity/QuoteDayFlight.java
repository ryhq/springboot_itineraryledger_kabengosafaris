package com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayFlight.Entity;

import com.itineraryledger.kabengosafaris.Flight.FlightLabels;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.itineraryledger.kabengosafaris.Flight.Entity.FlightFare;
import com.itineraryledger.kabengosafaris.Flight.Entity.FlightRoute;
import com.itineraryledger.kabengosafaris.Flight.Enums.MarkupType;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.Entity.QuoteDay;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A flight on a day of a quote.
 *
 * <p>The quote carries its own copy of the day tree, so a quote already sent keeps the trip it was
 * sent with even after somebody edits the itinerary it came from. This row is the flight half of
 * that copy.
 *
 * <p>The markup override travels down from the itinerary because it is a DECISION about this trip —
 * "flat 50 on this sector for this client" — and dropping it would quietly re-price the flight at
 * the airline's default.
 */
@Entity
@Table(name = "quote_day_flights",
    indexes = {
        @Index(name = "idx_qday_flight_day", columnList = "quote_day_id"),
        @Index(name = "idx_qday_flight_route", columnList = "flight_route_id"),
        @Index(name = "idx_qday_flight_fare", columnList = "flight_fare_id"),
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuoteDayFlight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_day_id", nullable = false)
    @JsonIgnore
    private QuoteDay quoteDay;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flight_route_id", nullable = false)
    private FlightRoute flightRoute;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_fare_id")
    private FlightFare flightFare;

    @Column(name = "passenger_count")
    private Integer passengerCount;

    @Column(name = "is_alternative", nullable = false)
    @Builder.Default
    private Boolean isAlternative = false;

    @Column(name = "is_included_in_price", nullable = false)
    @Builder.Default
    private Boolean isIncludedInPrice = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "markup_type", length = 20)
    private MarkupType markupType;

    @Column(name = "markup_value", precision = 10, scale = 2)
    private BigDecimal markupValue;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public boolean isPrimary() {
        return !Boolean.TRUE.equals(isAlternative);
    }

    /** "Air Excel ARS to ZNZ, 14:00" — see {@link FlightLabels}, which all three day trees share. */
    public String getDisplayName() {
        return FlightLabels.displayName(flightRoute, flightFare);
    }
}
