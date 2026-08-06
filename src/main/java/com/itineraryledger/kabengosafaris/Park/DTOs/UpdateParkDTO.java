package com.itineraryledger.kabengosafaris.Park.DTOs;

import com.itineraryledger.kabengosafaris.Park.ParkType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * UpdateParkDTO - Data Transfer Object for updating an existing Park
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateParkDTO {
    private String name;
    private String slug;
    /*
     * Enums arrive as Strings so a blank can CLEAR the field; null still means
     * "leave unchanged". Bound as the enum itself, an empty value makes Jackson
     * reject the whole request body, which is how a set value became impossible
     * to unset (see the charging-basis fix).
     */
    private String parkType;
    private String region;
    private String district;
    private String location;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String elevation;
    private String size;
    private String shortDescription;
    private String fullDescription;
    private String history;
    private String ecosystem;
    private String wildlife;
    private String vegetation;
    private String primaryImage;
    private String bestTimeToVisit;
    private String openingHours;
    private String accessInformation;
    private String tags;
    private Boolean isActive;
    private Boolean isWebActive;
}
