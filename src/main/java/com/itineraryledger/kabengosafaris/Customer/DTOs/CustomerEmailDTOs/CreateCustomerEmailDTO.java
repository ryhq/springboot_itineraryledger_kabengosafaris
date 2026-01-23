package com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerEmailDTOs;

import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerEmail.EmailType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CreateCustomerEmailDTO - Request DTO for creating customer emails
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCustomerEmailDTO {

    @NotBlank(message = "Customer ID is required")
    private String customerId; // Encoded customer ID

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @NotNull(message = "Email type is required")
    private EmailType emailType;

    @Builder.Default
    private Boolean isPrimary = false;

    @Builder.Default
    private Boolean isActive = true;

    @Size(max = 100, message = "Label must not exceed 100 characters")
    private String label;
}
