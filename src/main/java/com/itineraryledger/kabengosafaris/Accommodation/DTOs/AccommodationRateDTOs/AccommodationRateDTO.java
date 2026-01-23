package com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationRateDTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AccommodationRateDTO - Response DTO for AccommodationRate entity
 *
 * Used for API responses when returning rate data.
 * Contains obfuscated IDs and display-friendly fields.
 *
 * AccommodationRate is determined by the combination of:
 * - Accommodation
 * - Season (accommodation-specific)
 * - AccommodationRoomType
 * - AccommodationRoomStandard
 * - AccommodationBoardType
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccommodationRateDTO {

    /**
     * Obfuscated rate ID
     */
    private String id;

    // Accommodation information
    private String accommodationId;
    private String accommodationName;

    // Season information (accommodation-specific)
    private String seasonId;
    private String seasonName;
    private String seasonType;

    // Room Type information
    private String roomTypeId;
    private String roomTypeName;
    private String bedConfiguration;

    // Room Standard information
    private String roomStandardId;
    private String roomStandardName;

    // Board Type information
    private String boardTypeId;
    private String boardTypeName;

    /**
     * Rate information
     *
     * Rack Rate: The price charged to the customer (revenue)
     * STO Rate: The cost we pay on behalf of the customer (expense/tour operator rate)
     * Profit Amount: Rack Rate - STO Rate (the profit we make)
     * Profit Percentage: (Profit Amount / Rack Rate) * 100
     *
     * Business logic: Rack Rate >= STO Rate (we charge at least what we pay)
     */
    private BigDecimal rackRate;
    private BigDecimal stoRate;
    private String currency;
    private BigDecimal profitAmount;
    private BigDecimal profitPercentage;

    /**
     * Rate charging model
     *
     * TRUE: Per Person Sharing (PPS) - rate is per guest, common in safari lodges/camps
     *       Example: Double room at $150/person = $300 total for 2 guests
     * FALSE: Per Room - rate is per room regardless of occupancy, common in hotels
     *       Example: Double room at $250/room = $250 total regardless of occupancy
     */
    private Boolean isPerPerson;

    // Metadata
    private String notes;
    private Boolean isActive;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
