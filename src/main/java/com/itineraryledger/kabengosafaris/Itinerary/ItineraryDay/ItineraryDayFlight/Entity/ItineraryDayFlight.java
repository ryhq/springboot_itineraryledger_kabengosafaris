package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayFlight.Entity;

import com.itineraryledger.kabengosafaris.Flight.FlightLabels;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.itineraryledger.kabengosafaris.Flight.Entity.FlightFare;
import com.itineraryledger.kabengosafaris.Flight.Entity.FlightRoute;
import com.itineraryledger.kabengosafaris.Flight.Enums.MarkupType;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Entity.ItineraryDay;

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
 * A flight on one day of a trip.
 *
 * <p>Shaped like {@code ItineraryDayAccommodation}, because it is the same idea: a choice attached
 * to a day, with alternatives sitting beside the primary and kept out of the total.
 *
 * <p><strong>The fare is nullable and that is the point.</strong> An itinerary is a product, not a
 * booking. A planner choosing "Arusha to Zanzibar" is choosing a <em>sector</em>; whether the party
 * takes the 12:30 or the 14:00 is settled when the dates are real and the seats are held. A line
 * with a route but no fare still prices — off the cheapest current fare for that route — and says
 * that is what it did.
 */
@Entity
@Table(name = "itinerary_day_flights",
    indexes = {
        @Index(name = "idx_iday_flight_day", columnList = "itinerary_day_id"),
        @Index(name = "idx_iday_flight_route", columnList = "flight_route_id"),
        @Index(name = "idx_iday_flight_fare", columnList = "flight_fare_id"),
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryDayFlight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "itinerary_day_id", nullable = false)
    @JsonIgnore
    private ItineraryDay itineraryDay;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flight_route_id", nullable = false)
    private FlightRoute flightRoute;

    /** The chosen departure, once somebody has chosen one. See the class note. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_fare_id")
    private FlightFare flightFare;

    /** Null means everybody on the trip — the usual case, and the one that survives a pax change. */
    @Column(name = "passenger_count")
    private Integer passengerCount;

    /**
     * An option rather than the plan. Alternatives are costed and shown, never added to the total —
     * the same rule the accommodation calculator already follows.
     */
    @Column(name = "is_alternative", nullable = false)
    @Builder.Default
    private Boolean isAlternative = false;

    @Column(name = "is_included_in_price", nullable = false)
    @Builder.Default
    private Boolean isIncludedInPrice = true;

    /** Beats the fare's markup, which beats the airline's. Null means "inherit". */
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

    /** "Air Excel ARS → ZNZ, 12:30" — how a day reads on a driver's sheet. */
    /** "Air Excel ARS to ZNZ, 14:00" — see {@link FlightLabels}, which all three day trees share. */
    public String getDisplayName() {
        return FlightLabels.displayName(flightRoute, flightFare);
    }
}
