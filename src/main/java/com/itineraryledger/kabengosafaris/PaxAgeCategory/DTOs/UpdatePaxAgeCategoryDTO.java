package com.itineraryledger.kabengosafaris.PaxAgeCategory.DTOs;

import com.itineraryledger.kabengosafaris.PaxAgeCategory.PaxAgeCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UpdatePaxAgeCategoryDTO - Request DTO for updating a PaxAgeCategory
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePaxAgeCategoryDTO {

    private String name;
    private PaxAgeCategory.CategoryType categoryType;
    private Integer minAge;
    private Integer maxAge;
    private String description;
    private Boolean isActive;
}
