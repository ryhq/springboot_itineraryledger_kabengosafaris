package com.itineraryledger.kabengosafaris.PaxNationCategory.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.PaxNationCategory.PaxNationCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for PaxNationCategory
 *
 * Used for API responses when returning pax nation category data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaxNationCategoryDTO {

    private String id; // Obfuscated ID

    private String name;

    private PaxNationCategory.CategoryType categoryType;

    private String categoryTypeDisplayName;

    private String categoryTypeDescription;

    private String description;

    private Integer priorityFactor;

    private String priorityDisplay;

    private Boolean isActive;

    private Boolean isSystem;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
