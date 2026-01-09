package com.itineraryledger.kabengosafaris.Season.DTOs.SeasonPeriodDTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.itineraryledger.kabengosafaris.Config.MonthDayDeserializer;
import com.itineraryledger.kabengosafaris.Config.MonthDaySerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.MonthDay;

/**
 * SeasonPeriodDTO - Response DTO for SeasonPeriod entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SeasonPeriodDTO {

    private String id; // Obfuscated ID
    private String seasonId; // Obfuscated Season ID
    private String seasonName;

    @JsonDeserialize(using = MonthDayDeserializer.class)
    @JsonSerialize(using = MonthDaySerializer.class)
    private MonthDay startDate; // e.g., "06-01" for June 1st

    @JsonDeserialize(using = MonthDayDeserializer.class)
    @JsonSerialize(using = MonthDaySerializer.class)
    private MonthDay endDate;   // e.g., "08-31" for August 31st

    private Integer year;       // Optional: specific year, null for recurring

    private String notes;
    private Boolean isActive;
    private Boolean isSystem; // True if created by initializer, protected from deletion
    private Boolean isRecurring; // Computed: true if year is null
    private Boolean isYearWrapping; // Computed: true if startDate > endDate (e.g., Dec-Jan)
    private Integer durationDays; // Computed: approximate duration

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
