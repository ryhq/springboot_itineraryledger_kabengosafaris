package com.itineraryledger.kabengosafaris.PaxNationCategory.DTOs;

import com.itineraryledger.kabengosafaris.PaxNationCategory.PaxNationCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an existing PaxNationCategory
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePaxNationCategoryDTO {

    private String name;

    /*
     * Enums arrive as Strings so a blank can CLEAR the field; null still means
     * "leave unchanged". Bound as the enum itself, an empty value makes Jackson
     * reject the whole request body, which is how a set value became impossible
     * to unset (see the charging-basis fix).
     */
    private String categoryType;

    private String description;

    private Integer priorityFactor;

    private Boolean isActive;
}
