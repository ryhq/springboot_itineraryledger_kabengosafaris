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
 * AccommodationRoomStandard Entity - Defines room categories/classes for accommodations
 *
 * Represents the room quality/category level:
 * - Garden Room, Standard Room, Deluxe Room, Suite, Presidential Suite
 * - Bungalow, Villa, Tent, Family Tent, Honeymoon Suite
 * - Economy, Business Class, etc.
 *
 * IMPROVED from old version:
 * - Uses self-referencing Accommodation (no separate branch entity!)
 * - Amenities support
 * - View/location information
 */
@Entity
@Table(name = "accommodation_room_standards", indexes = {
    @Index(name = "idx_accommodation_room_standard_accommodation_id", columnList = "accommodation_id"),
    @Index(name = "idx_accommodation_room_standard_name", columnList = "name"),
    @Index(name = "idx_accommodation_room_standard_is_active", columnList = "is_active")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_accommodation_room_standard_accommodation_name", columnNames = {"accommodation_id", "name"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccommodationRoomStandard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "accommodation_id", nullable = false)
    private Accommodation accommodation;

    @Column(nullable = false, length = 100)
    private String name; // e.g., "Deluxe Garden Room", "Standard Bungalow", "Family Tent"

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "max_occupancy")
    private Integer maxOccupancy; // Maximum guests for this room standard

    @Lob
    @Column(columnDefinition = "TEXT")
    private String amenities; // Comma-separated or JSON list: "WiFi, AC, Minibar, Balcony"

    @Column(name = "view_type", length = 100)
    private String viewType; // e.g., "Garden View", "Ocean View", "Mountain View", "Pool View"

    @Column(name = "floor_level", length = 50)
    private String floorLevel; // e.g., "Ground Floor", "First Floor", "Second Floor"

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
