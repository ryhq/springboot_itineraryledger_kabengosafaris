package com.itineraryledger.kabengosafaris.User.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MFASetupResponse {
    private String qrCodeUri;  // Data URI for QR code image
    private String secret;  // Base32-encoded secret (optional, for manual entry)
    private String totpUrl;
    private String setupToken;  // Temporary token for confirming MFA
    private String message;
}
