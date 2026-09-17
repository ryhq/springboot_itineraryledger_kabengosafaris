package com.itineraryledger.kabengosafaris.Flight.DTOs;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** An airstrip as the panel and the public route picker read it. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AirstripDTO {
    private String id;
    private String code;
    private String name;
    private String slug;
    private String displayName;
    private String region;
    private String district;
    private String country;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String notes;
    private Boolean isActive;
    /** How many sectors touch this strip — the figure that decides whether it can be deleted. */
    private Long routeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
