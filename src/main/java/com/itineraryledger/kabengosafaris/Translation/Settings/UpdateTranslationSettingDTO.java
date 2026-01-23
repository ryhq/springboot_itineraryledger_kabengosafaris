package com.itineraryledger.kabengosafaris.Translation.Settings;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTranslationSettingDTO {
    private String settingValue;
    private Boolean active = true;
}
