package com.itineraryledger.kabengosafaris.ParkTariff.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for upserting park-tariff relationships
 *
 * Supports bulk operations with status flag:
 * - status = true: Create or update the relationship
 * - status = false: Delete the relationship
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParkTariffUpsertDTO {
    @NotBlank(message = "Tariff ID is required")
    private String tariffId;

    @NotBlank(message = "Park ID is required")
    private String parkId;

    private String notes;

    @NotNull(message = "Status is required")
    private Boolean status; // true = create/update, false = delete
}
