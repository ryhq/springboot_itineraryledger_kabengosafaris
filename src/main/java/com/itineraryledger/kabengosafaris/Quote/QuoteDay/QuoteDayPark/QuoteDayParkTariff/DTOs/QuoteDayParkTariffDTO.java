package com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkTariff.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuoteDayParkTariffDTO {
    private String id;
    private String quoteDayParkId;
    private String parkId;
    private String parkName;
    private String tariffId;
    private String tariffName;
    private String notes;
    private Boolean isIncludedInPrice;
    private LocalDateTime createdAt;
}
