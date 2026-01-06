package com.itineraryledger.kabengosafaris.Accommodation.Entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * AccommodationRoomType Entity - Defines room types for accommodations
 *
 * Represents the physical room configuration (SINGLE, DOUBLE, TWIN, TRIPLE, etc.)
 * Based on bed arrangement and occupancy.
 *
 * IMPROVED from old version:
 * - Uses self-referencing Accommodation (no separate branch entity!)
 * - Better validation
 * - Flexible custom types
 */
@Entity
@Table(name = "accommodation_room_types", indexes = {
    @Index(name = "idx_accommodation_room_type_accommodation_id", columnList = "accommodation_id"),
    @Index(name = "idx_accommodation_room_type_name", columnList = "name"),
    @Index(name = "idx_accommodation_room_type_is_active", columnList = "is_active")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_accommodation_room_type_accommodation_name", columnNames = {"accommodation_id", "name"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccommodationRoomType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "accommodation_id", nullable = false)
    private Accommodation accommodation;

    @Column(nullable = false, length = 100)
    private String name; // e.g., "Single Room", "Double Room", "Twin Room"

    @Column(name = "bed_configuration", length = 200)
    private String bedConfiguration; // e.g., "1 King Bed", "2 Single Beds", "1 Queen + 1 Single"

    @Column(name = "max_occupancy")
    private Integer maxOccupancy; // Maximum number of guests

    @Column(name = "min_occupancy")
    @Builder.Default
    private Integer minOccupancy = 1; // Minimum number of guests

    @Column(columnDefinition = "TEXT")
    private String description; // Detailed description of the room type

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Check if a given occupancy count is valid for this room type
     */
    public boolean isValidOccupancy(int occupancy) {
        if (maxOccupancy == null) {
            return occupancy >= (minOccupancy != null ? minOccupancy : 1);
        }
        int min = minOccupancy != null ? minOccupancy : 1;
        return occupancy >= min && occupancy <= maxOccupancy;
    }
}
