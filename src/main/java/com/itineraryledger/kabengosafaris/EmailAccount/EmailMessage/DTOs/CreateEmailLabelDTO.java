package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailLabelColor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateEmailLabelDTO {

    @NotBlank
    @Size(max = 60)
    private String name;

    @NotNull
    private EmailLabelColor color;
}
