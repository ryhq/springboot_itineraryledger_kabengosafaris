package com.itineraryledger.kabengosafaris.Backup.BackupSettings;

import com.itineraryledger.kabengosafaris.GlobalEnums.SettingDataType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for BackupSetting
 * Used to transfer backup setting data between layers
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupSettingDTO {

    private Long id;
    private String settingKey;
    private String settingValue;
    private SettingDataType dataType;
    private String description;
    private Boolean active;
    private Boolean isSystemDefault;
    private BackupSetting.Category category;
    private Boolean requiresRestart;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
