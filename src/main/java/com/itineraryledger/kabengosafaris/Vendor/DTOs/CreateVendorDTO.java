package com.itineraryledger.kabengosafaris.Vendor.DTOs;

import com.itineraryledger.kabengosafaris.Vendor.Enums.VendorType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateVendorDTO {

    @NotBlank(message = "Vendor name is required")
    @Size(max = 200)
    private String name;

    @NotNull(message = "Vendor type is required")
    private VendorType type;

    private String contactPerson;

    @Email(message = "Email must be valid")
    private String email;

    private String phone;
    private String taxId;
    private String address;
    private String city;
    private String country;

    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
    private String preferredCurrency;

    private String paymentTerms;
    private String notes;

    @Builder.Default
    private Boolean isActive = true;
}
