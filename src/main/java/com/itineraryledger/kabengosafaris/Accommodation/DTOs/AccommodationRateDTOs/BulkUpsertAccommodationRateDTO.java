package com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationRateDTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * BulkUpsertAccommodationRateDTO - Request DTO for bulk upsert operations
 *
 * The upsert logic will automatically determine whether to create or update
 * based on existing rate records for the given combination of:
 * accommodation + season + roomType + roomStandard + boardType
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkUpsertAccommodationRateDTO {

    @NotBlank(message = "Accommodation ID is required")
    private String accommodationId;

    @NotBlank(message = "Season ID is required")
    private String seasonId;

    @NotBlank(message = "Room Type ID is required")
    private String roomTypeId;

    @NotBlank(message = "Room Standard ID is required")
    private String roomStandardId;

    @NotBlank(message = "Board Type ID is required")
    private String boardTypeId;

    @Positive(message = "Rack rate must be positive")
    private BigDecimal rackRate;

    @Positive(message = "STO rate must be positive if provided")
    private BigDecimal stoRate;

    @NotBlank(message = "Currency is required")
    private String currency;

    private String notes;

    private Boolean isActive;

    /**
     * Rate charging model
     *
     * TRUE (default): Per Person Sharing (PPS) - rate is per guest, common in safari lodges
     * FALSE: Per Room - rate is per room regardless of occupancy, common in hotels
     */
    private Boolean isPerPerson;
}
