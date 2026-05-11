package com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkActivity.DTOs;

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
public class QuoteDayParkActivityDTO {
    private String id;
    private String quoteDayParkId;
    private String parkId;
    private String parkName;
    private String activityId;
    private String activityName;

    private Integer sortOrder;
    private BigDecimal durationHours;
    private String notes;
    private Boolean isIncludedInPrice;
    private String startTime;
    private String endTime;

    private LocalDateTime createdAt;
}
