package com.itineraryledger.kabengosafaris.Public.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationCategory;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationType;
import com.itineraryledger.kabengosafaris.Public.Annotations.Translatable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Public-safe Accommodation DTO - excludes sensitive business fields (tin, vrn)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicAccommodationDTO {

    @Translatable private String name;
    private String slug;
    private AccommodationType accommodationType;
    @Translatable private String accommodationTypeDisplayName;
    @Translatable private String accommodationTypeDescription;
    private AccommodationCategory category;
    @Translatable private String categoryDisplayName;
    @Translatable private String categoryDescription;
    private Integer categoryApproximateStars;

    private String logoUrl;
    private String website;

    // Multi-Branch Support
    private Boolean hasBranch;
    private Boolean isHeadquarters;
    private String parentAccommodationSlug;
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
    @Translatable private String shortDescription;
    @Translatable private String details;
    @Translatable private String amenities;
    @Translatable private String services;
    @Translatable private String nearbyAttractions;

    // Policies
    @Translatable private String termsAndConditions;
    @Translatable private String cancellationPolicy;
    @Translatable private String checkInPolicy;
    @Translatable private String checkOutPolicy;
    @Translatable private String childPolicy;
    @Translatable private String petPolicy;

    // Pricing
    private String priceRange;
    private String currency;

    // Seasonal
    @Translatable private String bestSeason;
    private String operatingSeason;

    @Translatable private String tags;

    // Image
    private String primaryImageUrl;

    // Counts
    private Integer imageCount;
    private Integer roomTypeCount;
    private Integer roomStandardCount;
    private Integer boardTypeCount;
}
