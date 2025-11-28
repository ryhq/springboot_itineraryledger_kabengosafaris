package com.itineraryledger.kabengosafaris.User.DTOs;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MFAVerifyRequest {
    @NotBlank(message = "MFA code is required")
    private String code;  // 6-digit TOTP code or backup code
}