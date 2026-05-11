package com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayActivity.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateQuoteDayActivityDTO {
    private String activityId;
    private Integer sortOrder;
    private BigDecimal durationHours;
    private String startTime;
    private String endTime;
    private String notes;
    private Boolean isIncludedInPrice;
    private Boolean isOptional;
}
