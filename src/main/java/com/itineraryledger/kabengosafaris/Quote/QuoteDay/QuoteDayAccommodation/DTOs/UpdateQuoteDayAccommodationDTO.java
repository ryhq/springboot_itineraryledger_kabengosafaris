package com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayAccommodation.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateQuoteDayAccommodationDTO {
    private String accommodationId;
    private String roomTypeId;
    private String roomStandardId;
    private String boardTypeId;
    private Integer roomCount;
    private Boolean isAlternative;
    private String notes;
}
