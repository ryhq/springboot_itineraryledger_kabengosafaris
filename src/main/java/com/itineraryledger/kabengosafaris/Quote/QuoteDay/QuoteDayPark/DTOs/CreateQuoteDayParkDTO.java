package com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.DTOs;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Entity.ItineraryDayPark.ParkEntryType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateQuoteDayParkDTO {

    @NotBlank(message = "Park ID is required")
    private String parkId;

    private ParkEntryType entryType;
    private Integer sortOrder;
    private String arrivalTime;
    private String departureTime;
    private String notes;
}
