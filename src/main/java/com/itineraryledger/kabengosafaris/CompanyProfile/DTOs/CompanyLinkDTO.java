package com.itineraryledger.kabengosafaris.CompanyProfile.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyLinkDTO {
    private String id;
    private String url;
    /** the url without scheme or trailing slash — what a footer shows. */
    private String display;
    private String linkType;
    private String label;
    private Boolean isPrimary;
    private Boolean isActive;
    private Integer displayOrder;
}
