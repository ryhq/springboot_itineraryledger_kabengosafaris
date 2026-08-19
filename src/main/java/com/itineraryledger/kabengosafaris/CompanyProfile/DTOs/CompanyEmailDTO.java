package com.itineraryledger.kabengosafaris.CompanyProfile.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyEmailDTO {
    private String id;
    private String email;
    private String emailType;
    private String label;
    private Boolean isPrimary;
    private Boolean isActive;
    private Integer displayOrder;
}
