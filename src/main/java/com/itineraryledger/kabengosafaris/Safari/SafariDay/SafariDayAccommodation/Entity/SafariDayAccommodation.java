package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationBoardType;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRoomStandard;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRoomType;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.Entity.SafariDay;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * SafariDayAccommodation Entity - Lodging for a specific safari day
 *
 * Copied from ItineraryDayAccommodation when Safari is created.
 * Represents the accommodation details for a day in the safari.
 * Includes room configuration (type, standard, board type).
 * Supports alternative/backup accommodation options.
 *
 * Safari-specific additions:
 * - confirmationNumber: Booking confirmation from accommodation
 * - confirmedAt: When the booking was confirmed
 * - checkInTime: Actual check-in time
 * - checkOutTime: Actual check-out time
 * - roomNumbers: Assigned room numbers
 * - guestFeedback: Guest feedback about the accommodation
 */
@Entity
@Table(name = "safari_day_accommodations",
    indexes = {
        @Index(name = "idx_sda_day_id", columnList = "safari_day_id"),
        @Index(name = "idx_sda_accommodation_id", columnList = "accommodation_id"),
        @Index(name = "idx_sda_room_type_id", columnList = "room_type_id"),
        @Index(name = "idx_sda_room_standard_id", columnList = "room_standard_id"),
        @Index(name = "idx_sda_board_type_id", columnList = "board_type_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafariDayAccommodation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "safari_day_id", nullable = false)
    @JsonIgnore
    private SafariDay safariDay;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "accommodation_id", nullable = false)
    private Accommodation accommodation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_type_id", nullable = false)
    private AccommodationRoomType roomType; // e.g., Single, Double, Twin, Triple

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_standard_id", nullable = false)
    private AccommodationRoomStandard roomStandard; // e.g., Standard, Deluxe, Suite

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "board_type_id", nullable = false)
    private AccommodationBoardType boardType; // e.g., Room Only, B&B, Half Board, Full Board

    @Column(name = "room_count")
    @Builder.Default
    private Integer roomCount = 1;

    @Builder.Default
    @Column(name = "is_alternative", nullable = false)
    private Boolean isAlternative = false; // True if this is a backup/alternative option

    @Lob
    @Column(columnDefinition = "TEXT")
    private String notes;

    // ========================
    // SAFARI-SPECIFIC FIELDS
    // ========================

    /**
     * Booking confirmation number from the accommodation
     */
    @Column(name = "confirmation_number", length = 100)
    private String confirmationNumber;

    /**
     * When the booking was confirmed with the accommodation
     */
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    /**
     * Actual check-in time
     */
    @Column(name = "check_in_time", length = 10)
    private String checkInTime;

    /**
     * Actual check-out time
     */
    @Column(name = "check_out_time", length = 10)
    private String checkOutTime;

    /**
     * Assigned room numbers (comma-separated if multiple)
     */
    @Column(name = "room_numbers", length = 200)
    private String roomNumbers;

    /**
     * Guest feedback about the accommodation
     */
    @Lob
    @Column(name = "guest_feedback", columnDefinition = "TEXT")
    private String guestFeedback;

    /**
     * Special arrangements or requests for this booking
     */
    @Lob
    @Column(name = "special_arrangements", columnDefinition = "TEXT")
    private String specialArrangements;

    /**
     * Status of the booking
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "booking_status", length = 20)
    @Builder.Default
    private BookingStatus bookingStatus = BookingStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ========================
    // HELPER METHODS
    // ========================

    /**
     * Get display name for this accommodation entry
     * E.g., "Serena Lodge - Deluxe Double (Full Board)"
     */
    @Transient
    public String getDisplayName() {
        StringBuilder sb = new StringBuilder();
        if (accommodation != null) {
            sb.append(accommodation.getName());
        }
        if (roomStandard != null) {
            sb.append(" - ").append(roomStandard.getName());
        }
        if (roomType != null) {
            sb.append(" ").append(roomType.getName());
        }
        if (boardType != null) {
            sb.append(" (").append(boardType.getName()).append(")");
        }
        return sb.toString();
    }

    /**
     * Check if this is the primary accommodation (not alternative)
     */
    @Transient
    public boolean isPrimary() {
        return !isAlternative;
    }

    /**
     * Mark booking as confirmed
     */
    public void confirmBooking(String confirmationNum) {
        this.confirmationNumber = confirmationNum;
        this.confirmedAt = LocalDateTime.now();
        this.bookingStatus = BookingStatus.CONFIRMED;
    }

    // ========================
    // ENUM
    // ========================

    public enum BookingStatus {
        PENDING("Pending", "Booking request sent, awaiting confirmation"),
        CONFIRMED("Confirmed", "Booking confirmed by accommodation"),
        CANCELLED("Cancelled", "Booking was cancelled"),
        NO_SHOW("No Show", "Guest did not show up"),
        COMPLETED("Completed", "Stay was completed");

        private final String displayName;
        private final String description;

        BookingStatus(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }
}
