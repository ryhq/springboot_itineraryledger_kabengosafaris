package com.itineraryledger.kabengosafaris.Vendor.DTOs;

import com.itineraryledger.kabengosafaris.Vendor.Enums.VendorType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** All fields optional — null means "leave unchanged". */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateVendorDTO {

    @Size(max = 200)
    private String name;

    private VendorType type;

    private String contactPerson;

    @Email(message = "Email must be valid")
    private String email;

    private String phone;
    private String taxId;
    private String address;
    private String city;
    private String country;

    @Size(min = 3, max = 3)
    private String preferredCurrency;

    private String paymentTerms;
    private String notes;
    private Boolean isActive;
}
