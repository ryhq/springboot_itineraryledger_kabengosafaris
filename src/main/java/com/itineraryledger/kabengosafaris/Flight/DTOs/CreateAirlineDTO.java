package com.itineraryledger.kabengosafaris.Flight.DTOs;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAirlineDTO {

    @NotBlank(message = "Airline name is required")
    @Size(max = 150)
    private String name;

    @Size(max = 10) private String code;
    @Size(max = 255) private String website;
    private Integer baggageKg;
    private String baggageNotes;

    /** PERCENT or AMOUNT. Both parts are needed or neither: a value with no type means nothing. */
    private String markupType;

    @DecimalMin(value = "0.0", message = "A markup cannot be negative")
    private BigDecimal markupValue;

    private String bookingTerms;
    private String cancellationPolicy;
    private String internalNotes;
    private Boolean isActive;
}
