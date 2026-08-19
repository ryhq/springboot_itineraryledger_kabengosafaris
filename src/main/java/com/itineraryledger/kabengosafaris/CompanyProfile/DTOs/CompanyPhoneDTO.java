package com.itineraryledger.kabengosafaris.CompanyProfile.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyPhoneDTO {
    private String id;
    private String countryCode;
    private String phoneNumber;
    /** country code + number, exactly as a document prints it. */
    private String formatted;
    private String phoneType;
    private String label;
    private Boolean isWhatsApp;
    private String operatingHours;
    private Boolean isPrimary;
    private Boolean isActive;
    private Integer displayOrder;
}
