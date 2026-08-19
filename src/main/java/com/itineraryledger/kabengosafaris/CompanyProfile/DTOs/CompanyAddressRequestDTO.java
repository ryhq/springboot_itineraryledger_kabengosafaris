package com.itineraryledger.kabengosafaris.CompanyProfile.DTOs;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** Create or patch one company address. On create, at least one line or a city is required. */
@Data
public class CompanyAddressRequestDTO {

    /** OFFICE · POSTAL · BILLING · WAREHOUSE · OTHER */
    private String addressType;

    @Size(max = 100)
    private String label;

    @Size(max = 200)
    private String lineOne;

    @Size(max = 200)
    private String lineTwo;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String region;

    @Size(max = 40)
    private String postalCode;

    @Size(max = 100)
    private String country;

    private Boolean isPrimary;
    private Boolean isActive;
    private Integer displayOrder;
}
