package com.itineraryledger.kabengosafaris.Customer.DTOs;

import com.itineraryledger.kabengosafaris.Customer.Enums.CustomerSource;
import com.itineraryledger.kabengosafaris.Customer.Enums.CustomerType;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * UpdateCustomerDTO - Request DTO for updating existing customers
 *
 * All fields are optional - only provided fields will be updated.
 *
 * NOTE: Emails and phones are NOT updated through this DTO.
 * They must be updated using their respective services.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCustomerDTO {

    // ========================
    // CUSTOMER TYPE
    // ========================

    private CustomerType customerType;

    // ========================
    // PERSONAL INFORMATION
    // ========================

    @Size(max = 10, message = "Title must not exceed 10 characters")
    private String title;

    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @Size(max = 200, message = "Company name must not exceed 200 characters")
    private String companyName;

    // ========================
    // IDENTITY DOCUMENTS
    // ========================

    @Size(max = 100, message = "Nationality must not exceed 100 characters")
    private String nationality;

    @Size(max = 100, message = "Residency must not exceed 100 characters")
    private String residency;

    @Size(max = 50, message = "Passport number must not exceed 50 characters")
    private String passportNumber;

    private LocalDate passportExpiry;

    private LocalDate dateOfBirth;

    // ========================
    // ADDRESS
    // ========================

    private String address;

    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @Size(max = 100, message = "State must not exceed 100 characters")
    private String state;

    @Size(max = 100, message = "Country must not exceed 100 characters")
    private String country;

    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    private String postalCode;

    // ========================
    // PREFERENCES
    // ========================

    @Size(max = 10, message = "Preferred language must not exceed 10 characters")
    private String preferredLanguage;

    @Size(max = 10, message = "Preferred currency must not exceed 10 characters")
    private String preferredCurrency;

    // ========================
    // ACQUISITION & SOURCE
    // ========================

    private CustomerSource source;

    @Size(max = 200, message = "Referred by must not exceed 200 characters")
    private String referredBy;

    // ========================
    // SPECIAL REQUIREMENTS
    // ========================

    private String dietaryRequirements;

    private String medicalConditions;

    private String specialRequests;

    private String interests;

    // ========================
    // INTERNAL MANAGEMENT
    // ========================

    private String internalNotes;

    private Boolean isVip;

    private Boolean isBlacklisted;

    private String blacklistReason;

    private Boolean isActive;
}
