package com.itineraryledger.kabengosafaris.Flight.DTOs;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.itineraryledger.kabengosafaris.Flight.Enums.MarkupType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightFareDTO {
    private String id;
    private String code;
    private String flightRouteId;
    private String sectorLabel;
    private String airlineName;
    private LocalTime etd;
    private LocalTime eta;
    private String departureLabel;
    private LocalDate validFrom;
    private LocalDate validTo;
    private String operatingMonths;
    /** "June to November" — the month list read back as words. */
    private String operatingWindow;
    private BigDecimal netFare;
    private BigDecimal grossFare;
    private BigDecimal taxesAndFees;
    private String currency;
    private BigDecimal childPercent;
    private Integer minimumSeats;
    private MarkupType markupType;
    private BigDecimal markupValue;
    private String remarks;
    private LocalDateTime retiredAt;
    private Boolean isRetired;
    private Boolean isActive;
    /** What one adult actually costs a client, once the markup in force is applied. */
    private BigDecimal sellingPerAdult;
    private String pricingNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
