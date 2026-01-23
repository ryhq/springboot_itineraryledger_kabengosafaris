package com.itineraryledger.kabengosafaris.ImageSettings;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateImageSettingDTO {
    private String settingValue;
    private Boolean active = true;
}
