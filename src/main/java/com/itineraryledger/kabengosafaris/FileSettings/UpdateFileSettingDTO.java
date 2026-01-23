package com.itineraryledger.kabengosafaris.FileSettings;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFileSettingDTO {
    private String settingValue;
    private Boolean active = true;
}
