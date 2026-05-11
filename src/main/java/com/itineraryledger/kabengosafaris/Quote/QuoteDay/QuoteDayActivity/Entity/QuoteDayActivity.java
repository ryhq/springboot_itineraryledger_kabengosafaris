package com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayActivity.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.itineraryledger.kabengosafaris.Activity.Activity;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.Entity.QuoteDay;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * QuoteDayActivity - standalone activity on a quote's day (not park-bound).
 * Snapshotted from ItineraryDayActivity. Operational completion/feedback
 * fields live on SafariDayActivity.
 */
@Entity
@Table(name = "quote_day_activities",
    indexes = {
        @Index(name = "idx_qday_activity_day_id", columnList = "quote_day_id"),
        @Index(name = "idx_qday_activity_activity_id", columnList = "activity_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuoteDayActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_day_id", nullable = false)
    @JsonIgnore
    private QuoteDay quoteDay;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "duration_hours", precision = 5, scale = 2)
    private BigDecimal durationHours;

    @Column(name = "start_time", length = 10)
    private String startTime;

    @Column(name = "end_time", length = 10)
    private String endTime;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    @Column(name = "is_included_in_price", nullable = false)
    private Boolean isIncludedInPrice = true;

    @Builder.Default
    @Column(name = "is_optional", nullable = false)
    private Boolean isOptional = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
