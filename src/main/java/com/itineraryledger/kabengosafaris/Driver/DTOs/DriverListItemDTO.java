package com.itineraryledger.kabengosafaris.Driver.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Driver.Enums.DriverStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DriverListItemDTO {

    private String id;
    private String fullName;
    private String phone;
    private String licenseNumber;
    private DriverStatus status;
    private String statusDisplayName;
    private Boolean isActive;
}
