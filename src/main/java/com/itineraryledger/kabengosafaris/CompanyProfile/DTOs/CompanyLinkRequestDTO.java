package com.itineraryledger.kabengosafaris.CompanyProfile.DTOs;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** Create or patch one company link. On create, `url` is required. */
@Data
public class CompanyLinkRequestDTO {

    @Size(max = 500)
    private String url;

    /** WEBSITE · BOOKING · FACEBOOK · INSTAGRAM · TRIPADVISOR · LINKEDIN · X · YOUTUBE · TIKTOK · OTHER */
    private String linkType;

    @Size(max = 100)
    private String label;

    private Boolean isPrimary;
    private Boolean isActive;
    private Integer displayOrder;
}
