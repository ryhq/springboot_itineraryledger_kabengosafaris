package com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationDTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationCategory;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AccommodationDTO - Response DTO for accommodation data
 *
 * Contains all accommodation information to be returned to clients
 * IDs are encoded using IdObfuscator for security
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccommodationDTO {

    private String id; // Encoded ID
    private String name;
    private String slug;
    private AccommodationType accommodationType;
    private String accommodationTypeDisplayName;
    private String accommodationTypeDescription;
    private AccommodationCategory category;
    private String categoryDisplayName;
    private String categoryDescription;
    private Integer categoryApproximateStars;

    // Business Information
    private String tin;
    private String vrn;
    private String logoUrl;
    private String website;

    // Multi-Branch Support
    private Boolean hasBranch;
    private Boolean isHeadquarters;
    private String parentAccommodationId; // Encoded ID
    private String parentAccommodationName;

    // Location Information
    private String region;
    private String district;
    private String location;
    private String address;

    // GPS Coordinates
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String elevation;

    // Capacity and Facilities
    private Integer totalRooms;
    private Integer totalBeds;
    private Integer maxGuests;
    private Integer starRating;

    // Description and Content
    private String shortDescription;
    private String details;
    private String amenities;
    private String services;
    private String nearbyAttractions;

    // Policies and Terms
    private String termsAndConditions;
    private String cancellationPolicy;
    private String checkInPolicy;
    private String checkOutPolicy;
    private String childPolicy;
    private String petPolicy;

    // Pricing Information
    private String priceRange;
    private String currency;

    // Seasonal Information
    private String bestSeason;
    private String operatingSeason;

    // Tags
    private String tags;

    // Status
    private Boolean isActive;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Counts
    private Integer emailCount;
    private Integer phoneCount;
    private Integer imageCount;
    private Integer branchCount;
    private Integer roomTypeCount;
    private Integer roomStandardCount;
    private Integer boardTypeCount;
    private Integer rateCount;
    private Integer documentCount;
}
