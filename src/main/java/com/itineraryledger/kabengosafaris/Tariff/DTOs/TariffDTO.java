package com.itineraryledger.kabengosafaris.Tariff.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Activity.ChargingBasis;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * TariffDTO - Response DTO for Tariff entity
 *
 * Used for API responses when returning tariff data.
 * Contains obfuscated ID and display-friendly fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TariffDTO {

    /**
     * Obfuscated tariff ID for external use
     */
    private String id;

    /**
     * Tariff name
     */
    private String name;

    /**
     * URL-friendly slug
     */
    private String slug;

    /**
     * Charging basis enum value
     */
    private ChargingBasis chargingBasis;

    /**
     * Human-readable charging basis display name
     * e.g., "Per Person", "Per Vehicle"
     */
    private String chargingBasisDisplayName;

    /**
     * Tariff description
     */
    private String description;

    /**
     * Whether this tariff requires age category for rate lookup
     */
    private Boolean requiresAgeCategory;

    /**
     * Active status
     */
    private Boolean isActive;

    /**
     * System tariff flag
     */
    private Boolean isSystem;

    /**
     * Creation timestamp
     */
    private LocalDateTime createdAt;

    /**
     * Last update timestamp
     */
    private LocalDateTime updatedAt;

    /**
     * Count of parks linked to this tariff (optional, for list views)
     */
    private Long parkCount;
}
