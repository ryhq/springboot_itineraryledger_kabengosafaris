package com.itineraryledger.kabengosafaris.User.DTOs;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MFABackupCodesResponse {
    private List<String> backupCodes;  // 10 backup codes
    private String message;
}
