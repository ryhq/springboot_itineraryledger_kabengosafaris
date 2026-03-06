package com.itineraryledger.kabengosafaris.Public.DTOs;

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
 * Public-safe Accommodation DTO - excludes sensitive business fields (tin, vrn)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicAccommodationDTO {

    private String id;
    private String name;
    private String slug;
    private AccommodationType accommodationType;
    private String accommodationTypeDisplayName;
    private String accommodationTypeDescription;
    private AccommodationCategory category;
    private String categoryDisplayName;
    private String categoryDescription;
    private Integer categoryApproximateStars;

    private String logoUrl;
    private String website;

    // Multi-Branch Support
    private Boolean hasBranch;
    private Boolean isHeadquarters;
    private String parentAccommodationId;
    private String parentAccommodationName;

    // Location
    private String region;
    private String district;
    private String location;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String elevation;

    // Capacity
    private Integer totalRooms;
    private Integer totalBeds;
    private Integer maxGuests;
    private Integer starRating;

    // Descriptions
    private String shortDescription;
    private String details;
    private String amenities;
    private String services;
    private String nearbyAttractions;

    // Policies
    private String termsAndConditions;
    private String cancellationPolicy;
    private String checkInPolicy;
    private String checkOutPolicy;
    private String childPolicy;
    private String petPolicy;

    // Pricing
    private String priceRange;
    private String currency;

    // Seasonal
    private String bestSeason;
    private String operatingSeason;

    private String tags;
    private Boolean isActive;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Counts
    private Integer imageCount;
    private Integer roomTypeCount;
    private Integer roomStandardCount;
    private Integer boardTypeCount;
}
