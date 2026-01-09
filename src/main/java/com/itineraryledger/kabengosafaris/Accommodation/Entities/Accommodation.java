package com.itineraryledger.kabengosafaris.Accommodation.Entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.itineraryledger.kabengosafaris.Season.Season;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Accommodation Entity - Manages hotels, lodges, camps, and other accommodation facilities
 *
 * Represents tourism accommodation providers including:
 * - Hotels, Lodges, Tented Camps, Guesthouses, etc.
 * - Location and contact information
 * - Multi-branch support for accommodation chains
 * - Images, amenities, and detailed descriptions
 */
@Entity
@Table(name = "accommodations", indexes = {
    @Index(name = "idx_accommodation_name", columnList = "name"),
    @Index(name = "idx_accommodation_type", columnList = "accommodation_type"),
    @Index(name = "idx_accommodation_category", columnList = "category"),
    @Index(name = "idx_accommodation_region", columnList = "region"),
    @Index(name = "idx_accommodation_district", columnList = "district"),
    @Index(name = "idx_accommodation_is_active", columnList = "is_active"),
    @Index(name = "idx_accommodation_has_branch", columnList = "has_branch"),
    @Index(name = "idx_accommodation_star_rating", columnList = "star_rating")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Accommodation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(unique = true, length = 250)
    private String slug; // URL-friendly name (e.g., "serengeti-serena-safari-lodge")

    @Enumerated(EnumType.STRING)
    @Column(name = "accommodation_type", nullable = false, length = 50)
    private AccommodationType accommodationType; // HOTEL, LODGE, TENTED_CAMP, etc.

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 50)
    private AccommodationCategory category; // LUXURY, MID_RANGE, BUDGET, etc.

    // Business Information
    @Column(name = "tin", length = 20)
    private String tin; // Taxpayer Identification Number

    @Column(name = "vrn", length = 20)
    private String vrn; // VAT Registration Number

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "website", length = 500)
    private String website;

    // Multi-Branch Support
    @Builder.Default
    @Column(name = "has_branch", nullable = false)
    private Boolean hasBranch = false;

    @Column(name = "is_headquarters", nullable = false)
    @Builder.Default
    private Boolean isHeadquarters = true; // True if this is the main branch

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_accommodation_id")
    private Accommodation parentAccommodation; // Reference to headquarters if this is a branch

    // Location Information
    @Column(length = 100)
    private String region; // e.g., "Arusha", "Serengeti", etc.

    @Column(length = 100)
    private String district;

    @Column(columnDefinition = "TEXT")
    private String location; // Detailed location description

    @Column(columnDefinition = "TEXT")
    private String address; // Physical/postal address

    // GPS Coordinates
    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(length = 50)
    private String elevation; // e.g., "1,400m above sea level"

    // Capacity and Facilities
    @Column(name = "total_rooms")
    private Integer totalRooms;

    @Column(name = "total_beds")
    private Integer totalBeds;

    @Column(name = "max_guests")
    private Integer maxGuests;

    @Column(name = "star_rating")
    private Integer starRating; // 1-5 star rating

    // Description and Content
    @Column(columnDefinition = "TEXT")
    private String shortDescription; // Brief overview (150-200 chars)

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String details; // Detailed description of the accommodation

    @Lob
    @Column(columnDefinition = "TEXT")
    private String amenities; // Comma-separated or JSON list of amenities

    @Lob
    @Column(columnDefinition = "TEXT")
    private String services; // Services offered (e.g., spa, restaurant, bar, etc.)

    @Lob
    @Column(columnDefinition = "TEXT")
    private String nearbyAttractions; // Nearby parks, attractions, etc.

    // Policies and Terms
    @Lob
    @Column(name = "terms_and_conditions", columnDefinition = "LONGTEXT")
    private String termsAndConditions;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String cancellationPolicy;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String checkInPolicy; // Check-in time and requirements

    @Lob
    @Column(columnDefinition = "TEXT")
    private String checkOutPolicy; // Check-out time and requirements

    @Lob
    @Column(columnDefinition = "TEXT")
    private String childPolicy; // Children and extra bed policy

    @Lob
    @Column(columnDefinition = "TEXT")
    private String petPolicy; // Pets allowed or not

    // Pricing Information (optional, for reference)
    @Column(name = "price_range", length = 100)
    private String priceRange; // e.g., "$100 - $500 per night"

    @Column(name = "currency", length = 10)
    @Builder.Default
    private String currency = "USD";

    // Seasonal Information
    @Column(name = "best_season", length = 200)
    private String bestSeason; // Best time to visit

    @Column(name = "operating_season", length = 200)
    private String operatingSeason; // When the accommodation is open

    // Tags for filtering and categorization
    @Lob
    @Column(columnDefinition = "TEXT")
    private String tags; // Comma-separated tags: "Safari Lodge, Pool, Wifi, Family Friendly"

    // Status and Visibility
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Additional notes
    @Lob
    @Column(name = "internal_notes", columnDefinition = "LONGTEXT")
    private String internalNotes; // Staff notes, not visible to public

    // Relationships
    @OneToMany(mappedBy = "accommodation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AccommodationEmail> emails = new ArrayList<>();

    @OneToMany(mappedBy = "accommodation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AccommodationPhone> phones = new ArrayList<>();

    @OneToMany(mappedBy = "accommodation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AccommodationImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "parentAccommodation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Accommodation> branches = new ArrayList<>();

    @OneToMany(mappedBy = "accommodation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AccommodationRoomType> roomTypes = new ArrayList<>();

    @OneToMany(mappedBy = "accommodation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AccommodationRoomStandard> roomStandards = new ArrayList<>();

    @OneToMany(mappedBy = "accommodation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AccommodationBoardType> boardTypes = new ArrayList<>();

    @OneToMany(mappedBy = "accommodation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AccommodationRate> rates = new ArrayList<>();

    @OneToMany(mappedBy = "accommodation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AccommodationDocument> documents = new ArrayList<>();

    @OneToMany(mappedBy = "accommodation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Season> seasons = new ArrayList<>();

    // Helper methods for managing relationships
    public void addEmail(AccommodationEmail email) {
        emails.add(email);
        email.setAccommodation(this);
    }

    public void removeEmail(AccommodationEmail email) {
        emails.remove(email);
        email.setAccommodation(null);
    }

    public void addPhone(AccommodationPhone phone) {
        phones.add(phone);
        phone.setAccommodation(this);
    }

    public void removePhone(AccommodationPhone phone) {
        phones.remove(phone);
        phone.setAccommodation(null);
    }

    public void addImage(AccommodationImage image) {
        images.add(image);
        image.setAccommodation(this);
    }

    public void removeImage(AccommodationImage image) {
        images.remove(image);
        image.setAccommodation(null);
    }

    public void addBranch(Accommodation branch) {
        branches.add(branch);
        branch.setParentAccommodation(this);
        branch.setIsHeadquarters(false);
    }

    public void removeBranch(Accommodation branch) {
        branches.remove(branch);
        branch.setParentAccommodation(null);
        branch.setIsHeadquarters(true);
    }

    public void addRoomType(AccommodationRoomType roomType) {
        roomTypes.add(roomType);
        roomType.setAccommodation(this);
    }

    public void removeRoomType(AccommodationRoomType roomType) {
        roomTypes.remove(roomType);
        roomType.setAccommodation(null);
    }

    public void addRoomStandard(AccommodationRoomStandard roomStandard) {
        roomStandards.add(roomStandard);
        roomStandard.setAccommodation(this);
    }

    public void removeRoomStandard(AccommodationRoomStandard roomStandard) {
        roomStandards.remove(roomStandard);
        roomStandard.setAccommodation(null);
    }

    public void addBoardType(AccommodationBoardType boardType) {
        boardTypes.add(boardType);
        boardType.setAccommodation(this);
    }

    public void removeBoardType(AccommodationBoardType boardType) {
        boardTypes.remove(boardType);
        boardType.setAccommodation(null);
    }

    public void addRate(AccommodationRate rate) {
        rates.add(rate);
        rate.setAccommodation(this);
    }

    public void removeRate(AccommodationRate rate) {
        rates.remove(rate);
        rate.setAccommodation(null);
    }

    public void addDocument(AccommodationDocument document) {
        documents.add(document);
        document.setAccommodation(this);
    }

    public void removeDocument(AccommodationDocument document) {
        documents.remove(document);
        document.setAccommodation(null);
    }

    public void addSeason(Season season) {
        seasons.add(season);
        season.setAccommodation(this);
        season.setIsGlobal(false);
    }

    public void removeSeason(Season season) {
        seasons.remove(season);
        season.setAccommodation(null);
    }
}
