package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateFolderDTO {
    @NotBlank(message = "Folder name is required")
    private String name;
}
