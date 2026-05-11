package com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayAccommodation.DTOs;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateQuoteDayAccommodationDTO {

    @NotBlank(message = "Accommodation ID is required")
    private String accommodationId;

    @NotBlank(message = "Room Type ID is required")
    private String roomTypeId;

    @NotBlank(message = "Room Standard ID is required")
    private String roomStandardId;

    @NotBlank(message = "Board Type ID is required")
    private String boardTypeId;

    @Min(value = 1, message = "Room count must be at least 1")
    private Integer roomCount;

    private Boolean isAlternative;
    private String notes;
}
