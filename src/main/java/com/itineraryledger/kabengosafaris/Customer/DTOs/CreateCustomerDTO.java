package com.itineraryledger.kabengosafaris.Customer.DTOs;

import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerEmail;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerPhone;
import com.itineraryledger.kabengosafaris.Customer.Enums.CustomerSource;
import com.itineraryledger.kabengosafaris.Customer.Enums.CustomerType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * CreateCustomerDTO - Request DTO for creating new customers
 *
 * Validates all required fields for customer creation:
 * - Customer type (INDIVIDUAL, CORPORATE, TRAVEL_AGENT)
 * - Personal/company information
 * - At least one email or phone is required
 * - Address and preferences
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCustomerDTO {

    // ========================
    // CUSTOMER TYPE
    // ========================

    @NotNull(message = "Customer type is required")
    private CustomerType customerType;

    // ========================
    // PERSONAL INFORMATION
    // ========================

    @Size(max = 10, message = "Title must not exceed 10 characters")
    private String title; // Mr, Mrs, Ms, Dr, etc.

    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName; // Required for INDIVIDUAL

    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName; // Required for INDIVIDUAL

    @Size(max = 200, message = "Company name must not exceed 200 characters")
    private String companyName; // Required for CORPORATE/TRAVEL_AGENT

    // ========================
    // CONTACT INFORMATION
    // ========================

    /**
     * List of emails to create for this customer.
     * At least one email or phone is required.
     */
    @Valid
    private List<CustomerEmailDTO> emails;

    /**
     * List of phones to create for this customer.
     * At least one email or phone is required.
     */
    @Valid
    private List<CustomerPhoneDTO> phones;

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
    @Builder.Default
    private String preferredLanguage = "en";

    @Size(max = 10, message = "Preferred currency must not exceed 10 characters")
    @Builder.Default
    private String preferredCurrency = "USD";

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

    private String interests; // JSON array

    // ========================
    // INTERNAL MANAGEMENT
    // ========================

    private String internalNotes;

    @Builder.Default
    private Boolean isVip = false;

    @Builder.Default
    private Boolean isActive = true;

    // ========================
    // NESTED DTOS
    // ========================

    /**
     * DTO for creating customer emails
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CustomerEmailDTO {

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        private String email;

        private CustomerEmail.EmailType emailType;

        @Builder.Default
        private Boolean isPrimary = false;

        @Size(max = 100, message = "Label must not exceed 100 characters")
        private String label;
    }

    /**
     * DTO for creating customer phones
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CustomerPhoneDTO {

        @NotBlank(message = "Phone number is required")
        @Size(max = 50, message = "Phone number must not exceed 50 characters")
        private String phoneNumber;

        @Size(max = 10, message = "Country code must not exceed 10 characters")
        private String countryCode;

        private CustomerPhone.PhoneType phoneType;

        @Builder.Default
        private Boolean isPrimary = false;

        @Builder.Default
        private Boolean isWhatsApp = false;

        @Size(max = 100, message = "Label must not exceed 100 characters")
        private String label;
    }
}
