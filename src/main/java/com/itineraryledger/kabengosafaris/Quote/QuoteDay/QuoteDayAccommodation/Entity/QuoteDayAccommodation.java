package com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayAccommodation.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationBoardType;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRoomStandard;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRoomType;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.Entity.QuoteDay;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * QuoteDayAccommodation - lodging on a quote's day.
 *
 * Snapshotted from ItineraryDayAccommodation when a Quote is generated.
 * Operational fields (confirmationNumber, bookingStatus, checkInTime,
 * roomNumbers, guestFeedback, specialArrangements) live on SafariDayAccommodation.
 */
@Entity
@Table(name = "quote_day_accommodations",
    indexes = {
        @Index(name = "idx_qda_day_id", columnList = "quote_day_id"),
        @Index(name = "idx_qda_accommodation_id", columnList = "accommodation_id"),
        @Index(name = "idx_qda_room_type_id", columnList = "room_type_id"),
        @Index(name = "idx_qda_room_standard_id", columnList = "room_standard_id"),
        @Index(name = "idx_qda_board_type_id", columnList = "board_type_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuoteDayAccommodation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_day_id", nullable = false)
    @JsonIgnore
    private QuoteDay quoteDay;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "accommodation_id", nullable = false)
    private Accommodation accommodation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_type_id", nullable = false)
    private AccommodationRoomType roomType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_standard_id", nullable = false)
    private AccommodationRoomStandard roomStandard;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "board_type_id", nullable = false)
    private AccommodationBoardType boardType;

    @Column(name = "room_count")
    @Builder.Default
    private Integer roomCount = 1;

    @Builder.Default
    @Column(name = "is_alternative", nullable = false)
    private Boolean isAlternative = false;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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

    @Transient
    public boolean isPrimary() {
        return !isAlternative;
    }
}
