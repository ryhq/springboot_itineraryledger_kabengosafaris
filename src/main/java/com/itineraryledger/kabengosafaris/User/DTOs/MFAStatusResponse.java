package com.itineraryledger.kabengosafaris.User.DTOs;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MFAStatusResponse {
    private Boolean mfaEnabled;
    private LocalDateTime enabledAt;  // When MFA was activated
    private LocalDateTime lastVerifiedAt;  // Last successful 2FA verification
}