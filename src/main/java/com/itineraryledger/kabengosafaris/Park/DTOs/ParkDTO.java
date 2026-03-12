package com.itineraryledger.kabengosafaris.Park.DTOs;

import com.itineraryledger.kabengosafaris.Park.ParkType;
import com.itineraryledger.kabengosafaris.Public.Annotations.Translatable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ParkDTO - Data Transfer Object for Park entity
 * Contains obfuscated ID for secure data transfer
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParkDTO {
    private String id;
    @Translatable private String name;
    private String slug;
    private ParkType parkType;
    private String region;
    private String district;
    private String location;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String elevation;
    private String size;
    @Translatable private String shortDescription;
    @Translatable private String fullDescription;
    @Translatable private String history;
    @Translatable private String ecosystem;
    @Translatable private String wildlife;
    @Translatable private String vegetation;
    private String primaryImage;
    @Translatable private String bestTimeToVisit;
    private String openingHours;
    @Translatable private String accessInformation;
    @Translatable private String tags;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
