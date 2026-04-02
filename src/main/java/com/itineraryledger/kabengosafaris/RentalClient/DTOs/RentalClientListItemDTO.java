package com.itineraryledger.kabengosafaris.RentalClient.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.RentalClient.Enums.RentalClientType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RentalClientListItemDTO {

    private String id;
    private String displayName;
    private RentalClientType clientType;
    private String clientTypeDisplayName;
    private String phone;
    private String email;
    private Boolean isActive;
}
