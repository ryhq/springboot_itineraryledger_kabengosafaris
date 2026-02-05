package com.itineraryledger.kabengosafaris.Safari.SafariPax.DTOs;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UpsertSafariPaxDTO - Data Transfer Object for creating/updating SafariPax
 * Used for bulk upsert operations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpsertSafariPaxDTO {

    @NotBlank(message = "Nation category ID is required")
    private String nationCategoryId; // Obfuscated ID

    @NotBlank(message = "Age category ID is required")
    private String ageCategoryId; // Obfuscated ID

    @NotNull(message = "Pax count is required")
    @Min(value = 1, message = "Pax count must be at least 1")
    private Integer count;

    private String specialRequirements;
    private String notes;
}
