package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailLabelColor;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateEmailLabelDTO {

    @Size(max = 60)
    private String name;

    private EmailLabelColor color;
}
