package com.itineraryledger.kabengosafaris.Season;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;

/**
 * SeasonPeriod Entity - Defines date ranges for seasons
 *
 * Represents a specific date range within a season. A season can have multiple periods
 * to handle scenarios like:
 * - Multiple non-consecutive peak periods in a year
 * - Recurring annual seasons (uses MonthDay to ignore year)
 * - Flexible seasonal definitions
 *
 * IMPROVED from old version:
 * - Better validation for date ranges
 * - Overlap detection support
 * - Year-agnostic recurring periods using MonthDay
 * - Active period detection
 */
@Entity
@Table(name = "season_periods", indexes = {
    @Index(name = "idx_season_period_season_id", columnList = "season_id"),
    @Index(name = "idx_season_period_dates", columnList = "start_date, end_date"),
    @Index(name = "idx_season_period_is_active", columnList = "is_active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeasonPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    /**
     * Start date of the season period (Month-Day only, year-agnostic for recurring seasons)
     * Stored as VARCHAR in format "MM-dd" (e.g., "06-01" for June 1st)
     */
    @Convert(converter = MonthDayConverter.class)
    @Column(name = "start_date", nullable = false, length = 5)
    private MonthDay startDate;

    /**
     * End date of the season period (Month-Day only, year-agnostic for recurring seasons)
     * Stored as VARCHAR in format "MM-dd" (e.g., "08-31" for August 31st)
     */
    @Convert(converter = MonthDayConverter.class)
    @Column(name = "end_date", nullable = false, length = 5)
    private MonthDay endDate;

    /**
     * Optional: Specific year if this period applies to a particular year only
     * If null, the period recurs annually
     */
    @Column(name = "year")
    private Integer year;

    @Column(columnDefinition = "TEXT")
    private String notes; // Optional notes about this period

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * Indicates if this is a system period (created by initializer)
     * TRUE = System period (protected from deletion, created with system season)
     * FALSE = User-created period (can be deleted)
     * System periods can only be updated, never deleted
     */
    @Builder.Default
    @Column(name = "is_system", nullable = false)
    private Boolean isSystem = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Check if a given date falls within this season period
     * Handles year-wrapping periods (e.g., Dec 15 - Jan 15)
     */
    public boolean containsDate(LocalDate date) {
        if (!isActive) {
            return false;
        }

        // If specific year is set, check year first
        if (year != null && date.getYear() != year) {
            return false;
        }

        MonthDay checkDate = MonthDay.from(date);

        // Handle periods that don't wrap around year boundary
        if (startDate.compareTo(endDate) <= 0) {
            return checkDate.compareTo(startDate) >= 0 && checkDate.compareTo(endDate) <= 0;
        } else {
            // Handle periods that wrap around year boundary (e.g., Dec 15 - Jan 15)
            return checkDate.compareTo(startDate) >= 0 || checkDate.compareTo(endDate) <= 0;
        }
    }

    /**
     * Check if this period overlaps with another period
     */
    public boolean overlaps(SeasonPeriod other) {
        if (other == null || !this.isActive || !other.isActive) {
            return false;
        }

        // If both have specific years and they're different, no overlap
        if (this.year != null && other.year != null && !this.year.equals(other.year)) {
            return false;
        }

        // Complex overlap detection for MonthDay ranges
        // This is a simplified version; for production, consider edge cases
        return this.startDate.equals(other.startDate) ||
               this.endDate.equals(other.endDate) ||
               (this.startDate.compareTo(other.endDate) <= 0 && this.endDate.compareTo(other.startDate) >= 0);
    }

    /**
     * Get the duration in days (approximate, doesn't account for leap years unless year is specified)
     */
    public int getDurationDays() {
        if (year != null) {
            LocalDate start = LocalDate.of(year, startDate.getMonth(), startDate.getDayOfMonth());
            LocalDate end = LocalDate.of(year, endDate.getMonth(), endDate.getDayOfMonth());

            if (end.isBefore(start)) {
                // Wraps around year
                end = end.plusYears(1);
            }

            return (int) java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
        } else {
            // Approximate for non-year-specific periods
            if (startDate.compareTo(endDate) <= 0) {
                LocalDate start = LocalDate.of(2024, startDate.getMonth(), startDate.getDayOfMonth());
                LocalDate end = LocalDate.of(2024, endDate.getMonth(), endDate.getDayOfMonth());
                return (int) java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
            } else {
                // Year wrapping
                LocalDate start = LocalDate.of(2024, startDate.getMonth(), startDate.getDayOfMonth());
                LocalDate end = LocalDate.of(2025, endDate.getMonth(), endDate.getDayOfMonth());
                return (int) java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
            }
        }
    }

    /**
     * Check if this is a recurring (annual) period
     */
    public boolean isRecurring() {
        return year == null;
    }

    /**
     * Check if this period wraps around the year boundary
     */
    public boolean isYearWrapping() {
        return startDate.compareTo(endDate) > 0;
    }

    /**
     * Check if this is a system period (protected from deletion)
     */
    public boolean isSystemPeriod() {
        return isSystem != null && isSystem;
    }
}
