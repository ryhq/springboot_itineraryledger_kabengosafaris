package com.itineraryledger.kabengosafaris.Safari.SafariPax.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * SafariPaxDTO - Data Transfer Object for SafariPax entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SafariPaxDTO {
    private String id;
    private String safariId;

    // Category information
    private String nationCategoryId;
    private String nationCategoryName;
    private String ageCategoryId;
    private String ageCategoryName;

    // Counts
    private Integer count;
    private Integer actualCount;
    private Integer effectiveCount;
    private Boolean hasCountVariance;

    // Safari-specific
    private LocalDateTime confirmedAt;
    private String specialRequirements;
    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
