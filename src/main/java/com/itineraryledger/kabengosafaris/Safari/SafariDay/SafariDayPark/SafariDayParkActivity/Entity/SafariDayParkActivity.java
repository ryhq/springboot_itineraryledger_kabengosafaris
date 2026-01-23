package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.itineraryledger.kabengosafaris.ParkActivity.ParkActivity;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.Entity.SafariDayPark;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SafariDayParkActivity Entity - Activities within a specific park visit
 *
 * Copied from ItineraryDayParkActivity when Safari is created.
 * Represents activities that are available in a specific park.
 * References the ParkActivity join entity (Park + Activity combination).
 *
 * Safari-specific additions:
 * - isCompleted: Whether the activity was completed
 * - completedAt: When the activity was completed
 * - actualDurationHours: Actual time spent on the activity
 * - sightingsNotes: Notable sightings during this activity
 * - guestExperience: Guest feedback about this activity
 */
@Entity
@Table(name = "safari_day_park_activities",
    indexes = {
        @Index(name = "idx_sdpa_day_park_id", columnList = "safari_day_park_id"),
        @Index(name = "idx_sdpa_park_id", columnList = "park_id"),
        @Index(name = "idx_sdpa_activity_id", columnList = "activity_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafariDayParkActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "safari_day_park_id", nullable = false)
    @JsonIgnore
    private SafariDayPark safariDayPark;

    /**
     * Reference to ParkActivity (park + activity combination)
     * Uses @JoinColumns for composite key reference
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
        @JoinColumn(name = "park_id", referencedColumnName = "park_id", nullable = false),
        @JoinColumn(name = "activity_id", referencedColumnName = "activity_id", nullable = false)
    })
    private ParkActivity parkActivity;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "duration_hours", precision = 5, scale = 2)
    private BigDecimal durationHours;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    @Column(name = "is_included_in_price", nullable = false)
    private Boolean isIncludedInPrice = true;

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
     * Actual duration of the activity in hours
     */
    @Column(name = "actual_duration_hours", precision = 5, scale = 2)
    private BigDecimal actualDurationHours;

    /**
     * Notable sightings during this specific activity
     * E.g., "Spotted leopard with cubs near watering hole"
     */
    @Lob
    @Column(name = "sightings_notes", columnDefinition = "TEXT")
    private String sightingsNotes;

    /**
     * Guest experience/feedback for this activity
     */
    @Lob
    @Column(name = "guest_experience", columnDefinition = "TEXT")
    private String guestExperience;

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

    /**
     * Start time of the activity
     */
    @Column(name = "start_time", length = 10)
    private String startTime;

    /**
     * End time of the activity
     */
    @Column(name = "end_time", length = 10)
    private String endTime;

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
     * Mark activity as completed with actual duration
     */
    public void markCompleted(BigDecimal actualDuration) {
        this.isCompleted = true;
        this.completedAt = LocalDateTime.now();
        this.actualDurationHours = actualDuration;
    }

    /**
     * Mark activity as skipped with reason
     */
    public void markSkipped(String reason) {
        this.isSkipped = true;
        this.skipReason = reason;
    }
}
