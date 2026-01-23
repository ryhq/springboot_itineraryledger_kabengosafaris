package com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerEmailDTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerEmail.EmailType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * CustomerEmailDTO - Response DTO for customer email data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerEmailDTO {

    private String id; // Encoded ID
    private String customerId; // Encoded customer ID
    private String customerDisplayName;
    private String email;
    private EmailType emailType;
    private String emailTypeDisplayName;
    private String emailTypeDescription;
    private Boolean isPrimary;
    private Boolean isActive;
    private String label;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
