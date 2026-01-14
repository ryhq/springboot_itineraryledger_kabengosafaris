package com.itineraryledger.kabengosafaris.Tariff.DTOs;

import com.itineraryledger.kabengosafaris.Activity.ChargingBasis;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * TariffWithNotesDTO - Response DTO for Tariff with park-specific notes
 *
 * Used when fetching tariffs in the context of a specific park.
 * Includes the relationship-specific notes from ParkTariff.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TariffWithNotesDTO {

    private String id;
    private String name;
    private String slug;
    private ChargingBasis chargingBasis;
    private String chargingBasisDisplayName;
    private String description;
    private Boolean requiresAgeCategory;
    private Boolean isActive;
    private Boolean isSystem;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Park-specific notes from the ParkTariff relationship
     */
    private String notes;
}
