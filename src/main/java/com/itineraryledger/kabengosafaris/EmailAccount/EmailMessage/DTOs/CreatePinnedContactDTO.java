package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreatePinnedContactDTO {

    @NotBlank
    @Email
    @Size(max = 254)
    private String email;

    @Size(max = 120)
    private String name;

    @Size(max = 120)
    private String role;
}
