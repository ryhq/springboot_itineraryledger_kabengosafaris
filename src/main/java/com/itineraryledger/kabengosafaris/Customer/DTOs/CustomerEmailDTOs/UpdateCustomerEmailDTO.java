package com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerEmailDTOs;

import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerEmail.EmailType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UpdateCustomerEmailDTO - Request DTO for updating customer emails
 * All fields are optional - only provided fields will be updated
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCustomerEmailDTO {

    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    private EmailType emailType;

    private Boolean isPrimary;

    private Boolean isActive;

    @Size(max = 100, message = "Label must not exceed 100 characters")
    private String label;
}
