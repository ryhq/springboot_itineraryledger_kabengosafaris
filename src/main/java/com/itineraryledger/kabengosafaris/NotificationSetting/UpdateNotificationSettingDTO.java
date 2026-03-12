package com.itineraryledger.kabengosafaris.NotificationSetting;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateNotificationSettingDTO {

    private String settingValue;
    private Boolean active;
}
