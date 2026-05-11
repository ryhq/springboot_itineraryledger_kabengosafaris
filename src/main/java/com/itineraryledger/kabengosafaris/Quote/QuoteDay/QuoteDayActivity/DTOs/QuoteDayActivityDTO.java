package com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayActivity.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuoteDayActivityDTO {
    private String id;
    private String quoteDayId;
    private String activityId;
    private String activityName;

    private Integer sortOrder;
    private BigDecimal durationHours;
    private String startTime;
    private String endTime;
    private String notes;

    private Boolean isIncludedInPrice;
    private Boolean isOptional;

    private LocalDateTime createdAt;
}
