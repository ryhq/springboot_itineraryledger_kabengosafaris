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
    /*
     * Enums arrive as Strings so a blank can CLEAR the field; null still means
     * "leave unchanged". Bound as the enum itself, an empty value makes Jackson
     * reject the whole request body, which is how a set value became impossible
     * to unset (see the charging-basis fix).
     */
    private String categoryType;
    private Integer minAge;
    private Integer maxAge;
    private String description;
    private Boolean isActive;
}
