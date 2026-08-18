package com.itineraryledger.kabengosafaris.Safari.AvailabilityRequest.Entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.Entity.SafariDayAccommodation;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One night this request asked about — the coverage join.
 *
 * The same shape billing uses to let one expense cover several days: a row per subject rather than
 * a column on the subject. It is what lets a stay answer "has anyone asked about me?", and what
 * lets the guard say "asked about two of these three nights" instead of guessing from the property
 * and the dates.
 *
 * The night's DATE is copied in as well as referenced. The stay row can be edited or removed after
 * the mail has gone, and a request that could no longer say which night it asked about would be a
 * record of nothing.
 */
@Entity
@Table(
    name = "availability_request_stays",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_availability_request_stay",
        columnNames = {"availability_request_id", "safari_day_accommodation_id"}),
    indexes = {
        @Index(name = "idx_ar_stay_request", columnList = "availability_request_id"),
        @Index(name = "idx_ar_stay_stay", columnList = "safari_day_accommodation_id")
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityRequestStay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "availability_request_id", nullable = false)
    private AvailabilityRequest availabilityRequest;

    /**
     * The stay row itself.
     *
     * Nulled rather than cascaded when the stay is deleted: the ask still happened, and a property
     * that was written to about a night since removed is exactly the situation somebody needs to
     * see before writing again.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "safari_day_accommodation_id")
    private SafariDayAccommodation stay;

    @Column(name = "safari_day_id")
    private Long safariDayId;

    @Column(name = "night_date")
    private LocalDate nightDate;

    @Column(name = "day_number")
    private Integer dayNumber;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
