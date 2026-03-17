package com.itineraryledger.kabengosafaris.Park;

import com.itineraryledger.kabengosafaris.ParkActivity.ParkActivity;
import com.itineraryledger.kabengosafaris.ParkTariff.ParkTariff;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Park Entity - Manages Tanzania National Parks and Reserves
 *
 * Includes National Parks, Wildlife Reserves, Game Reserves, Conservation Areas, etc.
 */
@Entity
@Table(name = "parks", indexes = {
    @Index(name = "idx_park_type", columnList = "park_type"),
    @Index(name = "idx_region", columnList = "region"),
    @Index(name = "idx_is_active", columnList = "is_active"),
    @Index(name = "idx_park_web_active", columnList = "is_web_active"),
    @Index(name = "idx_name", columnList = "name")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Park {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(unique = true, length = 150)
    private String slug; // URL-friendly name (e.g., "serengeti-national-park")

    @Enumerated(EnumType.STRING)
    @Column(name = "park_type", nullable = false, length = 50)
    private ParkType parkType;

    // Location Information
    @Column(length = 100)
    private String region; // e.g., "Arusha", "Mara", "Morogoro"

    @Column(length = 100)
    private String district;

    @Column(columnDefinition = "TEXT")
    private String location; // General location description

    // GPS Coordinates (for park center/entrance)
    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(length = 50)
    private String elevation; // e.g., "850m - 1,850m"

    @Column(length = 50)
    private String size; // e.g., "14,763 km²" for Serengeti

    // Description and Content
    @Column(columnDefinition = "TEXT", nullable = false)
    private String shortDescription; // Brief overview (150-200 chars)

    @Lob
    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String fullDescription; // Detailed information including wildlife, landscape, ecosystem

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String history; // Historical background and establishment

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String ecosystem; // Description of the ecosystem and biodiversity

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String wildlife; // Major wildlife species found in the park

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String vegetation; // Types of vegetation and flora

    // Media
    @Column(length = 500)
    private String primaryImage; // Main image URL/path

    // Visiting Information
    @Column(length = 100)
    private String bestTimeToVisit; // e.g., "June-October (Dry season)", "Year-round"

    @Column(columnDefinition = "TEXT")
    private String openingHours; // e.g., "6:00 AM - 6:00 PM"

    @Lob
    @Column(columnDefinition = "TEXT")
    private String accessInformation; // How to access the park

    // Tags for filtering and categorization
    @Lob
    @Column(columnDefinition = "TEXT")
    private String tags; // JSON array: ["Big Five", "Great Migration", "Bird Watching", "Climbing"]

    // Status and Visibility
    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "is_web_active", nullable = false)
    private Boolean isWebActive = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Additional notes
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String internalNotes; // Staff notes, not visible to public

    @OneToMany(mappedBy = "park", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ParkActivity> parkActivities = new ArrayList<>();

    @OneToMany(mappedBy = "park", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ParkTariff> parkTariffs = new ArrayList<>();
}
