package com.itineraryledger.kabengosafaris.Flight.DTOs;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.itineraryledger.kabengosafaris.Flight.Enums.MarkupType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AirlineDTO {
    private String id;
    private String code;
    private String name;
    private String slug;
    private String website;
    private Integer baggageKg;
    private String baggageNotes;
    private MarkupType markupType;
    private BigDecimal markupValue;
    /** "13% per person" — what the panel shows without the reader doing the joining up. */
    private String markupSummary;
    private String bookingTerms;
    private String cancellationPolicy;
    private String internalNotes;
    private Boolean isActive;
    private Long routeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
