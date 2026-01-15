package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationBoardType;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRoomStandard;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRoomType;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Entity.ItineraryDay;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * ItineraryDayAccommodation Entity - Lodging for a specific day
 *
 * Represents the accommodation details for a day in the itinerary.
 * Includes room configuration (type, standard, board type).
 * Supports alternative/backup accommodation options.
 */
@Entity
@Table(name = "itinerary_day_accommodations",
    indexes = {
        @Index(name = "idx_ida_day_id", columnList = "itinerary_day_id"),
        @Index(name = "idx_ida_accommodation_id", columnList = "accommodation_id"),
        @Index(name = "idx_ida_room_type_id", columnList = "room_type_id"),
        @Index(name = "idx_ida_room_standard_id", columnList = "room_standard_id"),
        @Index(name = "idx_ida_board_type_id", columnList = "board_type_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryDayAccommodation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "itinerary_day_id", nullable = false)
    @JsonIgnore
    private ItineraryDay itineraryDay;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "accommodation_id", nullable = false)
    private Accommodation accommodation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id")
    private AccommodationRoomType roomType; // e.g., Single, Double, Twin, Triple

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_standard_id")
    private AccommodationRoomStandard roomStandard; // e.g., Standard, Deluxe, Suite

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_type_id")
    private AccommodationBoardType boardType; // e.g., Room Only, B&B, Half Board, Full Board

    @Column(name = "room_count")
    @Builder.Default
    private Integer roomCount = 1;

    @Column(name = "nights")
    @Builder.Default
    private Integer nights = 1;

    @Builder.Default
    @Column(name = "is_alternative", nullable = false)
    private Boolean isAlternative = false; // True if this is a backup/alternative option

    @Lob
    @Column(columnDefinition = "TEXT")
    private String notes;

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
}
