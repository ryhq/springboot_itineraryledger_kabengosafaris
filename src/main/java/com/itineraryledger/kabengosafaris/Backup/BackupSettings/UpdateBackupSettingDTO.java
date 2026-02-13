package com.itineraryledger.kabengosafaris.Backup.BackupSettings;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for updating BackupSetting values
 * Only the settingValue can be updated; other fields are immutable
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBackupSettingDTO {

    /**
     * The new value for the setting
     */
    private String settingValue;

    /**
     * Whether the setting is active
     */
    private Boolean active;
}
