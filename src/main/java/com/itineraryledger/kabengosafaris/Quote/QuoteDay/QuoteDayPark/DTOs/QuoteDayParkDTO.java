package com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Entity.ItineraryDayPark.ParkEntryType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuoteDayParkDTO {
    private String id;
    private String quoteDayId;
    private String parkId;
    private String parkName;
    private ParkEntryType entryType;
    private String entryTypeDisplayName;
    private Integer sortOrder;
    private String arrivalTime;
    private String departureTime;
    private String notes;
    private Long parkActivityCount;
    private Long parkTariffCount;
    private LocalDateTime createdAt;
}
