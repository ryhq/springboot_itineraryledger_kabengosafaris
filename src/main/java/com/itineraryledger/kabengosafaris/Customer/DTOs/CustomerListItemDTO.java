package com.itineraryledger.kabengosafaris.Customer.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Customer.Enums.CustomerSource;
import com.itineraryledger.kabengosafaris.Customer.Enums.CustomerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * CustomerListItemDTO - Lightweight DTO for customer list views
 *
 * Contains essential customer information for lists/tables.
 * Does not include full notes, documents, or detailed information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerListItemDTO {

    private String id; // Encoded ID
    private String code;
    private CustomerType customerType;
    private String customerTypeDisplayName;
    private String displayName; // Full name or company name
    private String primaryEmail;
    private String primaryPhone;
    private String nationality;
    private String country;
    private CustomerSource source;
    private String sourceDisplayName;
    private Boolean isVip;
    private Boolean isBlacklisted;
    private Boolean isActive;
    private Boolean canBook;
    private Integer totalBookings;
    private BigDecimal totalSpent;
    private LocalDateTime lastBookingDate;
    private LocalDateTime createdAt;
}
