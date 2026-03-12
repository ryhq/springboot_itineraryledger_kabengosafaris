package com.itineraryledger.kabengosafaris.NotificationSetting;

import com.itineraryledger.kabengosafaris.GlobalEnums.SettingDataType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSettingDTO {

    private Long id;
    private String settingKey;
    private String settingValue;
    private SettingDataType dataType;
    private String description;
    private Boolean active;
    private Boolean isSystemDefault;
    private NotificationSetting.Category category;
    private Boolean requiresRestart;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
