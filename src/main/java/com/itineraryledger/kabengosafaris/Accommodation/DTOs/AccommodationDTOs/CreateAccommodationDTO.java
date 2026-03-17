package com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationDTOs;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationCategory;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * CreateAccommodationDTO - Request DTO for creating new accommodations
 *
 * Validates all required fields for accommodation creation:
 * - Basic information (name, type, category)
 * - Business information (TIN, VRN, website)
 * - Location details (region, district, GPS coordinates)
 * - Capacity and facilities
 * - Descriptions and policies
 * - Branch information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAccommodationDTO {

    // Basic Information
    @NotBlank(message = "Accommodation name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    private String slug; // Optional - will be auto-generated from name if not provided

    @NotNull(message = "Accommodation type is required")
    private AccommodationType accommodationType;

    private AccommodationCategory category;

    // Business Information
    @Size(max = 20, message = "TIN must not exceed 20 characters")
    private String tin;

    @Size(max = 20, message = "VRN must not exceed 20 characters")
    private String vrn;

    @Size(max = 500, message = "Website must not exceed 500 characters")
    private String website;

    // Multi-Branch Support
    @Builder.Default
    private Boolean hasBranch = false;

    @Builder.Default
    private Boolean isHeadquarters = true;

    private String parentAccommodationId; // Encoded ID of parent accommodation if this is a branch

    // Location Information
    @Size(max = 100, message = "Region must not exceed 100 characters")
    private String region;

    @Size(max = 100, message = "District must not exceed 100 characters")
    private String district;

    private String location; // Detailed location description

    private String address; // Physical/postal address

    // GPS Coordinates
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private BigDecimal latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private BigDecimal longitude;

    @Size(max = 50, message = "Elevation must not exceed 50 characters")
    private String elevation;

    // Capacity and Facilities
    @Min(value = 1, message = "Total rooms must be at least 1")
    private Integer totalRooms;

    @Min(value = 1, message = "Total beds must be at least 1")
    private Integer totalBeds;

    @Min(value = 1, message = "Max guests must be at least 1")
    private Integer maxGuests;

    @Min(value = 1, message = "Star rating must be between 1 and 5")
    @Max(value = 5, message = "Star rating must be between 1 and 5")
    private Integer starRating;

    // Description and Content
    @Size(max = 500, message = "Short description must not exceed 500 characters")
    private String shortDescription;

    private String details; // Detailed description

    private String amenities; // Comma-separated or JSON list

    private String services; // Services offered

    private String nearbyAttractions; // Nearby parks, attractions

    // Policies and Terms
    private String termsAndConditions;

    private String cancellationPolicy;

    private String checkInPolicy;

    private String checkOutPolicy;

    private String childPolicy;

    private String petPolicy;

    // Pricing Information
    @Size(max = 100, message = "Price range must not exceed 100 characters")
    private String priceRange;

    @Size(max = 10, message = "Currency must not exceed 10 characters")
    @Builder.Default
    private String currency = "USD";

    // Seasonal Information
    @Size(max = 200, message = "Best season must not exceed 200 characters")
    private String bestSeason;

    @Size(max = 200, message = "Operating season must not exceed 200 characters")
    private String operatingSeason;

    // Tags
    private String tags; // Comma-separated tags

    // Status
    @Builder.Default
    private Boolean isActive = true;
    private Boolean isWebActive;

    // Internal Notes
    private String internalNotes; // Staff notes, not visible to public
}
