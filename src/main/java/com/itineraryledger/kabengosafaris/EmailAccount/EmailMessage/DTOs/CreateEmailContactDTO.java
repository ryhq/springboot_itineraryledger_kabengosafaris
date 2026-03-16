package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateEmailContactDTO {

    @NotBlank(message = "Email address is required")
    @Email(message = "Must be a valid email address")
    private String emailAddress;

    private String displayName;

    private Boolean isStarred;
}
