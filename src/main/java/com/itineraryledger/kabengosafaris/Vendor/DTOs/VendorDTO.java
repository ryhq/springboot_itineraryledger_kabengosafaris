package com.itineraryledger.kabengosafaris.Vendor.DTOs;

import com.itineraryledger.kabengosafaris.Vendor.Enums.VendorType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorDTO {

    private String id;                  // obfuscated
    private String code;
    private String name;
    private VendorType type;
    private String typeDisplayName;

    private String contactPerson;
    private String email;
    private String phone;
    private String taxId;
    private String address;
    private String city;
    private String country;
    private String preferredCurrency;
    private String paymentTerms;
    private String notes;
    private Boolean isActive;

    // audit
    private String createdById;
    private String createdByName;
    private String updatedById;
    private String updatedByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
