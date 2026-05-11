package com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayAccommodation.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuoteDayAccommodationDTO {
    private String id;
    private String quoteDayId;

    private String accommodationId;
    private String accommodationName;
    private String roomTypeId;
    private String roomTypeName;
    private String roomStandardId;
    private String roomStandardName;
    private String boardTypeId;
    private String boardTypeName;

    private Integer roomCount;
    private Boolean isAlternative;
    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
