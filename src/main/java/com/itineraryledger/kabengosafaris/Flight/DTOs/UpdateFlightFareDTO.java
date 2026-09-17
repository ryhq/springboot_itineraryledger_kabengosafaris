package com.itineraryledger.kabengosafaris.Flight.DTOs;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Null means "leave alone". Send an empty markupType to clear a fare's override. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFlightFareDTO {
    @JsonFormat(pattern = "HH:mm") private LocalTime etd;
    @JsonFormat(pattern = "HH:mm") private LocalTime eta;
    private String departureLabel;
    private LocalDate validFrom;
    private LocalDate validTo;
    private String operatingMonths;
    private BigDecimal netFare;
    private BigDecimal grossFare;
    private BigDecimal taxesAndFees;
    private String currency;
    private BigDecimal childPercent;
    private Integer minimumSeats;
    private String markupType;
    private BigDecimal markupValue;
    private String remarks;
    private Boolean isActive;
    /** True retires this fare, false brings it back. Retiring never deletes. */
    private Boolean isRetired;
}
