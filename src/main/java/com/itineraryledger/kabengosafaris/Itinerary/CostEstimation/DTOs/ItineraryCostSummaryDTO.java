package com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ItineraryCostSummaryDTO {

    private String currency;
    private BigDecimal accommodationRack;
    private BigDecimal parkFeesRack;
    private BigDecimal activitiesRack;
    private BigDecimal grandTotalRack;
}
