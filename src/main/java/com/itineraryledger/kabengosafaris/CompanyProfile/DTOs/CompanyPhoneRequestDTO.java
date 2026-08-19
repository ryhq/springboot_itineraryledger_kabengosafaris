package com.itineraryledger.kabengosafaris.CompanyProfile.DTOs;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** Create or patch one company phone. On create, `phoneNumber` is required. */
@Data
public class CompanyPhoneRequestDTO {

    @Size(max = 10)
    private String countryCode;

    @Size(max = 50)
    private String phoneNumber;

    /** MOBILE · LANDLINE · WHATSAPP · RECEPTION · RESERVATIONS · EMERGENCY · FAX · TOLL_FREE · OTHER */
    private String phoneType;

    @Size(max = 100)
    private String label;

    private Boolean isWhatsApp;

    @Size(max = 200)
    private String operatingHours;

    private Boolean isPrimary;
    private Boolean isActive;
    private Integer displayOrder;
}
