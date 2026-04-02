package com.itineraryledger.kabengosafaris.RentalClient.DTOs;

import com.itineraryledger.kabengosafaris.RentalClient.Enums.RentalClientType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRentalClientDTO {

    @NotNull(message = "Client type is required")
    private RentalClientType clientType;

    private String firstName;
    private String lastName;
    private String companyName;
    private String taxId;
    private String phone;
    private String email;
    private String address;
    private String notes;
    private Boolean isActive;
}
