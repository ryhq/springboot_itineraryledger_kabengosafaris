package com.itineraryledger.kabengosafaris.Tariff.DTOs;

import com.itineraryledger.kabengosafaris.Activity.ChargingBasis;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UpdateTariffDTO - Request DTO for updating an existing Tariff
 *
 * All fields are optional - only provided fields will be updated.
 * At least one field must be provided for a valid update.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTariffDTO {

    /**
     * Updated tariff name
     * Must be unique (case-insensitive, excluding current tariff)
     * Max 150 characters
     */
    private String name;

    /**
     * Updated slug
     * Must be unique (excluding current tariff)
     */
    private String slug;

    /**
     * Updated charging basis
     * NOTE: Changing charging basis may affect existing rates
     */
    private ChargingBasis chargingBasis;

    /**
     * Updated description
     */
    private String description;

    /**
     * Updated internal notes
     */
    private String internalNotes;

    /**
     * Updated active status
     */
    private Boolean isActive;
}
