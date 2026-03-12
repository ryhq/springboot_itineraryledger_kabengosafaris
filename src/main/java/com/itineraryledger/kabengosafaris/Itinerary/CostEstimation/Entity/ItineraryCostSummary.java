package com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Entity;

import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "itinerary_cost_summaries",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_itinerary_currency", columnNames = {"itinerary_id", "currency"})
    },
    indexes = {
        @Index(name = "idx_ics_itinerary_id", columnList = "itinerary_id"),
        @Index(name = "idx_ics_currency", columnList = "currency")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryCostSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "itinerary_id", nullable = false)
    private Itinerary itinerary;

    @Column(nullable = false, length = 3)
    private String currency;

    // Rack prices (public-facing)
    @Column(name = "accommodation_rack", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal accommodationRack = BigDecimal.ZERO;

    @Column(name = "park_fees_rack", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal parkFeesRack = BigDecimal.ZERO;

    @Column(name = "activities_rack", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal activitiesRack = BigDecimal.ZERO;

    @Column(name = "grand_total_rack", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal grandTotalRack = BigDecimal.ZERO;

    // STO prices (internal only)
    @Column(name = "accommodation_sto", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal accommodationSto = BigDecimal.ZERO;

    @Column(name = "park_fees_sto", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal parkFeesSto = BigDecimal.ZERO;

    @Column(name = "activities_sto", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal activitiesSto = BigDecimal.ZERO;

    @Column(name = "grand_total_sto", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal grandTotalSto = BigDecimal.ZERO;

    @Column(name = "has_incomplete_rates", nullable = false)
    @Builder.Default
    private Boolean hasIncompleteRates = false;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    @Column(name = "start_date_used", nullable = false)
    private LocalDate startDateUsed;
}
