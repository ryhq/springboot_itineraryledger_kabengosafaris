package com.itineraryledger.kabengosafaris.Security.SecuritySettings;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSecuritySettingDTO {
    private String settingValue;
    private String description;
    private Boolean active = true;
}
