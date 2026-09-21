package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayFlight.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.itineraryledger.kabengosafaris.Flight.Entity.FlightFare;
import com.itineraryledger.kabengosafaris.Flight.Entity.FlightRoute;
import com.itineraryledger.kabengosafaris.Flight.Enums.MarkupType;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.Entity.SafariDay;

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
 * A flight on a day of a safari — a trip that is actually running.
 *
 * <p>Same shape as the quote's, plus the operational state a running trip needs: whether the seat
 * has been asked for, held or ticketed, and the reference the airline gave us. The accommodation
 * rows on a safari already work this way.
 */
@Entity
@Table(name = "safari_day_flights",
    indexes = {
        @Index(name = "idx_sday_flight_day", columnList = "safari_day_id"),
        @Index(name = "idx_sday_flight_route", columnList = "flight_route_id"),
        @Index(name = "idx_sday_flight_fare", columnList = "flight_fare_id"),
        @Index(name = "idx_sday_flight_status", columnList = "booking_status"),
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafariDayFlight {

    /**
     * Where a seat has got to.
     *
     * <p>Its own states rather than the accommodation booking statuses: a seat is TICKETED, which a
     * room never is, and a ticket number is the thing an operations sheet needs at the airstrip.
     */
    public enum BookingStatus {
        /** Sold to the client; nothing asked of the airline yet. This is where every seat starts. */
        PENDING,
        /** The airline is holding it, usually against a deadline. */
        HELD,
        /** Paid and issued. Air Excel issues 15 days before travel or the reservation drops. */
        TICKETED,
        CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "safari_day_id", nullable = false)
    @JsonIgnore
    private SafariDay safariDay;

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

    /**
     * PENDING on arrival, always.
     *
     * <p>A trip that has been sold has not had its seats held, and a row that arrives claiming
     * otherwise is how a party reaches an airstrip without one.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "booking_status", length = 30)
    @Builder.Default
    private BookingStatus bookingStatus = BookingStatus.PENDING;

    @Column(name = "ticket_reference", length = 100)
    private String ticketReference;

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

    /** True once the seat is actually ours, which is the only state worth relying on. */
    public boolean isTicketed() {
        return bookingStatus == BookingStatus.TICKETED;
    }
}
