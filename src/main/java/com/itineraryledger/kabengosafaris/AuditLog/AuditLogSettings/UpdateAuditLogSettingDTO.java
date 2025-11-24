package com.itineraryledger.kabengosafaris.AuditLog.AuditLogSettings;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for updating audit log settings
 * Used when receiving requests to update audit log configuration
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAuditLogSettingDTO {
    private String settingValue;
    private String description;
    private Boolean active = true;
}
