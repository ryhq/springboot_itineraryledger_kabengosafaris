package com.itineraryledger.kabengosafaris.Tariff.DTOs;

import com.itineraryledger.kabengosafaris.Activity.ChargingBasis;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CreateTariffDTO - Request DTO for creating a new Tariff
 *
 * Contains only the fields that can be set during creation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTariffDTO {

    /**
     * Tariff name (required)
     * Must be unique (case-insensitive)
     * Max 150 characters
     */
    private String name;

    /**
     * Optional custom slug
     * If not provided, will be auto-generated from name
     */
    private String slug;

    /**
     * Charging basis (required)
     * Determines how rates are calculated and whether age category is needed
     */
    private ChargingBasis chargingBasis;

    /**
     * Optional description
     */
    private String description;

    /**
     * Optional internal notes (staff only)
     */
    private String internalNotes;

    /**
     * Active status (default: true)
     */
    private Boolean isActive;
}
