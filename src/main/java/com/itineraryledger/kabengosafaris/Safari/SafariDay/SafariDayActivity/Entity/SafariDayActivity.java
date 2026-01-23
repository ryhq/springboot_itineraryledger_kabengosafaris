package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.itineraryledger.kabengosafaris.Activity.Activity;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.Entity.SafariDay;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SafariDayActivity Entity - Standalone activities per safari day
 *
 * Copied from ItineraryDayActivity when Safari is created.
 * Represents activities that are not tied to a specific park.
 * E.g., "City tour", "Cultural visit", "Airport transfer"
 *
 * Safari-specific additions:
 * - isCompleted: Whether the activity was completed
 * - completedAt: When the activity was completed
 * - actualStartTime: Actual time activity started
 * - actualEndTime: Actual time activity ended
 * - feedback: Guest feedback about this activity
 */
@Entity
@Table(name = "safari_day_activities",
    indexes = {
        @Index(name = "idx_safari_day_activity_day_id", columnList = "safari_day_id"),
        @Index(name = "idx_safari_day_activity_activity_id", columnList = "activity_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafariDayActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "safari_day_id", nullable = false)
    @JsonIgnore
    private SafariDay safariDay;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "duration_hours", precision = 5, scale = 2)
    private BigDecimal durationHours;

    @Column(name = "start_time", length = 10)
    private String startTime; // e.g., "06:00"

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
    private Boolean isOptional = false; // Optional add-on activity

    // ========================
    // SAFARI-SPECIFIC FIELDS
    // ========================

    /**
     * Whether this activity was completed
     */
    @Builder.Default
    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted = false;

    /**
     * When the activity was marked as completed
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Actual time the activity started (for tracking)
     */
    @Column(name = "actual_start_time", length = 10)
    private String actualStartTime;

    /**
     * Actual time the activity ended (for tracking)
     */
    @Column(name = "actual_end_time", length = 10)
    private String actualEndTime;

    /**
     * Guest feedback or comments about this activity
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String feedback;

    /**
     * Whether this activity was skipped
     */
    @Builder.Default
    @Column(name = "is_skipped", nullable = false)
    private Boolean isSkipped = false;

    /**
     * Reason for skipping the activity
     */
    @Column(name = "skip_reason", length = 500)
    private String skipReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ========================
    // HELPER METHODS
    // ========================

    /**
     * Mark activity as completed
     */
    public void markCompleted() {
        this.isCompleted = true;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Mark activity as skipped with reason
     */
    public void markSkipped(String reason) {
        this.isSkipped = true;
        this.skipReason = reason;
    }
}
