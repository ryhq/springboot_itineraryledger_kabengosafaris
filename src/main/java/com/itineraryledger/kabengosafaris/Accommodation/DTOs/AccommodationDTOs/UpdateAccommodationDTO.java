package com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationDTOs;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationCategory;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * UpdateAccommodationDTO - Data Transfer Object for updating an existing Accommodation
 * All fields are optional - only provided fields will be updated
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAccommodationDTO {

    // Basic Information
    private String name;
    private String slug;
    /*
     * Enums arrive as Strings so a blank can CLEAR the field; null still means
     * "leave unchanged". Bound as the enum itself, an empty value makes Jackson
     * reject the whole request body, which is how a set value became impossible
     * to unset (see the charging-basis fix).
     */
    private String accommodationType;
    private String category;

    // Business Information
    private String tin;
    private String vrn;
    private String logoUrl;
    private String website;

    // Multi-Branch Support
    private Boolean hasBranch;
    private Boolean isHeadquarters;
    private String parentAccommodationId; // Encoded ID

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
    private Boolean isWebActive;

    // Internal Notes
    private String internalNotes;
}
