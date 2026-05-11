package com.itineraryledger.kabengosafaris.Quote.QuotePax.DTOs;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Upsert payload for QuotePax. Used for bulk add/update calls — clients send
 * the desired pax mix and the service reconciles via the unique
 * (quote_id, nation_category_id, age_category_id) constraint.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpsertQuotePaxDTO {

    @NotBlank(message = "Nation category ID is required")
    private String nationCategoryId;

    @NotBlank(message = "Age category ID is required")
    private String ageCategoryId;

    @NotNull(message = "Pax count is required")
    @Min(value = 1, message = "Pax count must be at least 1")
    private Integer count;

    private String notes;
}
