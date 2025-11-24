package com.itineraryledger.kabengosafaris.AuditLog.AuditLogSettings;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogSettings.AuditLogSetting.Category;
import com.itineraryledger.kabengosafaris.GlobalEnums.SettingDataType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)

public class AuditLogSettingDTO {
    private String id;
    private String settingKey;
    private String settingValue;
    private SettingDataType dataType;
    private String description;
    private Boolean active = true;
    private Boolean isSystemDefault = false;
    private String categoryDisplayName;
    private Category category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
