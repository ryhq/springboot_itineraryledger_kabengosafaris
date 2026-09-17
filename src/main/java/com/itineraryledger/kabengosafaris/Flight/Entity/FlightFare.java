package com.itineraryledger.kabengosafaris.Flight.Entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.itineraryledger.kabengosafaris.Flight.Enums.MarkupType;

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
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One published line of an airline's price list: a departure, the window it is sold in, what it
 * costs and what it is taxed.
 *
 * <p><strong>There is deliberately no FlightSchedule entity.</strong> The obvious design splits
 * "the 08:00 departure" from "what the 08:00 costs", but the source disproves it: on the Air Excel
 * sheet the fare and the tax both vary <em>per departure</em> — Kilimanjaro to Grumeti is taxed 25
 * on one departure and 30 on another — so a schedule table would be one row per fare row and the
 * join would buy nothing. One published line is one row here.
 *
 * <h2>Two dates and a month list, doing different jobs</h2>
 *
 * <p>{@code validFrom}/{@code validTo} is the contract window and is <strong>what selects this fare
 * for a travel date</strong>. {@code operatingMonths} is which months the route actually flies, and
 * is <strong>advisory only</strong>: the price list's "Season" column says "June - November" but
 * charges the same fare on every one of those days, so it governs availability, never price. Flying
 * outside it raises a warning and nothing more.
 *
 * <h2>Money</h2>
 *
 * <p>{@code netFare} is what we pay and is the only basis for a quote. {@code grossFare} is the
 * airline's published price, kept so a client can be shown what booking direct would cost. The Air
 * Excel sheet publishes gross only, so an import of it leaves {@code netFare} null — and a null net
 * fare raises a rate issue rather than quietly pricing a trip at a number nobody agreed to.
 *
 * <p>{@code taxesAndFees} is per person and is <strong>never marked up</strong>. It is collected on
 * the airline's behalf and the invoice has to reconcile against theirs.
 *
 * <h2>Retired, never deleted</h2>
 *
 * <p>A price list is reissued annually. The new one writes new rows and sets {@code retiredAt} on
 * these; nothing is removed, because an itinerary or a sent quote points here and a sent quote must
 * keep the price it was sent with.
 */
@Entity
@Table(name = "flight_fares",
    indexes = {
        @Index(name = "idx_flight_fare_route", columnList = "flight_route_id"),
        @Index(name = "idx_flight_fare_validity", columnList = "valid_from, valid_to"),
        @Index(name = "idx_flight_fare_is_active", columnList = "is_active"),
        @Index(name = "idx_flight_fare_retired", columnList = "retired_at"),
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_flight_fare_code", columnNames = "code"),
        @UniqueConstraint(name = "uk_flight_fare_departure",
            columnNames = { "flight_route_id", "valid_from", "etd", "operating_months" }),
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightFare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 60)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flight_route_id", nullable = false)
    private FlightRoute flightRoute;

    /** Null on 65 of 350 rows, where the sheet says TBC. A time nobody has fixed is not a time. */
    @Column(name = "etd")
    private LocalTime etd;

    @Column(name = "eta")
    private LocalTime eta;

    /** What makes this departure recognisable as the same one after next year's reissue. */
    @Column(name = "departure_label", length = 40)
    private String departureLabel;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to", nullable = false)
    private LocalDate validTo;

    /**
     * Which months this departure flies: "6,7,8,9,10,11". Part of the row's identity, not decoration
     * — one sector can publish the same departure time with a different arrival in each season.
     *
     * <p>A year-round row stores all twelve explicitly. Null means no window was stated at all,
     * which is what an on-request sector has.
     */
    @Column(name = "operating_months", length = 40)
    private String operatingMonths;

    /** What we pay. Null until the airline's net fares are loaded. */
    @Column(name = "net_fare", precision = 12, scale = 2)
    private BigDecimal netFare;

    /** What the airline publishes. Reference only, never the basis of a quote. */
    @Column(name = "gross_fare", precision = 12, scale = 2)
    private BigDecimal grossFare;

    @Column(name = "taxes_and_fees", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal taxesAndFees = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    /**
     * Air Excel carries children under 14 at exactly 70% of the adult fare on all 350 rows, so this
     * is a rule with an override rather than a second fare column that could drift out of step.
     */
    @Column(name = "child_percent", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal childPercent = new BigDecimal("70.00");

    @Column(name = "minimum_seats")
    private Integer minimumSeats;

    /** Overrides the airline's default. Null means "whatever the airline says". */
    @Enumerated(EnumType.STRING)
    @Column(name = "markup_type", length = 20)
    private MarkupType markupType;

    @Column(name = "markup_value", precision = 10, scale = 2)
    private BigDecimal markupValue;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    /** Set when a newer price list supersedes this row. Never deleted. */
    @Column(name = "retired_at")
    private LocalDateTime retiredAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public String generateCode() {
        return id == null ? null : String.format("FLF-%05d", id);
    }

    @Transient
    public boolean isRetired() {
        return retiredAt != null;
    }

    @Transient
    public boolean hasNetFare() {
        return netFare != null && netFare.signum() > 0;
    }

    @Transient
    public boolean hasMarkup() {
        return markupType != null && markupValue != null;
    }

    /** True when this fare's contract window covers the date a trip actually flies. */
    @Transient
    public boolean coversDate(LocalDate date) {
        if (date == null || validFrom == null || validTo == null) return false;
        return !date.isBefore(validFrom) && !date.isAfter(validTo);
    }

    /**
     * True when the route flies in that date's month.
     *
     * <p>Advisory. A false answer means "this route is out of season then, ask the airline", not
     * "there is no price": the price is the same all year, which is exactly why the operating window
     * is kept apart from the validity window.
     */
    @Transient
    public boolean fliesIn(LocalDate date) {
        if (date == null) return false;
        if (operatingMonths == null || operatingMonths.isBlank()) return true;
        String wanted = String.valueOf(date.getMonthValue());
        for (String month : operatingMonths.split(",")) {
            if (month.trim().equals(wanted)) return true;
        }
        return false;
    }
}
