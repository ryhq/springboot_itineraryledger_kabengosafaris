package com.itineraryledger.kabengosafaris.Safari.Entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.itineraryledger.kabengosafaris.Customer.Entity.Customer;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Safari.Enums.SafariPhase;
import com.itineraryledger.kabengosafaris.Safari.Enums.SafariState;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.Entity.SafariDay;
import com.itineraryledger.kabengosafaris.Safari.SafariPax.Entity.SafariPax;
import com.itineraryledger.kabengosafaris.User.User;

import java.time.temporal.ChronoUnit;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Safari Entity - Represents an actual safari booking/instance
 *
 * A Safari is created from an Itinerary template with actual dates.
 * It inherits the structure from the Itinerary but can be independently modified.
 *
 * Key differences from Itinerary:
 * - Has concrete start/end dates
 * - Has state lifecycle (DRAFT → CONFIRMED → UPCOMING → ONGOING → COMPLETED)
 * - Can be cancelled, postponed, or put on hold
 * - Links back to source Itinerary (set null if itinerary deleted)
 */
@Entity
@Table(name = "safaris",
    indexes = {
        @Index(name = "idx_safari_itinerary_id", columnList = "itinerary_id"),
        @Index(name = "idx_safari_customer_id", columnList = "customer_id"),
        @Index(name = "idx_safari_code", columnList = "code"),
        @Index(name = "idx_safari_state", columnList = "state"),
        @Index(name = "idx_safari_start_date", columnList = "start_date"),
        @Index(name = "idx_safari_end_date", columnList = "end_date"),
        @Index(name = "idx_safari_is_active", columnList = "is_active"),
        @Index(name = "idx_safari_created_by_id", columnList = "created_by_id"),
        @Index(name = "idx_safari_updated_by_id", columnList = "updated_by_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_safari_code", columnNames = {"code"})
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Safari {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========================
    // ITINERARY REFERENCE
    // ========================

    /**
     * Reference to the source Itinerary template.
     * Set to NULL if the Itinerary is deleted (keeps Safari intact).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_id", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JsonIgnore
    private Itinerary itinerary;

    /**
     * The customer this safari is for.
     * Every safari must be associated with a customer.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnore
    private Customer customer;

    // ========================
    // SAFARI IDENTIFICATION
    // ========================

    @NotBlank(message = "Safari name is required")
    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 50, unique = true)
    private String code; // Auto-generated: SAF-5D4N-00001

    @Column(length = 250, unique = true)
    private String slug;


    // ========================
    // DATES
    // ========================

    @NotNull(message = "Start date is required")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    // ========================
    // STATE & STATUS
    // ========================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private SafariState state = SafariState.DRAFT;

    /**
     * Reason for cancellation or postponement
     */
    @Column(name = "state_reason", length = 500)
    private String stateReason;

    /**
     * Date when state was last changed
     */
    @Column(name = "state_changed_at")
    private LocalDateTime stateChangedAt;

    // ========================
    // DURATION & CAPACITY
    // ========================

    @Min(value = 1, message = "Total days must be at least 1")
    @Column(name = "total_days", nullable = false)
    private Integer totalDays;

    @Min(value = 0, message = "Total nights cannot be negative")
    @Column(name = "total_nights", nullable = false)
    private Integer totalNights;

    @Min(value = 1, message = "Car count must be at least 1")
    @Column(name = "car_count", nullable = false)
    @Builder.Default
    private Integer carCount = 1;

    // ========================
    // DESCRIPTIONS
    // ========================

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String highlights;

    @Column(name = "start_location", length = 200)
    private String startLocation;

    @Column(name = "end_location", length = 200)
    private String endLocation;

    // ========================
    // SAFARI-SPECIFIC NOTES
    // ========================

    /**
     * Special requests from the client
     */
    @Lob
    @Column(name = "special_requests", columnDefinition = "TEXT")
    private String specialRequests;

    /**
     * Dietary requirements or restrictions
     */
    @Lob
    @Column(name = "dietary_requirements", columnDefinition = "TEXT")
    private String dietaryRequirements;

    /**
     * Internal notes for staff
     */
    @Lob
    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;

    /**
     * Emergency contact information
     */
    @Column(name = "emergency_contact", length = 500)
    private String emergencyContact;

    // ========================
    // METADATA
    // ========================

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // ========================
    // AUDIT FIELDS
    // ========================

    /**
     * User who created this safari
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    /**
     * User who last updated this safari
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_id")
    private User updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ========================
    // RELATIONSHIPS
    // ========================

    @OneToMany(mappedBy = "safari", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SafariPax> paxList = new ArrayList<>();

    @OneToMany(mappedBy = "safari", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("dayNumber ASC")
    private List<SafariDay> days = new ArrayList<>();

    // ========================
    // HELPER METHODS
    // ========================

    public void addPax(SafariPax pax) {
        paxList.add(pax);
        pax.setSafari(this);
    }

    public void removePax(SafariPax pax) {
        paxList.remove(pax);
        pax.setSafari(null);
    }

    public void addDay(SafariDay day) {
        days.add(day);
        day.setSafari(this);
    }

    public void removeDay(SafariDay day) {
        days.remove(day);
        day.setSafari(null);
    }

    /**
     * Get total pax count across all categories
     */
    @Transient
    public int getTotalPaxCount() {
        return paxList.stream()
            .mapToInt(pax -> pax.getCount() != null ? pax.getCount() : 0)
            .sum();
    }

    /**
     * Check if safari can be edited
     */
    @Transient
    public boolean isEditable() {
        return state.isEditable();
    }

    /**
     * Check if safari can be cancelled
     */
    @Transient
    public boolean isCancellable() {
        return state.isCancellable();
    }

    /**
     * Get display name with dates
     */
    @Transient
    public String getDisplayName() {
        return String.format("%s (%s - %s)", name, startDate, endDate);
    }

    /**
     * Check if safari has started
     */
    @Transient
    public boolean hasStarted() {
        return !LocalDate.now().isBefore(startDate);
    }

    /**
     * Check if safari has ended
     */
    @Transient
    public boolean hasEnded() {
        return LocalDate.now().isAfter(endDate);
    }

    // ========================
    // LIFECYCLE CALLBACKS
    // ========================

    @PrePersist
    protected void onCreate() {
        if (this.state == null) {
            this.state = SafariState.DRAFT;
        }
        this.stateChangedAt = LocalDateTime.now();
    }

    /**
     * Manually change state with reason
     */
    public void changeState(SafariState newState, String reason) {
        this.state = newState;
        this.stateReason = reason;
        this.stateChangedAt = LocalDateTime.now();
    }

    // ========================
    // PHASE CALCULATION (TIME-BASED)
    // ========================

    /**
     * Get the current time-based phase of the safari.
     * This is automatically calculated based on dates.
     */
    @Transient
    public SafariPhase getCurrentPhase() {
        LocalDate today = LocalDate.now();
        long daysUntilStart = ChronoUnit.DAYS.between(today, startDate);
        long daysSinceEnd = ChronoUnit.DAYS.between(endDate, today);

        // Pre-safari phases
        if (today.isBefore(startDate)) {
            if (daysUntilStart > 30) {
                return SafariPhase.FAR_FUTURE;
            } else if (daysUntilStart >= 8) {
                return SafariPhase.UPCOMING;
            } else if (daysUntilStart >= 3) {
                return SafariPhase.STARTING_SOON;
            } else if (daysUntilStart >= 1) {
                return SafariPhase.IMMINENT;
            } else {
                return SafariPhase.TODAY;
            }
        }

        // During safari phases
        if (!today.isBefore(startDate) && !today.isAfter(endDate)) {
            int currentDayNumber = (int) ChronoUnit.DAYS.between(startDate, today) + 1;
            int totalDaysCount = totalDays != null ? totalDays : 1;

            if (currentDayNumber == 1) {
                return SafariPhase.DAY_ONE;
            } else if (currentDayNumber == totalDaysCount) {
                return SafariPhase.LAST_DAY;
            } else if (currentDayNumber <= 3) {
                return SafariPhase.EARLY_DAYS;
            } else if (currentDayNumber >= totalDaysCount - 1) {
                return SafariPhase.FINAL_DAYS;
            } else {
                return SafariPhase.MID_SAFARI;
            }
        }

        // Post-safari phases
        if (daysSinceEnd <= 7) {
            return SafariPhase.JUST_ENDED;
        } else if (daysSinceEnd <= 30) {
            return SafariPhase.RECENTLY_ENDED;
        } else {
            return SafariPhase.PAST;
        }
    }

    /**
     * Get the current day number if safari is in progress
     * Returns null if safari hasn't started or has ended
     */
    @Transient
    public Integer getCurrentDayNumber() {
        LocalDate today = LocalDate.now();
        if (today.isBefore(startDate) || today.isAfter(endDate)) {
            return null;
        }
        return (int) ChronoUnit.DAYS.between(startDate, today) + 1;
    }

    /**
     * Get days until safari starts (negative if already started/ended)
     */
    @Transient
    public long getDaysUntilStart() {
        return ChronoUnit.DAYS.between(LocalDate.now(), startDate);
    }

    /**
     * Get days since safari ended (negative if not ended)
     */
    @Transient
    public long getDaysSinceEnd() {
        return ChronoUnit.DAYS.between(endDate, LocalDate.now());
    }

    /**
     * Check if safari is in progress (currently running)
     */
    @Transient
    public boolean isInProgress() {
        LocalDate today = LocalDate.now();
        return !today.isBefore(startDate) && !today.isAfter(endDate);
    }

    /**
     * Check if this is a critical phase requiring urgent attention
     */
    @Transient
    public boolean isUrgentPhase() {
        return getCurrentPhase().isUrgent();
    }

    /**
     * Generate safari code based on days, nights, and ID
     * Format: SAF-{days}D{nights}N-{1000 + id}
     */
    @Transient
    public String generateCode() {
        if (id == null || totalDays == null || totalNights == null) {
            return null;
        }
        return String.format("SAF-%dD%dN-%05d", totalDays, totalNights, 1000 + id);
    }
}
