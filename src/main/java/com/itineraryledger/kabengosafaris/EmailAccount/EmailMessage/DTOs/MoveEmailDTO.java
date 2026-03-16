package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoveEmailDTO {
    @NotBlank(message = "Target folder ID is required")
    private String targetFolderId;
}
