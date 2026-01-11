package com.itineraryledger.kabengosafaris.PaxAgeCategory.DTOs;

import com.itineraryledger.kabengosafaris.PaxAgeCategory.PaxAgeCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CreatePaxAgeCategoryDTO - Request DTO for creating a new PaxAgeCategory
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePaxAgeCategoryDTO {

    private String name; // Required
    private PaxAgeCategory.CategoryType categoryType;
    private Integer minAge; // Required
    private Integer maxAge; // Required
    private String description;
    private Boolean isActive; // Defaults to true if not provided
}
