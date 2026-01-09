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
 * UpdateSeasonPeriodDTO - Request DTO for updating a SeasonPeriod
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSeasonPeriodDTO {

    @JsonDeserialize(using = MonthDayDeserializer.class)
    @JsonSerialize(using = MonthDaySerializer.class)
    private MonthDay startDate; // Format "MM-DD" e.g., "06-01"

    @JsonDeserialize(using = MonthDayDeserializer.class)
    @JsonSerialize(using = MonthDaySerializer.class)
    private MonthDay endDate;   // Format "MM-DD" e.g., "08-31"

    private Integer year;
    private String notes;
    private Boolean isActive;

    /**
     * Note: seasonId cannot be changed after creation
     * If you need to move a period to a different season, delete and recreate
     */
}
