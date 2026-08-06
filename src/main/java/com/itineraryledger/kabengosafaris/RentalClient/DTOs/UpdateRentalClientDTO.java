package com.itineraryledger.kabengosafaris.RentalClient.DTOs;

import com.itineraryledger.kabengosafaris.RentalClient.Enums.RentalClientType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateRentalClientDTO {

    /*
     * Enums arrive as Strings so a blank can CLEAR the field; null still means
     * "leave unchanged". Bound as the enum itself, an empty value makes Jackson
     * reject the whole request body, which is how a set value became impossible
     * to unset (see the charging-basis fix).
     */
    private String clientType;
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
