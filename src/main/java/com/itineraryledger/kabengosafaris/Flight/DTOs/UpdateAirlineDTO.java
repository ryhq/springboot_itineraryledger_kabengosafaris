package com.itineraryledger.kabengosafaris.Flight.DTOs;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Null means "leave alone". To clear a markup send an empty markupType. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAirlineDTO {
    @Size(max = 150) private String name;
    @Size(max = 10) private String code;
    @Size(max = 255) private String website;
    private Integer baggageKg;
    private String baggageNotes;
    private String markupType;
    @DecimalMin(value = "0.0") private BigDecimal markupValue;
    private String bookingTerms;
    private String cancellationPolicy;
    private String internalNotes;
    private Boolean isActive;
}
