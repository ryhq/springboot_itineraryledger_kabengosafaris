package com.itineraryledger.kabengosafaris.Customer.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Customer.Enums.CustomerSource;
import com.itineraryledger.kabengosafaris.Customer.Enums.CustomerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * CustomerDTO - Response DTO for customer data
 *
 * Contains all customer information to be returned to clients.
 * IDs are encoded using IdObfuscator for security.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerDTO {

    private String id; // Encoded ID
    private String code;

    // ========================
    // CUSTOMER TYPE
    // ========================

    private CustomerType customerType;
    private String customerTypeDisplayName;
    private String customerTypeDescription;

    // ========================
    // PERSONAL/COMPANY INFORMATION
    // ========================

    private String title;
    private String salutation;
    private String firstName;
    private String lastName;
    private String companyName;
    private String displayName; // Computed: Full name for individuals, company name for others

    // ========================
    // CONTACT INFORMATION
    // ========================

    private String primaryEmail; // Primary email from emails list
    private String primaryPhone; // Primary phone from phones list

    // ========================
    // IDENTITY DOCUMENTS
    // ========================

    private String nationality;
    private String residency;
    private String passportNumber;
    private LocalDate passportExpiry;
    private LocalDate dateOfBirth;
    private Boolean passportExpiringSoon; // True if passport expires within 6 months

    // ========================
    // ADDRESS
    // ========================

    private String address;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private String fullAddress; // Computed: formatted full address

    // ========================
    // PREFERENCES
    // ========================

    private String preferredLanguage;
    private String preferredCurrency;

    // ========================
    // ACQUISITION & SOURCE
    // ========================

    private CustomerSource source;
    private String sourceDisplayName;
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

    // ========================
    // COMPUTED STATISTICS
    // ========================

    private Integer totalBookings;
    private BigDecimal totalSpent;
    private LocalDateTime lastBookingDate;
    private Boolean canBook; // Computed: isActive && !isBlacklisted

    // ========================
    // STATUS & METADATA
    // ========================

    private Boolean isActive;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ========================
    // RELATIONSHIP COUNTS
    // ========================

    private Integer emailCount;
    private Integer phoneCount;
    private Integer documentCount;
    private Integer noteCount;
}
