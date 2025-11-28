package com.itineraryledger.kabengosafaris.User.DTOs;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class  MFAVerifyResponse {
    private Boolean verified;
    private String message;
    private List<String> backupCodes;  // Only on successful first-time verify
}