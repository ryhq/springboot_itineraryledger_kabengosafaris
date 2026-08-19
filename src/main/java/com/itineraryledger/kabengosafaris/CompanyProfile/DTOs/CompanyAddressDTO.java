package com.itineraryledger.kabengosafaris.CompanyProfile.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyAddressDTO {
    private String id;
    private String addressType;
    private String label;
    private String lineOne;
    private String lineTwo;
    private String city;
    private String region;
    private String postalCode;
    private String country;
    /** the one-line form documents print. */
    private String formatted;
    private Boolean isPrimary;
    private Boolean isActive;
    private Integer displayOrder;
}
