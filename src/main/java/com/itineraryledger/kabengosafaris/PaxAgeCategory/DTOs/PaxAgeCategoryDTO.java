package com.itineraryledger.kabengosafaris.PaxAgeCategory.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.PaxAgeCategory.PaxAgeCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * PaxAgeCategoryDTO - Response DTO for PaxAgeCategory entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaxAgeCategoryDTO {

    private String id; // Obfuscated ID
    private String name;
    private PaxAgeCategory.CategoryType categoryType;
    private String categoryTypeDisplayName;
    private String categoryTypeDescription;
    private Integer minAge;
    private Integer maxAge;
    private String ageRangeDisplay; // e.g., "0-5 years", "17+ years"
    private String description;
    private Boolean isActive;
    private Boolean isSystem; // True if created by initializer, protected from deletion

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
