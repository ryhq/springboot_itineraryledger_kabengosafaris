package com.itineraryledger.kabengosafaris.Flight.Entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A sector one airline flies — Air Excel, Arusha to Zanzibar. 243 of these from one price list.
 *
 * <p>This is the stable thing a trip points at. A planner choosing a flight is choosing a
 * <em>sector</em>, not a departure time: the 12:30 and the 14:00 to Zanzibar are the same decision
 * at the itinerary stage, and which one the guests actually take is settled when the dates are real.
 * That is why {@code ItineraryDayFlight} references a route and only optionally a fare.
 *
 * <p>Unique on (airline, origin, destination). Two rows for Air Excel's Arusha→Zanzibar would be a
 * duplicated import rather than two products; the departures that differ live on {@code FlightFare}.
 */
@Entity
@Table(name = "flight_routes",
    indexes = {
        @Index(name = "idx_flight_route_airline", columnList = "airline_id"),
        @Index(name = "idx_flight_route_origin", columnList = "origin_airstrip_id"),
        @Index(name = "idx_flight_route_destination", columnList = "destination_airstrip_id"),
        @Index(name = "idx_flight_route_is_active", columnList = "is_active"),
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_flight_route_code", columnNames = "code"),
        @UniqueConstraint(name = "uk_flight_route_sector",
            columnNames = { "airline_id", "origin_airstrip_id", "destination_airstrip_id" }),
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 60)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "airline_id", nullable = false)
    private Airline airline;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "origin_airstrip_id", nullable = false)
    private Airstrip originAirstrip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_airstrip_id", nullable = false)
    private Airstrip destinationAirstrip;

    /**
     * 109 of 350 rows on the Air Excel sheet say "N/A" — not scheduled, flown on request only.
     *
     * <p>A trip may still use one; it warns rather than blocks, which is the house convention for
     * an incomplete price everywhere else.
     */
    @Column(name = "is_on_request", nullable = false)
    @Builder.Default
    private Boolean isOnRequest = false;

    @Column(name = "minimum_seats")
    private Integer minimumSeats;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Derived from the row id once it has one, like every other coded record here. */
    public String generateCode() {
        return id == null ? null : String.format("FLR-%05d", id);
    }

    /** "ARS → ZNZ", the way a driver's sheet and a route picker both want to read it. */
    public String getSectorLabel() {
        String from = originAirstrip == null ? "?" : originAirstrip.getCode();
        String to = destinationAirstrip == null ? "?" : destinationAirstrip.getCode();
        return from + " → " + to;
    }
}
