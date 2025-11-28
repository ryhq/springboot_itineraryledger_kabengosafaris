package com.itineraryledger.kabengosafaris.User.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MFARequiredResponse {
    private String tempToken;  // Token for MFA verification endpoint
    private String message;

    public MFARequiredResponse(String tempToken) {
        this.tempToken = tempToken;
        this.message = "MFA verification required. Please provide your MFA code via /api/mfa/verify-login endpoint.";
    }
}
