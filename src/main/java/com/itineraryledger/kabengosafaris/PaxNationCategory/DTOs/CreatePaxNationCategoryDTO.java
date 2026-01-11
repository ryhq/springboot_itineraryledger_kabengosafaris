package com.itineraryledger.kabengosafaris.PaxNationCategory.DTOs;

import com.itineraryledger.kabengosafaris.PaxNationCategory.PaxNationCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new PaxNationCategory
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePaxNationCategoryDTO {

    private String name;

    private PaxNationCategory.CategoryType categoryType;

    private String description;

    private Integer priorityFactor;

    private Boolean isActive;
}
