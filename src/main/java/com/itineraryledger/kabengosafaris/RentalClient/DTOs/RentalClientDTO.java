package com.itineraryledger.kabengosafaris.RentalClient.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.RentalClient.Enums.RentalClientType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RentalClientDTO {

    private String id;
    private RentalClientType clientType;
    private String clientTypeDisplayName;
    private String displayName;
    private String firstName;
    private String lastName;
    private String companyName;
    private String taxId;
    private String phone;
    private String email;
    private String address;
    private String notes;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String nextId;
    private String previousId;
}
