package com.itineraryledger.kabengosafaris.Season.DTOs.SeasonDTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Season.DTOs.SeasonPeriodDTOs.SeasonPeriodDTO;
import com.itineraryledger.kabengosafaris.Season.Season;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SeasonDTO - Response DTO for Season entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SeasonDTO {

    private String id; // Obfuscated ID
    private String name;
    private Season.SeasonType seasonType;
    private String seasonTypeDisplayName;
    private String seasonTypeDescription;
    private String description;
    private Boolean isGlobal;
    private Boolean isActive;
    private Boolean isSystem; // True if created by initializer, protected from deletion

    // Accommodation details (only for accommodation-specific seasons)
    private String accommodationId;
    private String accommodationName;

    // Season periods
    private List<SeasonPeriodDTO> seasonPeriods;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
