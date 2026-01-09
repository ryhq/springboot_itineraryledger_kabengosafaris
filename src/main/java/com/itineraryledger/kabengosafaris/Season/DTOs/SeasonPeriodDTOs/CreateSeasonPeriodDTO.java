package com.itineraryledger.kabengosafaris.Season.DTOs.SeasonPeriodDTOs;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.itineraryledger.kabengosafaris.Config.MonthDayDeserializer;
import com.itineraryledger.kabengosafaris.Config.MonthDaySerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.MonthDay;

/**
 * CreateSeasonPeriodDTO - Request DTO for creating a new SeasonPeriod
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSeasonPeriodDTO {

    /**
     * Obfuscated Season ID
     * Required when creating a period standalone (not within season creation)
     */
    private String seasonId;

    @JsonDeserialize(using = MonthDayDeserializer.class)
    @JsonSerialize(using = MonthDaySerializer.class)
    private MonthDay startDate; // Required: Format "MM-DD" e.g., "06-01" for June 1st

    @JsonDeserialize(using = MonthDayDeserializer.class)
    @JsonSerialize(using = MonthDaySerializer.class)
    private MonthDay endDate;   // Required: Format "MM-DD" e.g., "08-31" for August 31st

    /**
     * Optional: Specific year for non-recurring periods
     * If null, the period recurs annually
     */
    private Integer year;

    private String notes;
    private Boolean isActive; // Defaults to true if not provided
}
